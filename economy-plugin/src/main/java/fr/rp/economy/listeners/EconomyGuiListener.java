package fr.rp.economy.listeners;

import fr.rp.economy.EconomyRP;
import fr.rp.economy.managers.TradeManager;
import fr.rp.economy.models.Quete;
import fr.rp.economy.models.TradeSession;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

public class EconomyGuiListener implements Listener {

    private final EconomyRP plugin;

    public EconomyGuiListener(EconomyRP plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        String titre = event.getView().getTitle();

        if (titre.contains("Porte-monnaie")) {
            event.setCancelled(true);
            gererClicPorteMonnaie(event);
            return;
        }

        if (titre.contains("Tableau des quetes")) {
            event.setCancelled(true);
            gererClicQuete(event);
            return;
        }

        if (titre.contains("🔄 Echange")) {
            gererClicEchange(event);
        }
    }

    private void gererClicPorteMonnaie(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player joueur)) return;
        ItemStack item = event.getCurrentItem();
        if (item == null) return;

        if (item.getType() == Material.EMERALD) {
            joueur.performCommand("baltop");
            joueur.closeInventory();
        } else if (item.getType() == Material.CHEST) {
            plugin.getWalletGui().ouvrirQuetes(joueur);
        }
    }

    private void gererClicQuete(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player joueur)) return;
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getItemMeta() == null) return;

        int slot = event.getRawSlot();
        String nomAffiche = ChatColor.stripColor(item.getItemMeta().getDisplayName());

        // --- Navigation ---
        if (slot == 45) { // page précédente
            int page = plugin.getWalletGui().getPage(joueur);
            plugin.getWalletGui().ouvrirQuetes(joueur, page - 1, plugin.getWalletGui().getFiltre(joueur));
            return;
        }
        if (slot == 53) { // page suivante
            int page = plugin.getWalletGui().getPage(joueur);
            plugin.getWalletGui().ouvrirQuetes(joueur, page + 1, plugin.getWalletGui().getFiltre(joueur));
            return;
        }

        // --- Filtres dimension ---
        if (slot == 9) {
            plugin.getWalletGui().ouvrirQuetes(joueur, 0, "overworld");
            return;
        }
        if (slot == 10) {
            plugin.getWalletGui().ouvrirQuetes(joueur, 0, "nether");
            return;
        }
        if (slot == 11) {
            plugin.getWalletGui().ouvrirQuetes(joueur, 0, "end");
            return;
        }
        if (slot == 12) {
            plugin.getWalletGui().ouvrirQuetes(joueur, 0, "any");
            return;
        }

        // --- Quête globale (slot 4) ---
        if (slot == 4 && nomAffiche.contains("⚡")) {
            if (plugin.getQuestManager().aDejaCompleteGlobale(joueur.getUniqueId())) {
                joueur.sendMessage(ChatColor.RED + "Vous avez déjà validé la quête globale de ce cycle.");
                return;
            }
            double recompenseAvant = plugin.getQuestManager().getRecompenseGlobaleActuelle();
            boolean succes = plugin.getQuestManager().validerQueteGlobale(joueur);
            if (succes) {
                joueur.sendMessage(ChatColor.GREEN + "✔ Quête globale validée ! Vous recevez " +
                        plugin.getEconomyManager().formater(recompenseAvant) + ".");
                joueur.closeInventory();
            } else {
                joueur.sendMessage(ChatColor.RED + "Il te manque des objets pour valider la quête globale.");
            }
            return;
        }

        // --- Quêtes fixes ---
        // Le nom peut contenir le tag [Nether] etc.
        String nomClean = nomAffiche
                .replace("[Nether] ", "")
                .replace("[End] ", "")
                .replace("[OW] ", "")
                .replace("⚡ ", "")
                .trim();

        Quete queteCliquee = plugin.getQuestManager().getQuetes().values().stream()
                .filter(q -> q.getNom().equals(nomClean) || q.getNom().equals(nomAffiche))
                .findFirst().orElse(null);

        if (queteCliquee == null) return;

        boolean succes = plugin.getQuestManager().validerQuete(joueur, queteCliquee);
        if (succes) {
            joueur.sendMessage(ChatColor.GREEN + "✔ Quete '" + queteCliquee.getNom() + "' validee ! Vous recevez " +
                    plugin.getEconomyManager().formater(queteCliquee.getRecompense()) + ".");
            joueur.closeInventory();
        } else {
            joueur.sendMessage(ChatColor.RED + "Il te manque des objets pour valider cette quete.");
        }
    }

    private void gererClicEchange(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player joueur)) return;
        TradeManager tradeManager = plugin.getTradeManager();
        TradeSession session = tradeManager.getSession(joueur.getUniqueId());
        if (session == null) {
            event.setCancelled(true);
            return;
        }

        boolean estA = joueur.getUniqueId().equals(session.getJoueurA());
        int slot = event.getRawSlot();

        if (slot >= 9 && slot <= 17) {
            event.setCancelled(true);
            if (slot == TradeManager.SLOT_CONFIRMER_A && estA) {
                tradeManager.confirmer(session, joueur.getUniqueId());
            } else if (slot == TradeManager.SLOT_CONFIRMER_B && !estA) {
                tradeManager.confirmer(session, joueur.getUniqueId());
            }
            return;
        }

        if (slot >= TradeManager.TAILLE) return;

        boolean zoneA = slot <= 8;
        boolean zoneB = slot >= 19 && slot <= 25;

        if ((estA && zoneB) || (!estA && zoneA)) {
            event.setCancelled(true);
            return;
        }

        session.resetConfirmations();
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        String titre = event.getView().getTitle();
        if (!titre.contains("🔄 Echange")) return;
        if (!(event.getPlayer() instanceof Player joueur)) return;

        TradeSession session = plugin.getTradeManager().getSession(joueur.getUniqueId());
        if (session != null && !session.isConfirmeA() && !session.isConfirmeB()) {
            plugin.getTradeManager().annuler(session);
        }
    }
}
