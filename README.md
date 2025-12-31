# 🎫 EventBooking

<div align="center">

![EventBooking Banner](https://img.shields.io/badge/EventBooking-Platform-blue?style=for-the-badge)
![Java](https://img.shields.io/badge/Java-17-orange?style=for-the-badge&logo=java)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen?style=for-the-badge&logo=spring)
![Vaadin](https://img.shields.io/badge/Vaadin-24-00B4F0?style=for-the-badge&logo=vaadin)

**Plateforme intégrée de billetterie et gestion d'événements de bout en bout**

[Fonctionnalités](#-fonctionnalités-clés) • [Installation](#-installation) • [Technologies](#-stack-technologique) • [Architecture](#-architecture) • [Démo](#-comptes-de-démonstration)

</div>

---

## 📋 À propos

**EventBooking** est une application web d'entreprise complète qui orchestre l'intégralité du cycle de vie des événements. De la création à la réservation, en passant par la modération et les analytics, cette plateforme offre une expérience fluide pour tous les acteurs de l'écosystème événementiel.

### 🎯 Pour qui ?

- **🎪 Organisateurs** : Créez, gérez et analysez vos événements
- **👥 Clients** : Découvrez, réservez et évaluez des événements
- **⚙️ Administrateurs** : Supervisez et modérez l'ensemble de la plateforme

---

## ✨ Fonctionnalités Clés

### 👨‍💼 Espace Organisateur

- 📊 **Tableau de bord analytique** avec CA, taux de remplissage et statistiques en temps réel
- 📅 **Gestion complète d'événements** (création, modification, publication)
- 📄 **Export PDF** des listes de réservations pour la logistique
- 💰 **Suivi financier** détaillé par événement

### 🛍️ Espace Client

- 🔍 **Recherche et filtrage** d'événements par catégorie, date et localisation
- 🎟️ **Réservation intelligente** avec validation en temps réel des disponibilités
- 📧 **Confirmation par email** avec code de réservation unique (`EVT-XXXXX`)
- 📱 **Billets PDF numériques** générés automatiquement
- ⭐ **Système d'avis** et notation (1-5 étoiles) post-événement

### 🔐 Espace Administrateur

- 👥 **Gestion des utilisateurs** (activation/désactivation, modification des rôles)
- 🎭 **Modération globale** de tous les événements et réservations
- 📈 **Monitoring complet** de la plateforme
- 🛡️ **Gestion des privilèges** en temps réel

### 🌟 Fonctionnalités Transversales

- 🔒 **Authentification sécurisée** avec Spring Security
- 📧 **Notifications automatiques** (inscription, réservation, confirmation)
- 🔑 **Récupération de compte** par jeton sécurisé
- 🌓 **Mode sombre/clair** dynamique
- 📱 **Interface responsive** adaptée à tous les écrans

---

## 🚀 Installation

### Prérequis

- Java 17+
- Maven 3.6+
- H2 

### Étapes

1. **Cloner le repository**
   ```bash
   git clone https://github.com/votre-username/eventbooking.git
   cd eventbooking
   ```

2. **Configurer le stockage des images**
   
   Modifiez le chemin `UPLOAD_RELATIVE_PATH` dans `ImageUtils.java` :
   ```java
   private static final String UPLOAD_RELATIVE_PATH= "/votre/chemin/uploads/";
   ```

3. **Configurer la base de données** (optionnel)
   
   Éditez `application.properties` :
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/eventbooking
   spring.datasource.username=votre_user
   spring.datasource.password=votre_password
   ```

4. **Lancer l'application**
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```

5. **Accéder à l'application**
   
   Ouvrez votre navigateur : `http://localhost:8080`

---

## 🔑 Comptes de Démonstration

| Rôle | Email | Mot de passe |
|------|-------|--------------|
| 🔧 **Administrateur** | `achraf4ettanouti2@gmail.com` | `Achraf@2004` |
| 🎪 **Organisateur** | `sakinadaoudi38@gmail.com` | `Organizateur1@23` |
| 👤 **Client** | `wsrh2024@gmail.com` | `Sakina@234` |

---

## 🛠️ Stack Technologique

### Backend
- **Java 17** - Langage principal
- **Spring Boot 3.x** - Framework applicatif
- **Spring Security** - Authentification et autorisation
- **Spring Data JPA** - Accès aux données
- **Hibernate** - ORM

### Frontend
- **Vaadin Flow 24** - Framework UI Java full-stack
- **Responsive Design** - Interface adaptative

### Base de Données
- **H2** - Développement (en mémoire)
- **MySQL** - Production

### Génération de Documents
- **iText 7** - Création de billets et rapports PDF

### Communication
- **Spring Mail** - Service d'envoi d'emails

---

## 🏗️ Architecture

### Design Patterns

Le projet implémente une architecture multicouche robuste :

```
┌─────────────────────────────────────────┐
│         Couche Présentation             │
│            (Vaadin UI)                  │
├─────────────────────────────────────────┤
│          Couche Service                 │
│        (Logique Métier)                 │
├─────────────────────────────────────────┤
│      Couche Repository (DAO)            │
│        (Spring Data JPA)                │
├─────────────────────────────────────────┤
│        Base de Données                  │
│         (H2)                    │
└─────────────────────────────────────────┘
```

### Principes Clés

- **DTOs (Data Transfer Objects)** : Séparation entités/présentation
- **Mappers** : Conversion automatisée Entité ↔ DTO
- **Exceptions métier** : Gestion centralisée des erreurs
- **Séparation des préoccupations** : Chaque couche a sa responsabilité

---

## 📂 Structure du Projet

```
src/main/java/org/example/reservation_event/
├── 📦 classes/           # Entités JPA (User, Event, Reservation, Review)
├── 📋 dtos/              # Data Transfer Objects
├── 🏷️  Enums/            # Types énumérés (Roles, Status)
├── ⚠️  Exceptions/        # Exceptions personnalisées
├── 🔄 mappers/           # Transformations Entité/DTO
├── 📧 email/             # Service de notifications
├── 💾 repositories/      # Interfaces JPA
├── 🎯 services/          # Contrats de service
├── ⚙️  ServicesImpl/      # Implémentations métier
└── 🎨 ui/                # Interfaces Vaadin
    ├── admin/            # Vues administrateur
    ├── client/           # Vues client
    └── public/           # Vues publiques
```

---

## 🎨 Captures d'Écran

> _Section à compléter avec des captures d'écran de votre application_

---

## 🤝 Contribution

Les contributions sont les bienvenues ! N'hésitez pas à :

1. Fork le projet
2. Créer une branche (`git checkout -b feature/AmazingFeature`)
3. Commit vos changements (`git commit -m 'Add AmazingFeature'`)
4. Push vers la branche (`git push origin feature/AmazingFeature`)
5. Ouvrir une Pull Request

---
## 🙏 Remerciements

- Spring Boot pour le framework robuste
- Vaadin pour l'excellent framework UI
- iText pour la génération de PDF
- La communauté open-source

---
