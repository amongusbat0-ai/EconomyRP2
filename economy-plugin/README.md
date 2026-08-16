# EconomyRP

Plugin d'économie RP pour Minecraft (Paper 1.21+)  
Argent virtuel, trade joueur, villageois, quêtes fixes + **quête globale progressive toutes les 12h**.

## Installation

1. Télécharge le dernier `EconomyRP.jar` :
   - Onglet **Actions** → dernier workflow → artifact `EconomyRP-jar`
   - ou onglet **Releases**
2. Place le `.jar` dans le dossier `plugins/` de ton serveur Paper/Spigot
3. Redémarre le serveur
4. Config : `plugins/EconomyRP/config.yml`

## Commandes joueurs

| Commande | Description |
|----------|-------------|
| `/balance` `/bal` | Voir son solde |
| `/pay <joueur> <montant>` | Payer un joueur |
| `/baltop` | Classement des plus riches |
| `/wallet` | GUI porte-monnaie |
| `/quests` | Tableau des quêtes (+ quête globale) |
| `/trade <joueur>` | Échange d'objets |
| `/questcreate <joueur> <récompense> <description>` | Proposer une quête à un joueur |
| `/questaccept` / `/questrefuse` | Répondre à une quête |
| `/questvalider <joueur>` | Valider une quête joueur |

## Commandes admin (`economyrp.admin`)

```
/eco give|take|set <joueur> <montant>
/questadmin create <id> <materiau> <quantite> <recompense> <difficulte> <dimension> <nom...>
/questadmin delete <id>
/questadmin list
/questadmin reload
/questadmin global new    # force une nouvelle quête globale 12h
/questadmin global info
```

**Dimensions** : `overworld` | `nether` | `end` | `any`  
**Difficultés** : `facile` | `moyen` | `difficile` | `tres_difficile` | `legendaire`

## Quête globale (12h)

- Une quête aléatoire est choisie toutes les **12 heures**
- N'importe quel joueur peut la valider **une fois** par cycle
- Chaque validation **augmente** la quantité requise (+18 %) et la récompense (+22 %)
- Affichée en haut de `/quests`

## Build local

```bash
mvn -B clean package
# → target/EconomyRP.jar
```

## GitHub Actions

À chaque push sur `main` / `master` :
1. Compilation Maven (Java 21)
2. Upload de l'artifact `EconomyRP-jar`
3. Création d'une Release avec le `.jar`
