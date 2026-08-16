package fr.rp.economy.managers;

import fr.rp.economy.EconomyRP;

import java.io.File;
import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class DatabaseManager {

    private final EconomyRP plugin;
    private Connection connection;

    public DatabaseManager(EconomyRP plugin) {
        this.plugin = plugin;
    }

    public void connecter() {
        try {
            File dbFile = new File(plugin.getDataFolder(), "economyrp.db");
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            creerTables();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Impossible de se connecter a la base de donnees", e);
        }
    }

    private void creerTables() throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.execute("""
                CREATE TABLE IF NOT EXISTS comptes (
                    uuid TEXT PRIMARY KEY,
                    pseudo TEXT NOT NULL,
                    solde REAL NOT NULL DEFAULT 0
                )
            """);
            st.execute("""
                CREATE TABLE IF NOT EXISTS quetes_joueurs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    quete_id TEXT NOT NULL,
                    joueur_uuid TEXT NOT NULL,
                    progression INTEGER NOT NULL DEFAULT 0,
                    terminee INTEGER NOT NULL DEFAULT 0
                )
            """);
            st.execute("""
                CREATE TABLE IF NOT EXISTS quetes_proposees (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    createur_uuid TEXT NOT NULL,
                    createur_pseudo TEXT NOT NULL,
                    cible_uuid TEXT NOT NULL,
                    description TEXT NOT NULL,
                    recompense REAL NOT NULL,
                    statut TEXT NOT NULL DEFAULT 'EN_ATTENTE'
                )
            """);
        }
    }

    public void fermer() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Erreur fermeture base de donnees", e);
        }
    }

    public void assurerCompte(UUID uuid, String pseudo, double soldeDepart) {
        String sql = "INSERT INTO comptes (uuid, pseudo, solde) VALUES (?, ?, ?) " +
                "ON CONFLICT(uuid) DO UPDATE SET pseudo = excluded.pseudo";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, pseudo);
            ps.setDouble(3, soldeDepart);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Erreur assurerCompte", e);
        }
    }

    public double getSolde(UUID uuid) {
        String sql = "SELECT solde FROM comptes WHERE uuid = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getDouble("solde");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Erreur getSolde", e);
        }
        return 0;
    }

    public void setSolde(UUID uuid, double montant) {
        String sql = "UPDATE comptes SET solde = ? WHERE uuid = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setDouble(1, Math.max(0, montant));
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Erreur setSolde", e);
        }
    }

    public void ajouterSolde(UUID uuid, double montant) {
        setSolde(uuid, getSolde(uuid) + montant);
    }

    public boolean retirerSolde(UUID uuid, double montant) {
        double actuel = getSolde(uuid);
        if (actuel < montant) return false;
        setSolde(uuid, actuel - montant);
        return true;
    }

    /** Retourne un top classe par solde decroissant, cle = pseudo, valeur = solde */
    public Map<String, Double> getTop(int limite) {
        Map<String, Double> top = new LinkedHashMap<>();
        String sql = "SELECT pseudo, solde FROM comptes ORDER BY solde DESC LIMIT ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, limite);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) top.put(rs.getString("pseudo"), rs.getDouble("solde"));
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Erreur getTop", e);
        }
        return top;
    }

    // ---------- Quetes fixes ----------

    public int getProgression(UUID uuid, String queteId) {
        String sql = "SELECT progression FROM quetes_joueurs WHERE joueur_uuid = ? AND quete_id = ? AND terminee = 0";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, queteId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("progression");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Erreur getProgression", e);
        }
        return 0;
    }

    public void setProgression(UUID uuid, String queteId, int valeur) {
        String select = "SELECT id FROM quetes_joueurs WHERE joueur_uuid = ? AND quete_id = ? AND terminee = 0";
        try (PreparedStatement ps = connection.prepareStatement(select)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, queteId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String update = "UPDATE quetes_joueurs SET progression = ? WHERE id = ?";
                try (PreparedStatement ups = connection.prepareStatement(update)) {
                    ups.setInt(1, valeur);
                    ups.setInt(2, rs.getInt("id"));
                    ups.executeUpdate();
                }
            } else {
                String insert = "INSERT INTO quetes_joueurs (quete_id, joueur_uuid, progression, terminee) VALUES (?, ?, ?, 0)";
                try (PreparedStatement ins = connection.prepareStatement(insert)) {
                    ins.setString(1, queteId);
                    ins.setString(2, uuid.toString());
                    ins.setInt(3, valeur);
                    ins.executeUpdate();
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Erreur setProgression", e);
        }
    }

    public void marquerQueteTerminee(UUID uuid, String queteId) {
        String sql = "UPDATE quetes_joueurs SET terminee = 1 WHERE joueur_uuid = ? AND quete_id = ? AND terminee = 0";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, queteId);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Erreur marquerQueteTerminee", e);
        }
    }

    // ---------- Quetes proposees entre joueurs ----------

    public void proposerQuete(UUID createur, String pseudoCreateur, UUID cible, String description, double recompense) {
        String sql = "INSERT INTO quetes_proposees (createur_uuid, createur_pseudo, cible_uuid, description, recompense, statut) " +
                "VALUES (?, ?, ?, ?, ?, 'EN_ATTENTE')";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, createur.toString());
            ps.setString(2, pseudoCreateur);
            ps.setString(3, cible.toString());
            ps.setString(4, description);
            ps.setDouble(5, recompense);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Erreur proposerQuete", e);
        }
    }

    public java.util.List<QueteProposee> getQuetesEnAttente(UUID cible) {
        java.util.List<QueteProposee> liste = new java.util.ArrayList<>();
        String sql = "SELECT * FROM quetes_proposees WHERE cible_uuid = ? AND statut = 'EN_ATTENTE'";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, cible.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                liste.add(new QueteProposee(
                        rs.getInt("id"),
                        UUID.fromString(rs.getString("createur_uuid")),
                        rs.getString("createur_pseudo"),
                        UUID.fromString(rs.getString("cible_uuid")),
                        rs.getString("description"),
                        rs.getDouble("recompense")
                ));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Erreur getQuetesEnAttente", e);
        }
        return liste;
    }

    public void changerStatutQuete(int id, String statut) {
        String sql = "UPDATE quetes_proposees SET statut = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, statut);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Erreur changerStatutQuete", e);
        }
    }

    public QueteProposee getQueteAcceptee(UUID createur, UUID cible) {
        String sql = "SELECT * FROM quetes_proposees WHERE createur_uuid = ? AND cible_uuid = ? AND statut = 'ACCEPTEE' LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, createur.toString());
            ps.setString(2, cible.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new QueteProposee(
                        rs.getInt("id"),
                        UUID.fromString(rs.getString("createur_uuid")),
                        rs.getString("createur_pseudo"),
                        UUID.fromString(rs.getString("cible_uuid")),
                        rs.getString("description"),
                        rs.getDouble("recompense")
                );
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Erreur getQueteAcceptee", e);
        }
        return null;
    }

    public record QueteProposee(int id, UUID createur, String pseudoCreateur, UUID cible, String description, double recompense) {}
}
