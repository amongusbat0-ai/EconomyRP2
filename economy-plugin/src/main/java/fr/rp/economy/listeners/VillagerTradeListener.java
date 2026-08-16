package fr.rp.economy.listeners;

import fr.rp.economy.EconomyRP;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.MerchantRecipe;

import java.util.ArrayList;
import java.util.List;

public class VillagerTradeListener implements Listener {

    private final EconomyRP plugin;

    public VillagerTradeListener(EconomyRP plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onSpawn(EntitySpawnEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof AbstractVillager villager) {
            // Applique l'ajustement une fois que les recettes par defaut sont generees (prochain tick)
            plugin.getServer().getScheduler().runTask(plugin, () -> ajusterRecettes(villager));
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (event.getRightClicked() instanceof AbstractVillager villager) {
            ajusterRecettes(villager);
        }
    }

    /**
     * Rend les echanges plus attractifs pour le joueur tout en restant coherents :
     * - reduit le prix en emeraudes demande par le villageois (dans une fourchette raisonnable)
     * - augmente legerement la quantite d'objets recus en echange d'emeraudes
     * - ne descend jamais en dessous de 1 emeraude ni au dessus des maximums vanilla
     */
    private void ajusterRecettes(AbstractVillager villager) {
        double multiplicateur = plugin.getConfig().getDouble("multiplicateur-attractivite", 1.4);
        List<MerchantRecipe> recettes = new ArrayList<>(villager.getRecipes());
        List<MerchantRecipe> nouvelles = new ArrayList<>();

        for (MerchantRecipe recette : recettes) {
            List<org.bukkit.inventory.ItemStack> ingredients = new ArrayList<>(recette.getIngredients());
            if (ingredients.isEmpty()) {
                nouvelles.add(recette);
                continue;
            }

            // Si le premier ingredient est de l'emeraude, le joueur ACHETE un objet -> reduire le prix
            // Sinon le joueur VEND un objet contre emeraude -> augmenter la quantite recue
            org.bukkit.inventory.ItemStack premier = ingredients.get(0);
            if (premier.getType() == org.bukkit.Material.EMERALD) {
                int nouveauPrix = Math.max(1, (int) Math.round(premier.getAmount() / multiplicateur));
                premier.setAmount(nouveauPrix);
                ingredients.set(0, premier);
            } else if (recette.getResult().getType() == org.bukkit.Material.EMERALD) {
                int nouvelleQuantite = Math.min(64, (int) Math.round(recette.getResult().getAmount() * multiplicateur));
                recette.getResult().setAmount(nouvelleQuantite);
            }

            MerchantRecipe copie = new MerchantRecipe(
                    recette.getResult(),
                    recette.getUses(),
                    recette.getMaxUses(),
                    recette.hasExperienceReward(),
                    recette.getVillagerExperience(),
                    recette.getPriceMultiplier()
            );
            copie.setIngredients(ingredients);
            nouvelles.add(copie);
        }

        villager.setRecipes(nouvelles);
    }
}
