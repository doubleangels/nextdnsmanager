<p align="center">
  <img src="https://img.shields.io/github/release/doubleangels/NextDNSManager.svg?logo=github&label=GitHub%20Build&style=for-the-badge" alt="GitHub Build">
  <img src="https://img.shields.io/f-droid/v/com.doubleangels.nextdnsmanagement.svg?logo=F-Droid&label=F-Droid%20Build&style=for-the-badge" alt="F-Droid Build">
  <img src="https://img.shields.io/github/actions/workflow/status/doubleangels/nextdnsmanager/.github/workflows/deploy.yml?label=Deployment%20Pipeline&style=for-the-badge" alt="Main Deployment">
  <img src="https://img.shields.io/github/actions/workflow/status/doubleangels/nextdnsmanager/.github/workflows/test-dev.yml?label=Development%20Testing&style=for-the-badge" alt="Development Testing">
  <img src="https://img.shields.io/librariesio/github/doubleangels/nextdnsmanager?label=Dependencies&style=for-the-badge" alt="Dependencies">
  <img src="https://img.shields.io/github/issues/doubleangels/nextdnsmanager?label=GitHub%20Issues&style=for-the-badge" alt="GitHub Issues">
  <img src="https://img.shields.io/github/issues-pr/doubleangels/nextdnsmanager?label=GitHub%20Pull%20Requests&style=for-the-badge" alt="GitHub Pull Requests">
</p>

<p align="center">
  <img src="icons/web/icon-192.png" alt="NextDNS Manager Icon" width="96">
  <br>
  <a href="https://play.google.com/store/apps/details?id=com.doubleangels.nextdnsmanagement">
    <img src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png" alt="Get it on Google Play" height="48">
  </a>
  <a href="https://f-droid.org/en/packages/com.doubleangels.nextdnsmanagement">
    <img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png" alt="Get it on F-Droid" height="48">
  </a>
</p>

<p align="center">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/1.png" alt="Screenshot of NextDNS Manager" width="250">
</p>

# NextDNS Manager

