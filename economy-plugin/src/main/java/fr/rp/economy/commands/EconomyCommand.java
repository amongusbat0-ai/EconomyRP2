package fr.rp.economy.commands;

import fr.rp.economy.EconomyRP;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public class EconomyCommand implements CommandExecutor {

    private final EconomyRP plugin;

    public EconomyCommand(EconomyRP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        switch (label.toLowerCase()) {
            case "balance", "bal", "argent", "money" -> handleBalance(sender, args);
            case "pay" -> handlePay(sender, args);
            case "baltop" -> handleBaltop(sender);
        }
        return true;
    }

    private void handleBalance(CommandSender sender, String[] args) {
        OfflinePlayer cible;
        if (args.length >= 1) {
            cible = Bukkit.getOfflinePlayer(args[0]);
        } else if (sender instanceof Player joueur) {
            cible = joueur;
        } else {
            sender.sendMessage(ChatColor.RED + "Usage: /balance <joueur>");
            return;
        }
        double solde = plugin.getEconomyManager().getSolde(cible.getUniqueId());
        sender.sendMessage(ChatColor.GOLD + "Solde de " + cible.getName() + " : " + ChatColor.YELLOW +
                plugin.getEconomyManager().formater(solde));
    }

    private void handlePay(CommandSender sender, String[] args) {
        if (!(sender instanceof Player expediteur)) {
            sender.sendMessage(ChatColor.RED + "Seuls les joueurs peuvent utiliser cette commande.");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /pay <joueur> <montant>");
            return;
        }
        OfflinePlayer destinataire = Bukkit.getOfflinePlayer(args[0]);
        if (destinataire.getUniqueId().equals(expediteur.getUniqueId())) {
            expediteur.sendMessage(ChatColor.RED + "Vous ne pouvez pas vous payer vous-meme.");
            return;
        }
        double montant;
        try {
            montant = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            expediteur.sendMessage(ChatColor.RED + "Montant invalide.");
            return;
        }
        if (montant <= 0) {
            expediteur.sendMessage(ChatColor.RED + "Le montant doit etre positif.");
            return;
        }
        boolean succes = plugin.getEconomyManager().payer(expediteur, destinataire, montant);
        if (!succes) {
            expediteur.sendMessage(ChatColor.RED + "Solde insuffisant.");
        }
    }

    private void handleBaltop(CommandSender sender) {
        Map<String, Double> top = plugin.getDatabaseManager().getTop(10);
        sender.sendMessage(ChatColor.GOLD + "=== Top 10 des joueurs les plus riches ===");
        int rang = 1;
        for (Map.Entry<String, Double> entree : top.entrySet()) {
            sender.sendMessage(ChatColor.YELLOW + "#" + rang + " " + ChatColor.WHITE + entree.getKey() +
                    ChatColor.GRAY + " - " + ChatColor.GOLD + plugin.getEconomyManager().formater(entree.getValue()));
            rang++;
        }
    }
}
