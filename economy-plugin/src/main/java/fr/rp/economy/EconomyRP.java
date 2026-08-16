package fr.rp.economy;

import fr.rp.economy.commands.*;
import fr.rp.economy.gui.WalletGui;
import fr.rp.economy.listeners.EconomyGuiListener;
import fr.rp.economy.listeners.PlayerJoinListener;
import fr.rp.economy.listeners.VillagerEconomyListener;
import fr.rp.economy.listeners.VillagerTradeListener;
import fr.rp.economy.managers.DatabaseManager;
import fr.rp.economy.managers.EconomyManager;
import fr.rp.economy.managers.QuestManager;
import fr.rp.economy.managers.TradeManager;
import org.bukkit.plugin.java.JavaPlugin;

public class EconomyRP extends JavaPlugin {

    private static EconomyRP instance;

    private DatabaseManager databaseManager;
    private EconomyManager economyManager;
    private QuestManager questManager;
    private TradeManager tradeManager;
    private WalletGui walletGui;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        databaseManager = new DatabaseManager(this);
        databaseManager.connecter();

        economyManager = new EconomyManager(this);
        questManager = new QuestManager(this);
        questManager.charger();
        tradeManager = new TradeManager(this);
        walletGui = new WalletGui(this);

        // Commandes
        EconomyCommand economyCommand = new EconomyCommand(this);
        getCommand("balance").setExecutor(economyCommand);
        getCommand("pay").setExecutor(economyCommand);
        getCommand("baltop").setExecutor(economyCommand);
        getCommand("eco").setExecutor(new EcoAdminCommand(this));

        WalletCommand walletCommand = new WalletCommand(this);
        getCommand("wallet").setExecutor(walletCommand);
        getCommand("quests").setExecutor(walletCommand);

        getCommand("trade").setExecutor(new TradeCommand(this));
        getCommand("questcreate").setExecutor(new QuestCreateCommand(this));

        QuestResponseCommand questResponseCommand = new QuestResponseCommand(this);
        getCommand("questaccept").setExecutor(questResponseCommand);
        getCommand("questrefuse").setExecutor(questResponseCommand);
        getCommand("questvalider").setExecutor(new QuestValidateCommand(this));

        QuestAdminCommand questAdmin = new QuestAdminCommand(this);
        getCommand("questadmin").setExecutor(questAdmin);
        getCommand("questadmin").setTabCompleter(questAdmin);

        // Listeners
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new EconomyGuiListener(this), this);
        getServer().getPluginManager().registerEvents(new VillagerTradeListener(this), this);
        getServer().getPluginManager().registerEvents(new VillagerEconomyListener(this), this);

        getLogger().info("EconomyRP a ete active avec succes !");
    }

    @Override
    public void onDisable() {
        if (questManager != null) questManager.arreter();
        if (databaseManager != null) databaseManager.fermer();
        getLogger().info("EconomyRP a ete desactive.");
    }

    public static EconomyRP getInstance() { return instance; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public EconomyManager getEconomyManager() { return economyManager; }
    public QuestManager getQuestManager() { return questManager; }
    public TradeManager getTradeManager() { return tradeManager; }
    public WalletGui getWalletGui() { return walletGui; }
}
