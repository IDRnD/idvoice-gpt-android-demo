Android IDVoiceGPT
===========================================

**The application code is compatible with VoiceSDK 3.12.1.**

This application is intended to demonstrate IDVoice® integration with ChatGPT voice controls to authenticate users while they’re speaking to the chatbot.

The solution applies frictionless voice biometrics to secure access to a speech-enabled ChatGPT chat session. As applications for verbal chatbots proliferate, securing access from unauthorized users is becoming an increasingly common requirement. IDVoice performs speaker verification in the background while the user is speaking, avoiding added friction for authorized users.

Before you begin
----------------

- Get a IDVoice SDK distribution from ID R&D
- Create a [new Google Cloud Platform project](https://cloud.google.com/speech-to-text/docs/before-you-begin) from the Cloud Console or use an existing one. Once you have the credentials.json downloaded you are ready for installation steps.

Installation
------------

- Clone the repository.
- Create/edit local.properties in project´s root and add:
  - ID_VOICE_LICENSE="Your IDVoice SDK license" and
  - OPEN_AI_KEY="Your OpenAI key".
- Copy credentials.json file downloaded from Google Cloud Platform console to app/src/main/res/raw directory, create it if doesn't exist.
- Copy java/voicesdk-aar-full-release.aar from Android IDVoice SDK package received from ID R&D to app/libs folder.

Usage
-----

- Import the project through Android Studio.
- Connect a device/emulator with API level 21 as minimum. 
- Enable microphone access from running device if it is an emulator. 
- Run the project.

Documentation
-------------

- [IDVoice introduction](https://docs.idrnd.net/voice/) for basics about our technology.
- [IDVoice quick start installation](https://docs.idrnd.net/voice/sdk/quick-start-installation/) for installation details.
- [Quick start speaker verification guide](https://docs.idrnd.net/voice/sdk/quick-start-idvoice/) for IDVoice speaker verification details.
- [Quick start voice liveness](https://docs.idrnd.net/voice/sdk/quick-start-idlive-voice/) for IDVoice voice liveness details.
- [Google Cloud Platform example](https://github.com/GoogleCloudPlatform/android-docs-samples) was used as base for speech recognition

Developer tips
--------------

- See [`IDVoiceManager.kt`](app/src/main/java/net/idrnd/idvoicegpt/idrnd/IDVoiceManager.kt) for user enrollment, liveness and speaker verifications