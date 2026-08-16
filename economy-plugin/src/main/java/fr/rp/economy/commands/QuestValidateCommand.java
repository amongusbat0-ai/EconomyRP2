package fr.rp.economy.commands;

import fr.rp.economy.EconomyRP;
import fr.rp.economy.managers.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class QuestValidateCommand implements CommandExecutor {

    private final EconomyRP plugin;

    public QuestValidateCommand(EconomyRP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player createur)) {
            sender.sendMessage(ChatColor.RED + "Cette commande est reservee aux joueurs.");
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /questvalider <joueur>");
            return true;
        }

        Player accepteur = Bukkit.getPlayer(args[0]);
        if (accepteur == null) {
            createur.sendMessage(ChatColor.RED + "Ce joueur n'est pas en ligne.");
            return true;
        }

        List<DatabaseManager.QueteProposee> quetes = plugin.getDatabaseManager().getQuetesEnAttente(accepteur.getUniqueId());
        // getQuetesEnAttente ne renvoie que le statut EN_ATTENTE ; on cherche via une methode dediee pour ACCEPTEE
        DatabaseManager.QueteProposee quete = plugin.getDatabaseManager().getQueteAcceptee(createur.getUniqueId(), accepteur.getUniqueId());

        if (quete == null) {
            createur.sendMessage(ChatColor.RED + "Aucune quete acceptee en cours avec ce joueur.");
            return true;
        }

        plugin.getDatabaseManager().changerStatutQuete(quete.id(), "TERMINEE");
        plugin.getEconomyManager().donner(accepteur.getUniqueId(), quete.recompense());

        createur.sendMessage(ChatColor.GREEN + "✔ Quete validee ! " + accepteur.getName() + " a recu " +
                plugin.getEconomyManager().formater(quete.recompense()) + ".");
        accepteur.sendMessage(ChatColor.GREEN + "✔ " + createur.getName() + " a valide votre quete ! Vous recevez " +
                plugin.getEconomyManager().formater(quete.recompense()) + ".");
        return true;
    }
}
