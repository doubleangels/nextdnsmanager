# NextDNS Manager Rewrite Plan

Status: **Phase 1 and Phase 2 complete, awaiting approval. No implementation code has been written.**
Date: 2026-11-20. Branch: `native-api-app`.

This document covers four things.

1. Audit of the current native app (commit `54d7065` and follow-ups).
2. Feature parity analysis against my.nextdns.io.
3. Modern Android standards and a cohesive Material 3 design system.
4. Architecture, security model and the milestone roadmap.

---

## 1. Audit of the current app

The branch already replaced the WebView wrapper with a Kotlin, Jetpack Compose app on `api.nextdns.io`. The findings below are about that native code. It has no automated tests.

### 1.1 Root causes by severity

#### Critical

| # | Problem | Where | Consequence |
|---|---|---|---|
| C1 | The filtered uncaught exception handler returns without calling the default handler for "ignored" exceptions. The ignore list matches loose substrings such as `cancel` and `timeout`. | `sentry/SentryManager.java` `installFilteredUncaughtExceptionHandler` | A matching exception on the main thread kills the main thread and leaves the process alive, so the app freezes instead of crashing. |
| C2 | If Keystore or Tink fails, storage falls back to plaintext and the API key is written in clear text. The plaintext copy survives later successful launches because migration is already marked complete. | `sharedpreferences/SharedPreferencesManager.java` `handleEncryptionFailure` | A full-account API key sits on disk in plaintext while onboarding says it is stored encrypted. |
| C3 | When app lock is on and the user removes the device screen lock, `unlock()` returns silently because `canAuthenticate()` is false. | `MainActivity.kt` `unlock` | Permanent lockout. Clearing app data is the only way out. |

#### High, correctness and races

| # | Problem | Where |
|---|---|---|
| H1 | Every method is retried on 429 and 5xx, including POST. `Retry-After` is ignored. Adding a rewrite or domain during a slow 5xx can create duplicates. | `api/NextDnsApi.kt` `execute` |
| H2 | Paging race. `reload()` cancels the old job, whose `finally { loading = false }` runs after the new job set `loading = true`. `loadMore` then fires a second fetch with `cursor = null` and rows are duplicated. | `ui/logs/LogsViewModel.kt` |
| H3 | The live log stream runs in `viewModelScope` with no lifecycle awareness. It keeps using data and battery in the background, never reconnects after a drop and never de-duplicates against the page it continues from. | `ui/logs/LogsViewModel.kt` |
| H4 | Optimistic rollback restores a whole snapshot. If add A fails after add B succeeded, B disappears from the UI although the server has it. Two quick toggles send concurrent PATCH calls that can arrive out of order. | `data/EntryListController.kt`, `ui/profile/ProfileViewModel.kt` |
| H5 | Sign-in stores the candidate key before validating it, so concurrent requests use an unverified key. A rotation cancels the scope, the `CancellationException` is caught as a normal error, the old key is restored and the cancellation is reported to Sentry. | `ui/SessionViewModel.kt` `signIn` |
| H6 | App lock shows one frame of the dashboard on cold start because `locked` starts false and the lock is evaluated in a `LaunchedEffect` after first composition. The timeout counts from the last unlock instead of from going to the background, and uses the wall clock. | `MainActivity.kt`, `ui/NextDnsApp.kt` |
| H7 | A revoked key produces a separate error on every screen. Nothing routes the user back to sign-in. | all ViewModels |

#### High, security and privacy

| # | Problem |
|---|---|
| S1 | OkHttp follows redirects and only strips `Authorization` on a cross-host redirect, never `X-Api-Key`. A redirect to another host would leak the key. |
| S2 | `addPathSegments("profiles/$id/${list}/$entryId")` resolves `..` and splits on `/`. An entry ID such as `../../x` retargets the request. |
| S3 | The Sentry Gradle plugin runs with defaults, which bytecode-instruments OkHttp, file I/O and Compose. HTTP breadcrumbs then contain URLs like `logs?search=<domain>`, which is browsing history. View hierarchy, root check, extra context and all auto breadcrumbs are on. |
| S4 | Firebase Messaging initialises and subscribes to a topic on every sign-in without consent. The notification permission is requested with no rationale. |
| S5 | `setRecentsScreenshotEnabled` only exists on API 33+. On API 32 the dashboard and query log appear in Recents. |

