# Lera Kindle Transfer

Lera Kindle Transfer is a small JavaFX desktop app for sending ebook files from macOS to a Kindle running KOReader over SSH/SFTP. It is intentionally not a general SFTP client: it only uploads selected local books into the configured KOReader books folder.

## What it does

- Tests the KOReader SSH/SFTP connection.
- Can initialize key-based SSH with **Init secure connection**.
- Starts in a simplified connection view when a local private key already exists, with **Advanced connection** available for SSH details.
- Accepts `.epub`, `.pdf`, `.mobi`, `.azw3`, `.cbz`, `.cbr`, `.txt`, `.docx`, `.fb2`, and `.djvu`.
- Supports Finder drag and drop and multi-file selection.
- Sanitizes uploaded filenames.
- Avoids silent overwrites by creating names like `Book (1).epub`.
- Uploads to `filename.ext.part`, verifies file size, then renames to `filename.ext`.
- Stores config at `~/Library/Application Support/Lera Kindle Transfer/config.json`.
- Stores generated SSH keys at `~/Library/Application Support/Lera Kindle Transfer/keys/`.
- Stores logs at `~/Library/Logs/Lera Kindle Transfer/app.log`.
- Does not store passwords or private key passphrases.

## Requirements

- JDK 21 or newer. The build emits Java 21 bytecode.
- Gradle, or a Gradle wrapper added to the project.
- macOS for `.app` and `.dmg` packaging with `jpackage`.

## Run in development

```bash
./gradlew run
```

If you do not have a Gradle wrapper in the checkout yet, install Gradle and run:

```bash
gradle run
```

## Build

```bash
./gradlew build
```

## Package for macOS

Build a self-contained `.app` bundle:

```bash
./gradlew packageMacApp
```

The app image is written to:

```text
build/jpackage/app/Lera Kindle Transfer.app
```

Build a `.dmg` installer:

```bash
./gradlew packageMacDmg
```

The DMG is written under:

```text
build/jpackage/dmg/
```

Both packaging tasks require macOS and a JDK 21 that includes `jpackage`.

## Cross-platform releases

JavaFX/jpackage packages are built natively per operating system. The project does not cross-compile a Windows installer from macOS; instead, GitHub Actions builds on macOS, Windows, and Linux runners.

The release workflow creates:

- macOS `.dmg`
- Windows self-contained app image `.zip`
- Linux self-contained app image `.tar.gz`

To publish a release, push a tag:

```bash
git tag v1.0.1
git push origin v1.0.1
```

You can also run the **Release** workflow manually from GitHub Actions and choose an existing tag, such as `v1.0.1`.

## Expected KOReader settings

- Host: the IP address shown by KOReader.
- Port: `2222`.
- Username: `root`.
- Remote folder: `/mnt/us/books`.

Enable the KOReader server on the Kindle:

```text
KOReader > Network > SSH server
```

## Init secure connection

The **Init secure connection** button guides first-time setup for key-based SSH:

1. On the Kindle, open KOReader, enable Wi-Fi, open `Network > SSH server`, enable temporary passwordless login, and start the SSH server.
2. In the app, enter the Kindle IP address and click **Init secure connection**.
3. The app connects as `root` / `root`, creates an ECDSA key pair for this Mac, and appends the public key to KOReader's `settings/SSH/authorized_keys`.
4. The app asks you to stop the KOReader SSH server, disable temporary passwordless login, and start the SSH server again.
5. The app tests the new private-key connection and saves the configuration as private-key auth.

After a key has been created, the app opens in a simplified connection view. It keeps the Kindle IP visible and hides the port, username, authentication mode, private key, and passphrase fields. Use **Advanced connection** to show those settings again.

On Kindle, the app writes the public key only to:

```text
/mnt/us/koreader/settings/SSH/authorized_keys
```

It does not browse the remote filesystem.

## Security note

The no-password mode is for KOReader temporary passwordless SSH mode. Use it only on trusted home Wi-Fi and only while transferring books. Passwords and private key passphrases are requested at runtime and are never saved.

This first version accepts the Kindle SSH host key automatically to keep setup simple for non-technical users on a trusted home network.

## Troubleshooting

### App cannot connect

Make sure KOReader is open, Wi-Fi is enabled, and the SSH server is started in `KOReader > Network > SSH server`.

### Wrong IP

Use the IP address shown by KOReader after Wi-Fi is enabled. The IP can change between Wi-Fi sessions.

### Wi-Fi not enabled

Enable Wi-Fi on the Kindle first, then start the KOReader SSH server.

### SSH server not started

Start it in KOReader before pressing **Test Connection**.

### Authentication failed

Check whether KOReader is using password, private key, or temporary no-password mode. Passwords and passphrases are not saved by the app.

### Transfer interrupted

Keep the Kindle awake and near Wi-Fi until the transfer completes. Incomplete `.part` files created by the app are removed when possible after a failed upload.
