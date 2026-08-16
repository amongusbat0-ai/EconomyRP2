package fr.rp.economy.commands;

import fr.rp.economy.EconomyRP;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TradeCommand implements CommandExecutor {

    private final EconomyRP plugin;

    public TradeCommand(EconomyRP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player joueur)) {
            sender.sendMessage(ChatColor.RED + "Cette commande est reservee aux joueurs.");
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /trade <joueur>");
            return true;
        }
        Player cible = Bukkit.getPlayer(args[0]);
        if (cible == null) {
            joueur.sendMessage(ChatColor.RED + "Ce joueur n'est pas en ligne.");
            return true;
        }
        if (cible.getUniqueId().equals(joueur.getUniqueId())) {
            joueur.sendMessage(ChatColor.RED + "Vous ne pouvez pas commercer avec vous-meme.");
            return true;
        }

        if (plugin.getTradeManager().aUneInvitationDe(joueur, cible)) {
            plugin.getTradeManager().demarrerEchange(joueur, cible);
        } else {
            plugin.getTradeManager().inviter(joueur, cible);
        }
        return true;
    }
}