#### Medium

- No input validation. Pasted URLs, IDN names, trailing dots, rewrite answers and profile name lengths are not checked. Server domains are shown raw, so homoglyphs and bidi control characters are possible.
- Errors are untyped. Sign-in returns the magic string `"invalid_key"`. User-facing errors are hardcoded English strings. Unknown exceptions show their raw message.
- Unbounded `response.body.string()` and unbounded SSE line length.
- Overview fires 13 parallel requests per refresh with retry backoff on top, which invites 429 storms.
- Every request decrypts the key from EncryptedSharedPreferences on the main thread.
- Unneeded permissions: `WAKE_LOCK`, `ACCESS_WIFI_STATE`, `ACCESS_NOTIFICATION_POLICY`, and the `http` `<queries>` entry.
- Copied domains are not marked sensitive (`ClipDescription.EXTRA_IS_SENSITIVE`).
- The connection check against `test.nextdns.io` ignores the HTTP status, so HTML responses become `SerializationException` reports in Sentry.
- Accessibility gaps. Log status is conveyed by colour and icon only, charts have no semantics, and the log list has no stable keys.
- Blocklist, native tracker, service and category catalogs are hardcoded in `ProfileScreen.kt` and go stale. TLDs are free text.

#### Process

- Zero tests. CI runs `./gradlew test` against nothing.
- `MissingTranslation` lint was downgraded so English-only strings pass.
- `gradle.properties` opts out of AGP 9 defaults (`android.builtInKotlin=false`, `android.newDsl=false`, `android.r8.strictFullModeForKeepRules=false`, legacy configuration cache flag).

### 1.2 Dependency audit

| Current | Replacement | Reason |
|---|---|---|
| `androidx.security:security-crypto` (EncryptedSharedPreferences and Tink) | Android Keystore AES-256-GCM key, ciphertext in `android.util.AtomicFile` under `noBackupFilesDir` | Deprecated library, large transitive Tink dependency, and the cause of the plaintext fallback |
| `androidx.biometric` and `androidx.appcompat` | `android.hardware.biometrics.BiometricPrompt` with `BIOMETRIC_STRONG or DEVICE_CREDENTIAL` | minSdk 32 supports the API 30+ authenticator flags natively. AppCompat exists only for biometrics and locale storage |
| `androidx.core:core-splashscreen` | Platform `SplashScreen` API (31+) with an `OnPreDrawListener` hold | Native on minSdk 32 |
| AppCompat locale service | `android.app.LocaleManager` (33+) | API 32 falls back to the system language |
| `material-icons-extended` | About 30 Material Symbols vector drawables | Huge artifact and no longer maintained |
| OkHttp and Okio | `javax.net.ssl.HttpsURLConnection` (platform TLS through Conscrypt) | Small surface (JSON REST plus one SSE stream). See decision D1 |
| `kotlinx-serialization` JSON tree walking | Keep the library, switch to typed `@Serializable` DTOs | First-party Kotlin, compile-time, no reflection |
| Sentry plugin defaults | Keep Sentry, disable `tracingInstrumentation` and `autoInstallation` | Opt-in reporting with nothing beyond the exception itself |

### 1.3 Unhandled edge cases

- Screen lock removed while app lock is on.
- Keystore key invalidated or keyset corrupted.
- Process death or rotation during sign-in.
- Key revoked mid-session.
- 429 with `Retry-After`.
- Offline start (the user waits for a 15 second timeout).
- Stream disconnect, network change or Doze.
- Long live sessions passing the 2,000 entry cap while paging continues.
- Timezone change while charts are visible.
- Pasted URLs, IDN, trailing dots and wildcards in domain input.
- Selected profile deleted on the server.
- Server strings containing control characters.
- List limits (5,000 denylist or allowlist entries, 500 rewrites, 50 shared users) returned as `listLimitExceeded`.

