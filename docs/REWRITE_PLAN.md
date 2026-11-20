# NextDNS Manager Rewrite Roadmap

Status: **Planning complete, awaiting approval. No implementation code has been written.**
Last updated: 2026-11-20. Branch: `native-api-app`.

This is the single source of truth for the rewrite. It covers the audit of the current code, feature parity with the NextDNS dashboard, modern Android standards, the Material 3 design system, security, distribution changes, release operations, community communication and an ordered roadmap. The app keeps its name and stays NextDNS only.

## Contents

1. [Decisions](#1-decisions)
2. [Audit of the current app](#2-audit-of-the-current-app)
3. [Parity with my.nextdns.io](#3-parity-with-mynextdnsio)
4. [Distribution changes: F-Droid exit and single build](#4-distribution-changes-f-droid-exit-and-single-build)
5. [Architecture](#5-architecture)
6. [Security and privacy model](#6-security-and-privacy-model)
7. [Modern Android standards](#7-modern-android-standards)
8. [Material 3 design system](#8-material-3-design-system)
9. [Release, compliance and operations](#9-release-compliance-and-operations)
10. [Community communication plan](#10-community-communication-plan)
11. [Roadmap](#11-roadmap)
12. [Risks](#12-risks)

---

## 1. Decisions

### 1.1 Decided

| # | Decision |
|---|---|
| A1 | Ground-up native rewrite with the security, reliability and UX goals in this document |
| A2 | Parity with my.nextdns.io for everything profile-level. Account, password, 2FA and billing hand off to the web |
| A3 | Stop supporting F-Droid |
| A4 | Keep the name NextDNS Manager and the current branding. No rebrand |
| A5 | NextDNS only. No other DNS providers |
| A6 | Notify the current community before, during and after the transition |

### 1.2 Open, with recommendations

| # | Question | Recommendation |
|---|---|---|
| D1 | Networking stack | Platform `HttpsURLConnection`. Two fewer libraries and less instrumentation surface, at the cost of about 250 lines of owned client code and HTTP/1.1 only |
| D2 | Legacy settings migration | Drop `security-crypto` entirely. Upgrading users enter an API key anyway, so a one-time reset of theme, app lock and error reporting is acceptable |
| D3 | minSdk | Keep 32. API 32 loses per-app language only |
| D4 | Certificate pinning | None. System CAs only. A NextDNS certificate rotation would otherwise break the app |
| D5 | Build flavors and FCM | One build, Firebase removed. Announcements move to an in-app "What's new" screen (section 4.2) |
| D6 | Account features | Web hand-off to `my.nextdns.io/account` |
| D7 | Service favicons | Monogram avatars by default. Remote favicons only as an opt-in because they reveal viewed services to a third party |
| D8 | Share target (add a shared link's domain to the denylist or allowlist) | Yes, cheap and high value |
| D9 | Multiple NextDNS accounts | v1.1. The vault design supports it, but it is not needed for parity |
| D10 | Offline behavior | Clear offline state, no disk cache in v1 |
| D11 | Profile backup | Export and import of a profile's configuration as JSON |
| D12 | Home screen widget and Quick Settings tile | v1.1 |

---

## 2. Audit of the current app

The branch replaced the WebView wrapper with Kotlin and Jetpack Compose on `api.nextdns.io` (commit `54d7065`). Findings below refer to that code. There are no automated tests.

### 2.1 Critical

| # | Problem | Location | Consequence |
|---|---|---|---|
| C1 | The filtered uncaught exception handler returns without delegating for "ignored" exceptions, matched by loose substrings like `cancel` and `timeout` | `sentry/SentryManager.java` | A matching exception on the main thread kills it while the process lives on. The app freezes instead of crashing |
| C2 | Keystore or Tink failure falls back to plaintext and writes the API key in clear text. The plaintext copy survives later successful launches | `sharedpreferences/SharedPreferencesManager.java` | A full-account credential on disk in plaintext, while onboarding claims encryption |
| C3 | With app lock on and the device screen lock removed, `unlock()` returns silently | `MainActivity.kt` | Permanent lockout. Clearing data is the only way out |

### 2.2 High, correctness and races

| # | Problem | Location |
|---|---|---|
| H1 | All methods retried on 429 and 5xx, including POST. `Retry-After` ignored. Duplicate rewrites and entries are possible | `api/NextDnsApi.kt` |
| H2 | Paging race. The cancelled job's `finally` resets `loading` after the new job set it, so `loadMore` fires a duplicate first page | `ui/logs/LogsViewModel.kt` |
| H3 | Live stream ignores lifecycle, runs in the background, never reconnects and never de-duplicates | `ui/logs/LogsViewModel.kt` |
| H4 | Snapshot rollback erases concurrent successful edits. Concurrent PATCH calls on one setting can land out of order | `data/EntryListController.kt`, `ui/profile/ProfileViewModel.kt` |
| H5 | Sign-in stores the key before validation. Rotation cancels the scope, the cancellation is treated as an error and reported | `ui/SessionViewModel.kt` |
| H6 | App lock shows one frame of the dashboard on cold start. Timeout counts from last unlock on the wall clock, not from backgrounding | `MainActivity.kt`, `ui/NextDnsApp.kt` |
| H7 | A revoked key yields a separate error on every screen with no route back to sign-in | all ViewModels |

### 2.3 High, security and privacy

| # | Problem |
|---|---|
| S1 | OkHttp follows redirects and strips only `Authorization` on cross-host redirects, never `X-Api-Key` |
| S2 | `addPathSegments` resolves `..` and splits on `/`, so an entry ID like `../../x` retargets requests |
| S3 | Sentry Gradle plugin defaults instrument OkHttp, file I/O and Compose. Breadcrumbs carry URLs with `search=<domain>`. View hierarchy, root check and auto breadcrumbs are on |
| S4 | Firebase initialises and subscribes on every sign-in without consent. Notification permission requested without rationale |
| S5 | `setRecentsScreenshotEnabled` is API 33+, so API 32 shows the dashboard in Recents |

### 2.4 Medium

- No input validation for pasted URLs, IDN, trailing dots, rewrite answers or name lengths. Server domains rendered raw, allowing homoglyphs and bidi control characters.
- Untyped errors, including the magic string `"invalid_key"`, hardcoded English messages and raw exception text in the UI.
- Unbounded response bodies and SSE lines.
- Overview fires 13 parallel requests per refresh with retries on top.
- Key decrypted on the main thread for every request.
- Unneeded permissions `WAKE_LOCK`, `ACCESS_WIFI_STATE`, `ACCESS_NOTIFICATION_POLICY` and the `http` `<queries>` entry.
- Copied domains not marked sensitive.
- `test.nextdns.io` status code ignored, so HTML responses become Sentry reports.
- Accessibility gaps: status by colour only, charts without semantics, no stable list keys.
- Hardcoded blocklist, native tracker, service and category catalogs. TLDs as free text.

### 2.5 Process

- Zero tests. CI runs `./gradlew test` on nothing, and `test-dev.yml` only triggers on pushes to `dev`.
- `MissingTranslation` downgraded to a warning.
- `gradle.properties` opts out of AGP 9 defaults (built-in Kotlin, new DSL, R8 strict full mode).

### 2.6 Dependency plan

| Current | Replacement | Reason |
|---|---|---|
| `security-crypto` (EncryptedSharedPreferences, Tink) | Android Keystore AES-256-GCM plus `AtomicFile` in `noBackupFilesDir` | Deprecated, heavy, and the source of the plaintext fallback |
| `androidx.biometric`, `appcompat` | Platform `BiometricPrompt` with API 30+ authenticators | Native on minSdk 32 |
| `core-splashscreen` | Platform `SplashScreen` | Native on minSdk 32 |
| AppCompat locale service | `LocaleManager` | Native on 33+ |
| `material-icons-extended` | About 30 Material Symbols vector drawables | Huge and unmaintained |
| OkHttp, Okio | `HttpsURLConnection` (D1) | Small API surface |
| `kotlinx-serialization` tree walking | Typed `@Serializable` DTOs | Compile-time, no reflection |
| `firebase-messaging`, `google-services` plugin | Removed (D5) | Privacy, single build |
| Sentry defaults | Sentry with instrumentation and auto-installation off | Opt-in, scrubbed |

### 2.7 Unhandled edge cases

Screen lock removed under app lock. Keystore key invalidated. Process death or rotation during sign-in. Key revoked mid-session. 429 with `Retry-After`. Offline start. Stream drop, network change, Doze. Live sessions exceeding the entry cap. Timezone or DST change. Pasted URLs, IDN, trailing dots, wildcards. Selected profile deleted on the server. Control characters in server strings. List limits (`listLimitExceeded` at 5,000 list entries, 500 rewrites, 50 shared users). Edits made on the web while the app is open.

---

## 3. Parity with my.nextdns.io

### 3.1 Method

- Extracted routes, menus, API calls and translation keys from the dashboard bundle `https://my.nextdns.io/static/js/main.a1e3cbc7.js` on 2026-11-20.
- Read `https://nextdns.github.io/api/`.
- Probed catalog endpoints without authentication.

Public catalogs confirmed: `/privacy/blocklists?locale=`, `/privacy/natives`, `/parentalControl/services`, `/parentalControl/categories`, `/security/tlds` (with Spamhaus score), `/trackers/:id`.

Undocumented endpoints used by the dashboard, to verify with an API key in M0: `/profiles/:id/setup`, `/profiles/:id/setup/linkedIp` on `ipv4.api.nextdns.io`, `/profiles/:id/access`, the `parentalControl.recreation` object, `privacy.rotatingAdDomains`, `settings.ageVerification`, `/accounts/@me`, `/accounts/@me/usage`, `/account/apiKeys`. Unauthenticated `/accounts/@me` returns a bare `403`, unlike `/profiles`, which suggests account endpoints are session-only.

### 3.2 Matrix

Legend: ✅ present, 🟡 partial, ❌ missing, 🌐 deliberate web hand-off.

| Area | Site feature | Today | Plan |
|---|---|---|---|
| Setup | Status check with all states and remediation hints | 🟡 | Full status card |
| Setup | Endpoints: ID, DoH, DoT, IPv6, DNS stamp | ❌ | Copyable endpoint list |
| Setup | Android Private DNS guide | 🟡 | Copy hostname then open Settings, with a clear note that apps cannot set Private DNS |
| Setup | Platform guides | ❌ | Short cards linking to official guides |
| Setup | Linked IP, link current IP, DNS servers, update URL | ❌ | Section with the update token treated as a secret |
| Setup | DDNS hostname | ❌ | Validated set and unset |
| Setup | IPv6 test, device identification help | ❌ | Included |
| Security | Nine protection toggles | 🟡 generic | Typed toggles with labels and descriptions. Deprecated fields hidden |
| Security | Blocked TLDs with Spamhaus section | 🟡 free text | Searchable catalog picker |
| Privacy | Blocklists with description, entries, update date | 🟡 hardcoded | Live catalog |
| Privacy | Native tracking | 🟡 hardcoded | Live catalog |
| Privacy | Disguised trackers, affiliate links, rotating ad domains | 🟡 generic | Typed toggles |
| Parental | Services and categories with active switch | 🟡 hardcoded | Live catalog |
| Parental | Per-item recreation flag and weekly recreation schedule | ❌ | Schedule editor with time pickers |
| Parental | SafeSearch, YouTube restricted, block bypass | 🟡 generic | Typed toggles |
| Lists | Add, toggle, remove | ✅ | Keep with validation |
| Lists | Limit errors, search | ❌ | Typed error, local filter |
| Analytics | Counters, chart, domains, reasons, devices, protocols, query types, DNSSEC, encryption, IP versions, destinations | ✅ | Keep |
| Analytics | Device filter, root domains, reason drill-down, client IPs, custom range | ❌ | Add |
| Logs | Search, status, paging, allow or block, clear | ✅ | Keep, fix H2 |
| Logs | Error filter, device filter, raw mode, time format, tracker insights, download | ❌ | Add |
| Logs | Streaming | 🟡 | Rebuild with lifecycle, resume, backoff |
| Settings | Rename, logs toggles, block page, performance, web3, rewrites | ✅ | Keep |
| Settings | Site's nine retention options, location change, block page CA status, age verification | 🟡 or ❌ | Add |
| Settings | Sharing with roles, duplicate, delete, leave, create profile | ❌ | Add if the API key is accepted |
| Profiles | Role awareness (viewer read-only) | ❌ | Enforced in UI |
| Account | Usage | ❌ | In-app if API key accepted, else 🌐 |
| Account | API keys, email, password, 2FA, subscription, invoices, business, affiliate, delete account, invites | ❌ | 🌐 |

### 3.3 Verdict

Every profile-level feature is in scope. Account features need a password session or involve billing. Building them would mean collecting the user's password or conflicting with Google Play billing rules, so they hand off to `my.nextdns.io/account`. The app goes beyond the site with app lock, encrypted on-device key storage, a share target, profile backup and an Android-native Private DNS flow.

---

## 4. Distribution changes: F-Droid exit and single build

### 4.1 F-Droid exit

1. Ship one final F-Droid release of the legacy app with an in-app notice. It says that F-Droid support ends and links to Play and GitHub Releases. This change happens on the legacy release branch, not on `native-api-app`.
2. After the notice has been live for at least four weeks, open a merge request in `fdroiddata` that disables the app with a short reason. Otherwise F-Droid keeps building tags automatically, and the builds start failing once the foss flavor is removed.
3. Explain in the README and FAQ that F-Droid installs are signed with F-Droid's key, so moving to the Play or GitHub build means uninstalling first. The user enters their API key once, which the rewrite requires anyway.
4. Remove F-Droid badges, the installation section, the update-time table, the signing table and the FCM FAQ from the README.

### 4.2 Single build, Firebase removed (D5)

- Remove the `gms` and `foss` flavors, `google-services.json`, the Google Services plugin, `CustomFirebaseMessagingService`, `MessagingInitializer`, `FlavorHooks` and the `processFoss…GoogleServices` workaround.
- Remove the foss jobs from `deploy.yml` and `test-dev.yml`. GitHub Releases ships one signed `app-release.apk`. Play gets the bundle.
- Replace push announcements with a "What's new" screen shown once per version, from bundled release notes.
- The single build works on de-Googled devices, so the GitHub APK covers former F-Droid users. Document Obtainium as an update option for GitHub installs.
- Use the existing FCM topic **once more** from the legacy app to announce the transition (section 10) before Firebase is removed.

---

## 5. Architecture

### 5.1 Modules

```
:core              Pure Kotlin/JVM. Models, validators, AppError, HTTP client,
                   SSE parser, NextDNS DTOs and repositories, Session and
                   AppLock state machines (injected clock and lifecycle).
:platform          Android, no UI. Keystore vault, BiometricPrompt wrapper,
                   connectivity, preferences, clipboard, DownloadManager.
:app               Compose UI, ViewModels, design system, AppGraph.
:baselineprofile   Baseline profile generator and Macrobenchmark.
```

Feature packages in `:app`: `overview`, `logs`, `lists`, `setup`, `security`, `privacy`, `parental`, `settings`, `sharing`, `appsettings`, `onboarding`, `lock`.

### 5.2 Session

- `SessionState`: `Initializing`, `StorageUnavailable`, `SignedOut`, `Locked`, `Active(profiles, selected, role)`, `KeyRejected`.
- Any 401 or 403 moves the session to `KeyRejected` with one re-authentication screen.
- Replacing the API key in place is supported, so rotating a key does not require signing out.
- The vault stores secrets per account ID from day one, so multiple accounts (D9) can be added later without a storage migration.

### 5.3 Data flow and state

- Unidirectional flow. One immutable `UiState` per screen as `StateFlow`, collected with `collectAsStateWithLifecycle`. Streams use `WhileSubscribed(5_000)`.
- Typed results with a sealed `AppError`. API error codes map into it in the repositories.
- Mutations go through one serial queue per resource. Optimistic changes are an overlay of pending operations. A failure removes only its own operation and refetches.
- Retries only for idempotent calls, honouring `Retry-After` with jitter and at most two retries. POST is never retried.
- `Semaphore(4)` limits concurrent analytics requests.
- Catalogs are cached in memory for the session.
- Data refreshes when the app returns to the foreground, so edits made on the web are picked up.
- Manual dependency injection through `AppGraph`. Type-safe Navigation 3 with saved back stack.

### 5.4 Native API mapping

| Need | API |
|---|---|
| Secret storage | Keystore AES-256-GCM, StrongBox when available, `setUnlockedDeviceRequired(true)`, `AtomicFile`. Account ID and app lock flag bound as associated data |
| App lock | Platform `BiometricPrompt`, `ActivityLifecycleCallbacks` with an `elapsedRealtime()` background timer, `FLAG_SECURE` |
| Networking | `HttpsURLConnection`, no caches, no redirects, bounded reads, cancellation by `disconnect()` |
| Validation | `java.net.IDN`, `android.net.InetAddresses` |
| Connectivity | `ConnectivityManager.registerDefaultNetworkCallback` |
| Downloads | `DownloadManager` |
| Sharing in | `ACTION_SEND` intent filter for `text/plain` |
| Language, splash | `LocaleManager`, platform `SplashScreen` |

---

## 6. Security and privacy model

### 6.1 Assets

The API key (full account control), query logs (browsing history), profile configuration, the linked IP update token.

### 6.2 Threats and controls

| Adversary | Controls |
|---|---|
| Other apps | Only the launcher and share activities exported. No backups. Sensitive clipboard flag. No logging in release |
| Physical access | App lock with no first-frame flash. `FLAG_SECURE`. Biometric re-auth for destructive actions (delete profile, clear logs, change log location, sign out). Recovery means "sign out and wipe", never a bypass |
| Network attacker | TLS with system CAs. `X-Api-Key` attached only to `api.nextdns.io` and `ipv4.api.nextdns.io`. No redirects |
| Malicious server data | Typed decoding, `Unknown(raw)` for unknown enums, 4 MiB body cap, 64 KiB SSE line cap, control and bidi characters stripped, punycode in details |
| Third-party SDKs | Firebase removed. Sentry opt-in with a unit-tested scrubber and instrumentation off |
| Supply chain | Exact pins, Gradle dependency verification, wrapper validation, OSV-Scanner, secret scanning |

Out of scope: rooted or compromised OS, NextDNS server compromise beyond data validation.

### 6.3 Validation boundary

Validators in `:core` return typed results for the API key, profile IDs, domains (URL stripping, IDN to ASCII, label and total length, trailing dot), TLDs against the catalog, rewrite answers (IP literal or hostname), DDNS hostnames, profile names, invite emails and roles, recreation times and shared text from the share target. URL path segments are encoded one at a time, and `.` and `..` are rejected.

### 6.4 Data lifecycle

- **Memory.** The key is decrypted once per session. Logs, analytics and catalogs held in memory only, cleared on profile switch or sign-out.
- **Disk.** Vault file and non-sensitive preferences only. No HTTP cache. Exports go through `DownloadManager` or the system file picker to a location the user chooses.
- **Sign-out.** Deletes the vault entry and the Keystore key, cancels all jobs, clears state.
- **No encryption available.** Session-only mode. The key is never written in plaintext.

### 6.5 Verification

- Map every control to OWASP MASVS and record a test or a written justification for each.
- MobSF static scan of each release candidate.
- A local diagnostics export (scrubbed of the key, domains and IPs) for bug reports, since error reporting stays off by default.

---

## 7. Modern Android standards

### 7.1 Build and tooling

- Kotlin DSL build scripts, `plugins {}` blocks, `gradle/libs.versions.toml` with exact versions, Compose BOM.
- Remove AGP 9 opt-outs. Adopt built-in Kotlin, the new DSL, R8 strict full mode, stable configuration cache.
- Gradle dependency verification and wrapper checksum validation.
- Lint `warningsAsErrors`, `MissingTranslation` back to error at release.
- `explicitApi()` and `allWarningsAsErrors` in `:core`.
- 16 KB page size compliance. Drop `useLegacyPackaging`.
- Baseline profile through the `androidx.baselineprofile` plugin.
- Gradle Managed Devices for instrumented tests.

### 7.2 Testing pyramid

| Layer | Tooling | Scope |
|---|---|---|
| Unit | JUnit, `kotlinx-coroutines-test`, MockWebServer (test only) | Validators, client, SSE, repositories, state machines, ViewModels |
| Instrumented | AndroidX Test on managed devices | Vault, biometric wrapper, share intent |
| UI | Compose UI test | Onboarding, lock, add domain, toggle setting, viewer read-only |
| Screenshot | Compose Preview Screenshot Testing | Light, dark, dynamic, high contrast, 200% font, RTL |
| Performance | Macrobenchmark | Cold start, log scrolling |

### 7.3 Acceptance budgets

| Metric | Target |
|---|---|
| Cold start to first frame on a mid-range device | Under 500 ms |
| Release APK size | Under 8 MB |
| `:core` line coverage | At least 85% |
| Lint warnings | Zero |
| Crash-free sessions in beta | At least 99.5% before each rollout step |
| Jank in log scrolling | Under 1% slow frames |

Each milestone has a definition of done: builds, tests green, budgets not regressed, documentation updated, committed.

---

## 8. Material 3 design system

### 8.1 Foundations

- Stable Compose Material 3. Expressive components only once stable.
- Dynamic colour by default. Fallback light and dark schemes generated in Material Theme Builder from the existing brand colour (`@color/main`), plus medium and high contrast schemes chosen through `UiModeManager.getContrast()` on Android 14+.
- Custom status colour roles (`blocked`, `allowed`, `neutral` with on and container variants) for every scheme, harmonised to the primary. No hardcoded colours in screens.
- Default type scale with the system font. Monospace for domains, IDs and endpoints.
- M3 shape scale, tonal elevation through surface container roles, M3 motion tokens, reduced motion respected.
- Material Symbols Outlined, filled for the selected navigation item.
- Launcher icon gains a monochrome layer for Android 13+ themed icons.

### 8.2 Layout and navigation

- `NavigationSuiteScaffold` switches bar, rail and drawer by window size class.
- Destinations: Overview, Logs, Lists, Profile (Setup, Security, Privacy, Parental control, Settings) and App settings.
- `ListDetailPaneScaffold` for logs and lists on tablets and foldables.
- Top bar holds the profile switcher and the device filter.
- Edge-to-edge and predictive back everywhere.

### 8.3 Components by pattern

| Pattern | Component |
|---|---|
| Range | Segmented buttons plus `DateRangePicker` |
| Log search | `SearchBar` |
| Filters | `FilterChip` rows |
| Refresh | `PullToRefreshBox` |
| Removing list items | Swipe with an undo `Snackbar` |
| Irreversible actions | `AlertDialog` with an explicit verb, biometric re-auth when app lock is on. Delete profile requires typing the name |
| Catalog pickers | Full-screen dialog with search |
| Settings rows | `ListItem` with trailing `Switch`, whole row toggles |
| Recreation schedule | `TimePicker` dialogs |
| Loading, empty, error | Skeletons, shared empty and error composables with one action |
| Web hand-off | Informational card with a link to the dashboard |

### 8.4 Accessibility

48 dp targets. Status never by colour alone. Charts with semantic summaries and a table view. Usable at 200% font. RTL verified. WCAG AA contrast for custom roles. Headings and focus order for TalkBack. Bidi isolation for domains.

---

## 9. Release, compliance and operations

### 9.1 Upgrading users

The production app is the WebView version. Its users have never entered an API key. On first launch after the upgrade they see a one-screen explainer. It says what changed, why an API key is needed, where to find it on `my.nextdns.io/account`, and how the key is stored. Settings that cannot migrate are listed plainly.

### 9.2 Store compliance

- Play Data safety form rewritten. API key stored on device, query logs displayed but not collected, crash reports optional, no data shared, Firebase removed.
- Content rating questionnaire re-checked.
- Target API level kept current.

### 9.3 Translations

- 14 locales and hundreds of new strings. The existing Tally "incorrect translation" form stays. Weblate (free hosting for libre projects) is recommended for the new string volume.
- String freeze at the start of beta. Translators get four weeks before release.
- `check-strings.py` runs in CI. `MissingTranslation` becomes an error for release builds, with English fallback allowed only for strings added after the freeze.

### 9.4 CI and automation

- `test-dev.yml` runs on every push and pull request, including `native-api-app`.
- Jobs: unit tests, lint, managed-device instrumented tests, screenshot tests, dependency verification, OSV-Scanner, secret scanning, release build.
- `deploy.yml`: one APK and one bundle, signing for the new module layout, Play upload through fastlane `supply`.
- Dependabot configured for the version catalog and GitHub Actions.

### 9.5 Release process

1. Play internal testing for maintainers.
2. Closed testing for community volunteers, plus GitHub pre-releases.
3. Open testing for at least two weeks.
4. Production staged rollout at 5%, 20%, 50% and 100%, gated on the crash-free budget and no open critical issues.
5. Rollback plan. Halt the rollout in Play Console and ship a fix forward. Keep the last legacy build signed and archived.
6. Parity sign-off checklist generated from section 3.2 before production.

### 9.6 Documentation

- README updated for the native app, the single build and the F-Droid exit.
- FAQ: why an API key is needed, why F-Droid ended, how to move from F-Droid, why account settings open the website.
- CONTRIBUTING, an architecture overview and short decision records in `docs/adr/`.
- SECURITY.md updated with the threat model summary and reporting process.
- Privacy policy updated for the removal of Firebase and the API key model before the beta opens.

### 9.7 Legal

- Keep the existing non-affiliation disclaimer.
- Confirm that the bundled `nextdns_logo` drawable and the store graphics are acceptable use for an unofficial app, or replace them with an original icon.

---

## 10. Community communication plan

### 10.1 Channels

| Channel | Audience | Use |
|---|---|---|
| Pinned GitHub Discussion (or pinned issue) | Contributors, power users | Canonical announcement and Q&A thread, linked from everything else |
| README banner | Visitors | Short notice linking to the announcement |
| Legacy app FCM topic `general` (one final use) | Play and GitHub gms users | Transition notice and beta invitation |
| Legacy app in-app notice | All legacy users including F-Droid | Shown once per launch until dismissed |
| Play listing "What's new" | Play users | Release notes |
| r/nextdns and the NextDNS help forum | NextDNS community | Beta call and launch post, following each community's self-promotion rules |
| GitHub Releases | Sideload users | Changelog and migration notes |

### 10.2 Timeline

T is the production launch date. Dates are relative so the plan survives schedule changes.

| When | Action | Message |
|---|---|---|
| T-12 weeks | Announcement 1: pinned Discussion and README banner | The app is being rebuilt natively on the NextDNS API, F-Droid support ends, Firebase is removed. Why, what stays the same, a rough timeline and how to join the beta |
| T-10 weeks | Courtesy message to NextDNS (section 3.1 questions) | Unofficial app, API usage, which undocumented endpoints are supported for API keys |
| T-8 weeks | Final legacy release with in-app notice, also on F-Droid. One FCM push to `general` | F-Droid ends, beta sign-up link |
| T-6 weeks | Closed beta opens. Announcement 2 | How to join, what to test, how to report issues |
| T-4 weeks | String freeze. Call for translators | Weblate link |
| T-3 weeks | Open beta. Migration guide and FAQ published | Step-by-step upgrade, including from F-Droid |
| T-1 week | Release candidate. Store listing assets ready | Launch date confirmed |
| T | Staged rollout begins. Announcement 3 in all channels | What's new, links |
| T+2 weeks | Rollout reaches 100%. `fdroiddata` merge request to disable the app (at least four weeks after the T-8 notice) | F-Droid listing retired |
| T+4 weeks | Retrospective post | What landed, known issues, what's next (multiple accounts, widget, tile) |

### 10.3 Message guidelines

- Lead with what users must do, if anything, and when.
- Explain the API key requirement honestly. It replaces signing in through a web page, and it is stored encrypted on the device.
- Keep the non-affiliation statement.
- Give F-Droid users a concrete path (GitHub APK, optionally with Obtainium) and acknowledge the inconvenience.
- One canonical thread for questions, so answers do not fragment across channels.

---

## 11. Roadmap

Stages are ordered so that each depends only on earlier ones. Every milestone ends with a commit, green CI and a short summary of what was built, how it was tested and the trade-offs.

### Stage 0. Decisions and preparation (no app code)

| ID | Milestone | Exit criteria |
|---|---|---|
| M0.1 | Resolve D1 to D12 | Recorded in section 1 |
| M0.2 | API verification with a real NextDNS key for every endpoint in section 3.1 | Results table committed. Plan adjusted where endpoints are unavailable |
| M0.3 | Community announcement 1 and NextDNS outreach | Section 10 timeline started |

### Stage 1. Foundation

| ID | Milestone | Exit criteria |
|---|---|---|
| M1 | Build modernisation: Kotlin DSL, version catalog, module split, AGP opt-outs removed, dependency verification, strict lint, CI on every push | Empty modules build. CI runs unit tests, lint and scans |
| M2 | `:core` validators, models, `AppError` | Table-driven tests including path traversal, IDN, pasted URLs, bidi |
| M3 | HTTP client with host-bound key injection | Tests: errors in a 200 body, 400, 401 and 403, 429 with `Retry-After`, no POST retry, no redirects, oversize body, malformed JSON, cancellation |
| M4 | `:platform` Keystore vault | Instrumented tests: invalidated key, corrupt file, no plaintext fallback |
| M5 | Session and AppLock state machines | Tests: no first-frame flash, background timeout, clock change, credential removal recovery, sign-in cancellation, `KeyRejected`, key replacement |

### Stage 2. Data layer

| ID | Milestone | Exit criteria |
|---|---|---|
| M6 | NextDNS DTOs and repositories for every endpoint in section 3, catalogs included | Repository tests against recorded fixtures |
| M7 | SSE log stream with resume by `id`, reconnect with backoff, dedup and lifecycle awareness | Parser tests for partial lines, comments, oversize lines, reconnects |

### Stage 3. App shell and design system

| ID | Milestone | Exit criteria |
|---|---|---|
| M8 | Design system: dynamic, fallback and contrast schemes, status roles, typography, shapes, icons with monochrome launcher layer, shared components | Screenshot tests for all schemes |
| M9 | Native shell: platform splash, BiometricPrompt, `FLAG_SECURE`, LocaleManager, connectivity, adaptive navigation, Firebase and flavors removed, old dependencies removed | App starts, locks and navigates on phone and tablet |
| M10 | Onboarding: API key entry with validation, upgrade explainer, key replacement, read-only role handling | UI tests |

### Stage 4. Features

| ID | Milestone | Exit criteria |
|---|---|---|
| M11 | Overview and analytics parity | Device filter, root toggle, reason drill-down, client IPs, custom range |
| M12 | Logs parity | Filters, raw mode, streaming, insights, time toggle, download, list-detail layout |
| M13 | Lists, security, privacy and parental parity | Live catalogs, TLD picker, recreation schedule, typed toggles, list limits |
| M14 | Setup parity | Status, endpoints, Private DNS flow, linked IP, DDNS, guides |
| M15 | Settings and profile parity | Retention, location, CA status, age verification, rewrites, sharing, create, duplicate, delete, leave, role awareness |

### Stage 5. Extras and hardening

| ID | Milestone | Exit criteria |
|---|---|---|
| M16 | Share target, profile backup and import, offline state, "What's new" screen | Tests for shared URL parsing and backup round trips |
| M17 | Telemetry and diagnostics: Sentry instrumentation off, scrubber tests, fixed uncaught handler, local diagnostics export | No network calls before opt-in |
| M18 | Security verification: MASVS mapping, biometric re-auth for destructive actions, MobSF scan, OSV clean | Report committed |
| M19 | Accessibility and polish: TalkBack pass, chart tables, 200% font, RTL, localized errors, performance budgets met | Screenshot and Macrobenchmark results |

### Stage 6. Release readiness

| ID | Milestone | Exit criteria |
|---|---|---|
| M20 | String freeze and translation round | All locales complete or explicitly deferred |
| M21 | Store and docs: listing, screenshots, Data safety, privacy policy, README, FAQ, SECURITY, ADRs | Reviewed and merged |
| M22 | Legacy branch final release with in-app notice and the last FCM push (outside this branch) | Shipped at T-8 weeks |
| M23 | Beta programme: internal, closed, open | Crash-free budget met, parity checklist signed off |

### Stage 7. Launch and follow-up

| ID | Milestone | Exit criteria |
|---|---|---|
| M24 | Staged production rollout and launch announcements | 100% rollout |
| M25 | F-Droid retirement merge request, README cleanup | Listing disabled |
| M26 | Retrospective and v1.1 planning (multiple accounts, widget, Quick Settings tile, encrypted offline cache) | Plan committed |

---

## 12. Risks

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Undocumented endpoints reject API keys | Medium | Setup, sharing and recreation drop to web hand-off | Verify in M0.2 before building them |
| F-Droid users do not see the notice | Medium | Users stuck on an old build | Four-week notice window, README, community posts |
| Upgrading users abandon the app at the API key step | Medium | Uninstalls, poor reviews | Explainer screen, clear instructions, beta feedback on copy |
| NextDNS API breaking changes (the API is still labelled beta) | Medium | Features break | Typed decoding with `Unknown`, fixture tests, fast release pipeline |
| Parity scope delays launch | Medium | Late release | Stage 4 milestones are independent. Lower-value parity items can ship in v1.1 |
