package fr.rp.economy.commands;

import fr.rp.economy.EconomyRP;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class EcoAdminCommand implements CommandExecutor {

    private final EconomyRP plugin;

    public EcoAdminCommand(EconomyRP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("economyrp.admin")) {
            sender.sendMessage(ChatColor.RED + "Vous n'avez pas la permission d'utiliser cette commande.");
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /eco <give|take|set> <joueur> <montant>");
            return true;
        }

        OfflinePlayer cible = Bukkit.getOfflinePlayer(args[1]);
        double montant;
        try {
            montant = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Montant invalide.");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "give" -> {
                plugin.getEconomyManager().donner(cible.getUniqueId(), montant);
                sender.sendMessage(ChatColor.GREEN + "✔ " + plugin.getEconomyManager().formater(montant) +
                        " ajoutes a " + cible.getName() + ".");
            }
            case "take" -> {
                plugin.getEconomyManager().retirer(cible.getUniqueId(), montant);
                sender.sendMessage(ChatColor.GREEN + "✔ " + plugin.getEconomyManager().formater(montant) +
                        " retires a " + cible.getName() + ".");
            }
            case "set" -> {
                plugin.getEconomyManager().definir(cible.getUniqueId(), montant);
                sender.sendMessage(ChatColor.GREEN + "✔ Solde de " + cible.getName() + " defini a " +
                        plugin.getEconomyManager().formater(montant) + ".");
            }
            default -> sender.sendMessage(ChatColor.RED + "Usage: /eco <give|take|set> <joueur> <montant>");
        }
        return true;
    }
}
