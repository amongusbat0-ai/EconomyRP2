package fr.rp.economy.managers;

import fr.rp.economy.EconomyRP;
import fr.rp.economy.models.Quete;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class QuestManager {

    private final EconomyRP plugin;
    private final Map<String, Quete> quetes = new LinkedHashMap<>();
    private final List<Quete> poolGlobale = new ArrayList<>();

    // État de la quête globale (12h)
    private Quete queteGlobaleActuelle;
    private int completionsGlobales = 0;
    private long debutCycle = 0L;
    private final Set<UUID> joueursAyantComplete = new HashSet<>();
    private BukkitTask tacheRotation;

    // Multiplicateurs de progression
    private static final double MULT_QUANTITE = 0.18;   // +18% quantite par completion
    private static final double MULT_RECOMPENSE = 0.22; // +22% recompense par completion
    private static final long DUREE_CYCLE_MS = 12L * 60 * 60 * 1000; // 12 heures

    public QuestManager(EconomyRP plugin) {
        this.plugin = plugin;
    }

    public void charger() {
        quetes.clear();
        poolGlobale.clear();

        ConfigurationSection section = plugin.getConfig().getConfigurationSection("quetes");
        if (section != null) {
            for (String id : section.getKeys(false)) {
                ConfigurationSection q = section.getConfigurationSection(id);
                if (q == null) continue;
                Quete quete = new Quete(
                        id,
                        q.getString("nom", id),
                        q.getString("description", ""),
                        q.getString("difficulte", "facile"),
                        q.getDouble("recompense", 0),
                        q.getString("materiau", "DIRT"),
                        q.getInt("quantite", 1),
                        q.getString("dimension", "any")
                );
                quetes.put(id, quete);
            }
        }

        // Pool pour la quête globale
        ConfigurationSection poolSec = plugin.getConfig().getConfigurationSection("quete-globale.pool");
        if (poolSec != null) {
            for (String id : poolSec.getKeys(false)) {
                ConfigurationSection q = poolSec.getConfigurationSection(id);
                if (q == null) continue;
                poolGlobale.add(new Quete(
                        "global_" + id,
                        q.getString("nom", id),
                        q.getString("description", ""),
                        q.getString("difficulte", "moyen"),
                        q.getDouble("recompense", 150),
                        q.getString("materiau", "IRON_INGOT"),
                        q.getInt("quantite", 16),
                        q.getString("dimension", "any")
                ));
            }
        }

        // Si pas de pool, on en génère à partir des quêtes moyennes/difficiles
        if (poolGlobale.isEmpty()) {
            for (Quete q : quetes.values()) {
                String d = q.getDifficulte().toLowerCase();
                if (d.contains("moyen") || d.contains("difficile")) {
                    poolGlobale.add(q);
                }
            }
        }

        chargerEtatGlobal();
        demarrerRotation();
    }

    private void chargerEtatGlobal() {
        debutCycle = plugin.getConfig().getLong("quete-globale.etat.debut-cycle", 0L);
        completionsGlobales = plugin.getConfig().getInt("quete-globale.etat.completions", 0);
        String idActuel = plugin.getConfig().getString("quete-globale.etat.id", null);

        joueursAyantComplete.clear();
        List<String> uuids = plugin.getConfig().getStringList("quete-globale.etat.completes");
        for (String s : uuids) {
            try {
                joueursAyantComplete.add(UUID.fromString(s));
            } catch (IllegalArgumentException ignored) {}
        }

        if (idActuel != null && !poolGlobale.isEmpty()) {
            queteGlobaleActuelle = poolGlobale.stream()
                    .filter(q -> q.getId().equals(idActuel) || q.getId().equals("global_" + idActuel))
                    .findFirst()
                    .orElse(null);
        }

        // Si cycle expiré ou pas de quête → nouvelle
        if (queteGlobaleActuelle == null || System.currentTimeMillis() - debutCycle >= DUREE_CYCLE_MS) {
            nouvelleQueteGlobale();
        }
    }

    private void sauvegarderEtatGlobal() {
        if (queteGlobaleActuelle != null) {
            plugin.getConfig().set("quete-globale.etat.id", queteGlobaleActuelle.getId());
        }
        plugin.getConfig().set("quete-globale.etat.debut-cycle", debutCycle);
        plugin.getConfig().set("quete-globale.etat.completions", completionsGlobales);
        List<String> uuids = new ArrayList<>();
        for (UUID u : joueursAyantComplete) {
            uuids.add(u.toString());
        }
        plugin.getConfig().set("quete-globale.etat.completes", uuids);
        plugin.saveConfig();
    }

    public void nouvelleQueteGlobale() {
        if (poolGlobale.isEmpty()) {
            queteGlobaleActuelle = null;
            return;
        }
        queteGlobaleActuelle = poolGlobale.get(ThreadLocalRandom.current().nextInt(poolGlobale.size()));
        completionsGlobales = 0;
        joueursAyantComplete.clear();
        debutCycle = System.currentTimeMillis();
        sauvegarderEtatGlobal();

        Bukkit.broadcastMessage(ChatColor.GOLD + "══════════════════════════════════");
        Bukkit.broadcastMessage(ChatColor.YELLOW + "  📜 NOUVELLE QUÊTE GLOBALE (12h)");
        Bukkit.broadcastMessage(ChatColor.WHITE + "  " + queteGlobaleActuelle.getNom());
        Bukkit.broadcastMessage(ChatColor.GRAY + "  " + queteGlobaleActuelle.getDescription());
        Bukkit.broadcastMessage(ChatColor.GRAY + "  Récompense de base : " + ChatColor.GOLD +
                plugin.getEconomyManager().formater(queteGlobaleActuelle.getRecompense()));
        Bukkit.broadcastMessage(ChatColor.DARK_GRAY + "  Plus de joueurs la font → plus elle est dure et payante !");
        Bukkit.broadcastMessage(ChatColor.GOLD + "══════════════════════════════════");
    }

    private void demarrerRotation() {
        if (tacheRotation != null) tacheRotation.cancel();
        // Vérifie toutes les 5 minutes
        tacheRotation = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (System.currentTimeMillis() - debutCycle >= DUREE_CYCLE_MS) {
                nouvelleQueteGlobale();
            }
        }, 20L * 60 * 5, 20L * 60 * 5);
    }

    public void arreter() {
        if (tacheRotation != null) tacheRotation.cancel();
        sauvegarderEtatGlobal();
    }

    // ---------- Getters quêtes fixes ----------

    public Map<String, Quete> getQuetes() {
        return quetes;
    }

    public Quete getQuete(String id) {
        return quetes.get(id);
    }

    public List<Quete> getQuetesParDimension(String dimension) {
        List<Quete> liste = new ArrayList<>();
        for (Quete q : quetes.values()) {
            if (q.getDimension().equals("any") || q.getDimension().equalsIgnoreCase(dimension)) {
                liste.add(q);
            }
        }
        return liste;
    }

    // ---------- Quête globale progressive ----------

    public Quete getQueteGlobaleBase() {
        return queteGlobaleActuelle;
    }

    /** Quantité actuelle (augmente avec les completions) */
    public int getQuantiteGlobaleActuelle() {
        if (queteGlobaleActuelle == null) return 0;
        return Math.max(1, (int) Math.ceil(queteGlobaleActuelle.getQuantite() * (1.0 + MULT_QUANTITE * completionsGlobales)));
    }

    /** Récompense actuelle (augmente avec les completions) */
    public double getRecompenseGlobaleActuelle() {
        if (queteGlobaleActuelle == null) return 0;
        return Math.round(queteGlobaleActuelle.getRecompense() * (1.0 + MULT_RECOMPENSE * completionsGlobales) * 10.0) / 10.0;
    }

    public int getCompletionsGlobales() {
        return completionsGlobales;
    }

    public long getTempsRestantCycleMs() {
        long reste = DUREE_CYCLE_MS - (System.currentTimeMillis() - debutCycle);
        return Math.max(0, reste);
    }

    public String getTempsRestantFormate() {
        long ms = getTempsRestantCycleMs();
        long heures = ms / (1000 * 60 * 60);
        long minutes = (ms / (1000 * 60)) % 60;
        return heures + "h " + minutes + "min";
    }

    public boolean aDejaCompleteGlobale(UUID uuid) {
        return joueursAyantComplete.contains(uuid);
    }

    public boolean validerQueteGlobale(Player joueur) {
        if (queteGlobaleActuelle == null) return false;
        if (joueursAyantComplete.contains(joueur.getUniqueId())) return false;

        Material material;
        try {
            material = Material.valueOf(queteGlobaleActuelle.getMateriau());
        } catch (IllegalArgumentException e) {
            return false;
        }

        int quantiteRequise = getQuantiteGlobaleActuelle();
        double recompense = getRecompenseGlobaleActuelle();

        int possede = compterMateriau(joueur, material);
        if (possede < quantiteRequise) return false;

        retirerMateriau(joueur, material, quantiteRequise);
        plugin.getEconomyManager().donner(joueur.getUniqueId(), recompense);

        joueursAyantComplete.add(joueur.getUniqueId());
        completionsGlobales++;
        sauvegarderEtatGlobal();

        Bukkit.broadcastMessage(ChatColor.AQUA + "⚡ " + joueur.getName() + ChatColor.GRAY +
                " a validé la quête globale ! (niveau " + completionsGlobales +
                ") → Prochaine validation plus dure et plus payante.");

        return true;
    }

    // ---------- Admin : créer une quête serveur ----------

    public boolean creerQueteAdmin(String id, String nom, String description, String difficulte,
                                   double recompense, String materiau, int quantite, String dimension) {
        if (quetes.containsKey(id)) return false;
        try {
            Material.valueOf(materiau.toUpperCase());
        } catch (IllegalArgumentException e) {
            return false;
        }

        String mat = materiau.toUpperCase();
        Quete nouvelle = new Quete(id, nom, description, difficulte, recompense, mat, quantite, dimension);
        quetes.put(id, nouvelle);

        // Sauvegarde dans config
        String path = "quetes." + id;
        plugin.getConfig().set(path + ".nom", nom);
        plugin.getConfig().set(path + ".description", description);
        plugin.getConfig().set(path + ".difficulte", difficulte);
        plugin.getConfig().set(path + ".recompense", recompense);
        plugin.getConfig().set(path + ".materiau", mat);
        plugin.getConfig().set(path + ".quantite", quantite);
        plugin.getConfig().set(path + ".dimension", dimension);
        plugin.saveConfig();
        return true;
    }

    public boolean supprimerQueteAdmin(String id) {
        if (!quetes.containsKey(id)) return false;
        quetes.remove(id);
        plugin.getConfig().set("quetes." + id, null);
        plugin.saveConfig();
        return true;
    }

    // ---------- Utilitaires inventaire ----------

    public int compterMateriau(Player joueur, Material material) {
        int total = 0;
        for (ItemStack item : joueur.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                total += item.getAmount();
            }
        }
        return total;
    }

    public void retirerMateriau(Player joueur, Material material, int quantite) {
        int restant = quantite;
        ItemStack[] contenu = joueur.getInventory().getContents();
        for (int i = 0; i < contenu.length && restant > 0; i++) {
            ItemStack item = contenu[i];
            if (item != null && item.getType() == material) {
                int aRetirer = Math.min(restant, item.getAmount());
                item.setAmount(item.getAmount() - aRetirer);
                restant -= aRetirer;
                if (item.getAmount() <= 0) {
                    joueur.getInventory().setItem(i, null);
                } else {
                    joueur.getInventory().setItem(i, item);
                }
            }
        }
    }

    /** Valide une quête fixe classique */
    public boolean validerQuete(Player joueur, Quete quete) {
        Material material;
        try {
            material = Material.valueOf(quete.getMateriau());
        } catch (IllegalArgumentException e) {
            return false;
        }
        int possede = compterMateriau(joueur, material);
        if (possede < quete.getQuantite()) return false;

        retirerMateriau(joueur, material, quete.getQuantite());
        plugin.getEconomyManager().donner(joueur.getUniqueId(), quete.getRecompense());
        return true;
    }
}
