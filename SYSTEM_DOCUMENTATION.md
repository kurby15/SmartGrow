# SmartGrow - System Flow & Architecture Documentation

This document outlines the end-to-end system flow, navigation structure, and architecture of the **SmartGrow** Android application.

---

## 1. System Overview
**SmartGrow** is a comprehensive AI-powered plant care assistant. It enables users to identify plant species, diagnose health issues, track growth in a digital garden, set care reminders, and engage with a global community of growers. The application follows a Single-Activity architecture (`MainActivity`) for its core workspace, utilizing fragments for modular navigation and feature isolation.

---

## 2. Application Launch & Authentication Flow

### A. Splash Screen (`SplashActivity`)
1. **Entry Point**: The application launches into `SplashActivity` with a pulse-glow animation.
2. **Transition**: After a 3.5-second delay, it routes the user to `GetStartedActivity`.

### B. Authentication & Session Management (`LoginActivity`)
1. **Session Verification**:
   - The app checks for an active Firebase user and a local `isLoggedIn` flag in `SharedPrefManager`.
   - **Authenticated**: Redirects directly to `MainActivity`.
   - **Unauthenticated**: Remains on `LoginActivity`.
2. **Login Options**:
   - Supports both **Email** and **Username** based authentication.
   - For username login, the app performs a lookup in the `users` collection to find the associated encrypted email.
3. **Password Recovery**:
   - **Forgot Password**: Driver clicks `"Forgot Password?"` which redirects to `ForgotPasswordActivity` to trigger a Firebase password reset email.
4. **Registration Flow (`RegisterStep5Activity`)**:
   - A direct registration process for creating a new account (Create Account).
   - **Real-time Validation**: Validates password requirements (8-12 characters, uppercase, numbers, and special characters) with a dynamic checklist.
   - **Security**: User data is encrypted via `FirebaseCryptoUtils` before being committed to the Firestore `users` collection.
   - **Visual Feedback**: Features a custom "Sprouting Leaf" animation and a progress percentage during the account setup phase.

---

## 3. Core Workspace Navigation Flow (`MainActivity`)

The `MainActivity` acts as the primary hub, managing a `BottomNavigationView` and a dynamic header for global actions.

```
                  +-----------------------------------+
                  |         SplashActivity            |
                  +-----------------+-----------------+
                                    |
                          +---------v---------+
                          | GetStartedActivity|
                          +---------+---------+
                                    |
                    Session Active? |
                   +----------------+----------------+
                   |                                 |
                [ YES ]                           [ NO ]
                   |                                 |
                   v                                 v
         +-------------------+             +-------------------+
         |   MainActivity    |             |   LoginActivity   +-----------+
         +---------+---------+             +---------+---------+           |
                   |                                 |                     |
                   |                +----------------+              [ New User? ]
                   |                |                                      |
                   |          [ Successful Auth ]                          v
                   |                |                          [ Create Account ]
                   | <--------------+                      (RegisterStep5Activity)
                   v
     +-------------------------------------------------------------+
     | Top Header & Central Assistant Trigger                      |
     +-----+---------------+---------------+---------------+-------+
           |               |               |               |
           v               v               v               v
     [ Profile ]     [ Community ]   [ Notification ]  [ AI Assistant ]
     (Fragment)       (Forum)           (Dialog)       (AI / Scanner)
           |                                               |
           |                                       +-------+-------+
           |                                       |               |
           |                                [ AI Chatbot ]  [ Plant Scanner ]
           |                                (BottomSheet)   (Camera/Gallery)
           v
     +-------------------------------------------------------------+
     | Bottom Navigation Bar                                       |
     +-----------------------------+-------------------------------+
                                   |
                          +--------+--------+
                          |                 |
                    [ Home Tab ]      [ My Garden Tab ]
                                            |
                                            v
                         +-----------------------------------------+
                         | My Garden Workspace (Dual-Tab)          |
                         +--------+-----------------------+--------+
                                  |                       |
                       [ My Garden View ]          [ Snap History ]
                       (Collection Hub)            (Chronological Log)
                                  |                       |
                    +-------------+-------------+         v
                    |             |             |   [ Save to Garden ]
             [ Live Search ] [ Care Stats ] [ Plant Diary ]  (Migration)
             (Name/SciName)  (Real-time)    (Overview/Care)
                    |             |             |
             [ View All ]    [ Reminders ]   [ Map/TTS ]
             (Inventory)     (Schedules)     (Habitat)
                    |             |
             [ Rename/Sort ] [ Delete/Archive ]
             (AI Suggested)  (Firestore Sync)
```

