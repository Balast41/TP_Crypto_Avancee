# Cryptographie avancée - Développement d'une application Mail Sécurisé

## Renseignements
### Université Polytechnique - INSA Hauts-de-France
#### Spécialité Informatique & Cybersécurité - 4A 2025/2026
#####  Made by : 
- Thomas Manche
- Dan Depredurand
- Quentin Balazot
- Noa Bukovec
- Léandre Lavoisier

## Déjà inclus

### Autorité
* Génération des PP
* Mise en place de clé RSA avec client pour communication sécurisé en HTTP
* Génération de clé IBE, avec sécurité (Code Client + 2FA)
* Multi-client possible avec tableau dynamique
* Communication à distance géré si infrastructure compatible

### Client
* Récupération des PP à distance
* Mise en place de clé RSA avec serveur pour communication sécurisé en HTTP
* Récupération de clé IBE, avec sécurité (Code Client + 2FA)
* Communication à distance géré si infrastructure compatible & Accès à Internet
* Interface graphique simple, avec 1 page login, 1 page Code et 1 page mail

### Mails
* Envoi de mail chiffrés, avec 1 clé IBE/AES pour tous (Objet+Message+Fichier) & Inclusion dans le message du mail (U & V)
* Récupération de liste de mails avec filtration possible
* Déchiffrement des mails possibles si les conditions sont remplis (U & V compris dans le message du mail avec la forme)
* Déchiffrement & Sauvegarde de PJ disponibles

## A inclure / A vérifier

### Mails
* Ajouter la possibilité de choisir entre envoyer un mail chiffré ou non (Paramètres à passer !)
* Inclure les PJs possibles dans un String[], en incluant les Path
* Montrer les PJs dans les mails dans l'application
* Essayer de ne pas télécharger les mails avant (Très important pour des raisons de sécurité !)
* Ajouter un dynamisme, pouvoir changer la liste des mails affichés (20 mails d'après, autre critère de filtration...)

### Autorité & Client
* Rajouter une verbose cohérente qui permet de voir les clés...
* Améliorer les interfaces graphiques, notamment sur la latence, pour avoir un truc cohérent & rapide
* Voir pour mettre en place du HTTPs avec des certificats, qui renforcerait la sécurité
* Rajouter des paramètres globaux qui facilitent la mise en place, notamment pour les paths...

## Dépendances & Comment lancer

### packages nécessaires
- jPBC
- Jakarta Mail

### Commandes de lancement - Linux

#### Compiler le fichier :
- javac -cp "Path vers jPBC & Jakarta Mail" -d . PathVers/src/TP_Crypto_Avancee/*.java
* Exemple : 
- javac -cp "lib/jpbc-2.0.0/jars/*:lib/JakartaMail/*:." -d . TPJavaMail/TP_Crypto_Avancee/src/TP_Crypto_Avancee/*.java

#### Lancer l'autorité : 
- java -cp "Path vers jPBC & Jakarta Mail" TP_Crypto_Avancee.HttpServeurAutorite
* Exemple :
- java -cp "lib/jpbc-2.0.0/jars/*:lib/JakartaMail/*:." TP_Crypto_Avancee.HttpServeurAutorite

#### Lancer le client : 
- java -cp "Path vers jPBC & Jakarta Mail" TP_Crypto_Avancee.HttpServeurAutorite
* Exemple : 
- java -cp ".:lib/jpbc-2.0.0/jars/*:lib/JakartaMail/*" TP_Crypto_Avancee.MainClient


