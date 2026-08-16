package fr.rp.economy.managers;

import fr.rp.economy.EconomyRP;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.UUID;

public class EconomyManager {

    private final EconomyRP plugin;

    public EconomyManager(EconomyRP plugin) {
        this.plugin = plugin;
    }

    public double getSolde(UUID uuid) {
        return plugin.getDatabaseManager().getSolde(uuid);
    }

    public String formater(double montant) {
        String symbole = plugin.getConfig().getString("symbole-monnaie", " pieces");
        return String.format("%,.2f%s", montant, symbole);
    }

    public void donner(UUID uuid, double montant) {
        plugin.getDatabaseManager().ajouterSolde(uuid, montant);
    }

    public boolean retirer(UUID uuid, double montant) {
        return plugin.getDatabaseManager().retirerSolde(uuid, montant);
    }

    public void definir(UUID uuid, double montant) {
        plugin.getDatabaseManager().setSolde(uuid, montant);
    }

    /** Tente un transfert de joueur a joueur. Retourne true si reussi. */
    public boolean payer(Player expediteur, OfflinePlayer destinataire, double montant) {
        if (montant <= 0) return false;
        if (!retirer(expediteur.getUniqueId(), montant)) return false;
        donner(destinataire.getUniqueId(), montant);

        expediteur.sendMessage(ChatColor.GREEN + "Vous avez envoye " + formater(montant) + " a " + destinataire.getName() + ".");
        Player enLigne = destinataire.getPlayer();
        if (enLigne != null) {
            enLigne.sendMessage(ChatColor.GREEN + "Vous avez recu " + formater(montant) + " de la part de " + expediteur.getName() + ".");
        }

        double seuil = plugin.getConfig().getDouble("seuil-broadcast", 1000.0);
        if (plugin.getConfig().getBoolean("broadcast-gros-paiements", true) && montant >= seuil) {
            Bukkit.broadcastMessage(ChatColor.GOLD + "💰 " + expediteur.getName() + " a envoye " +
                    formater(montant) + " a " + destinataire.getName() + " !");
        }
        return true;
    }
}