### A. AI Chat Assistant & Scanner
- **Central Trigger**: A specialized UI card (`cardNavChatAssistant`) provides access to:
  - **Plant Scanner**: Launches `CameraScannerActivity` to identify plants and diagnose health issues.
  - **AI Chatbot**: Opens a `BottomSheetDialog` for 24/7 care advice and app guidance.
- **Contextual Intelligence**: The AI engine (`PlantAnalyzer`) retains memory of the last analyzed plant and chat history (stored in Firestore) to provide relevant follow-up responses.

### B. My Garden Workspace Flow & Features
The **My Garden** module (`MyGardenFragment`) is an immersive sub-workspace managed via dual-tab navigation, serving as the central productivity hub for plant care:

*   **Navigation & Workspace Switching**:
    *   **Tab Toggle**: Seamless switching between the **Active Garden** (saved plants) and **Snap History** (AI scans).
    *   **Inventory Expansion**: "View All" action routes to `AllPlantsFragment` for full-screen management.
    *   **Growth Tracking Entry**: Direct deep-linking to `PlantDiaryActivity` from any plant card.
*   **Integrated Management Tools**:
    *   **Live Multi-Field Search**: Real-time filtering by Common Name or Scientific Name using a master-cache strategy.
    *   **AI-Assisted Identity Management**: Integrated **AI Name Suggestions** during renaming and global sync across all collections using `WriteBatch`.
    *   **Custom Organization**: Header popups for creating collections and sorting the inventory.
*   **Care & Alert Flow**:
    *   **Real-time Dashboard**: Summarizes pending tasks (Watering, Fertilizing, Sunlight) based on schedules and user-preferred times.
    *   **Automated Rescue Path**: Sick plants (<50% health) trigger automated system notifications and high-priority flagging in the care flow.

### C. Profile & Account Management (`ProfileFragment`)
- **User Statistics**: Displays counts for **Plants** (Diary), **Scans** (History), and **Posts** (Community).
- **Settings & Security**:
  - `AccountSecurityFragment`: Hub for security preferences.
  - `ChangePasswordFragment`: Internal secure password update flow.
- **Data Privacy**: Sensitive fields (Full Name, Phone, Email) are encrypted using **AES-256 GCM** via `SecurityUtils` before storage.

---

## 4. Feature Modules Detail Flow

### 1. Plant Identification & Health Scanner
- **Processing Engine**: Hybrid strategy utilizing **Gemini 1.5 Flash** for vision/diagnosis and **SambaNova (DeepSeek)** for text-based interaction.
- **Workflow**: Captures image -> Base64 encoding -> AI Vision API -> JSON Parsing -> Structured JSON Report (Species, Health Score, Pests, Care Guide).

### 2. My Garden (Digital Greenhouse)
The **My Garden** module (`MyGardenFragment`) is the core botanical management hub, facilitating health monitoring, collection maintenance, and historical tracking.

#### A. Interactive Workspace
*   **Dual-Tab Navigation**:
    *   **My Garden Tab**: Primary view for active plants. Displays a grid layout of the user's saved collection with real-time status indicators.
    *   **Snap History Tab**: A chronological log of all AI scans. Allows users to revisit past analyses or migrate identified plants into the permanent garden.
*   **Global Actions**:
    *   **Live Multi-Field Search**: Real-time filtering by Common Name or Scientific Name using a master-cache strategy for instantaneous results across both tabs.
    *   **Collection Management**: Support for creating custom collections and sorting via a dynamic header `PopupMenu`.
    *   **Expanded Inventory**: Direct navigation to `AllPlantsFragment` ("View All") for browsing large collections.

