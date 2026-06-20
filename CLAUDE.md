# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug

# Run unit tests (JVM, no device needed)
./gradlew test

# Run a single test class
./gradlew test --tests "com.example.jardullicalculadora.CalculoUtilTest"

# Run instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest

# Lint check
./gradlew lintDebug
```

Reports: `app/build/reports/tests/testDebugUnitTest/` (unit), `app/build/reports/lint-results-debug.html` (lint).

## Project Overview

Agricultural Android calculator for tractor weight distribution, ballast, wheels, and PDF report generation. Includes Supabase-based auth, subscription plans, and balance history.

**Stack:** Java 11, Android SDK 35 (min 24), Gradle 8.9.0 Kotlin DSL, iText 7.2.5 (PDF), OkHttp 4.12.0, Supabase (Auth + REST), EncryptedSharedPreferences (AES256-GCM), SQLite.

## Architecture

### Calculation Flow (Activity Chain)

Data flows linearly via `DadosJardulli` (Parcelable) passed through Intent extras (`"DADOS_JARDULLI"`):

```
MainActivity (hub)
  → TratorOperacaoActivity   (série, modelo, operação, engate, peso operador)
    → RodadosActivity         (rodados dianteiro/traseiro, eixos, grupos)
      → LastroLiquidoActivity (lastro líquido com multiplicador %)
        → PesoFrontalActivity (pesos frontais, traseiros, cabine)
          → RelatorioFinalActivity (cálculos finais + PDF)
```

Each activity accumulates data into the same `DadosJardulli` object and passes it forward. Receivers validate with `finish()` fallback if data is null.

### Account & Subscription Flow

```
MainActivity → AuthActivity (login/signup Supabase)
            → PerfilActivity (nome, CPF/CNPJ Mod 11, telefone)
            → PlanosActivity (plan selection)
            → HistoricoActivity (saved balances via RecyclerView)
            → SuporteActivity (email/WhatsApp)
```

### Key Architectural Decisions

- **No DI framework** — dependencies instantiated directly in activities
- **Callback-based async** — `SupabaseClient` uses `SupabaseCallback` interface, not LiveData/Coroutines. All UI updates must be wrapped in `runOnUiThread()`
- **Series data mostly hardcoded** — only JD 8R is in SQLite (`DBHelper`); the other 5 active series use parallel arrays in `TratorOperacaoActivity`. Active series index mapping: `indiceParaOriginal = {0, 1, 2, 3, 7, 8}`
- **Offline-first with cloud sync** — balances saved locally first, then async POST to Supabase
- **Plan cache TTL: 1 hour** — falls to TESTDRIVE if expired without sync. `getBalancosMesAtual()` uses `Math.max(cloudCount, localCount)` to prevent bypass

### Two Lastro Líquido Formulas

- Formula 1: `(valor × multiplicador) / 75 × 2`
- Formula 2: `(valor × multiplicador) / 40 × 2`
- Selection depends on series AND direction (dianteiro/traseiro) — see `usarCalculo1()` in `LastroLiquidoActivity`

### Key Classes

| Class | Role |
|-------|------|
| `DadosJardulli` | Parcelable DTO accumulating all data across the flow |
| `SupabaseClient` | Singleton OkHttp client for Supabase REST/Auth |
| `PlanoManager` | Plan cache + session via EncryptedSharedPreferences |
| `DBHelper` | SQLite v3 (`jardulli.db`) — tratores, rodados, histórico |
| `CalculoUtil` | Pure calculation functions (testable, no Android deps) |
| `DocumentValidator` | CPF/CNPJ Mod 11 validation |
| `Plano` | Enum: TESTDRIVE, FLEXIVEL, PROFISSIONAL, FROTISTA |
| `RelatorioFinalActivity` | Final weight calculations + iText PDF generation |

## Conventions

- **Language:** Portuguese (BR) for UI, variables, comments, logs
- **Layout files:** `activity_*.xml`
- **XML IDs:** camelCase with prefixes: `btn`, `spinner`, `text`/`txt`, `edit`
- **All activities:** portrait-locked
- **Data passing:** `Intent.putExtra("DADOS_JARDULLI", dados)` / `getParcelableExtra("DADOS_JARDULLI")`

## Configuration

Supabase credentials go in `local.properties` (not committed):

```properties
SUPABASE_URL=https://<project>.supabase.co
SUPABASE_ANON_KEY=<anon-public-key>
WHATSAPP_SUPORTE=<phone-number>
```

These are injected into `BuildConfig` via `app/build.gradle.kts`. In CI, they come from GitHub Actions secrets.

## Security Notes

- Tokens stored in EncryptedSharedPreferences; `jardulli_plano_prefs` excluded from auto-backup and device transfer via `backup_rules.xml` / `data_extraction_rules.xml`
- If EncryptedSharedPreferences creation fails (corrupted KeyStore), falls back to plain SharedPreferences
- Anon key is public by design — all data safety depends on Supabase RLS policies

## CI

GitHub Actions (`.github/workflows/android.yml`): lint → unit tests → assemble debug APK. Uses JDK 17 in CI (local dev uses JDK 23+).
