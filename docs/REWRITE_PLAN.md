# Rewrite, Rebrand and Multi-Provider Roadmap

Status: **Planning complete, awaiting approval. No implementation code has been written.**
Last updated: 2026-11-20. Branch: `native-api-app`.

This is the single source of truth for the rewrite. It covers the audit of the current code, feature parity with the NextDNS dashboard, the new Control D support, the rebrand, modern Android standards, the Material 3 design system, security, release operations, community communication and an ordered roadmap.

## Contents

1. [Decisions](#1-decisions)
2. [Audit of the current app](#2-audit-of-the-current-app)
3. [NextDNS parity with my.nextdns.io](#3-nextdns-parity-with-mynextdnsio)
4. [Control D support](#4-control-d-support)
5. [Rebrand](#5-rebrand)
6. [Distribution changes: F-Droid exit and single build](#6-distribution-changes-f-droid-exit-and-single-build)
7. [Architecture](#7-architecture)
8. [Security and privacy model](#8-security-and-privacy-model)
9. [Modern Android standards](#9-modern-android-standards)
10. [Material 3 design system](#10-material-3-design-system)
11. [Release, compliance and operations](#11-release-compliance-and-operations)
12. [Community communication plan](#12-community-communication-plan)
13. [Roadmap](#13-roadmap)
14. [Risks](#14-risks)

---

## 1. Decisions

### 1.1 Decided

| # | Decision | Source |
|---|---|---|
| A1 | Ground-up native rewrite with the security, reliability and UX goals in this document | Original brief |
| A2 | NextDNS feature parity with my.nextdns.io for everything profile-level. Account, password, 2FA and billing hand off to the web | Section 3 |
| A3 | Stop supporting F-Droid | Maintainer request |
| A4 | Complete rebrand to a provider-neutral name | Maintainer request |
| A5 | Support Control D alongside NextDNS | Maintainer request |
| A6 | Notify the current community before, during and after the transition | Maintainer request |

### 1.2 Open, with recommendations

| # | Question | Recommendation |
|---|---|---|
| D1 | Networking stack | Platform `HttpsURLConnection`. Two fewer libraries and less instrumentation surface, at the cost of about 300 lines of owned client code and HTTP/1.1 only |
| D2 | Legacy settings migration | Drop `security-crypto` entirely. Upgrading users re-enter an API key anyway, so a one-time reset of theme, app lock and error reporting is acceptable |
| D3 | minSdk | Keep 32. API 32 loses per-app language only |
| D4 | Certificate pinning | None. System CAs only. Provider certificate rotations would otherwise break the app |
| D5 | Build flavors and FCM | One build, Firebase removed. Announcements move to an in-app "What's new" screen (section 6.2) |
| D6 | Account features | Web hand-off for both providers |
| D7 | Service favicons | Monogram avatars by default. Remote favicons only as an opt-in because they reveal viewed services to a third party |
| D8 | Share target (add a shared link's domain to a block or allow rule) | Yes, cheap and high value |
| D9 | Multiple accounts | Yes. Required anyway once a user can hold both a NextDNS and a Control D account |
| D10 | Offline behavior | Clear offline state, no disk cache in v1. Revisit an encrypted last-overview cache after launch |
| D11 | Profile backup | Export and import of a profile's configuration as JSON, per provider. Cross-provider conversion is a later, separate decision |
| D12 | Home screen widget and Quick Settings tile | Post-launch (v1.1). Not needed for parity |
| D13 | Control D analytics and activity log | Not in the public API. Hand off to the Control D dashboard and ask Control D whether a supported endpoint exists (section 4.4) |
| D14 | New name | Pick from a vetted shortlist after trademark checks (section 5.2) |
| D15 | applicationId | Keep `com.doubleangels.nextdnsmanagement` so Play users upgrade in place. Rename only code namespaces (section 5.4) |

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
| OkHttp, Okio | `HttpsURLConnection` (D1) | Small API surface per provider |
| `kotlinx-serialization` tree walking | Typed `@Serializable` DTOs | Compile-time, no reflection |
| `firebase-messaging`, `google-services` plugin | Removed (D5) | Privacy, single build |
| Sentry defaults | Sentry with instrumentation and auto-installation off | Opt-in, scrubbed |

### 2.7 Unhandled edge cases

Screen lock removed under app lock. Keystore key invalidated. Process death or rotation during sign-in. Key revoked mid-session. 429 with `Retry-After`. Offline start. Stream drop, network change, Doze. Live sessions exceeding the entry cap. Timezone or DST change. Pasted URLs, IDN, trailing dots, wildcards. Selected profile deleted on the server. Control characters in server strings. List limits (`listLimitExceeded` at 5,000 list entries, 500 rewrites, 50 shared users). Edits made on the web while the app is open. Control D tokens restricted to allowed IPs failing when the phone changes networks.

---

## 3. NextDNS parity with my.nextdns.io

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

Every profile-level feature is in scope. Account features need a password session or involve billing. Building them would mean collecting the user's password or conflicting with Google Play billing rules, so they hand off to `my.nextdns.io/account`.

---

## 4. Control D support

### 4.1 What the public API offers

Source: `https://docs.controld.com/llms.txt` and the API reference, read 2026-11-20.

| Item | Detail |
|---|---|
| Base URL | `https://api.controld.com` |
| Auth | `Authorization: Bearer <token>`. Tokens are **Read** or **Write** and can be restricted to allowed IPs |
| Envelope | `{"success": true, "body": {...}}` or `{"success": false, "body": [], "error": {"message", "code"}}`. The first three digits of `code` match the HTTP status |
| Keys | Every object has a primary key `PK` |
| Orgs | `X-Force-Org-Id` header to act inside a sub-organization |
| Public catalog | `GET /services/categories` works without a token |

Documented resources:

| Resource | Endpoints |
|---|---|
| Profiles | List, create, modify, delete |
| Filters | Native and external lists, enable or disable one, batch modify |
| Services | Catalog by category, profile service rules (block, bypass, redirect through a proxy location) |
| Custom rules | Create, modify, delete. Actions BLOCK, BYPASS, SPOOF (IP or hostname), REDIRECT (proxy). Optional comment up to 64 characters |
| Folders | Create, modify, delete, list rules per folder, folder-level action |
| Default rule | Read and modify |
| Profile options | AI malware, DNS rebind protection, IPv4/IPv6 compatibility, DNSSEC off, block and redirect TTLs, SafeSearch, restricted YouTube and others |
| Restrictions | Catalog, enable, disable |
| Endpoints (devices) | List, create, modify, delete, types, assigned profiles, analytics level |
| Access | Known IPs (latest 50), authorize, remove |
| Misc | `/ip` (current IP and datacenter), `/proxies`, `/network` |
| Analytics | Only log levels and storage regions |
| Account | `/users`, billing (payments, products, subscriptions), organizations (members, sub-orgs) |
| Org network | Internal domains |

### 4.2 What it does not offer

The activity log, statistics and real-time logging shown in the Control D dashboard have **no documented API**. The app cannot offer Control D analytics or logs unless Control D provides a supported endpoint (D13). Undocumented dashboard endpoints will not be scraped, because they can break without notice and may breach Control D's terms.

### 4.3 Concept mapping

| Concept | NextDNS | Control D | Shared UI |
|---|---|---|---|
| Configuration unit | Profile | Profile | Profile switcher |
| Device identity | Device name in endpoint | Endpoint (device) with its own resolvers and one or more profiles | Provider-specific "Devices" screen |
| Domain overrides | Denylist and allowlist (active flag) | Custom rules with BLOCK, BYPASS, SPOOF, REDIRECT and folders | "Rules" screen with an action picker. Actions a provider lacks are hidden |
| DNS rewrites | Rewrites | SPOOF rules | Covered by Rules |
| Blocklists | Privacy blocklists | Filters (native with modes, external) | "Filters" screen |
| Services | Parental services (block) | Services (block, bypass, redirect) | "Services" screen |
| Categories | Parental categories | Native filters and service categories | Mapped inside Filters and Services |
| Fallback | None | Default rule | Control D only |
| Protection options | Security and privacy toggles | Profile options and restrictions | "Protection" screen with typed toggles |
| Read-only access | Viewer role | Read token | One `Permission.ReadOnly` state |
| Analytics and logs | Full API | Not available | Hidden for Control D with a dashboard link |
| Linked IP | Linked IP and DDNS | Authorized IPs, known IPs | Provider-specific section in Setup |
| Traffic redirection | None | Proxy locations | Control D only |

### 4.4 Control D scope for v1

In scope: account connection with a Read or Write token, profiles (list, create, rename, delete), filters with modes, services by category with locations, custom rules and folders, default rule, profile options, restrictions, endpoints (list, create, edit, delete, analytics level, resolver details with copy actions), known and authorized IPs, current IP and datacenter check, proxy location list, account summary from `/users`.

Hand-off to the Control D dashboard: analytics and activity log (unless D13 changes), billing, organizations, members and sub-organizations, internal domains, SSO and passkeys.

Specific handling:

- Read tokens put the whole account in read-only mode with an explanation.
- A 403 from a token with allowed IPs is explained ("This token only works from specific networks") instead of a generic error.
- Organization accounts work for the main organization. Sub-organization switching through `X-Force-Org-Id` is post-launch.

### 4.5 Outreach to providers

Send a short, courtesy message to both NextDNS and Control D before beta. It should state that the app is unofficial, describe how it uses the public APIs, and ask two questions. For NextDNS, whether the undocumented profile endpoints in section 3.1 are supported for API keys. For Control D, whether an analytics or activity log endpoint for tokens exists or is planned. No affiliation, endorsement or use of their logos should be implied.

---

## 5. Rebrand

### 5.1 Goals

- A name that works for any DNS provider, so adding a third provider later does not need another rebrand.
- No provider trademark in the app name, icon or developer name. Provider names appear only descriptively, for example "Manage your NextDNS and Control D settings".
- Continuity. Existing users upgrade in place and can still find the app by its old name for a while.

### 5.2 Choosing the name

1. Draft 8 to 10 candidates. Short, pronounceable, not a generic dictionary word alone, not containing "NextDNS", "Control D", "Next" or "Control".
2. Screen each against the USPTO, EUIPO and WIPO Global Brand databases in classes 9 and 42, Google Play and App Store search, GitHub organisation and repository names, and a domain.
3. Shortlist three. Optionally gather community feedback on the shortlist (not an open poll, which tends to surface names that fail the checks).
4. Decide, then reserve the domain, a GitHub repository name and social handles before announcing.

Illustrative directions only, each must pass step 2: a resolver theme ("Resolva"), a filter or gate theme ("Gatewise"), a control panel theme ("DNS Deck"). These are examples, not recommendations.

### 5.3 Brand system

- One seed colour that drives the Material 3 fallback schemes (section 10). Check contrast for the blocked and allowed status roles against it.
- Adaptive launcher icon with a **monochrome layer** for Android 13+ themed icons, a splash icon, and a notification-free design since FCM goes away.
- Store assets: icon, feature graphic, phone and tablet screenshots in all supported locales, short and full descriptions.
- A neutral illustration style for empty states. No provider logos inside the app, except where a provider's published brand guidelines explicitly allow descriptive use. Default to the provider name in text.
- Replace the bundled `nextdns_logo` drawable.

### 5.4 Technical rebrand

| Item | Plan |
|---|---|
| applicationId | Keep `com.doubleangels.nextdnsmanagement` (D15). Changing it creates a new Play listing and strands every user |
| Kotlin namespace and packages | Rename to `com.doubleangels.<newname>` during the module split in M0. The applicationId is independent |
| App label, strings, `donottranslate.xml` URLs | Update in M6 |
| GitHub repository | Rename at launch. GitHub redirects the old URL. Update README badges and links |
| Play listing | Rename title, add "formerly NextDNS Manager" in the full description for six months |
| Sentry | New project name and release naming. Old project archived |
| Tally forms, donation and privacy policy URLs | Recreate or update under the new name |
| Privacy policy | Rewrite to cover both providers and the removal of Firebase |
| `whatsnew/`, fastlane metadata | Rewrite for the launch release |
| Disclaimers | "Not affiliated with NextDNS Inc. or Control D." Generic, in the app and listing |

---

## 6. Distribution changes: F-Droid exit and single build

### 6.1 F-Droid exit

1. Ship one final F-Droid release of the legacy app with an in-app notice. It should say that F-Droid support ends, name the new app, and link to Play and GitHub. This change happens on the legacy release branch, not on `native-api-app`.
2. After the notice has been live for at least four weeks, open a merge request in `fdroiddata` that disables the app with a short reason. Otherwise F-Droid keeps building tags automatically, and the builds start failing once the foss flavor is removed.
3. Explain in the README and FAQ that F-Droid installs are signed with F-Droid's key, so moving to the Play or GitHub build means uninstalling first. The user re-enters their API key, which the rewrite requires anyway.
4. Remove F-Droid badges, the installation section, the update-time table, the signing table and the FCM FAQ from the README.

### 6.2 Single build, Firebase removed (D5)

- Remove the `gms` and `foss` flavors, `google-services.json`, the Google Services plugin, `CustomFirebaseMessagingService`, `MessagingInitializer`, `FlavorHooks` and the `processFoss…GoogleServices` workaround.
- Remove the foss jobs from `deploy.yml` and `test-dev.yml`. GitHub Releases ships one signed `app-release.apk`. Play gets the bundle.
- Replace push announcements with a "What's new" screen shown once per version, from bundled release notes.
- The single build works on de-Googled devices, so the GitHub APK covers former F-Droid users. Document Obtainium as an update option for GitHub installs.
- Use the existing FCM topic **once more** from the legacy app to announce the transition (section 12) before Firebase is removed.

---

## 7. Architecture

### 7.1 Modules

```
:core                 Pure Kotlin/JVM. Models, validators, AppError, HTTP client,
                      SSE parser, capability interfaces, Session, Accounts and
                      AppLock state machines (injected clock and lifecycle).
:provider:nextdns     Pure Kotlin/JVM. NextDNS DTOs and capability implementations.
:provider:controld    Pure Kotlin/JVM. Control D DTOs and capability implementations.
:platform             Android, no UI. Keystore vault, BiometricPrompt wrapper,
                      connectivity, preferences, clipboard, DownloadManager.
:app                  Compose UI, ViewModels, design system, AppGraph.
:baselineprofile      Baseline profile generator and Macrobenchmark.
```

### 7.2 Provider abstraction

Each provider implements only the capabilities it supports. The UI builds navigation from the capabilities of the active account. No lowest-common-denominator model is forced on both.

| Capability | NextDNS | Control D |
|---|---|---|
| `Profiles` (list, create, rename, delete, duplicate) | ✅ | ✅ (no duplicate) |
| `Analytics` | ✅ | ❌ |
| `QueryLogs` and `LogStream` | ✅ | ❌ |
| `Rules` (actions vary) | Allow, deny | Block, bypass, spoof, redirect, folders |
| `Filters` | Blocklists, natives, TLDs | Native with modes, external |
| `Services` | Block | Block, bypass, redirect |
| `Protection` toggles | Security, privacy, parental | Options, restrictions, default rule |
| `Schedules` | Recreation time | ❌ in public API |
| `Endpoints` | Setup endpoints | Devices with resolvers |
| `IpLinking` | Linked IP, DDNS | Authorized and known IPs |
| `Sharing` | Access with roles | ❌ (organizations hand off) |
| `ConnectionCheck` | `test.nextdns.io` | `/ip` |
| `ProfileBackup` | ✅ | ✅ |

A shared **contract test suite** runs against both adapters with recorded fixtures, so each capability behaves the same way regardless of provider.

### 7.3 Accounts and session

- `Account(id, provider, label, permission)` with its secret in the vault under that account ID. Multiple accounts are supported (D9). The top bar switches account and profile.
- `SessionState` per active account: `Initializing`, `StorageUnavailable`, `NoAccounts`, `Locked`, `Active(account, profiles, selected)`, `CredentialRejected(account)`.
- Any 401 or 403 moves that account to `CredentialRejected` with one re-authentication screen. The Control D allowed-IP case gets its own message.
- Replacing a credential in place is supported, so rotating a key does not require removing the account.

### 7.4 Data flow and state

- Unidirectional flow. One immutable `UiState` per screen as `StateFlow`, collected with `collectAsStateWithLifecycle`. Streams use `WhileSubscribed(5_000)`.
- Typed results with a sealed `AppError`. Provider error codes map into it in the adapters.
- Mutations go through one serial queue per resource. Optimistic changes are an overlay of pending operations. A failure removes only its own operation and refetches.
- Retries only for idempotent calls, honouring `Retry-After` with jitter and at most two retries. POST is never retried.
- `Semaphore(4)` limits concurrent analytics requests.
- Data refreshes when the app returns to the foreground, so edits made on the web are picked up.
- Manual dependency injection through `AppGraph`. Type-safe Navigation 3 with saved back stack.

### 7.5 Native API mapping

| Need | API |
|---|---|
| Secret storage | Keystore AES-256-GCM, StrongBox when available, `setUnlockedDeviceRequired(true)`, `AtomicFile`. Account ID, provider and app lock flag bound as associated data |
| App lock | Platform `BiometricPrompt`, `ActivityLifecycleCallbacks` with an `elapsedRealtime()` background timer, `FLAG_SECURE` |
| Networking | `HttpsURLConnection`, no caches, no redirects, bounded reads, cancellation by `disconnect()` |
| Validation | `java.net.IDN`, `android.net.InetAddresses` |
| Connectivity | `ConnectivityManager.registerDefaultNetworkCallback` |
| Downloads | `DownloadManager` |
| Sharing in | `ACTION_SEND` intent filter for `text/plain` |
| Language, splash | `LocaleManager`, platform `SplashScreen` |

---

## 8. Security and privacy model

### 8.1 Assets

NextDNS API keys and Control D tokens (full account control), query logs (browsing history), profile configuration, linked IP update tokens.

### 8.2 Threats and controls

| Adversary | Controls |
|---|---|
| Other apps | Only the launcher and share activities exported. No backups. Sensitive clipboard flag. No logging in release |
| Physical access | App lock with no first-frame flash. `FLAG_SECURE`. Biometric re-auth for destructive actions (delete profile, clear logs, change log location, remove account). Recovery means "remove account and wipe", never a bypass |
| Network attacker | TLS with system CAs. Credentials bound to hosts: `X-Api-Key` only to `api.nextdns.io` and `ipv4.api.nextdns.io`, `Authorization: Bearer` only to `api.controld.com`. No redirects |
| Malicious server data | Typed decoding, `Unknown(raw)` for unknown enums, 4 MiB body cap, 64 KiB SSE line cap, control and bidi characters stripped, punycode in details |
| Third-party SDKs | Firebase removed. Sentry opt-in with a unit-tested scrubber and instrumentation off |
| Supply chain | Exact pins, Gradle dependency verification, wrapper validation, OSV-Scanner, secret scanning |

Out of scope: rooted or compromised OS, provider server compromise beyond data validation.

### 8.3 Validation boundary

Validators in `:core` return typed results for NextDNS keys, Control D tokens, profile IDs, domains (URL stripping, IDN to ASCII, label and total length, trailing dot), TLDs against the catalog, rewrite and spoof targets (IP literal or hostname), proxy identifiers against `/proxies`, DDNS hostnames, profile and folder names, rule comments (64 characters for Control D), invite emails and roles, schedule times and shared text from the share target. URL path segments are encoded one at a time, and `.` and `..` are rejected.

### 8.4 Data lifecycle

- **Memory.** Credentials decrypted once per session. Logs, analytics and catalogs held in memory only, cleared on account switch, profile switch or sign-out.
- **Disk.** Vault file and non-sensitive preferences only. No HTTP cache. Exports go through `DownloadManager` or the system file picker to a location the user chooses.
- **Removal.** Removing an account deletes its vault entry and cancels its jobs. Removing the last account deletes the Keystore key.
- **No encryption available.** Session-only mode. Credentials are never written in plaintext.

### 8.5 Verification

- Map every control to OWASP MASVS and record a test or a written justification for each.
- MobSF static scan of each release candidate.
- A local diagnostics export (scrubbed of credentials, domains and IPs) for bug reports, since error reporting stays off by default.

---

## 9. Modern Android standards

### 9.1 Build and tooling

- Kotlin DSL build scripts, `plugins {}` blocks, `gradle/libs.versions.toml` with exact versions, Compose BOM.
- Remove AGP 9 opt-outs. Adopt built-in Kotlin, the new DSL, R8 strict full mode, stable configuration cache.
- Gradle dependency verification and wrapper checksum validation.
- Lint `warningsAsErrors`, `MissingTranslation` back to error at release.
- `explicitApi()` and `allWarningsAsErrors` in the pure Kotlin modules.
- 16 KB page size compliance. Drop `useLegacyPackaging`.
- Baseline profile through the `androidx.baselineprofile` plugin.
- Gradle Managed Devices for instrumented tests.

### 9.2 Testing pyramid

| Layer | Tooling | Scope |
|---|---|---|
| Unit | JUnit, `kotlinx-coroutines-test`, MockWebServer (test only) | Validators, client, SSE, adapters, state machines, ViewModels |
| Contract | Shared suite with recorded fixtures | Every capability against both providers |
| Instrumented | AndroidX Test on managed devices | Vault, biometric wrapper, share intent |
| UI | Compose UI test | Onboarding for both providers, lock, add rule, toggle, read-only mode, account switch |
| Screenshot | Compose Preview Screenshot Testing | Light, dark, dynamic, high contrast, 200% font, RTL |
| Performance | Macrobenchmark | Cold start, log scrolling |

### 9.3 Acceptance budgets

| Metric | Target |
|---|---|
| Cold start to first frame on a mid-range device | Under 500 ms |
| Release APK size | Under 8 MB |
| `:core` and provider line coverage | At least 85% |
| Lint warnings | Zero |
| Crash-free sessions in beta | At least 99.5% before each rollout step |
| Jank in log scrolling | Under 1% slow frames |

Each milestone has a definition of done: builds, tests green, budgets not regressed, documentation updated, committed.

---

## 10. Material 3 design system

### 10.1 Foundations

- Stable Compose Material 3. Expressive components only once stable.
- Dynamic colour by default. Brand fallback schemes from the seed in section 5.3, plus medium and high contrast schemes chosen through `UiModeManager.getContrast()` on Android 14+.
- Custom status colour roles (`blocked`, `allowed`, `redirected`, `neutral` with on and container variants) for every scheme, harmonised to the primary. No hardcoded colours in screens.
- Default type scale with the system font. Monospace for domains, IDs and endpoints.
- M3 shape scale, tonal elevation through surface container roles, M3 motion tokens, reduced motion respected.
- Material Symbols Outlined, filled for the selected navigation item.

### 10.2 Layout and navigation

- `NavigationSuiteScaffold` switches bar, rail and drawer by window size class.
- Destinations come from capabilities. For example: Overview, Logs, Rules, Filters, Services, Protection, Devices or Setup, Settings.
- `ListDetailPaneScaffold` for logs, rules and devices on tablets and foldables.
- Top bar holds the account and profile switcher and, for NextDNS, the device filter.
- Edge-to-edge and predictive back everywhere.

### 10.3 Components by pattern

| Pattern | Component |
|---|---|
| Provider choice in onboarding | Two large `ElevatedCard` options with the provider name in text |
| Range | Segmented buttons plus `DateRangePicker` |
| Log search | `SearchBar` |
| Filters | `FilterChip` rows |
| Refresh | `PullToRefreshBox` |
| Rule action | Segmented buttons for block, bypass, spoof, redirect, showing only supported actions |
| Removing list items | Swipe with an undo `Snackbar` |
| Irreversible actions | `AlertDialog` with an explicit verb and biometric re-auth when app lock is on |
| Catalog pickers | Full-screen dialog with search |
| Settings rows | `ListItem` with trailing `Switch`, whole row toggles |
| Schedules | `TimePicker` dialogs |
| Loading, empty, error | Skeletons, shared empty and error composables with one action |
| Unavailable capability | Informational card explaining the limit, with a link to the provider dashboard |

### 10.4 Accessibility

48 dp targets. Status never by colour alone. Charts with semantic summaries and a table view. Usable at 200% font. RTL verified. WCAG AA contrast for custom roles. Headings and focus order for TalkBack. Bidi isolation for domains.

---

## 11. Release, compliance and operations

### 11.1 Upgrading users

The production app is the WebView version. Its users have never entered an API key. On first launch after the upgrade they see a one-screen explainer. It says what changed, why an API key or token is needed, where to find it for each provider, and how the credential is stored. Settings that cannot migrate are listed plainly.

### 11.2 Store compliance

- Play Data safety form rewritten. Credentials stored on device, query logs displayed but not collected, crash reports optional, no data shared, Firebase removed.
- Play listing renamed with the old name kept in the description for six months.
- Content rating questionnaire re-checked.
- Target API level kept current.

### 11.3 Translations

- 14 locales today and hundreds of new strings. The existing Tally "incorrect translation" form stays. Weblate (hosted for free for libre projects) is recommended for the new string volume.
- String freeze at the start of beta. Translators get four weeks before release.
- `check-strings.py` runs in CI. `MissingTranslation` becomes an error for release builds, with English fallback allowed only for strings added after the freeze.

### 11.4 CI and automation

- `test-dev.yml` runs on every push and pull request, including `native-api-app`.
- Jobs: unit and contract tests, lint, managed-device instrumented tests, screenshot tests, dependency verification, OSV-Scanner, secret scanning, release build.
- `deploy.yml`: one APK and one bundle, signing for the new module layout, Play upload through fastlane `supply`.
- Dependabot configured for the version catalog and GitHub Actions.

### 11.5 Release process

1. Play internal testing for maintainers.
2. Closed testing for community volunteers, plus GitHub pre-releases.
3. Open testing for at least two weeks.
4. Production staged rollout at 5%, 20%, 50% and 100%, gated on the crash-free budget and no open critical issues.
5. Rollback plan. Halt the rollout in Play Console and ship a fix forward. Keep the last legacy build signed and archived.
6. Parity sign-off checklist generated from sections 3.2 and 4.4 before production.

### 11.6 Documentation

- README rewritten for the new name and both providers.
- FAQ: why an API key or token is needed, why F-Droid ended, why Control D has no logs in the app, how to move from F-Droid.
- CONTRIBUTING, an architecture overview and short decision records in `docs/adr/`.
- SECURITY.md updated with the new threat model summary and reporting process.

### 11.7 Legal

- Confirm the new name passes trademark checks before any public use.
- Descriptive, nominative use of "NextDNS" and "Control D" only, with the non-affiliation disclaimer.
- Privacy policy updated before the beta opens.

---

## 12. Community communication plan

### 12.1 Channels

| Channel | Audience | Use |
|---|---|---|
| Pinned GitHub Discussion (or pinned issue) | Contributors, power users | Canonical announcement and Q&A thread, linked from everything else |
| README banner | Visitors | Short notice linking to the announcement |
| Legacy app FCM topic `general` (one final use) | Play and GitHub gms users | Transition notice and beta invitation |
| Legacy app in-app notice | All legacy users including F-Droid | Shown once per launch until dismissed |
| Play listing "What's new" and description | Play users | Release notes and the rename |
| r/nextdns, NextDNS help forum, r/ControlD, Control D community | Provider communities | Launch posts, following each community's self-promotion rules |
| GitHub Releases | Sideload users | Changelog and migration notes |

### 12.2 Timeline

T is the production launch date. Dates are relative so the plan survives schedule changes.

| When | Action | Message |
|---|---|---|
| Before T-12 weeks | Name chosen and cleared (D14). Domain and repository name reserved | Internal |
| T-12 weeks | Announcement 1: pinned Discussion and README banner | The app is being rebuilt natively. It gets a new name, adds Control D, F-Droid support ends, Firebase is removed. Why, what stays the same, a rough timeline and how to join the beta |
| T-10 weeks | Courtesy messages to NextDNS and Control D (section 4.5) | Unofficial app, API usage, two questions |
| T-8 weeks | Final legacy release with in-app notice, also on F-Droid. One FCM push to `general` | F-Droid ends, the new name is coming, beta sign-up link |
| T-6 weeks | Closed beta opens. Announcement 2 | How to join, what to test, how to report issues |
| T-4 weeks | String freeze. Call for translators | Weblate link |
| T-3 weeks | Open beta. Migration guide and FAQ published | Step-by-step move, including from F-Droid |
| T-1 week | Release candidate. Store listing assets ready | Launch date confirmed |
| T | Staged rollout begins. Repository renamed. Announcement 3 in all channels | What's new, the new name, links |
| T+2 weeks | Rollout reaches 100%. `fdroiddata` merge request to disable the app (at least four weeks after the T-8 notice) | F-Droid listing retired |
| T+4 weeks | Retrospective post | What landed, known issues, what's next (widget, tile, sub-organizations) |
| T+6 months | Remove "formerly NextDNS Manager" from the listing. Archive the old Sentry project | |

### 12.3 Message guidelines

- Lead with what users must do, if anything, and when.
- Explain the API key or token requirement honestly. It replaces signing in through a web page, and it is stored encrypted on the device.
- Be explicit that the app is not affiliated with either provider.
- Give F-Droid users a concrete path (GitHub APK, optionally with Obtainium) and acknowledge the inconvenience.
- One canonical thread for questions, so answers do not fragment across channels.

---

## 13. Roadmap

Stages are ordered so that each depends only on earlier ones. Milestones inside a stage can overlap where noted. Every milestone ends with a commit, green CI and a short summary of what was built, how it was tested and the trade-offs.

### Stage 0. Decisions and preparation (no app code)

| ID | Milestone | Exit criteria |
|---|---|---|
| M0.1 | Resolve D1 to D15 | Recorded in section 1 |
| M0.2 | Name selection and trademark screening (section 5.2) | Name cleared, domain and repository name reserved |
| M0.3 | Brand seed colour, icon with monochrome layer, store asset plan | Assets in the repository |
| M0.4 | API verification with a real NextDNS key and a Control D token (Read and Write) for every endpoint in sections 3.1 and 4.1 | Results table committed. Plan adjusted where endpoints are unavailable |
| M0.5 | Community announcement 1 and provider outreach | Section 12 timeline started |

### Stage 1. Foundation

| ID | Milestone | Exit criteria |
|---|---|---|
| M1 | Build modernisation: Kotlin DSL, version catalog, module split with renamed namespaces, AGP opt-outs removed, dependency verification, strict lint, CI on every push | Empty modules build. CI runs unit tests, lint and scans |
| M2 | `:core` validators, models, `AppError`, capability interfaces | Table-driven tests including path traversal, IDN, pasted URLs, bidi |
| M3 | HTTP client with host-bound credential injection for both auth schemes | Tests: error envelopes for both providers, 401 and 403, 429 with `Retry-After`, no POST retry, no redirects, oversize body, malformed JSON, cancellation |
| M4 | `:platform` vault with multiple accounts | Instrumented tests: invalidated key, corrupt file, no plaintext fallback, per-account isolation |
| M5 | Accounts, Session and AppLock state machines | Tests: no first-frame flash, background timeout, clock change, credential removal recovery, sign-in cancellation, `CredentialRejected`, credential replacement |

### Stage 2. Provider data layers (M6 and M7 can run in parallel)

| ID | Milestone | Exit criteria |
|---|---|---|
| M6 | `:provider:nextdns` with every endpoint in section 3, including the SSE stream with resume, backoff and dedup | Contract suite green. SSE parser tests for partial lines, comments, oversize lines |
| M7 | `:provider:controld` with every endpoint in section 4.4 | Contract suite green. Read-token and allowed-IP error tests |

### Stage 3. App shell and design system

| ID | Milestone | Exit criteria |
|---|---|---|
| M8 | Design system: brand and dynamic schemes, contrast schemes, status roles, typography, shapes, icons, shared components | Screenshot tests for all schemes |
| M9 | Native shell: platform splash, BiometricPrompt, `FLAG_SECURE`, LocaleManager, connectivity, adaptive navigation from capabilities, Firebase and flavors removed, old dependencies removed | App starts, locks and navigates on phone and tablet |
| M10 | Onboarding and accounts: provider choice, credential entry with validation, upgrade explainer, account switcher, credential replacement, read-only mode | UI tests for both providers |

### Stage 4. NextDNS features

| ID | Milestone | Exit criteria |
|---|---|---|
| M11 | Overview and analytics parity | Device filter, root toggle, reason drill-down, client IPs, custom range |
| M12 | Logs parity | Filters, raw mode, lifecycle streaming, insights, time toggle, download, list-detail layout |
| M13 | Rules, filters, services and protection parity | Live catalogs, TLD picker, recreation schedule, typed toggles |
| M14 | Setup and settings parity | Status, endpoints, Private DNS flow, linked IP, DDNS, retention, location, CA status, age verification, rewrites, sharing, create, duplicate, delete, leave, role awareness |

### Stage 5. Control D features

| ID | Milestone | Exit criteria |
|---|---|---|
| M15 | Profiles, filters with modes, services with locations, default rule, options and restrictions | Contract and UI tests |
| M16 | Custom rules and folders with all four actions | Validation of spoof and redirect targets |
| M17 | Endpoints, resolvers, known and authorized IPs, `/ip` check, proxy list, account summary, dashboard hand-off cards | UI tests including read-only token |

### Stage 6. Cross-provider features and hardening

| ID | Milestone | Exit criteria |
|---|---|---|
| M18 | Share target, profile backup and import, offline state | Tests for shared URL parsing and backup round trips |
| M19 | Telemetry and diagnostics: Sentry instrumentation off, scrubber tests, fixed uncaught handler, local diagnostics export | Scrubber unit tests, no network calls before opt-in |
| M20 | Security verification: MASVS mapping, biometric re-auth for destructive actions, MobSF scan, OSV clean | Report committed |
| M21 | Accessibility and polish: TalkBack pass, chart tables, 200% font, RTL, localized errors, performance budgets met | Screenshot and Macrobenchmark results |

### Stage 7. Release readiness

| ID | Milestone | Exit criteria |
|---|---|---|
| M22 | String freeze and translation round | All locales complete or explicitly deferred |
| M23 | Store and legal: listing, screenshots, Data safety, privacy policy, README, FAQ, SECURITY, ADRs | Reviewed and merged |
| M24 | Legacy branch final release with in-app notice and the last FCM push (outside this branch) | Shipped at T-8 weeks |
| M25 | Beta programme: internal, closed, open | Crash-free budget met, parity checklist signed off |

### Stage 8. Launch and follow-up

| ID | Milestone | Exit criteria |
|---|---|---|
| M26 | Staged production rollout, repository rename, launch announcements | 100% rollout |
| M27 | F-Droid retirement merge request, README cleanup | Listing disabled |
| M28 | Retrospective and v1.1 planning (widget, Quick Settings tile, Control D sub-organizations, encrypted offline cache, Control D analytics if an API appears) | Plan committed |

---

## 14. Risks

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Undocumented NextDNS endpoints reject API keys | Medium | Setup, sharing and recreation features drop to web hand-off | Verify in M0.4 before building them |
| Control D never offers an analytics API | High | No Control D logs in the app | Clear in-app explanation and dashboard link. Revisit if an API appears |
| Chosen name conflicts with a trademark | Low after screening | Forced second rebrand | Formal search in M0.2 before any public use |
| F-Droid users do not see the notice | Medium | Users stuck on an old build | Four-week notice window, README, community posts, disabled listing explains the move |
| Upgrading users abandon the app at the API key step | Medium | Uninstalls, poor reviews | Explainer screen, provider-specific instructions, beta feedback on copy |
| Provider API breaking changes | Medium | Features break | Typed decoding with `Unknown`, contract tests, fast release pipeline |
| Scope creep from parity on two providers | High | Delays | Stage order lets NextDNS ship first if needed. Control D stages can move to a fast follow without blocking launch |
