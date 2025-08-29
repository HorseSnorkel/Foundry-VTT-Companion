# FoundryVTT Companion (Android) — Hybrid Integration

This package now defaults to a hybrid model:
- Actor sheet and the rest of Foundry run inside an in-app WebView (to preserve system/module compatibility).
- Chat uses a native UI for better typing, accessibility, and OS integration.
- A lightweight JavaScript bridge wires the WebView and native chat together:
  - Sending: Native sends chat into the page (client-side) using `ChatMessage.create`.
  - Receiving: The page notifies Android of chat events using Foundry `Hooks` and a JS interface.
- Optionally, you can deploy a small companion module on your Foundry server to expose a limited API (e.g., push notifications, actor listing). The app will be ready to integrate with such APIs.

What you get
- Adaptive UI for phones and tablets (Material 3 Window Size Classes).
- Three modes:
  - Hybrid (Recommended): WebView sheets + native chat via JS bridge.
  - WebView: Everything in WebView.
  - Native (Experimental): Placeholder for a companion API (ships with mock data).

Quick start
1. Prerequisites:
   - Foundry VTT served over HTTPS (recommended for modern Android WebView and secure cookies).
   - Android Studio Koala+ and a device (minSdk 24).
2. Build and run:
   - Open the project in Android Studio, run on your device.
3. Connect:
   - Choose "Hybrid (Recommended)".
   - Enter your Foundry server URL (e.g., `https://your-foundry.example.com`). You can put a direct world URL, or just the base.
   - Tap Connect. The WebView opens your server; sign in and open your world as usual.
4. Chat:
   - Use the native Chat tab to send messages; messages will also appear in the Foundry chat log.
   - Incoming chat messages are mirrored into the native Chat tab using the JS bridge.

Deployment details

A. Secure your Foundry deployment (recommended for any mobile access)
- Use HTTPS with a valid certificate.
- If behind a reverse proxy (e.g., Nginx, Caddy, Traefik), ensure:
  - HTTP/2 enabled.
  - `X-Forwarded-*` headers set so Foundry sees the correct origin/scheme.
  - WebSocket upgrade rules are configured (Foundry uses WebSockets/Socket.IO).
- Avoid cross-domain redirections during login if possible; keep WebView on the same origin for cookies/session.

B. Hybrid mode internals (no server changes required)
- The app injects a safe bootstrap script into the WebView that:
  - Exposes `window.FVTT_ANDROID.sendChat(text)` to send messages via `ChatMessage.create({content: text})`.
  - Subscribes to Foundry `Hooks.on('createChatMessage', ...)` and `Hooks.on('renderChatMessage', ...)` to forward chat events to Android via the `Android.onChatMessage(author, content, timestamp)` interface.
- Limitations:
  - Requires the Foundry client to be running in the WebView (user logged in and the world loaded).
  - Messages sent while the world is not active in WebView won't be delivered (no background chat).
  - Some systems/modules might override chat rendering; the `createChatMessage` hook is the most reliable signal.

C. Optional: Deploy a companion module (advanced)
- Purpose:
  - Provide a narrow, versioned API for mobile features such as push notifications, actor summaries, and chat webhooks.
  - Reduce coupling to Foundry internals across versions or module stacks.
- Outline:
  - Create a Foundry module (e.g., `fvtt-companion-api`) with a `module.json`.
  - On server startup, register a small Express router under `/modules/fvtt-companion-api/v1/...` and (optionally) a WebSocket/Socket.IO namespace for chat events.
  - Authenticate requests using short-lived tokens scoped to a user and world. Never expose admin endpoints.
  - Emit chat events from the server side when `ChatMessage` documents are created (server-level hook).
- Integration steps in the app:
  - Wire your Retrofit interface in `network/FoundryService.kt` to the routes you exposed.
  - Replace the TODOs in `SessionViewModel.connect` to obtain a token and bootstrap data (actors, selected actor).
  - In `HomeScreen`, if the companion API is reachable, you can prefer it for chat send/receive; otherwise the JS bridge works as a fallback.

D. Reverse proxy examples

Nginx minimal (snippet):
```
location / {
    proxy_pass         http://127.0.0.1:30000; # your Foundry internal address
    proxy_http_version 1.1;
    proxy_set_header   Upgrade $http_upgrade;
    proxy_set_header   Connection "upgrade";
    proxy_set_header   Host $host;
    proxy_set_header   X-Forwarded-Proto $scheme;
    proxy_set_header   X-Forwarded-For $proxy_add_x_forwarded_for;
}
```

Caddy minimal (snippet):
```
your-foundry.example.com {
    reverse_proxy 127.0.0.1:30000
}
```

E. Permissions and hardening on Android
- WebView is confined to your Foundry origin; avoid injecting third-party scripts.
- The app enables JavaScript and DOM Storage only; file access and geolocation are disabled.
- If your device policy requires it, you can further restrict mixed content and 3rd-party cookies.

FAQ
- Do I need to install a server module? No. Hybrid mode works without server changes. A module is only needed if you want push notifications, background sync, or stable native APIs.
- How do I load directly into a world? Paste the direct world URL into Server URL. Otherwise, load the base URL and choose your world inside Foundry.
- Why doesn’t native chat show old history? The WebView bridge forwards new events. You can enhance this by adding a companion API endpoint to page through history.

Where to plug in your own server API
- `app/src/main/java/com/example/foundryvttcompanion/network/FoundryService.kt`: Define your REST/WebSocket surface.
- `app/src/main/java/com/example/foundryvttcompanion/viewmodel/SessionViewModel.kt`: Replace mock data and wire authentication/bootstrap.
- `HomeScreen.kt`: The `webController.sendChat(text)` call can be made conditional on API availability (feature flag).

Security checklist
- HTTPS only.
- Short-lived tokens if using a companion API; store them in EncryptedSharedPreferences or Android Keystore-backed storage.
- Validate and sanitize any data that crosses the JS bridge; the provided bridge only sends plain text content.
