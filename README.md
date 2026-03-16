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
* Création d'une clé AES avec les 2 codes secrets (client + autorité) (Secret partagé)

### Client
* Récupération des PP à distance
* Mise en place de clé RSA avec serveur pour communication sécurisé en HTTP
* Récupération de clé IBE, avec sécurité (Code Client + 2FA)
* Communication à distance géré si infrastructure compatible & Accès à Internet
* Interface graphique simple, avec 1 page login, 1 page Code et 1 page mail
* Récupération des paramètres importants (Destinataire, objet, message & nom des fichiers) effectués
* Décryption des mails instantanés, sans action nécessaire de la part de l'utilisateur
* Création d'une clé AES avec les 2 codes secrets (client + autorité) (Secret partagé)

### Mails
* Envoi de mail chiffrés possibles, avec 1 clé IBE/AES pour tous (Objet+Message+Fichier) & Inclusion dans le message du mail (U & V)
* Récupération de liste de mails avec filtration possible
* Déchiffrement des mails possibles si les conditions sont remplis (U & V compris dans le message du mail avec la forme)
* Déchiffrement & Sauvegarde de PJ disponibles
* Possibilité de choisir entre chiffrer le mail ou non, via un paramètre dans l'envoi
* Possibilité d'envoi & de recevoir des fichiers directement dans l'application, et de les télécharger, SANS TELECHARGEMENT EN LOCAL ;)

## A inclure / A vérifier

### Mails
* Ajouter un dynamisme, pouvoir changer la liste des mails affichés (20 mails d'après, autre critère de filtration...)
* Travailler pour améliorer le dynamisme autour de l'application, notamment la récupération de mail s'ils sont chiffrés
### Autorité & Client
* Améliorer les interfaces graphiques, notamment sur la latence, pour avoir un truc cohérent & rapide
* Voir pour mettre en place du HTTPs avec des certificats, qui renforcerait la sécurité
* Rajouter des paramètres globaux qui facilitent la mise en place, notamment pour les paths...
* Voir pour retirer le RSA quand ca n'est pas nécessaire
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
- java -cp "Path vers jPBC & Jakarta Mail" TP_Crypto_Avancee.MainClient
* Exemple : 
- java -cp ".:lib/jpbc-2.0.0/jars/*:lib/JakartaMail/*" TP_Crypto_Avancee.MainClient


