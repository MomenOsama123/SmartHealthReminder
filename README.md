# Trusta

## Smart Health Reminder & Wellness Assistant

Trusta is an Android healthcare application designed to help users manage medications, reminders, alarms, daily health routines, reports, and AI-powered assistance through a modern and user-friendly experience.

The application aims to improve medication adherence, reduce missed doses, and provide users with organized health management tools supported by intelligent guidance and tracking features.

---

## Features

### Authentication
- User Registration
- Secure Login
- Google Sign-In
- Password Recovery
- Firebase Authentication Integration

### Reminder Management
- Create, Edit, and Delete Reminders
- Medication Scheduling
- Reminder Status Tracking
- Local Storage using Room Database

### Alarm System
- Create and Manage Alarms
- Alarm Scheduling
- Alarm Ringing Activity
- Enable / Disable Alarms
- Background Alarm Handling

### Search System
- Real-Time Search
- Reminder Search
- Alarm Search
- Search History
- Filtered Search Results

### Daily Planning
- Today Plan Overview
- Daily Health Routine Management
- Upcoming Activities Tracking

### Reports & Tracking
- Progress Monitoring
- Activity Reports
- Reminder Tracking
- User Health Progress Overview

### AI Assistant
- AI-Powered Health Guidance
- Medication Information Assistance
- Missed Dose Guidance
- Interactive Health Support Chatbot

### User Experience
- Welcome / Onboarding Screens
- Empty State Handling
- Material Design UI
- Light Theme
- Dark Theme
- System Theme Support

---

## Technologies Used

### Development
- Kotlin
- Android Studio

### Architecture
- MVVM (Model–View–ViewModel)

### Database
- Room Database
- Firebase Firestore

### Authentication
- Firebase Authentication

### Networking
- Retrofit

### Dependency Injection
- Koin

### Asynchronous Programming
- Kotlin Coroutines

### Navigation
- Android Navigation Component

### UI
- XML Layouts
- ViewBinding
- Material Design Components
- Android Jetpack Libraries
- Splash Screen API

---

## Architecture Overview

The project follows the MVVM architecture pattern combined with a feature-based modular organization to improve maintainability, scalability, and code separation.

```text
UI Layer
    ↓
ViewModel Layer
    ↓
Repository Layer
    ↓
Room Database / Firebase / APIs
```

### Main Components

- Activities & Fragments
- ViewModels
- Repositories
- Room DAOs
- Firebase Services
- Retrofit API Services
- AI Assistant Module

---

## Project Structure

```text
app
└── src
    ├── features
    │   ├── auth
    │   ├── alarm
    │   ├── chatbot
    │   ├── search
    │   ├── plan
    │   ├── settings
    │   ├── welcome
    │   ├── profileinfo
    │   ├── reports
    │   ├── schedule
    │   └── navigation
    │
    ├── data
    │   ├── dao
    │   ├── entities
    │   ├── repository
    │   └── database
    │
    ├── api
    ├── services
    ├── receivers
    ├── utils
    └── viewmodels
```

---

## Testing

The project includes testing support for several modules, including:

- Reports Module Testing
- Today Plan Testing
- Settings Testing
- Home Fragment Testing
- Reminder Features Testing

---

## Installation

### Clone the Repository

```bash
git clone https://github.com/YOUR_USERNAME/Trusta.git
```

### Open the Project

Open the project using Android Studio.

### Configure Firebase

Add your Firebase configuration file:

```text
google-services.json
```

inside the app module.

### Sync Dependencies

Allow Android Studio to download and sync all required dependencies.

### Run the Application

Run the project on an Android device or emulator.

---

## Screenshots

Screenshots will be added after the final UI version is completed.

---

## Future Improvements

- Advanced Health Analytics
- PDF Report Export
- Smart Health Recommendations
- Enhanced AI Features
- Improved Cloud Synchronization
- Expanded Health Monitoring Capabilities

---

## Project Status

🚧 Active Development

Trusta is currently being enhanced with additional dashboard, reporting, and user experience improvements as part of the graduation project development process.

---

## License

This project was developed for educational and graduation project purposes.
