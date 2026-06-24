# Trusta

## Smart Health Reminder & Wellness Assistant

Trusta is an Android healthcare application designed to help users manage their daily health routines through smart reminders, alarms, progress tracking, and AI-powered assistance.

The application aims to improve treatment adherence, reduce missed reminders, and provide users with an organized and user-friendly health management experience.

---

## Features

### Authentication
- User Registration
- Secure Login
- Google Sign-In
- Password Recovery

### Reminder Management
- Create, Edit, and Delete Reminders
- Reminder Status Tracking
- Daily Reminder Organization
- Local Storage using Room Database

### Alarm System
- Create and Manage Alarms
- Alarm Scheduling
- Alarm Ringing Activity
- Background Alarm Handling
- Enable / Disable Alarms

### Search System
- Real-Time Search
- Reminder Search
- Alarm Search
- Search History
- Filtered Search Results

### Daily Planning
- Today Plan Overview
- Upcoming Activities
- Daily Health Routine Management

### Reports & Tracking
- Activity Reports
- Progress Tracking
- Reminder Monitoring

### AI Assistant
- AI-Powered Health Guidance
- Medication Information Assistance
- General Health Support

### User Experience
- Welcome / Onboarding Screens
- Empty State Handling
- Light Theme
- Dark Theme
- System Theme Support
- Modern Material Design UI

---

## Tech Stack

### Development
- Kotlin
- Android Studio

### Architecture
- MVVM (Model-View-ViewModel)

### Database
- Room Database

### Authentication
- Firebase Authentication

### Networking
- Retrofit

### UI & Android Components
- XML Layouts
- Material Design Components
- Android Jetpack Libraries
- RecyclerView
- Navigation Components
- ViewBinding

---

## Architecture Overview

The application follows the MVVM architecture pattern to ensure maintainable, scalable, and organized code.

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
- DAOs
- Room Database
- Firebase Authentication
- Retrofit API Services

---

## Project Structure

## Project Structure

```text
app
└── src
    ├── features
    │   ├── activity
    │   ├── fragment
    │   ├── plan
    │   ├── schedule
    │   ├── details
    │   ├── settings
    │   ├── profileinfo
    │   └── reports
    │
    ├── data
    │   ├── dao
    │   ├── entities
    │   ├── repository
    │   └── database
    │
    ├── services
    ├── receivers
    ├── api
    ├── utils
    └── viewmodels
```
---

## Installation

### 1. Clone the Repository

```bash
git clone https://github.com/your-username/Trusta.git
```

### 2. Open the Project

Open the project using Android Studio.

### 3. Sync Dependencies

Allow Android Studio to download and sync all required dependencies.

### 4. Configure Firebase

Add your Firebase configuration file (`google-services.json`) if required.

### 5. Run the Application

Run the application on an Android device or emulator.

---

## Screenshots

Screenshots will be added after the final UI version is completed.

---

## Future Improvements

- Advanced Health Analytics
- PDF Report Export
- Enhanced Progress Tracking
- Smart Recommendations
- Improved AI Features
- Expanded Health Monitoring Capabilities

---

## Project Status

🚧 Active Development

Trusta is currently being enhanced with additional dashboard, reporting, and user experience improvements as part of the graduation project development process.