NextDNS Manager is an Android application that simplifies managing your [NextDNS](https://nextdns.io) configuration. NextDNS is a cloud-based DNS filter and firewall designed to protect your home, family, and online privacy. With NextDNS Manager, you can effortlessly control your NextDNS settings to ensure a safer and more secure digital experience.

**Compatible with any Android device running Android 12L or later.**

> **Note:** NextDNS Manager is a completely open-source project and is not officially affiliated with NextDNS.

---

## Table of Contents

- [Features](#features)
- [Installation](#installation)

  - [Which installation method should I use?](#which-installation-method-should-i-use)
  - [Google Play Store](#google-play-store)
  - [F-Droid Installation](#f-droid-installation)
  - [Manual Installation](#manual-installation)

- [FAQ](#faq)

  - [Are you associated with NextDNS? Is this an official app?](#are-you-associated-with-nextdns-is-this-an-official-app)
  - [What happened to the official NextDNS app?](#what-happened-to-the-official-nextdns-app)
  - [What features won't be able to be added to this app since it's not official?](#what-features-wont-be-able-to-be-added-to-this-app-since-its-not-official)
  - [Is this app secure? Can you access my account or view my DNS queries?](#is-this-app-secure-can-you-access-my-account-or-view-my-dns-queries)
  - [What is Sentry, and is it tracking me?](#what-is-sentry-and-is-it-tracking-me)
  - [I am new to using NextDNS and I don't understand how to use it, can you teach me?](#i-am-new-to-using-nextdns-and-i-dont-understand-how-to-use-it-can-you-teach-me)
  - [I have multiple versions of NextDNS Manager on my phone after the 5.5.0 update!](#multiple-versions)
  - [Why doesn't the app support Android versions before 12L?](#supported-android-versions)
  - [Will you bring back support for older Android versions?](#add-supported-android-versions)
  - [What is FCM, and why is it disabled in F-Droid builds?](#fcm)
  - [I've read all of this and still have a question, what now?](#ive-read-all-of-this-and-still-have-a-question-what-now)

- [Reporting Issues & Feedback](#reporting-issues--feedback)
- [Contributing](#contributing)
- [Security Policy](#security-policy)
- [Privacy & Terms](#privacy--terms)
- [Donations](#donations)
- [License](#license)

---

## Features

- **Intuitive Interface:**  
  Enjoy a user-friendly experience enhanced with dark mode, dynamic/themed icons, and support for 14 languages.

- **Comprehensive Configuration Management:**  
  Easily manage your NextDNS settings including filtering modes, blocklists, and whitelists.

- **Real-Time Statistics:**  
  Monitor DNS queries, blocked requests, and security events as they happen.

- **Enhanced Security and Privacy:**  
  Benefit from NextDNS' robust filtering capabilities to safeguard your online activities.

- **Multiple Installation Options:**  
  Download and install NextDNS Manager via the Google Play Store, F-Droid, or directly from GitHub.

---

## Installation

### Which installation method should I use?

Your choice depends on your privacy preferences and how frequently you want to receive updates:

- **Google Play Store:** Best for users who prefer automatic updates and seamless access to new features.
- **Manual APK Sideload:** Ideal for those who cannot use Google Play or prefer full control over updates.
- **F-Droid:** Suitable for users who prioritize open-source purity, though updates may take longer to become available.

> **Important Notes:**
>
> - F-Droid builds **do not** support FCM (push notifications).
> - FCM is enabled in Google Play builds for update and issue notifications.
> - Versions **before 5.5.0** do not support push notifications.

### Update Availability Comparison

| Method             | Update Availability |
| ------------------ | ------------------- |
| **Google Play**    | Within minutes      |
| **Sideloaded APK** | Within minutes      |
| **F-Droid**        | Up to 14 days       |

_F-Droid updates take longer because each release is manually signed in a secure, air-gapped environment._

### Google Play Store

Download NextDNS Manager from the [Google Play Store](https://play.google.com/store/apps/details?id=com.doubleangels.nextdnsmanagement).

### F-Droid Installation

Get NextDNS Manager on F-Droid from the [official page](https://f-droid.org/en/packages/com.doubleangels.nextdnsmanagement).

### Manual Installation

Download the latest APK directly from the [GitHub Releases page](https://github.com/doubleangels/NextDNSManager/releases).

---

## FAQ

Please read this list completely **before** you open an issue or your issue may be closed.

### <a id="are-you-associated-with-nextdns-is-this-an-official-app"></a>Are you associated with NextDNS? Is this an official app?

No, this is **not** an official app and I have no ties at all to NextDNS. Unfortunately, this means that the addition of certain features won't be possible. Read more about this below.

### <a id="what-happened-to-the-official-nextdns-app"></a>What happened to the official NextDNS app?

The official app appears to have been taken down. On Google Play, there are a number of reasons why an app may be removed or "unlisted", including violation of Google Play policies, requests from the developers themselves, or other reasons. Since I'm not affiliated with the developers, I'm not sure why this has happened or if/when the official app will return.

### <a id="what-features-wont-be-able-to-be-added-to-this-app-since-its-not-official"></a>What features won't be able to be added to this app since it's not official?

Unless NextDNS/Android make major changes, there are a few features that won't be able to be added. These include:

- Toggle on/off of NextDNS protection, through quick toggles and other means. This is a limitation of Android.
- Connecting through a VPN to NextDNS as was available in the official app. Since I don't have access to official infastructure, there is no VPN server available to facilitate a connection to.
- Changes (additions, removals, edits) to block lists or any of the parental control features.
- Addition of [NXEnhanced](https://github.com/hjk789/NXEnhanced)-like features. The developer of NXEnhanced has ceased development of his project after attempting to work with NextDNS and recieving no response.
- Changes (additions, removals, edits) of NextDNS payment methods.
- Changing of core features or functionality of NextDNS.

### <a id="is-this-app-secure-can-you-access-my-account-or-view-my-dns-queries"></a>Is this app secure? Can you access my account or view my DNS queries?

This is one of the benefits of open source! Anyone can look at all the code and verify for themselves that nothing nafarious is occuring with your data. NextDNS Manager has no access to your account and simply is a way to access the official dashboard on the go. You can think of the app as a very simplified web browser within an app that will only display NextDNS related sites. No information about the app (or your account) leaves your device.

### <a id="what-is-sentry-and-is-it-tracking-me"></a>What is Sentry, and is it tracking me?

No. [Sentry](https://github.com/getsentry/sentry) is a service for developers that gathers information about app crashes, bugs, and other errors and provides them to the developer. This information may contain information about your device (phone type, Android version, etc), about the app (app version, where in the app bugs are occurring, etc), and about the bugs themselves (crash data, stack traces, exceptions, etc). No personal information is collected about you, and nobody other than the maintainer of this project has access to the Sentry error data collected. Furthermore, this is an entirely opt-in option. As of version 5.0.0, there is a toggle in the settings to enable/disable Sentry within your app, and domains to whitelist/blacklist Sentry in your NextDNS configuration are provided. If you choose to disable Sentry, it is not initialized at all. If you choose to enable Sentry, thank you! Your bug and error data helps me push out bug fixes and improvements faster and more reliably.

If you're wondering about Sentry, [check this](https://github.com/doubleangels/nextdnsmanager/issues/445). Sentry is completely opt-in (both via a manual toggle and DNS whitelisting) and only collects anonymized data to help diagnose issues. The information I receive includes:

- **Device model and type**
- **Operating system version**
- **App version and build flavor**
- **Battery life, memory usage, and storage status when an error occurs**
- **Connection type (Wi-Fi/cellular) and VPN status at the time of an error** (No IP addresses are collected or logged)
- **App settings you have enabled** (dark mode, app lock, etc.)
- **Detailed crash reports and error logs**
- **Performance metrics for specific code sections**

This data is solely used to improve app stability by fixing bugs and errors. It remains anonymous, isn't shared with anyone else, and is not used for analytics.

### <a id="i-am-new-to-using-nextdns-and-i-dont-understand-how-to-use-it-can-you-teach-me"></a>I am new to using NextDNS and I don't understand how to use it, can you teach me?

Because NextDNS support and communication from the developers is lacking, I am happy to try and help any users that open an issue. For simple setup/configuration questions or specific questions about this app and its features, I will do whatever I can to help users get to a point where their configuration is working.

I also recommend the r/nextdns community on Reddit, there are many knowledgeable people who can help as well.

### <a id="multiple-versions"></a>I have multiple versions of NextDNS Manager on my phone after the 5.5.0 update

If you see multiple versions of the app on your device after updating to 5.5.0 and have questions, [check this](https://github.com/doubleangels/nextdnsmanager/issues/430).

### <a id="supported-android-versions"></a>Why doesn't the app support Android versions before 12L?

Android apps rely on API calls to interact with the operating system. Each new Android release introduces additional capabilities, which the app takes advantage of to enhance functionality, privacy, and security. Older Android versions lack support for these improvements, making them incompatible with the app's implementation.

### <a id="add-supported-android-versions"></a>Will you bring back support for older android versions?

No. The app is moving forward, not backward.

### <a id="fcm"></a>What is FCM, and why is it disabled in F-Droid builds?

FCM (Firebase Cloud Messaging) is used for push notifications. Currently, it is only utilized to send updates, important information, and known error/fix notifications from me, though it may be expanded in the future.

This feature is removed from F-Droid builds because it relies on Google's services, which are not permitted in F-Droid apps. **If you have de-Googled your device, the standard Google Play version may not function correctly. In that case, try using the `foss` build from the latest GitHub release or the F-Droid version.**

### <a id="ive-read-all-of-this-and-still-have-a-question-what-now"></a>I've read all of this and still have a question, what now?

Please open an [issue](https://github.com/doubleangels/NextDNSManager/issues).

---

## Reporting Issues & Feedback

If you encounter any issues or have suggestions to enhance NextDNS Manager, please take the following steps:

1. Check the [FAQ](#faq) section above for common questions.
2. Open a new [GitHub Issue](https://github.com/doubleangels/NextDNSManager/issues/new/choose) with a detailed description.

_Please note that contributions and responses may take time as this project is maintained in my free time._

---

## Contributing

Interested in contributing? Please review the [Contributing Guidelines](CONTRIBUTING.md) to learn how you can help improve NextDNS Manager.

---

## Security Policy

Learn about my [Security Policy](SECURITY.md) for reporting vulnerabilities and keeping your data safe.

---

## Privacy & Terms

- [Privacy Policy](https://doubleangels.github.io/privacypolicy/nextdns.html)
- [Terms and Conditions](https://doubleangels.github.io/privacypolicy/nextdns_terms.html)

---

## Donations

Donations are completely optional but always appreciated.  
[Donate Here](https://donate.stripe.com/4gw8yhbvH0mg6SQ7ss)

---

## License

NextDNS Manager is released under the [GPLv3 License](LICENSE).

---

I hope you enjoy using NextDNS Manager. Happy managing!
