# 🩺 Trusta – Smart Health Reminder & Wellness Assistant

Trusta is an Android healthcare application designed to help users manage medications, reminders, alarms, daily health routines, and wellness through a modern, intelligent, and user-friendly experience.

The application combines medication reminders, alarm scheduling, AI-powered assistance, daily planning, and health tracking into one centralized platform to help users stay organized and improve medication adherence.

---

## ✨ Features

### 🔐 Authentication

* Email & Password Authentication
* Google Sign-In
* Password Recovery
* Secure Firebase Authentication

### 💊 Medication Reminders

* Create, Edit and Delete Reminders
* Recurring Reminders
* Reminder Status Tracking
* Today's Reminder Dashboard
* Reminder Search

### ⏰ Alarm Management

* Schedule Medication Alarms
* Enable / Disable Alarms
* Alarm Notifications
* Alarm History

### 📅 Daily Planning

* Today's Plan
* Daily Schedule
* Calendar Notes
* Upcoming Activities

### 🔍 Smart Search

* Real-Time Search
* Search History
* Reminder & Alarm Filtering
* Fast Search using Room Database

### 📊 Dashboard

* Today's Reminders
* Pending Reminders
* Completed Reminders
* Daily Health Tips
* Quick Navigation

### 🤖 AI Health Assistant

* AI-powered Health Guidance
* Medication Assistance
* Interactive Chat Experience

### 📈 Reports

* Reminder Reports
* Activity Tracking
* Health Progress Overview

### ⚙️ User Settings

* Light Theme
* Dark Theme
* System Theme
* User Profile Management

---

# 🏗️ Architecture

The project follows the **MVVM (Model–View–ViewModel)** architecture combined with the Repository Pattern for better scalability and maintainability.

```
UI
│
├── Activities
├── Fragments
│
▼
ViewModels
│
▼
Repository
│
├── Room Database
├── Firebase
└── Retrofit APIs
```

---

# 🛠️ Tech Stack

### Language

* Kotlin

### Architecture

* MVVM Architecture
* Repository Pattern

### Local Database

* Room Database

### Cloud Services

* Firebase Authentication
* Firebase Firestore

### Networking

* Retrofit

### Dependency Injection

* Koin

### Concurrency

* Kotlin Coroutines
* Flow

### UI

* XML Layouts
* Material Design 3
* ViewBinding
* Navigation Component
* Splash Screen API

---

# 📂 Project Structure

```
app
├── features
│   ├── auth
│   ├── alarm
│   ├── chatbot
│   ├── dashboard
│   ├── search
│   ├── reports
│   ├── reminder
│   ├── schedule
│   ├── profile
│   ├── settings
│   └── welcome
│
├── data
│   ├── repository
│   ├── local
│   ├── remote
│   └── model
│
├── utils
└── viewmodel
```

---

# 📱 Main Screens

* Welcome
* Login
* Register
* Dashboard
* Medication Reminders
* Alarm Management
* Search
* Today's Plan
* Reports
* AI Assistant
* Profile
* Settings

---

# 🚀 Getting Started

### Clone the repository

```bash
git clone https://github.com/MomenOsama123/SmartHealthReminder.git
```

### Open the project

Open the project using **Android Studio**.

### Configure Firebase

Add your own:

```
google-services.json
```

inside the `app/` module.

### Sync Gradle

Wait until all dependencies are downloaded.

### Run

Run the application on an Android device or Emulator.

---

# 📸 Screenshots

> Screenshots will be added after the final UI polishing.

---

# 🔮 Future Improvements

* Smart Health Analytics
* PDF Report Export
* Cloud Backup & Synchronization
* Wearable Device Integration
* AI Health Recommendations
* Multi-language Support

---

# 👨‍💻 Developed As

Graduation Project – Faculty of Computers and Data Science

---

# 📄 License

This project is intended for educational and graduation project purposes.
