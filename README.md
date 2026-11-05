# Max

**Status: experimental test build (v1.0.0-test).** Unit tests and APK structure are verified in the build environment. On-screen UI rendering and device behavior (voice, automation, AI action success) are NOT verified yet - they need a real phone. Treat this as a test build, not a polished final.

Max is a native Android personal AI assistant: JARVIS-style butler persona, natural Hinglish/English conversation, real voice in/out, personality modes, editable memory, immediate stop control, live weather, a spoken morning briefing, and authorized phone control through real Android APIs.

Built with Kotlin + Jetpack Compose. Talks directly to the Gemini API with **your own API key**, stored encrypted on-device. No hosted server. No API key anywhere in this repo or the APK.

Max is his own app. He does not replace or change Ruby, ROXXY or ARIXY.

## Download

Get the APK from [Releases](../../releases) (Max-1.0.apk, debug-signed). Enable "install unknown apps" when Android asks. If Play Protect warns about an unknown developer, that is expected for a self-built debug APK - install only if you got it from this repo.

## Setup (2 minutes)

1. Install the APK and open Max.
2. **Settings tab**: paste your free Gemini key from https://aistudio.google.com/apikey and tap Save. Without a key, local commands still work: time, date, battery, torch, volume, weather, morning briefing, opening apps.
3. **Settings tab**: enter your name (optional - used for greetings).
4. **Control tab**: grant what you actually want to use:
   - **Accessibility** = screen reading + tap/scroll/type automation
   - **Microphone** = voice commands
   - **Camera** = photo vision in Chat
   - **Contacts** = call/WhatsApp by contact name
   - **Location** = weather + morning briefing
   - **Notification access** = message reading + Ghost auto-reply
   - **Overlay** = floating orb on top of other apps
   - **Write settings / DND** = brightness and Do-Not-Disturb control

## The five tabs

- **Home** - Max's waveform face, the animated orb (mirrors the real assistant state), status, quick actions (editable in Settings), mic + text input, and a persistent STOP button whenever anything is active.
- **Chat** - full conversation with context and follow-ups, voice input, camera vision, tap a bubble to copy, Stop and Clear controls.
- **Control** - supported actions, live permission status, and the task log: every automation step (current, done, failed, waiting confirmation).
- **Memory** - long-term memories: view, add, edit, delete, delete-all, or disable entirely. All on-device.
- **Settings** - name, API key, personality mode, voice speed/pitch, wake phrase, floating orb, Ghost auto-reply, quick actions.

## Personality modes

JARVIS Butler (default), Friendly, Sweet, Mischief, Anime-Inspired, Professional, Serious Support. The mode changes the actual system prompt and responses. Max has **no** romantic/jealous/possessive companion mode - that persona style in reference reels is scripted fiction, and it is not something Max pretends at.

## Weather and morning briefing

Keyless and free: weather comes from Open-Meteo (no API key, no account) using your approximate location, only at the moment you ask. Location is never stored or sent anywhere else. "Good morning briefing" (or the quick action) speaks the day, time, battery and weather. If location permission or internet is missing, Max says so instead of inventing weather.

## Stop control

Say or type "Max stop", "ruk jao", "bas band karo", "cancel", "stop speaking" - or tap STOP. It interrupts speech, cancels listening and pending automation steps, shows Stopped, and nothing resumes by itself. Stop matching runs locally before anything reaches the AI.

## Honest feature status

**Tested in this build (automated):**
- 18/18 unit tests pass: response JSON parsing (clean, fenced, plain-text), keyless local router (time/date/battery/torch/apps), weather-query detection, stop-command matching (including false-positive guards), personality prompt injection (name, memories, mode, butler "Sir" style), memory encode/decode.
- APK verified: package `com.rout.max`, label Max, adaptive + legacy launcher icons included, debug-signed.

**NOT verified (no device/emulator in the build environment):**
- On-screen rendering of the five tabs, face art, orb animations, dialogs and task log (compile-verified only)
- Everything below plus device behavior generally

**Implemented, needs your phone to verify (grants + real device):**
- Voice recognition/TTS (Android SpeechRecognizer + TTS, en-IN)
- Live weather fetch and morning briefing (needs location permission + internet)
- Accessibility screen control (tap, scroll, type, screenshot, UI-tree reading)
- Vision re-query: screen capture or camera photo analyzed by Gemini
- Alarms, timers, media control, volume, torch, Wi-Fi/Bluetooth panels, DND
- Calls and WhatsApp sends - always behind an in-app confirmation dialog
- Floating orb overlay + experimental "Hey Max" wake phrase (uses battery; true always-on wake word is NOT implemented and the app says so)
- Ghost auto-reply from notifications (only when the notification has a reply action, 60s per-sender limit)

**Deliberately NOT in Max (marketing claims in reference reels, not feasible or not honest in v1):**
- True always-on wake word (the "Hey Max" loop is best-effort experimental and says so)
- PC/laptop control, smart-home/light control, face authentication
- Image or 3D generation on command
- Romantic, jealous or possessive companion persona
- Any fake "done" - Max reports what actually ran in the task log, and says when something is not possible