---

## 2. Feature parity with my.nextdns.io

### 2.1 Method

- Downloaded the public dashboard bundle `https://my.nextdns.io/static/js/main.a1e3cbc7.js` (about 1 MB) on 2026-11-20 and extracted every route, menu, API call and translation key.
- Read the public API documentation at `https://nextdns.github.io/api/`.
- Probed the catalog endpoints the dashboard uses without authentication.

Confirmed public, no auth needed:

| Endpoint | Content |
|---|---|
| `GET /privacy/blocklists?locale=` | Blocklist catalog with name, website, description, entries, updatedOn |
| `GET /privacy/natives` | Native tracking vendors |
| `GET /parentalControl/services` | Services with website |
| `GET /parentalControl/categories` | Categories |
| `GET /security/tlds` | All TLDs with a `spamhaus` score |
| `GET /trackers/:id` | Tracker insights (owner company, category, prevalence) |

Endpoints the dashboard calls that are **not** in the public API docs. Each must be verified with a real API key during M0 because the dashboard authenticates with a session cookie, not `X-Api-Key`.

`/profiles/:id/setup`, `/profiles/:id/setup/linkedIp` (on `ipv4.api.nextdns.io`), `/profiles/:id/access`, `/profiles/:id/parentalControl` `recreation` object, `privacy.rotatingAdDomains`, `settings.ageVerification`, `/accounts/@me`, `/accounts/@me/usage`, `/accounts/@me/invites`, `/account/apiKeys`.

`GET /accounts/@me` without auth returns a plain `403 Forbidden`, while `/profiles` returns `{"errors":[{"code":"authRequired"}]}`. That difference suggests account endpoints are session-only. This needs confirmation.

### 2.2 Dashboard map

Routes: `/login`, `/signup`, `/reset-password/:token?`, `/start`, `/account`, `/invite/:token`, `/invoice/:code`, `/:profile/:menu`.

Profile menus: `setup`, `security`, `privacy`, `parentalcontrol`, `denylist`, `allowlist`, `analytics`, `logs`, `settings`. Every write control is disabled for the `viewer` role.

### 2.3 Parity matrix

Legend: ✅ present, 🟡 partial, ❌ missing, 🌐 hand off to the web on purpose.

#### Setup

| Site feature | App today | Plan |
|---|---|---|
| Live status check (ok, mismatch, unconfigured, error, block page states) | 🟡 simple `test.nextdns.io` check on Overview | Full status card with every state and the site's remediation hints |
| Endpoints: profile ID, DoH URL, DoT hostname, IPv6 addresses, DNS stamp | ❌ | Setup screen with copy actions |
| Android Private DNS guide | 🟡 button opens wireless settings | Guided card. Copy the DoT hostname, then open settings |
| Other platform guides (iOS, macOS, Windows, Linux, routers, browsers, Tailscale) | ❌ | Short per-platform cards with copy actions and links to the official guides |
| Linked IP: show linked IP, link the current IP, show DNS servers, update URL | ❌ | Setup screen section. The update token is treated as a secret and is never logged |
| DDNS hostname set and unset | ❌ | Same section, hostname validated client side |
| IPv6 support test (`test-ipv6.nextdns.io`) | ❌ | Run with the status check |
| Device identification instructions (DoH path, DoT prefix) | ❌ | Help card with a generator for device-named endpoints |

#### Security

