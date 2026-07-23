<div align="center">

# Wren

**A pocket-sized AI IDE for real development work.**

<p>
  <a href="https://github.com/kraqinc/Wren/releases">
    <img src="https://img.shields.io/github/v/release/kraqinc/Wren?include_prereleases&style=for-the-badge" alt="Latest release" />
  </a>
  <a href="./LICENSE">
    <img src="https://img.shields.io/github/license/kraqinc/Wren?style=for-the-badge" alt="License" />
  </a>
  <a href="./.github/workflows/build.yml">
    <img src="https://img.shields.io/github/actions/workflow/status/kraqinc/Wren/build.yml?branch=main&style=for-the-badge" alt="Build" />
  </a>
</p>

<p>
  <img src="https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin" />
  <img src="https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose" />
  <img src="https://img.shields.io/badge/Material%203-6750A4?style=for-the-badge&logo=materialdesign&logoColor=white" alt="Material 3" />
  <img src="https://img.shields.io/badge/Next.js-000000?style=for-the-badge&logo=nextdotjs&logoColor=white" alt="Next.js" />
  <img src="https://img.shields.io/badge/Prisma-2D3748?style=for-the-badge&logo=prisma" alt="Prisma" />
</p>

</div>

---

## Overview

Wren is a native Android IDE with an integrated AI agent, project workspace, file explorer, authentication flow, and a companion web login.

The repository is organized so Android, backend, and web can evolve independently while still sharing the same account and project ecosystem.

---

## Download

The recommended way to install Wren is through **GitHub Releases**.

- Download the latest release from the Releases page
- Install the Android build from the release assets
- Use the web login when you need browser-based access

---

## Repository Layout

```text
Wren/
├── android/            Native Android application
├── backend/            Next.js API + Prisma + MySQL
├── wren-login-web/     Static login page deployed on Vercel
├── .github/workflows/   CI pipelines
├── LICENSE             MIT license
└── README.md           Project overview
```

---

## Features

- Native Android app built with Kotlin, Jetpack Compose, and Material 3
- Real project workspace and file explorer
- AI-assisted development screens
- Authentication and session handling
- Backend API powered by Next.js and Prisma
- Web login connected to the backend API
- GitHub Actions CI for Android and backend builds

---

## Backend

The backend lives in `backend/` and uses:

- Next.js 14
- TypeScript
- Prisma 5
- MySQL
- `jose` for JWTs

Environment variables are documented in `backend/.env.example`.

---

## Web Login

The web login lives in `wren-login-web/` and is a static HTML/CSS/JS app.

It talks to the backend via `window.WREN_API_BASE`. Copy `wren-login-web/config.example.js` to `config.js` and set your backend URL before deploying.

---

## Android

Open `android/` in Android Studio to build the app.

The Android project includes:

- Compose UI
- Session management
- Storage permission flow
- Workspace, credits, AI, and owner screens

---

## CI / GitHub Actions

`.github/workflows/build.yml` runs:

- Android build (`./gradlew assembleDebug`)
- Backend build (`npm run build`)

---

## Getting Started

```bash
git clone https://github.com/kraqinc/Wren.git
cd Wren
```

Then open the platform you need:

- `android/` for the Android app
- `backend/` for the API
- `wren-login-web/` for the web login

---

## License

Released under the MIT License. See `LICENSE` for details.

</div>
