# Lera Kindle Transfer

<img src="packaging/LeraKindleTransfer.png" alt="Lera Kindle Transfer icon" width="96" align="right">

Lera Kindle Transfer is a small desktop app for sending books to a Kindle running KOReader over SSH/SFTP.

It is built for one job: pick local ebook files, connect to KOReader's SSH server, and upload them to the Kindle books folder without using terminal commands or a general SFTP client.

## Download

Download the latest release from:

https://github.com/ilaario/lera-kindle-transfer/releases/latest

Available packages:

- macOS `.dmg`
- Windows self-contained app `.zip`
- Linux self-contained app `.tar.gz`

The packaged app includes its own Java runtime.

## Features

- Tests the KOReader SSH/SFTP connection.
- Transfers `.epub`, `.pdf`, `.mobi`, `.azw3`, `.cbz`, `.cbr`, `.txt`, `.docx`, `.fb2`, and `.djvu` files.
- Supports drag and drop and multi-file selection.
- Includes **Init secure connection** for guided SSH key setup.
- Creates a local ECDSA key pair and installs the public key in KOReader.
- Starts in a simplified connection view after a key has been created.
- Keeps advanced SSH settings available behind **Advanced connection**.
- Sanitizes uploaded filenames.
- Avoids silent overwrites by creating names like `Book (1).epub`.
- Uploads as `filename.ext.part`, verifies file size, then renames to `filename.ext`.
- Does not save passwords or private key passphrases.

## Quick Start

1. Install KOReader on the Kindle.
2. Open KOReader, enable Wi-Fi, then start the SSH server from `Network > SSH server`.
3. Open Lera Kindle Transfer.
4. Enter the Kindle IP address shown by KOReader.
5. Click **Init secure connection** for first-time key setup, or **Test Connection** if you already configured SSH.
6. Select or drop books into the app.
7. Click **Transfer to Kindle**.

## KOReader Settings

Expected defaults:

- Host: the IP address shown by KOReader.
- Port: `2222`.
- Username: `root`.
- Remote folder: `/mnt/us/books`.

The secure setup flow writes the public key to:

```text
/mnt/us/koreader/settings/SSH/authorized_keys
```

It does not browse the remote filesystem.

## Secure Connection Setup

The **Init secure connection** button guides first-time setup for key-based SSH:

1. On the Kindle, open KOReader, enable Wi-Fi, open `Network > SSH server`, enable temporary passwordless login, and start the SSH server.
2. In the app, enter the Kindle IP address and click **Init secure connection**.
3. The app connects as `root` / `root`, creates an ECDSA key pair for this computer, and appends the public key to KOReader's `settings/SSH/authorized_keys`.
4. The app asks you to stop the KOReader SSH server, disable temporary passwordless login, and start the SSH server again.
5. The app tests the new private-key connection and saves the configuration as private-key auth.

After a key has been created, the app opens in a simplified connection view. It keeps the Kindle IP visible and hides the port, username, authentication mode, private key, and passphrase fields. Use **Advanced connection** to show those settings again.

## Local Files

macOS:

- Config: `~/Library/Application Support/Lera Kindle Transfer/config.json`
- Keys: `~/Library/Application Support/Lera Kindle Transfer/keys/`
- Logs: `~/Library/Logs/Lera Kindle Transfer/app.log`

Windows and Linux:

- Config: `~/.config/Lera Kindle Transfer/config.json`
- Keys: `~/.config/Lera Kindle Transfer/keys/`
- Logs: `~/.config/Lera Kindle Transfer/logs/app.log`

## Security Note

KOReader's temporary passwordless SSH mode is only for first-time setup on a trusted local network. Turn it off after the key has been installed.

The app accepts the Kindle SSH host key automatically to keep setup simple for non-technical users on home Wi-Fi. Passwords and private key passphrases are requested at runtime and are not saved.

## Troubleshooting

### App cannot connect

Make sure KOReader is open, Wi-Fi is enabled, and the SSH server is started in `KOReader > Network > SSH server`.

### Wrong IP

Use the IP address shown by KOReader after Wi-Fi is enabled. The IP can change between Wi-Fi sessions.

### Authentication failed

Check whether KOReader is using password, private key, or temporary no-password mode. Passwords and passphrases are not saved by the app.

### Transfer interrupted

Keep the Kindle awake and near Wi-Fi until the transfer completes. Incomplete `.part` files created by the app are removed when possible after a failed upload.

## Development

Requirements:

- JDK 21 or newer.
- The Gradle wrapper included in this repository.

Run in development:

```bash
./gradlew run
```

Build:

```bash
./gradlew build
```

Build a self-contained app image for the current operating system:

```bash
./gradlew packageAppImage
```

Build a macOS `.dmg`:

```bash
./gradlew packageMacDmg
```

## Releases

JavaFX and `jpackage` packages are built natively per operating system. GitHub Actions builds releases on macOS, Windows, and Linux runners.

To publish a release, push a tag:

```bash
git tag -a v1.0.3 -m "Release v1.0.3"
git push origin v1.0.3
```

The release workflow uploads:

- `Lera-Kindle-Transfer-macos.dmg`
- `Lera-Kindle-Transfer-windows-x64.zip`
- `Lera-Kindle-Transfer-linux-x64.tar.gz`

## Disclaimer

Lera Kindle Transfer is an independent project. It is not affiliated with Amazon, Kindle, or KOReader.