| Site feature | App today | Plan |
|---|---|---|
| Toggles: threat intelligence feeds, AI threat detection, Google Safe Browsing, cryptojacking, DNS rebinding, NRD, DDNS, parking, CSAM | 🟡 rendered generically from whatever booleans come back, some without labels | Typed model with a label and description for each. Deprecated `idnHomographs`, `typosquatting` and `dga` hidden |
| Blocked TLDs from the catalog, with a Spamhaus section | 🟡 free-text entry | Searchable catalog picker from `GET /security/tlds`, sorted by Spamhaus score |

#### Privacy

| Site feature | App today | Plan |
|---|---|---|
| Blocklists from the catalog with description, entry count, last update and website | 🟡 hardcoded short list | Live catalog with search, locale and details |
| Native tracking protection from the catalog | 🟡 hardcoded | Live catalog |
| Block disguised third-party trackers | 🟡 generic toggle | Typed toggle with description |
| Allow affiliate and tracking links | 🟡 generic toggle | Typed toggle |
| Block rotating ad domains (released 2026-10-30) | 🟡 generic toggle, no label | Typed toggle |

#### Parental control

| Site feature | App today | Plan |
|---|---|---|
| Services and categories from the catalog with active switch | 🟡 hardcoded catalog | Live catalog with service icons from the site's favicon source behind an opt-in, otherwise monogram avatars |
| Per-item recreation flag | ❌ | Toggle per item |
| Recreation time weekly schedule with timezone | ❌ | Weekly schedule editor (M3 time pickers), timezone from `ZoneId.systemDefault()` |
| SafeSearch, YouTube restricted mode, block bypass | 🟡 generic toggles | Typed toggles with descriptions |

#### Denylist and allowlist

| Site feature | App today | Plan |
|---|---|---|
| Add, enable or disable, remove | ✅ | Keep, add validation and serialized mutations |
| `listLimitExceeded` handling | ❌ | Typed error with a clear message |
| Search within the list | ❌ | Local filter field |

#### Analytics

| Site feature | App today | Plan |
|---|---|---|
| Counters (all, blocked, percent blocked) and queries chart | ✅ | Keep |
| Range selection | 🟡 24h, 7d, 30d, 90d | Add the site's presets and a custom range with the M3 date range picker |
| Device filter for the whole page | ❌ | Device selector in the top bar, `device` parameter on every call, including `__UNIDENTIFIED__` |
| Top domains, blocked domains | ✅ | Keep |
| Root domains toggle (`root=true`) | ❌ | Add |
| Drill-down by block reason (`reason=`) | ❌ | Tap a reason to see its blocked domains |
| Block reasons, devices, protocols, query types, DNSSEC, encryption, IP versions | ✅ | Keep |
| Destination countries and GAFAM share | ✅ | Keep |
| Client IPs with ISP, ASN, VPN and cellular flags, geo | ❌ | Add card |

#### Logs

| Site feature | App today | Plan |
|---|---|---|
| Search, status filter, paging | ✅ | Keep, fix H2 |
| `error` status filter | ❌ | Add |
| Device filter | ❌ | Add |
| Raw mode (`raw=1`) | ❌ | Add |
| Blocked only toggle | 🟡 via status filter | Covered |
| Real-time streaming | 🟡 | Rebuild with lifecycle, resume by `id`, reconnect with backoff |
| Absolute and relative time toggle | ❌ | Add |
| Entry insights (tracker owner, category, prevalence) | ❌ | `GET /trackers/:id` in the detail sheet |
| Allow or block from an entry | ✅ | Keep |
| Download logs | ❌ | `GET /logs/download?redirect=0`, then hand the URL to the system download manager |
| Clear logs | ✅ | Keep |

#### Settings

