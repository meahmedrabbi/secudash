# SecuDash

Android app usage report dashboard with built-in uninstall prevention.

**Package name:** `com.bfoxnet.dashboard`  
**Min SDK:** 26 (Android 8.0)

---

## Features

| Feature | Details |
|---------|---------|
| App Usage Dashboard | Per-app foreground time for Today / Last 7 days / Last 30 days |
| Uninstall prevention – Device Admin | `DeviceAdminReceiver` blocks deactivation and immediately re-prompts activation |
| Uninstall prevention – Accessibility | `ProtectionAccessibilityService` detects dangerous Settings screens and navigates away |
| Always runs in background | `MonitorService` foreground service with `START_STICKY` + `BootReceiver` for auto-restart after reboot |
| Permission gate on first launch | `MainActivity` checks all three required permissions before allowing access to the dashboard |

---

## Required permissions

The app will not open the dashboard until all three permissions are granted:

1. **App Usage Access** (`PACKAGE_USAGE_STATS`) – granted via *Settings → Apps → Special App Access → Usage access*
2. **Device Administrator** – granted via the Device Admin activation screen
3. **Accessibility Service** – enabled via *Settings → Accessibility → SecuDash Protection*

---

## Build

### Debug (local)

```bash
./gradlew assembleDebug
```

### Release (signed) – local

```bash
./gradlew assembleRelease
```

Then sign with `apksigner`:

```bash
apksigner sign \
  --ks release.keystore \
  --ks-pass pass:YOUR_STORE_PASS \
  --ks-key-alias secudash \
  --key-pass pass:YOUR_KEY_PASS \
  app/build/outputs/apk/release/app-release-unsigned.apk
```

---

## GitHub Actions – Automatic Build & Sign

The workflow at `.github/workflows/build.yml` builds and signs a release APK on every push or pull request to `main`/`master`.

### Required repository secrets

Go to **Settings → Secrets and variables → Actions** and add:

| Secret name | Value |
|-------------|-------|
| `KEYSTORE_BASE64` | Base-64-encoded release keystore (`.jks` / `.keystore`) |
| `KEYSTORE_PASSWORD` | Keystore password |
| `KEY_ALIAS` | Signing key alias inside the keystore |
| `KEY_PASSWORD` | Signing key password |

### Creating and encoding a keystore

```bash
keytool -genkey -v \
  -keystore release.keystore \
  -alias secudash \
  -keyalg RSA -keysize 2048 \
  -validity 10000 \
  -storepass YOUR_STORE_PASS \
  -keypass  YOUR_KEY_PASS \
  -dname "CN=SecuDash, OU=Mobile, O=BFoxNet, L=City, S=State, C=US"

# Encode to Base-64 and copy into the KEYSTORE_BASE64 secret
base64 -w 0 release.keystore
```

The signed APK is uploaded as a workflow artifact named **`secudash-release-apk`** and retained for 14 days.
