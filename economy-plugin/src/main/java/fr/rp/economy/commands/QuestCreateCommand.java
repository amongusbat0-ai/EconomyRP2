package fr.rp.economy.commands;

import fr.rp.economy.EconomyRP;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class QuestCreateCommand implements CommandExecutor {

    private final EconomyRP plugin;

    public QuestCreateCommand(EconomyRP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player createur)) {
            sender.sendMessage(ChatColor.RED + "Cette commande est reservee aux joueurs.");
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /questcreate <joueur> <recompense> <description>");
            return true;
        }

        Player cible = Bukkit.getPlayer(args[0]);
        if (cible == null) {
            createur.sendMessage(ChatColor.RED + "Ce joueur n'est pas en ligne.");
            return true;
        }
        if (cible.getUniqueId().equals(createur.getUniqueId())) {
            createur.sendMessage(ChatColor.RED + "Vous ne pouvez pas vous proposer une quete a vous-meme.");
            return true;
        }

        double recompense;
        try {
            recompense = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            createur.sendMessage(ChatColor.RED + "Le montant de la recompense est invalide.");
            return true;
        }
        if (recompense <= 0) {
            createur.sendMessage(ChatColor.RED + "La recompense doit etre positive.");
            return true;
        }
        if (plugin.getEconomyManager().getSolde(createur.getUniqueId()) < recompense) {
            createur.sendMessage(ChatColor.RED + "Vous n'avez pas assez d'argent pour proposer cette recompense.");
            return true;
        }

        String description = String.join(" ", List.of(args).subList(2, args.length));

        // L'argent est immediatement bloque (retire) chez le createur, rendu si refusee/annulee
        plugin.getEconomyManager().retirer(createur.getUniqueId(), recompense);
        plugin.getDatabaseManager().proposerQuete(createur.getUniqueId(), createur.getName(), cible.getUniqueId(), description, recompense);

        createur.sendMessage(ChatColor.GREEN + "✔ Quete proposee a " + cible.getName() + " (" +
                plugin.getEconomyManager().formater(recompense) + " bloques en attendant sa reponse).");
        cible.sendMessage(ChatColor.GOLD + "📜 " + createur.getName() + " vous propose une quete : " + ChatColor.WHITE + description);
        cible.sendMessage(ChatColor.GOLD + "Recompense : " + ChatColor.YELLOW + plugin.getEconomyManager().formater(recompense));
        cible.sendMessage(ChatColor.GRAY + "Tapez /questaccept " + createur.getName() + " pour accepter, ou /questrefuse " + createur.getName() + " pour refuser.");
        return true;
    }
}
