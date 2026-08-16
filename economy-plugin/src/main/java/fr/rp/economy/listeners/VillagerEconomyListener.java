package fr.rp.economy.listeners;

import fr.rp.economy.EconomyRP;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.inventory.MerchantRecipe;

import java.util.List;

/**
 * Remplace l'utilisation d'emeraudes physiques dans les trades villageois/marchands
 * ambulants par le solde du joueur dans l'economie du plugin. Le slot 2 d'un
 * MerchantInventory est le slot de resultat : le cliquer (ou shift-cliquer)
 * execute normalement le trade vanilla. On annule ce comportement et on gere
 * tout manuellement.
 */
public class VillagerEconomyListener implements Listener {

    private static final int SLOT_RESULTAT = 2;

    private final EconomyRP plugin;

    public VillagerEconomyListener(EconomyRP plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClickMerchant(InventoryClickEvent event) {
        if (!(event.getClickedInventory() instanceof MerchantInventory merchantInv)) return;
        if (event.getRawSlot() != SLOT_RESULTAT) return;
        if (!(event.getWhoClicked() instanceof Player joueur)) return;

        MerchantRecipe recette = merchantInv.getSelectedRecipe();
        if (recette == null) {
            event.setCancelled(true);
            return;
        }

        // On annule toujours le comportement vanilla : c'est nous qui gerons l'echange
        event.setCancelled(true);

        List<ItemStack> ingredients = recette.getIngredients();
        if (ingredients.isEmpty()) return;

        double prixEmeraude = plugin.getConfig().getDouble("prix-par-emeraude", 10.0);
        ItemStack premier = ingredients.get(0);
        ItemStack resultat = recette.getResult();

        boolean achatAvecEmeraudes = premier.getType() == Material.EMERALD;
        boolean venteContreEmeraudes = resultat.getType() == Material.EMERALD;

        if (achatAvecEmeraudes) {
            double cout = premier.getAmount() * prixEmeraude;
            if (plugin.getEconomyManager().getSolde(joueur.getUniqueId()) < cout) {
                joueur.sendMessage(ChatColor.RED + "Solde insuffisant pour cet echange (" +
                        plugin.getEconomyManager().formater(cout) + " requis).");
                return;
            }

            // S'il y a un deuxieme ingredient physique (ex: emeraude + livre), on le retire reellement
            if (ingredients.size() > 1 && ingredients.get(1) != null && ingredients.get(1).getType() != Material.AIR) {
                if (!joueur.getInventory().containsAtLeast(ingredients.get(1), ingredients.get(1).getAmount())) {
                    joueur.sendMessage(ChatColor.RED + "Il vous manque des objets pour cet echange.");
                    return;
                }
                joueur.getInventory().removeItem(ingredients.get(1));
            }

            plugin.getEconomyManager().retirer(joueur.getUniqueId(), cout);
            joueur.getInventory().addItem(resultat.clone());
            joueur.sendMessage(ChatColor.GREEN + "✔ Achat effectue : " + plugin.getEconomyManager().formater(cout));
        } else if (venteContreEmeraudes) {
            if (!joueur.getInventory().containsAtLeast(premier, premier.getAmount())) {
                joueur.sendMessage(ChatColor.RED + "Il vous manque des objets pour cet echange.");
                return;
            }
            double gain = resultat.getAmount() * prixEmeraude;
            joueur.getInventory().removeItem(premier);
            plugin.getEconomyManager().donner(joueur.getUniqueId(), gain);
            joueur.sendMessage(ChatColor.GREEN + "✔ Vente effectuee : +" + plugin.getEconomyManager().formater(gain));
        } else {
            // Trade sans emeraude du tout (rare) : on laisse faire le comportement vanilla normal
            event.setCancelled(false);
            return;
        }

        // Incremente l'usage de la recette pour que le villageois se "fatigue" comme en vanilla
        recette.setUses(recette.getUses() + 1);
        merchantInv.getMerchant().setRecipe(merchantInv.getSelectedRecipeIndex(), recette);
    }
}
