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

  - [I have multiple versions of NextDNS Manager on my phone after the 5.5.0 update!](#multiple-versions)
  - [What is Sentry, are you tracking me?](#sentry)
  - [Why does F-Droid show an antifeature warning about Sentry?](#antifeature-warning)
  - [Why doesn’t the app support Android versions before 12L?](#supported-android-versions)
  - [Will you bring back support for older Android versions?](#add-supported-android-versions)
  - [What is FCM, and why is it disabled in F-Droid builds?](#fcm)

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

### <a id="multiple-versions"></a>Multiple versions of NextDNS Manager after the 5.5.0 update

If you see multiple versions of the app on your device after updating to 5.5.0 and have questions, [check this](https://github.com/doubleangels/nextdnsmanager/issues/430).

### <a id="sentry"></a>What is Sentry, and is it tracking me?

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

### <a id="antifeature-warning"></a>Why does F-Droid show an Antifeature warning about Sentry?

This warning is misleading. Sentry is fully opt-in in multiple ways and does not collect any personal or identifiable information. I’m working with the F-Droid team to address and resolve this.

### <a id="supported-android-versions"></a>Why doesn’t the app support Android versions before 12L?

Android apps rely on API calls to interact with the operating system. Each new Android release introduces additional capabilities, which the app takes advantage of to enhance functionality, privacy, and security. Older Android versions lack support for these improvements, making them incompatible with the app’s implementation.

### <a id="add-supported-android-versions"></a>Will you bring back support for older android versions?

No. The app is moving forward, not backward.

### <a id="fcm"></a>What is FCM, and why is it disabled in F-Droid builds?

FCM (Firebase Cloud Messaging) is used for push notifications. Currently, it is only utilized to send updates, important information, and known error/fix notifications from me, though it may be expanded in the future.

This feature is removed from F-Droid builds because it relies on Google's services, which are not permitted in F-Droid apps. **If you have de-Googled your device, the standard Google Play version may not function correctly. In that case, try using the `foss` build from the latest GitHub release or the F-Droid version.**

---

## Reporting Issues & Feedback

If you encounter any issues or have suggestions to enhance NextDNS Manager, please take the following steps:

1. Check the [FAQ](FAQ.md) for common questions.
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
