# Keystores

One keystore lives in this folder, committed **on purpose**. It is not secret: its passwords are
written below.

| File | Alias | Store / key password | What it signs |
|---|---|---|---|
| `debug.keystore` | `thatstheword-debug` | `android` / `android` | Debug builds, and the debug-signed release APK CI builds for testing (`-PsignReleaseWithDebugKey`) |

Do not regenerate it: every debug install would have to be uninstalled. Committing it means
debug APKs from CI and from any machine share one signature and update each other in place, the
same arrangement as the family's apps. It is the game's own key (created on 12 Nov 2026, RSA 2048,
PKCS12, `CN=That's The Word Debug, O=callbackdev, C=IT`), not a copy of a sister's: each app's debug
builds are signed apart.

SHA-256 certificate fingerprints:

- `debug.keystore`: `66:0B:06:17:0A:8C:E2:01:40:C7:8A:1B:3B:A4:9F:EA:CC:D6:62:39:73:01:4B:EF:59:8D:31:B8:DF:EE:B0:75`
- release key: `14:D4:1B:20:DC:B0:B1:FC:DB:22:42:DE:0B:0C:3D:09:B5:55:07:7E:D5:4D:9B:34:13:F0:00:07:A3:D1:BE:4B`
  (the one published in the root README and pinned in `release.yml`; `CN=callbackdev`, made by the
  owner for this app alone and put in the four secrets on 12 Nov 2026, checked by a signed
  rehearsal and against `keytool` the same day)

## The release key

It must **never** enter the repo (`.gitignore` refuses `*.jks`, `*.keystore`, `*.p12`, `*.pfx`
outside `debug.keystore`). It signs every release for the life of the app, and a future Google
Play listing must reuse it (PLANNING.md §11, Google Play), so losing it means players cannot update
without uninstalling. It is kept outside the repo, with the family's keys and an offline backup.

Creating it (once, by the owner, on a machine of theirs; done on 12 Nov 2026, the file name and
the alias the owner's own):

```bash
keytool -genkeypair -v -keystore thatstheword-release.jks -storetype PKCS12 \
  -alias thatstheword -keyalg RSA -keysize 4096 -validity 10950
```

Reading its fingerprint (the certificate is encrypted with the store password, which keytool
asks for):

```bash
keytool -list -v -keystore thatstheword-release.jks -alias thatstheword
```

### In GitHub Actions

`release.yml` reads it from four repository secrets (Settings, Secrets and variables, Actions),
the same names as the family's:

- `KEYSTORE_BASE64`: the keystore file, `base64 -w0 thatstheword-release.jks`
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

The workflow decodes the keystore into the runner's temp folder, passes the rest to Gradle as
`ORG_GRADLE_PROJECT_*` variables, and stops by name if a secret is missing. Nothing secret is
written to the logs or to the command line. It then checks the APK's signature against the
fingerprint above, and refuses any other key.

### Locally

Put the four properties in `~/.gradle/gradle.properties`:

```properties
THATSTHEWORD_KEYSTORE=/absolute/path/to/thatstheword-release.jks
THATSTHEWORD_KEYSTORE_PASSWORD=...
THATSTHEWORD_KEY_ALIAS=thatstheword
THATSTHEWORD_KEY_PASSWORD=...
```

With all four set, `./gradlew :app:assembleRelease` signs with the release key; without them the
release build is unsigned, and `-PsignReleaseWithDebugKey` signs it with the debug key for
testing only.
