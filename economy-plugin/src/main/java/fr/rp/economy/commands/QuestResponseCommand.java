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

public class QuestResponseCommand implements CommandExecutor {

    private final EconomyRP plugin;

    public QuestResponseCommand(EconomyRP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player joueur)) {
            sender.sendMessage(ChatColor.RED + "Cette commande est reservee aux joueurs.");
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /" + label + " <joueur>");
            return true;
        }

        List<DatabaseManager.QueteProposee> enAttente = plugin.getDatabaseManager().getQuetesEnAttente(joueur.getUniqueId());
        DatabaseManager.QueteProposee quete = enAttente.stream()
                .filter(q -> q.pseudoCreateur().equalsIgnoreCase(args[0]))
                .findFirst().orElse(null);

        if (quete == null) {
            joueur.sendMessage(ChatColor.RED + "Aucune quete en attente de la part de ce joueur.");
            return true;
        }

        if (label.equalsIgnoreCase("questaccept")) {
            plugin.getDatabaseManager().changerStatutQuete(quete.id(), "ACCEPTEE");
            joueur.sendMessage(ChatColor.GREEN + "✔ Quete acceptee ! Une fois terminee, demande a " +
                    quete.pseudoCreateur() + " de confirmer avec /questvalider.");
            Player createur = Bukkit.getPlayer(quete.createur());
            if (createur != null) {
                createur.sendMessage(ChatColor.GREEN + joueur.getName() + " a accepte votre quete !");
            }
        } else {
            plugin.getDatabaseManager().changerStatutQuete(quete.id(), "REFUSEE");
            plugin.getEconomyManager().donner(quete.createur(), quete.recompense());
            joueur.sendMessage(ChatColor.YELLOW + "Quete refusee.");
            Player createur = Bukkit.getPlayer(quete.createur());
            if (createur != null) {
                createur.sendMessage(ChatColor.YELLOW + joueur.getName() + " a refuse votre quete. Vous etes rembourse.");
            }
        }
        return true;
    }
}
