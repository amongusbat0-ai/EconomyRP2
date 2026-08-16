package fr.rp.economy.commands;

import fr.rp.economy.EconomyRP;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class WalletCommand implements CommandExecutor {

    private final EconomyRP plugin;

    public WalletCommand(EconomyRP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player joueur)) {
            sender.sendMessage(ChatColor.RED + "Cette commande est reservee aux joueurs.");
            return true;
        }
        if (label.equalsIgnoreCase("wallet")) {
            plugin.getWalletGui().ouvrirPorteMonnaie(joueur);
        } else if (label.equalsIgnoreCase("quests")) {
            plugin.getWalletGui().ouvrirQuetes(joueur);
        }
        return true;
    }
}
