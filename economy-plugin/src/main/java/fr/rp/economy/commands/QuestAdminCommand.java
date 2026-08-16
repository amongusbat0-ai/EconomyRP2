package fr.rp.economy.commands;

import fr.rp.economy.EconomyRP;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class QuestAdminCommand implements CommandExecutor, TabCompleter {

    private final EconomyRP plugin;

    public QuestAdminCommand(EconomyRP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("economyrp.admin")) {
            sender.sendMessage(ChatColor.RED + "Vous n'avez pas la permission.");
            return true;
        }

        if (args.length == 0) {
            envoyerAide(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create", "creer" -> handleCreate(sender, args);
            case "delete", "supprimer" -> handleDelete(sender, args);
            case "list", "liste" -> handleList(sender);
            case "reload" -> {
                plugin.reloadConfig();
                plugin.getQuestManager().charger();
                sender.sendMessage(ChatColor.GREEN + "✔ Quêtes rechargées.");
            }
            case "global", "globale" -> handleGlobal(sender, args);
            default -> envoyerAide(sender);
        }
        return true;
    }

    private void envoyerAide(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== /questadmin ===");
        sender.sendMessage(ChatColor.YELLOW + "/questadmin create <id> <materiau> <quantite> <recompense> <difficulte> <dimension> <nom...>");
        sender.sendMessage(ChatColor.GRAY + "  Ex: /questadmin create diamant_nether DIAMOND 8 400 difficile nether Chasseur de diamants du Nether");
        sender.sendMessage(ChatColor.YELLOW + "/questadmin delete <id>");
        sender.sendMessage(ChatColor.YELLOW + "/questadmin list");
        sender.sendMessage(ChatColor.YELLOW + "/questadmin reload");
        sender.sendMessage(ChatColor.YELLOW + "/questadmin global new  " + ChatColor.GRAY + "→ force une nouvelle quête globale");
        sender.sendMessage(ChatColor.YELLOW + "/questadmin global info " + ChatColor.GRAY + "→ état de la quête globale");
    }

    private void handleCreate(CommandSender sender, String[] args) {
        // create <id> <materiau> <quantite> <recompense> <difficulte> <dimension> <nom...>
        if (args.length < 8) {
            sender.sendMessage(ChatColor.RED + "Usage: /questadmin create <id> <materiau> <quantite> <recompense> <difficulte> <dimension> <nom...>");
            sender.sendMessage(ChatColor.GRAY + "Difficultés: facile, moyen, difficile, tres_difficile, legendaire");
            sender.sendMessage(ChatColor.GRAY + "Dimensions: overworld, nether, end, any");
            return;
        }

        String id = args[1].toLowerCase().replace(" ", "_");
        String materiau = args[2].toUpperCase();
        int quantite;
        double recompense;
        try {
            quantite = Integer.parseInt(args[3]);
            recompense = Double.parseDouble(args[4]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Quantité ou récompense invalide.");
            return;
        }
        if (quantite <= 0 || recompense <= 0) {
            sender.sendMessage(ChatColor.RED + "Quantité et récompense doivent être positives.");
            return;
        }

        String difficulte = args[5].toLowerCase();
        String dimension = args[6].toLowerCase();
        if (!List.of("overworld", "nether", "end", "any").contains(dimension)) {
            sender.sendMessage(ChatColor.RED + "Dimension invalide (overworld, nether, end, any).");
            return;
        }

        String nom = String.join(" ", Arrays.copyOfRange(args, 7, args.length));
        String description = "Rendez " + quantite + " " + materiau.toLowerCase().replace("_", " ");

        try {
            Material.valueOf(materiau);
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ChatColor.RED + "Matériau Minecraft invalide : " + materiau);
            return;
        }

        boolean ok = plugin.getQuestManager().creerQueteAdmin(id, nom, description, difficulte, recompense, materiau, quantite, dimension);
        if (ok) {
            sender.sendMessage(ChatColor.GREEN + "✔ Quête serveur créée : " + ChatColor.YELLOW + nom +
                    ChatColor.GRAY + " (id: " + id + ")");
            sender.sendMessage(ChatColor.GRAY + "Dimension: " + dimension + " | Diff: " + difficulte +
                    " | Récompense: " + plugin.getEconomyManager().formater(recompense));
        } else {
            sender.sendMessage(ChatColor.RED + "Impossible de créer la quête (id déjà existant ou matériau invalide).");
        }
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /questadmin delete <id>");
            return;
        }
        String id = args[1].toLowerCase();
        if (plugin.getQuestManager().supprimerQueteAdmin(id)) {
            sender.sendMessage(ChatColor.GREEN + "✔ Quête '" + id + "' supprimée.");
        } else {
            sender.sendMessage(ChatColor.RED + "Aucune quête avec l'id '" + id + "'.");
        }
    }

    private void handleList(CommandSender sender) {
        var quetes = plugin.getQuestManager().getQuetes();
        sender.sendMessage(ChatColor.GOLD + "=== Quêtes serveur (" + quetes.size() + ") ===");
        for (var q : quetes.values()) {
            sender.sendMessage(ChatColor.YELLOW + q.getId() + ChatColor.GRAY + " → " +
                    ChatColor.WHITE + q.getNom() + ChatColor.DARK_GRAY + " [" + q.getDimension() + "] " +
                    ChatColor.GOLD + plugin.getEconomyManager().formater(q.getRecompense()));
        }
    }

    private void handleGlobal(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /questadmin global <new|info>");
            return;
        }
        switch (args[1].toLowerCase()) {
            case "new", "nouvelle" -> {
                plugin.getQuestManager().nouvelleQueteGlobale();
                sender.sendMessage(ChatColor.GREEN + "✔ Nouvelle quête globale générée et annoncée.");
            }
            case "info" -> {
                var q = plugin.getQuestManager().getQueteGlobaleBase();
                if (q == null) {
                    sender.sendMessage(ChatColor.RED + "Aucune quête globale active.");
                    return;
                }
                sender.sendMessage(ChatColor.GOLD + "=== Quête Globale ===");
                sender.sendMessage(ChatColor.WHITE + q.getNom());
                sender.sendMessage(ChatColor.GRAY + q.getDescription());
                sender.sendMessage(ChatColor.GRAY + "Niveau actuel : " + ChatColor.YELLOW + plugin.getQuestManager().getCompletionsGlobales());
                sender.sendMessage(ChatColor.GRAY + "Quantité requise : " + ChatColor.YELLOW + plugin.getQuestManager().getQuantiteGlobaleActuelle() + " " + q.getMateriau());
                sender.sendMessage(ChatColor.GRAY + "Récompense : " + ChatColor.GOLD + plugin.getEconomyManager().formater(plugin.getQuestManager().getRecompenseGlobaleActuelle()));
                sender.sendMessage(ChatColor.GRAY + "Temps restant : " + ChatColor.AQUA + plugin.getQuestManager().getTempsRestantFormate());
            }
            default -> sender.sendMessage(ChatColor.RED + "Usage: /questadmin global <new|info>");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("economyrp.admin")) return List.of();
        if (args.length == 1) {
            return filter(List.of("create", "delete", "list", "reload", "global"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("global")) {
            return filter(List.of("new", "info"), args[1]);
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("delete") || args[0].equalsIgnoreCase("supprimer"))) {
            return filter(new ArrayList<>(plugin.getQuestManager().getQuetes().keySet()), args[1]);
        }
        if (args.length == 3 && (args[0].equalsIgnoreCase("create") || args[0].equalsIgnoreCase("creer"))) {
            return filter(Arrays.stream(Material.values())
                    .filter(m -> m.isItem() && !m.isAir())
                    .map(Enum::name)
                    .collect(Collectors.toList()), args[2]);
        }
        if (args.length == 6 && (args[0].equalsIgnoreCase("create") || args[0].equalsIgnoreCase("creer"))) {
            return filter(List.of("facile", "moyen", "difficile", "tres_difficile", "legendaire"), args[5]);
        }
        if (args.length == 7 && (args[0].equalsIgnoreCase("create") || args[0].equalsIgnoreCase("creer"))) {
            return filter(List.of("overworld", "nether", "end", "any"), args[6]);
        }
        return List.of();
    }

    private List<String> filter(List<String> options, String input) {
        String lower = input.toLowerCase();
        return options.stream().filter(s -> s.toLowerCase().startsWith(lower)).collect(Collectors.toList());
    }
}
