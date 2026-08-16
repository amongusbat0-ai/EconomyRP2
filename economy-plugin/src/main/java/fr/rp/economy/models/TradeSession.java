package fr.rp.economy.models;

import org.bukkit.inventory.Inventory;

import java.util.UUID;

public class TradeSession {

    private final UUID joueurA;
    private final UUID joueurB;
    private final Inventory inventaire;
    private boolean confirmeA = false;
    private boolean confirmeB = false;

    public TradeSession(UUID joueurA, UUID joueurB, Inventory inventaire) {
        this.joueurA = joueurA;
        this.joueurB = joueurB;
        this.inventaire = inventaire;
    }

    public UUID getJoueurA() { return joueurA; }
    public UUID getJoueurB() { return joueurB; }
    public Inventory getInventaire() { return inventaire; }

    public boolean isConfirmeA() { return confirmeA; }
    public boolean isConfirmeB() { return confirmeB; }
    public void setConfirmeA(boolean valeur) { this.confirmeA = valeur; }
    public void setConfirmeB(boolean valeur) { this.confirmeB = valeur; }

    public boolean autrePartie(UUID uuid) {
        return uuid.equals(joueurA) || uuid.equals(joueurB);
    }

    public UUID getAutre(UUID uuid) {
        return uuid.equals(joueurA) ? joueurB : joueurA;
    }

    public void resetConfirmations() {
        confirmeA = false;
        confirmeB = false;
    }
}
