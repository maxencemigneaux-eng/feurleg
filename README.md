# Feurleg

Mod Fabric **côté client** (Minecraft 1.21.1) qui affiche en permanence les timers des légendaires du menu `/leg`.

## Fonctionnement
- Ouvre `/leg` une fois : le mod lit les timers affichés (zone + temps) et les décompte tout seul ensuite.
- À chaque réouverture de `/leg`, les timers sont recalés.
- Les timers sont enregistrés par serveur (heure réelle) : ils survivent à une déconnexion ou un redémarrage.
- Alerte (son + message) à 1 min et à 0.

## Heure Minecraft du spawn
Chaque zone a sa propre heure. Le mod l'apprend quand tu es **dans la zone et que tu ouvres /leg** (une fois par zone suffit),
puis la fait avancer tout seul. Tant qu'une zone n'est pas connue, elle affiche `--:--`. Bleu = nuit, jaune clair = jour.

## Touches (modifiables dans Options > Commandes)
- **K** : afficher / cacher le HUD
- **F8** (dans un menu) : enregistre tout le texte du menu dans `config/feurleg-dump.txt` (debug)

## Réglages : `config/feurleg.json`
- `corner` : `haut-gauche`, `haut-droite`, `bas-gauche`, `bas-droite`
- `offsetX`, `offsetY`, `scale`, `backgroundAlpha`
- `timerFormat` : `mm:ss` (défaut) ou `h:mm` si les timers du serveur sont en heures:minutes
- `showMinecraftTime` : heure Minecraft du spawn (bleu = nuit), `showCountdown` : compte à rebours
- `warnSeconds`, `alertSound`, `alertMessage`

## Compilation
Pousse le dossier sur GitHub : GitHub Actions compile le mod et dépose le `.jar` dans le dossier `telechargement`.
