package fr.rp.economy.gui;

import fr.rp.economy.EconomyRP;
import fr.rp.economy.models.Quete;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WalletGui {

    private final EconomyRP plugin;
    // Mémoire de la page actuelle par joueur
    private final Map<java.util.UUID, Integer> pages = new java.util.HashMap<>();
    private final Map<java.util.UUID, String> filtres = new java.util.HashMap<>(); // any / overworld / nether / end

    private static final int SLOTS_PAR_PAGE = 21; // 0-20 contenu, 21-26 navigation

    public WalletGui(EconomyRP plugin) {
        this.plugin = plugin;
    }

    public void ouvrirPorteMonnaie(Player joueur) {
        Inventory inv = Bukkit.createInventory(null, 27, ChatColor.DARK_GREEN + "💰 Porte-monnaie");

        double solde = plugin.getEconomyManager().getSolde(joueur.getUniqueId());

        ItemStack argent = new ItemStack(Material.GOLD_INGOT);
        ItemMeta meta = argent.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Votre solde");
        meta.setLore(List.of(ChatColor.YELLOW + plugin.getEconomyManager().formater(solde)));
        argent.setItemMeta(meta);
        inv.setItem(13, argent);

        inv.setItem(11, creerItem(Material.EMERALD, ChatColor.GREEN + "Classement (/baltop)",
                List.of(ChatColor.GRAY + "Voir les joueurs les plus riches")));
        inv.setItem(15, creerItem(Material.CHEST, ChatColor.AQUA + "Quetes (/quests)",
                List.of(ChatColor.GRAY + "Voir les quetes disponibles")));

        joueur.openInventory(inv);
    }

    public void ouvrirQuetes(Player joueur) {
        ouvrirQuetes(joueur, pages.getOrDefault(joueur.getUniqueId(), 0),
                filtres.getOrDefault(joueur.getUniqueId(), "any"));
    }

    public void ouvrirQuetes(Player joueur, int page, String filtreDim) {
        pages.put(joueur.getUniqueId(), page);
        filtres.put(joueur.getUniqueId(), filtreDim);

        Inventory inv = Bukkit.createInventory(null, 54, ChatColor.DARK_AQUA + "📜 Tableau des quetes");

        // --- Quête globale en haut (slots 0-8) ---
        afficherQueteGlobale(inv, joueur);

        // --- Filtres dimension (slot 9-12) ---
        inv.setItem(9, creerItem(Material.GRASS_BLOCK, ChatColor.GREEN + "Overworld",
                List.of(ChatColor.GRAY + "Filtrer les quêtes Overworld",
                        filtreDim.equals("overworld") ? ChatColor.GREEN + "✔ Actif" : ChatColor.DARK_GRAY + "Clique")));
        inv.setItem(10, creerItem(Material.NETHERRACK, ChatColor.RED + "Nether",
                List.of(ChatColor.GRAY + "Filtrer les quêtes Nether",
                        filtreDim.equals("nether") ? ChatColor.GREEN + "✔ Actif" : ChatColor.DARK_GRAY + "Clique")));
        inv.setItem(11, creerItem(Material.END_STONE, ChatColor.LIGHT_PURPLE + "End",
                List.of(ChatColor.GRAY + "Filtrer les quêtes End",
                        filtreDim.equals("end") ? ChatColor.GREEN + "✔ Actif" : ChatColor.DARK_GRAY + "Clique")));
        inv.setItem(12, creerItem(Material.COMPASS, ChatColor.WHITE + "Toutes",
                List.of(ChatColor.GRAY + "Afficher toutes les quêtes",
                        filtreDim.equals("any") ? ChatColor.GREEN + "✔ Actif" : ChatColor.DARK_GRAY + "Clique")));

        // --- Liste des quêtes (slots 18-38 = 21 slots) ---
        List<Quete> liste = new ArrayList<>();
        for (Quete q : plugin.getQuestManager().getQuetes().values()) {
            if (filtreDim.equals("any") || q.getDimension().equals("any") || q.getDimension().equals(filtreDim)) {
                liste.add(q);
            }
        }

        int totalPages = Math.max(1, (int) Math.ceil(liste.size() / (double) SLOTS_PAR_PAGE));
        if (page >= totalPages) page = totalPages - 1;
        if (page < 0) page = 0;
        pages.put(joueur.getUniqueId(), page);

        int start = page * SLOTS_PAR_PAGE;
        int end = Math.min(start + SLOTS_PAR_PAGE, liste.size());

        int slot = 18;
        for (int i = start; i < end; i++) {
            Quete quete = liste.get(i);
            inv.setItem(slot, creerItemQuete(joueur, quete));
            slot++;
        }

        // --- Navigation ---
        if (page > 0) {
            inv.setItem(45, creerItem(Material.ARROW, ChatColor.YELLOW + "← Page précédente",
                    List.of(ChatColor.GRAY + "Page " + page + "/" + totalPages)));
        }
        inv.setItem(49, creerItem(Material.PAPER, ChatColor.AQUA + "Page " + (page + 1) + "/" + totalPages,
               List.of(ChatColor.GRAY + String.valueOf(liste.size()) + " quêtes")
        if (page < totalPages - 1) {
            inv.setItem(53, creerItem(Material.ARROW, ChatColor.YELLOW + "Page suivante →",
                    List.of(ChatColor.GRAY + "Page " + (page + 2) + "/" + totalPages)));
        }

        joueur.openInventory(inv);
    }

    private void afficherQueteGlobale(Inventory inv, Player joueur) {
        var qm = plugin.getQuestManager();
        Quete base = qm.getQueteGlobaleBase();

        if (base == null) {
            inv.setItem(4, creerItem(Material.BARRIER, ChatColor.RED + "Pas de quête globale",
                    List.of(ChatColor.GRAY + "Revenez plus tard")));
            return;
        }

        boolean dejaFait = qm.aDejaCompleteGlobale(joueur.getUniqueId());
        int quantite = qm.getQuantiteGlobaleActuelle();
        double recompense = qm.getRecompenseGlobaleActuelle();
        int possede = qm.compterMateriau(joueur, safeMaterial(base.getMateriau()));
        boolean peutValider = !dejaFait && possede >= quantite;

        Material icone = dejaFait ? Material.EMERALD_BLOCK :
                (peutValider ? Material.NETHER_STAR : Material.CLOCK);

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GOLD + "⚡ QUÊTE GLOBALE (12h)");
        lore.add(ChatColor.GRAY + base.getDescription());
        lore.add("");
        lore.add(ChatColor.GRAY + "Niveau : " + ChatColor.YELLOW + qm.getCompletionsGlobales() +
                ChatColor.DARK_GRAY + " (plus = plus dur)");
        lore.add(ChatColor.GRAY + "Requis : " + (peutValider ? ChatColor.GREEN : ChatColor.RED) +
                possede + "/" + quantite + " " + base.getMateriau());
        lore.add(ChatColor.GRAY + "Récompense : " + ChatColor.GOLD + plugin.getEconomyManager().formater(recompense));
        lore.add(ChatColor.GRAY + "Temps restant : " + ChatColor.AQUA + qm.getTempsRestantFormate());
        lore.add("");
        if (dejaFait) {
            lore.add(ChatColor.GREEN + "✔ Déjà validée ce cycle");
        } else if (peutValider) {
            lore.add(ChatColor.GREEN + "» Clique pour valider !");
        } else {
            lore.add(ChatColor.RED + "Il te manque des objets");
        }
        lore.add(ChatColor.DARK_GRAY + "Chaque validation rend la quête");
        lore.add(ChatColor.DARK_GRAY + "plus difficile et plus payante.");

        inv.setItem(4, creerItem(icone, (peutValider ? ChatColor.GREEN : ChatColor.YELLOW) + "⚡ " + base.getNom(), lore));
    }

    private ItemStack creerItemQuete(Player joueur, Quete quete) {
        Material icone = switch (quete.getDifficulte().toLowerCase()) {
            case "legendaire" -> Material.NETHER_STAR;
            case "tres_difficile", "très_difficile" -> Material.NETHERITE_INGOT;
            case "difficile" -> Material.DIAMOND;
            case "moyen" -> Material.IRON_INGOT;
            default -> Material.COPPER_INGOT;
        };

        int possede = plugin.getQuestManager().compterMateriau(joueur, safeMaterial(quete.getMateriau()));
        boolean complete = possede >= quete.getQuantite();

        String dimTag = switch (quete.getDimension()) {
            case "nether" -> ChatColor.RED + "[Nether] ";
            case "end" -> ChatColor.LIGHT_PURPLE + "[End] ";
            case "overworld" -> ChatColor.GREEN + "[OW] ";
            default -> "";
        };

        List<String> lore = List.of(
                ChatColor.GRAY + quete.getDescription(),
                ChatColor.GRAY + "Difficulte: " + ChatColor.WHITE + quete.getDifficulte(),
                ChatColor.GRAY + "Dimension: " + ChatColor.WHITE + quete.getDimension(),
                ChatColor.GRAY + "Recompense: " + ChatColor.GOLD + plugin.getEconomyManager().formater(quete.getRecompense()),
                ChatColor.GRAY + "Progression: " + (complete ? ChatColor.GREEN : ChatColor.RED) +
                        possede + "/" + quete.getQuantite(),
                "",
                complete ? ChatColor.GREEN + "Clique pour valider !" : ChatColor.RED + "Il te manque des objets"
        );

        return creerItem(icone, (complete ? ChatColor.GREEN : ChatColor.YELLOW) + dimTag + quete.getNom(), lore);
    }

    public int getPage(Player joueur) {
        return pages.getOrDefault(joueur.getUniqueId(), 0);
    }

    public String getFiltre(Player joueur) {
        return filtres.getOrDefault(joueur.getUniqueId(), "any");
    }

    private Material safeMaterial(String nom) {
        try {
            return Material.valueOf(nom);
        } catch (IllegalArgumentException e) {
            return Material.DIRT;
        }
    }

    private ItemStack creerItem(Material material, String nom, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(nom);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
