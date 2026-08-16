package fr.rp.economy.managers;

import fr.rp.economy.EconomyRP;
import fr.rp.economy.models.TradeSession;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TradeManager {

    private final EconomyRP plugin;
    /** Invitations en attente : cible -> auteur de la demande */
    private final Map<UUID, UUID> invitations = new HashMap<>();
    /** Sessions actives : joueur -> session (les deux joueurs pointent vers la meme session) */
    private final Map<UUID, TradeSession> sessions = new HashMap<>();

    public static final int TAILLE = 27;
    public static final int SLOT_CONFIRMER_A = 18;
    public static final int SLOT_CONFIRMER_B = 26;
    public static final int SLOT_STATUT_A = 9;
    public static final int SLOT_STATUT_B = 17;

    public TradeManager(EconomyRP plugin) {
        this.plugin = plugin;
    }

    public void inviter(Player demandeur, Player cible) {
        invitations.put(cible.getUniqueId(), demandeur.getUniqueId());
        cible.sendMessage(ChatColor.GREEN + demandeur.getName() + " vous propose un echange. Tapez " +
                ChatColor.YELLOW + "/trade " + demandeur.getName() + ChatColor.GREEN + " pour accepter.");
        demandeur.sendMessage(ChatColor.GREEN + "Demande d'echange envoyee a " + cible.getName() + ".");
    }

    public boolean aUneInvitationDe(Player cible, Player demandeur) {
        UUID auteur = invitations.get(cible.getUniqueId());
        return auteur != null && auteur.equals(demandeur.getUniqueId());
    }

    public void demarrerEchange(Player joueurA, Player joueurB) {
        invitations.remove(joueurA.getUniqueId());
        invitations.remove(joueurB.getUniqueId());

        Inventory inv = Bukkit.createInventory(null, TAILLE, ChatColor.DARK_PURPLE + "🔄 Echange: " +
                joueurA.getName() + " <-> " + joueurB.getName());

        remplirBordures(inv, joueurA.getName(), joueurB.getName());

        TradeSession session = new TradeSession(joueurA.getUniqueId(), joueurB.getUniqueId(), inv);
        sessions.put(joueurA.getUniqueId(), session);
        sessions.put(joueurB.getUniqueId(), session);

        joueurA.openInventory(inv);
        joueurB.openInventory(inv);
    }

    private void remplirBordures(Inventory inv, String nomA, String nomB) {
        ItemStack separateur = creerItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 9; i <= 17; i++) inv.setItem(i, separateur);

        inv.setItem(SLOT_STATUT_A, creerItem(Material.RED_STAINED_GLASS_PANE, ChatColor.YELLOW + nomA + ": en attente"));
        inv.setItem(SLOT_STATUT_B, creerItem(Material.RED_STAINED_GLASS_PANE, ChatColor.YELLOW + nomB + ": en attente"));
        inv.setItem(SLOT_CONFIRMER_A, creerItem(Material.LIME_DYE, ChatColor.GREEN + "Confirmer l'echange"));
        inv.setItem(SLOT_CONFIRMER_B, creerItem(Material.LIME_DYE, ChatColor.GREEN + "Confirmer l'echange"));
    }

    public TradeSession getSession(UUID uuid) {
        return sessions.get(uuid);
    }

    public void confirmer(TradeSession session, UUID uuid) {
        if (uuid.equals(session.getJoueurA())) session.setConfirmeA(true);
        else session.setConfirmeB(true);

        Inventory inv = session.getInventaire();
        Player joueurA = Bukkit.getPlayer(session.getJoueurA());
        Player joueurB = Bukkit.getPlayer(session.getJoueurB());

        inv.setItem(SLOT_STATUT_A, creerItem(
                session.isConfirmeA() ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE,
                ChatColor.YELLOW + (joueurA != null ? joueurA.getName() : "?") + ": " + (session.isConfirmeA() ? "pret" : "en attente")));
        inv.setItem(SLOT_STATUT_B, creerItem(
                session.isConfirmeB() ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE,
                ChatColor.YELLOW + (joueurB != null ? joueurB.getName() : "?") + ": " + (session.isConfirmeB() ? "pret" : "en attente")));

        if (session.isConfirmeA() && session.isConfirmeB()) {
            executerEchange(session);
        }
    }

    private void executerEchange(TradeSession session) {
        Player joueurA = Bukkit.getPlayer(session.getJoueurA());
        Player joueurB = Bukkit.getPlayer(session.getJoueurB());
        Inventory inv = session.getInventaire();

        List<ItemStack> objetsA = new java.util.ArrayList<>();
        List<ItemStack> objetsB = new java.util.ArrayList<>();
        for (int i = 0; i <= 8; i++) {
            ItemStack item = inv.getItem(i);
            if (item != null) objetsA.add(item);
        }
        for (int i = 19; i <= 25; i++) {
            ItemStack item = inv.getItem(i);
            if (item != null) objetsB.add(item);
        }

        if (joueurA != null) {
            objetsB.forEach(item -> joueurA.getInventory().addItem(item));
            joueurA.sendMessage(ChatColor.GREEN + "Echange termine avec succes !");
            joueurA.closeInventory();
        }
        if (joueurB != null) {
            objetsA.forEach(item -> joueurB.getInventory().addItem(item));
            joueurB.sendMessage(ChatColor.GREEN + "Echange termine avec succes !");
            joueurB.closeInventory();
        }

        terminerSession(session);
    }

    public void annuler(TradeSession session) {
        Player joueurA = Bukkit.getPlayer(session.getJoueurA());
        Player joueurB = Bukkit.getPlayer(session.getJoueurB());
        Inventory inv = session.getInventaire();

        for (int i = 0; i <= 8; i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && joueurA != null) joueurA.getInventory().addItem(item);
        }
        for (int i = 19; i <= 25; i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && joueurB != null) joueurB.getInventory().addItem(item);
        }

        if (joueurA != null) joueurA.sendMessage(ChatColor.RED + "L'echange a ete annule.");
        if (joueurB != null) joueurB.sendMessage(ChatColor.RED + "L'echange a ete annule.");

        terminerSession(session);
    }

    private void terminerSession(TradeSession session) {
        sessions.remove(session.getJoueurA());
        sessions.remove(session.getJoueurB());
    }

    private ItemStack creerItem(Material material, String nom) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(nom);
        item.setItemMeta(meta);
        return item;
    }
}