| Site feature | App today | Plan |
|---|---|---|
| Rename profile | ✅ | Keep |
| Logs enable, drop IPs, drop domains | ✅ | Keep |
| Retention (1h, 6h, 1d, 1w, 1m, 3m, 6m, 1y, 2y) | 🟡 custom option list | Use the site's exact nine options |
| Storage location change with confirmation | 🟡 read-only | Editable with the site's warning dialog |
| Block page enable | ✅ | Keep |
| Block page CA trust status and install help | ❌ | Status line and a link to `nextdns.io/ca` |
| Performance: ECS, cache boost, CNAME flattening | ✅ | Typed toggles |
| Web3 | ✅ | Keep |
| Age verification (beta) | ❌ | Add with the acknowledgment text |
| Rewrites | ✅ | Keep, validate answers, never retry POST |
| Access sharing: invite by email, editor or viewer role, pending state, remove | ❌ | Add, if `X-Api-Key` is accepted |
| Duplicate profile | ❌ | Add |
| Delete profile | ❌ | Add with typed-name confirmation |
| Leave a shared profile | ❌ | Add |

#### Profiles and roles

| Site feature | App today | Plan |
|---|---|---|
| Switch profile | ✅ | Keep |
| Create profile with the site's defaults | ❌ | Add |
| Role awareness (owner, editor, viewer) | ❌ | Read the role and disable writes for viewers like the site does |

#### Account

| Site feature | App today | Plan |
|---|---|---|
| Usage and quota | ❌ | In-app card if `/accounts/@me/usage` accepts the API key, otherwise 🌐 |
| API keys management | 🟡 shows the stored key suffix | 🌐 The key is required to talk to the API, so creating or revoking keys from inside the app is circular |
| Email, password, two-factor | ❌ | 🌐 These need a password session and are out of scope for an API-key client |
| Subscription, payment methods, invoices, business info | ❌ | 🌐 Selling or managing digital subscriptions in an Android app requires Google Play Billing. Linking out is the safe option |
| Affiliate balance and payout | ❌ | 🌐 |
| Delete account | ❌ | 🌐 |
| Accept invite links (`/invite/:token`) | ❌ | 🌐 Optional App Link later |

### 2.4 Verdict on "100 percent"

Every profile-level feature of the dashboard (setup, security, privacy, parental control, lists, analytics, logs, settings, profile lifecycle and sharing) is in scope and planned. Account features that need a password session or involve billing are handed off to `my.nextdns.io/account` with a single "Manage account on the web" entry. Building those in-app would mean collecting the user's NextDNS password or bypassing Play billing rules, both of which are worse than a hand-off. The app goes beyond the site with app lock, biometric protection of the key, a local connection status check and an Android-native Private DNS flow.

---

## 3. Modern Android standards

### 3.1 Build and tooling

- Gradle Kotlin DSL (`build.gradle.kts`), `plugins {}` blocks and a `gradle/libs.versions.toml` version catalog with exact versions.
- Remove the AGP 9 opt-outs in `gradle.properties`. Adopt built-in Kotlin, the new DSL, R8 strict full mode and the stable configuration cache.
- Compose BOM for aligned Compose versions.
- Gradle dependency verification (`gradle/verification-metadata.xml`) and wrapper checksum validation in CI.
- Lint with `warningsAsErrors`. `MissingTranslation` returns to error once strings are translated.
- Kotlin `explicitApi()` and `allWarningsAsErrors` in `:core`.
- 16 KB page size compliance. Remove `useLegacyPackaging = true` and confirm native libraries are aligned.
- Baseline profile generated with the `androidx.baselineprofile` plugin and Macrobenchmark, replacing the hand-written file.
- Gradle Managed Devices for instrumented tests in CI.

### 3.2 Architecture standards

- Unidirectional data flow. Each screen has one immutable `UiState`, exposed as `StateFlow` and collected with `collectAsStateWithLifecycle`.
- Streams use `SharingStarted.WhileSubscribed(5_000)` so work stops in the background.
- Repositories expose `suspend` functions and `Flow`. ViewModels never hold Android `Context`.
- Typed results with a sealed `AppError`. No string matching on errors.
- Manual dependency injection through an `AppGraph`. No service locator singletons like `AppContainer`.
- Type-safe navigation with Navigation 3 (stable), with back stack state saved across process death.
- `SavedStateHandle` for user input that must survive process death, except secrets.

