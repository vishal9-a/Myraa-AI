# Myraa — Google Play Store Listing & Compliance Guide

## 1. Artifact to Upload
- **Target File**: `app/build/outputs/bundle/playRelease/app-play-release.aab`
- **Flavor**: `play`
- **Application ID**: `com.myraa.assistant`
- **Namespace**: `com.myraa.assistant`

---

## 2. Store Listing Copy

### App Title (Max 30 characters)
Myraa: Voice & Website Forge

### Short Description (Max 80 characters)
Voice-first AI assistant in Hindi & English with 3D visuals & Website Forge.

### Full Description
Meet **Myraa**, your real-time voice-first personal AI companion designed for Android. 

Whether you converse in Hindi, Hinglish, or English, Myraa understands you seamlessly with full-duplex conversational audio, interactive 3D visuals, and powerful on-device capabilities.

🌟 **HIGHLIGHT FEATURES**
• **Real-Time Gemini Live Voice**: Talk to Myraa naturally, interrupt anytime, and hear instant spoken responses.
• **Offline Hands-Free Mode**: Say "Hey Myraa" or "Myraa" anytime—even when your screen is locked—to place calls, send messages, toggle flashlights, and manage phone settings without needing an active internet connection.
• **10 Selectable Personalities**: Customize your companion's tone, including Friendly, Teacher, Developer, Professional, and theatrical Bollywood Nautanki.
• **3D Website Forge**: Speak a single command like *"Myraa, ek cafe ki website banao"* and watch a complete, animated 3D website stream and render live in front of your eyes.
• **Privacy First**: Audio is analyzed in RAM only. No voice recordings or private conversations are ever stored on disk or uploaded without permission.

---

## 3. Data Safety Declarations
- **Audio Data**: Processed transiently for real-time speech recognition and wake word detection. Not shared with third parties.
- **Contacts**: Queried strictly on-device to resolve contact names during user-initiated calls and messages.
- **Personal Info**: Only user-entered settings (such as chosen nickname and personality) are retained in private on-device storage.

---

## 4. Permission Justifications
- **FOREGROUND_SERVICE_MICROPHONE**: Required for continuous real-time voice interaction while keeping the app in background and hands-free wake word listening.
- **READ_CONTACTS**: Required to resolve friend and family contact names for voice commands like "Call Rahul".
- **CALL_PHONE**: Allows the assistant to initiate outgoing calls directly upon explicit voice command.

---

## 5. Submission Checklist
- [x] Tested build variant: `assemblePlayRelease` / `bundlePlayRelease`
- [x] Zero accessibility permissions in Play edition manifest
- [x] No SEND_SMS permissions declared (standard Intent composers used)
- [x] M3 edge-to-edge support with minimum 48dp touch targets
- [x] Custom high-resolution adaptive launcher icon configured
