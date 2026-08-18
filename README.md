# Real Fit — Android (Kotlin)

Original native Android client for **Real Fit**, a multi-tenant management
platform for gyms and training studios.

> **Archived.** This is the first-generation Kotlin codebase. Real Fit was later
> rebuilt in React Native + TypeScript so iOS and Android could share one
> codebase. The app that ships today comes from that rewrite; this repository is
> kept as a reference for the original native implementation.

## The product

Real Fit is live on the App Store and running in production at a gym.

- **App Store** — https://apps.apple.com/ar/app/real-fit/id6756183596
- **Product site** — https://realfitness.web.app/

## What this codebase covers

Written in Kotlin against a Firebase backend (Firestore, Auth, Storage, Cloud
Messaging), it implements the core of the platform:

- **Multi-tenant** — one app serving many gyms, each with its own branding
- **Roles** — separate permissions for administrator, seller and manager
- **Bookings** — live class capacity, coach assignment, session lobby
- **Payments** — MercadoPago, auto-debit, cash, transfer, credit wallet
- **Attendance** — check-in by QR, NFC card or PIN
- **Gamification** — ranks, avatars, results wall, tournaments
- **Billing plans** — standard and partner tiers with automated pricing

Backend configuration (`google-services.json`) is intentionally not committed.

## Built by

[Forja](https://estudio-forja.vercel.app) — software studio, Argentina.