### 3.3 Testing pyramid

| Layer | Tooling | Scope |
|---|---|---|
| Unit (JVM) | JUnit, `kotlinx-coroutines-test`, MockWebServer (test only) | Validators, HTTP client, SSE parser, repositories, state machines, ViewModels |
| Instrumented | AndroidX Test on Gradle Managed Devices | Keystore vault, biometric wrapper contract, migration |
| UI | Compose UI test | Onboarding, lock, add domain, toggle setting, viewer read-only |
| Screenshot | Compose Preview Screenshot Testing (AGP) | Light, dark, dynamic, font scale 200%, RTL |
| Performance | Macrobenchmark | Cold start, log list scrolling |

---

## 4. Material 3 design system

### 4.1 Foundations

- **Library.** Compose Material 3 stable. Expressive components are adopted only when they reach stable.
- **Color.**
  - Dynamic color on by default (Android 12+).
  - Brand fallback light and dark schemes generated in Material Theme Builder from one seed colour.
  - Medium and high contrast schemes, selected from `UiModeManager.getContrast()` on Android 14+.
- **Semantic status colours.** Blocked, allowed and default are custom colour roles (`blocked`, `onBlocked`, `blockedContainer`, and so on) defined for every scheme and harmonised toward the current primary. No hardcoded `Color(0xFF...)` in screens.
- **Typography.** The default M3 type scale with the system font. Screens use roles (`titleLarge`, `bodyMedium`, `labelSmall`) only. Domains and IDs use a monospace `FontFamily` for legibility.
- **Shape.** The M3 shape scale. Cards `medium`, sheets and dialogs `extraLarge`, chips `small`.
- **Elevation.** Tonal elevation only, through surface container roles (`surfaceContainerLow` to `surfaceContainerHighest`).
- **Motion.** M3 easing and duration tokens. Animations are disabled when the system animator scale is 0.
- **Icons.** Material Symbols Outlined, one weight, filled variant for the selected navigation item.

### 4.2 Layout and navigation

- `NavigationSuiteScaffold` switches between navigation bar, rail and drawer by window size class.
- Destinations: Overview, Logs, Lists, Profile (Setup, Security, Privacy, Parental control, Settings) and App settings.
- `ListDetailPaneScaffold` for logs and lists on tablets and foldables. The log detail opens as a pane instead of a bottom sheet on large screens.
- Edge-to-edge with correct inset handling on every screen, and predictive back with the system animation.
- `TopAppBar` with `enterAlwaysScrollBehavior`. The profile switcher and device filter live in the top bar.

### 4.3 Components by pattern

| Pattern | Component |
|---|---|
| Range selection | `SingleChoiceSegmentedButtonRow`, plus `DateRangePicker` for a custom range |
| Log search | `SearchBar` with recent searches kept in memory only |
| Filters | `FilterChip` row |
| Refresh | `PullToRefreshBox` |
| Destructive actions on list rows | Swipe to dismiss with an undo `Snackbar` instead of a confirmation dialog |
| Irreversible actions (clear logs, delete profile, change log location) | `AlertDialog` with an explicit verb. Delete profile requires typing the name |
| Catalog pickers | Full-screen dialog with search and `ListItem` rows |
| Settings rows | `ListItem` with `Switch` in the trailing slot, the whole row toggles |
| Recreation schedule | `TimePicker` in a dialog for each day |
| Loading | Skeleton placeholders for cards, `LinearProgressIndicator` for paging |
| Empty and error states | Shared composables with icon, title, body and one action |
| Feedback | `SnackbarHost` owned by the scaffold. One-shot events come from the ViewModel |

### 4.4 Accessibility

- Minimum 48 dp touch targets. Status is never conveyed by colour alone (icon, text and `contentDescription`).
- Charts expose a summary through semantics and offer a "View as table" alternative.
- Content stays usable at 200% font scale. RTL is verified.
- WCAG AA contrast for custom colour roles in every scheme.
- Focus order and headings (`semantics { heading() }`) for TalkBack.
- Domains from logs are shown with bidi isolation, and the detail sheet shows the punycode form for IDN names.

