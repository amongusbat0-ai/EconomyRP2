package fr.rp.economy.listeners;

import fr.rp.economy.EconomyRP;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

    private final EconomyRP plugin;

    public PlayerJoinListener(EconomyRP plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        double soldeDepart = plugin.getConfig().getDouble("solde-depart", 100.0);
        plugin.getDatabaseManager().assurerCompte(event.getPlayer().getUniqueId(), event.getPlayer().getName(), soldeDepart);
    }
}
