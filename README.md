# 🚀 InShift – Smart Workforce Attendance & Presence Intelligence System

## 📌 Project Description
InShift is an advanced workforce management system designed to improve employee attendance tracking, shift management, and workplace accountability using biometric authentication, location verification, and AI-powered insights.

The system integrates secure authentication (Passkeys/WebAuthn), real-time notifications, and presence intelligence analysis to detect suspicious behaviors, enforce attendance rules, and provide actionable insights for administrators.

---

## 🎯 Features

### 👨‍💼 Employee Features
- Secure login with email & password
- Passkey (WebAuthn) biometric verification
- Attendance check-in:
  - Mobile biometric + GPS
  - Web manual check-in (with location)
- View shifts (calendar & list)
- Shift management:
  - Request reschedule
  - Swap shifts
  - Pick open shifts
- Overtime (OT):
  - Start/stop timer
  - Submit requests
  - Track status
- Real-time notifications (FCM)
- Attendance & OT analytics

---

### 🧑‍💻 Admin Features
- Unified Attendance + Intelligence Dashboard
- Suspicious behavior detection
- Presence analytics (location & lateness trends)
- AI-powered risk insights
- Send notifications to employees
- Device trust management
- Approve/reject OT and devices
- Daily presence monitoring

---

### 🤖 AI & Intelligence
- AI-generated risk summaries
- Behavior pattern detection:
  - Frequent lateness
  - Location anomalies
  - Missed presence checks
- AI-based recommendations

---

## 🏗️ Tech Stack

### Backend
- Spring Boot (Custom JWT authentication)
- WebAuthn (Passkeys)
- REST APIs
- Firebase Cloud Messaging (FCM)
- OpenRouter API (AI integration)

### Frontend
- React.js
- Service Worker (Notifications)
- Analytics Dashboard

---

## 🔄 Core Workflows

### ✅ Attendance Flow
1. User logs in (email + password)
2. Device is verified (trusted device)
3. User completes biometric verification
4. Location is captured
5. Attendance is recorded

---

### 📡 Presence Check Flow
1. System generates daily presence plan
2. Notification sent via FCM
3. User responds via device/mobile
4. System evaluates response (time + location)
5. AI analyzes behavior

---

### 🔐 Device Trust Flow
- Device auto-enrollment
- Admin approval required
- Passkey tied to device fingerprint
- Only trusted devices allowed

---


## ⚙️ Setup Instructions

### 🔧 Backend
```bash
git clone https://github.com/your-repo/inshift-backend.git
cd inshift-backend

# Configure application.properties

mvn clean install
mvn spring-boot:run