---

## 5. Architecture and security

### 5.1 Modules

```
:core      Pure Kotlin/JVM. Models, validators, AppError, HTTP client,
           SSE parser, NextDNS DTOs and repositories, Session and AppLock
           state machines with injected clock and lifecycle.
:platform  Android, no UI. Keystore vault, legacy migration, native
           BiometricPrompt wrapper, connectivity, preferences, clipboard.
:app       Compose UI, ViewModels, design system, AppGraph, flavors.
```

Feature packages in `:app`: `overview`, `logs`, `lists`, `setup`, `security`, `privacy`, `parental`, `settings`, `sharing`, `appsettings`, `onboarding`, `lock`.

### 5.2 State and data flow

- `SessionManager` exposes `StateFlow<SessionState>` with `Initializing`, `StorageUnavailable`, `SignedOut`, `Locked`, `Active(profiles, selected, role)` and `KeyRejected`.
- Any 401 or 403 moves the session to `KeyRejected`, which shows a single re-authentication screen.
- Mutations go through one serial queue per resource, so writes on the same list or setting key are ordered.
- Optimistic changes are an overlay of pending operations over server state. A failure removes only its own operation and triggers a refetch.
- Retries are limited to idempotent calls (GET, PUT, DELETE, PATCH with absolute values). POST is never retried. `Retry-After` is honoured with jitter and at most two retries.
- A shared `Semaphore(4)` limits concurrent analytics requests.
- Catalogs (blocklists, natives, services, categories, TLDs, trackers) are cached in memory for the session.

### 5.3 Native API mapping

| Need | API |
|---|---|
| Secret storage | `KeyGenParameterSpec` AES-256-GCM, StrongBox when available, `setUnlockedDeviceRequired(true)`. Ciphertext and IV in `AtomicFile`. The app lock flag is bound as GCM associated data |
| App lock | Platform `BiometricPrompt`, `ActivityLifecycleCallbacks` with a `SystemClock.elapsedRealtime()` background timer, `FLAG_SECURE` while locked or when "Hide in Recents" is on |
| Networking | `HttpsURLConnection` with `useCaches = false`, `instanceFollowRedirects = false`, bounded reads, cancellation through `disconnect()` |
| Validation | `java.net.IDN`, `android.net.InetAddresses.isNumericAddress` |
| Connectivity | `ConnectivityManager.registerDefaultNetworkCallback` |
| Downloads | `DownloadManager` for log exports |
| Per-app language | `LocaleManager` |
| Splash | Platform `SplashScreen` |
| Transport policy | Network security config with cleartext off and system trust anchors only |

### 5.4 Threat model

Assets: the API key (full account control), query logs (browsing history), profile configuration, linked IP update token.

| Adversary | Controls |
|---|---|
| Other apps | Only the launcher activity is exported. No backups. Clipboard copies marked sensitive. No logging in release builds |
| Physical access | App lock with no first-frame flash. `FLAG_SECURE`. Recovery is "sign out and wipe", never a bypass |
| Network attacker | TLS with system CAs. The key header is attached only when the host is `api.nextdns.io` or `ipv4.api.nextdns.io`. No redirects |
| Malicious server data | Typed decoding, unknown enum values kept as `Unknown(raw)`, 4 MiB body cap, 64 KiB SSE line cap, control and bidi characters stripped |
| Third-party SDKs | Sentry opt-in with a unit-tested `beforeSend` scrubber. FCM opt-in, gms flavor only |
| Supply chain | Exact version pins, dependency verification, wrapper validation |

Out of scope: rooted or compromised OS, and NextDNS server compromise beyond data validation.

### 5.5 Input validation

