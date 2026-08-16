package fr.rp.economy.models;

public class Quete {
    private final String id;
    private final String nom;
    private final String description;
    private final String difficulte;
    private final double recompense;
    private final String materiau;
    private final int quantite;
    private final String dimension; // overworld, nether, end, any

    public Quete(String id, String nom, String description, String difficulte,
                 double recompense, String materiau, int quantite, String dimension) {
        this.id = id;
        this.nom = nom;
        this.description = description;
        this.difficulte = difficulte;
        this.recompense = recompense;
        this.materiau = materiau;
        this.quantite = quantite;
        this.dimension = dimension == null ? "any" : dimension.toLowerCase();
    }

    public Quete(String id, String nom, String description, String difficulte,
                 double recompense, String materiau, int quantite) {
        this(id, nom, description, difficulte, recompense, materiau, quantite, "any");
    }

    public String getId() { return id; }
    public String getNom() { return nom; }
    public String getDescription() { return description; }
    public String getDifficulte() { return difficulte; }
    public double getRecompense() { return recompense; }
    public String getMateriau() { return materiau; }
    public int getQuantite() { return quantite; }
    public String getDimension() { return dimension; }
}