#### B. Intelligent Care Engine
*   **Dynamic Task Tracking**: Real-time counters for **Watering**, **Fertilizing**, and **Sunlight/Health Checks**.
*   **Advanced Scheduling Logic (`isTaskDue`)**:
    *   Calculates due tasks by comparing the current date against `lastWateredDate`, `lastFertilizedDate`, etc.
    *   Supports multiple care frequencies: "Every Day", "Every 2/3 Days", "Weekly", "Monthly".
    *   **Preferred Time Threshold**: Counters only increment once the user’s specific `preferredTime` (e.g., 8:00 AM) has passed for the day.
*   **Health-Driven Prioritization**:
    *   **Emergency Flagging**: Plants with a health score **below 50%** are automatically flagged for all care tasks regardless of their standard schedule.
    *   **Pre-emptive Alerts**: Watering is automatically suggested for plants with health **below 60%**.
*   **System Integration**: A background polling service (`clockHandler`) refreshes statuses every 60 seconds to handle midnight transitions.

#### C. Core Plant Management Functions
*   **History-to-Garden Migration**: One-click functionality to save scanned items from 'Snap History' to 'My Garden'.
*   **Automated Rescue System**: Saving a high-stress plant (<50% health) automatically triggers system-level notifications for Water, Sunlight, and Fertilizer via `NotificationHelper`.
*   **Smart Identity Management**:
    *   **AI Name Suggestions**: The renaming interface parses raw AI JSON data to suggest accurate names.
    *   **Multi-Collection Sync**: Renaming a plant triggers a `WriteBatch` to keep `diary` and `diary_history` records synchronized.
*   **Plant Inventory Control**: Secure Firestore deletion, manual reminder setup via `PlantReminderBottomSheet`, and deep-linking to growth logs.

#### D. Detailed Growth Diary (`PlantDiaryActivity`)
*   **Deep-Dive Analysis**: A multi-tab view (Overview, Schedule, History) providing comprehensive plant insights.
*   **Botanical Data Visualization**:
    *   **Habitat Mapping**: Integrated Google Maps showing species geographic distribution.
    *   **Match Confidence**: Real-time visualization of AI identification accuracy.
    *   **Characteristics Explorer**: Displays ultimate height, spread, leaf type, colors, soil, and hardiness.
*   **Enhanced Accessibility**:
    *   **Text-to-Speech (TTS)**: Audible readout of plant identity, scientific names, and health status.
    *   **Visual Condition Feed**: Base64 decoding for viewing captured photos and health preview images.

### 3. Community Forum
- **Social Interaction**: Users can share photos and gardening queries to a global feed via the `CommunityForumFragment`.

### 4. Smart Care Reminders
- **Automation**: Managed via `AlarmManager` and `AlarmReceiver`.
- **Functionality**: Triggers system-level notifications for watering, fertilizing, or repotting tasks even if the app is in the background.

---

## 5. Background Services & Data Security

### A. Data Architecture
- **Firestore**: Primary database for profiles, diary entries, forum posts, and chat sessions.
- **Local Storage**: `SharedPrefManager` for session caching and user metadata.
- **Media Handling**: Uses **Glide** for asset caching and Base64 for local image processing.

### B. Security Implementation
- **Field-Level Encryption**: All sensitive user profile data is encrypted before transit to Firestore.
- **Session Integrity**: Mandatory re-authentication for sensitive account management tasks.

---

## 6. Architectural Patterns & Technologies

- **Single-Activity Architecture**: Centralized navigation via `MainActivity`.
- **Hybrid AI Ecosystem**: Dual-engine routing (Gemini & DeepSeek) for optimized vision and chat capabilities.
- **Reactive UI**: Real-time Firestore listeners for data syncing.
- **Material Design 3**: Modern, immersive UI with Lottie animations and shimmer loading.