Everything entering `:core` passes a validator that returns a typed result. Covered inputs: API key charset and length, profile IDs, domains (URL stripping, IDN to ASCII, label and total length, trailing dot), TLDs against the catalog, rewrite answers (IP literal or domain), DDNS hostnames, profile names, invite emails and roles, recreation times, FCM payloads. URL path segments are encoded one at a time and `.` or `..` are rejected.

### 5.6 Data lifecycle

- **In memory.** The key is decrypted once per session. Logs, analytics and catalogs live only in memory and are cleared on sign-out or profile switch.
- **On disk.** The vault file and non-sensitive preferences only. No HTTP cache. Log exports go to the user-chosen Downloads location through `DownloadManager`.
- **Sign-out.** Delete the vault file and Keystore key, cancel all jobs and clear state.
- **No encryption available.** Session-only mode. The key is never written in plaintext.

---

## 6. Roadmap

Each milestone builds, ships its own tests and is committed separately.

| Milestone | Content |
|---|---|
| M0 | Android SDK in the container, Kotlin DSL and version catalog, module split, remove AGP opt-outs, dependency verification, strict lint. Verify every undocumented endpoint from section 2.1 against a real API key |
| M1 | `:core` validators, models and `AppError`, with table-driven tests (path traversal, IDN, pasted URLs, bidi characters) |
| M2 | HTTP client. Tests cover errors in a 200 body, 401 and 403, 429 with `Retry-After`, no POST retry, redirects not followed, oversized bodies, malformed JSON and cancellation |
| M3 | NextDNS DTOs and repositories for every endpoint in section 2. SSE parser with resume, backoff and dedup |
| M4 | Keystore vault and legacy migration. Instrumented tests for invalidated key, corrupt file and no plaintext fallback |
| M5 | Session and AppLock state machines. Tests for no first-frame flash, background timeout, clock change, credential removal, sign-in cancellation and `KeyRejected` |
| M6 | Native shell and design system. Platform splash, BiometricPrompt, LocaleManager, theme with dynamic, brand and contrast schemes, status colour roles, adaptive navigation. Remove AppCompat, biometric, splashscreen and the icon library |
| M7 | Overview and Analytics parity: device filter, root toggle, reason drill-down, IPs card, custom range |
| M8 | Logs parity: device, raw and error filters, lifecycle-aware streaming, insights, time toggle, download, adaptive list-detail |
| M9 | Lists, Security, Privacy and Parental parity: live catalogs, TLD picker, recreation schedule and per-item flags |
| M10 | Setup screen: status, endpoints, Private DNS flow, linked IP, DDNS, guides |
| M11 | Settings parity: retention options, location change, block page CA, age verification, rewrites, sharing, duplicate, delete, leave, create profile, role-aware read-only UI |
| M12 | Sentry and FCM hardening: fix the uncaught handler, scrubber tests, FCM opt-in with the permission requested in context |
| M13 | Accessibility and polish: TalkBack pass, chart tables, 200% font scale, RTL, screenshot tests, localized error strings |
| M14 | Release hardening: R8 audit, baseline profile, upgrade test from the previous APK, README and privacy policy update |

---

## 7. Open decisions

| # | Question | Recommendation |
|---|---|---|
| D1 | Networking stack | `HttpsURLConnection`. Two fewer libraries and less instrumentation surface, at the cost of about 250 lines of owned client code and HTTP/1.1 only |
| D2 | Has this native branch shipped to users? | If not, drop `security-crypto` entirely and accept a one-time reset of theme, app lock and Sentry settings. If yes, keep it for one release to migrate those values |
| D3 | minSdk | Keep 32. API 32 users lose per-app language only |
| D4 | Certificate pinning | No pinning. System CAs only. A NextDNS certificate rotation would otherwise break the app |
| D5 | FCM announcements | Opt-in toggle, off by default |
| D6 | Account features | Hand off to the web as described in section 2.3 |
| D7 | Service favicons | Monogram avatars by default. Loading `favicons.nextdns.io` reveals the viewed services to that host, so it would be an opt-in |
