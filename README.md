# Fil

> Le fil de vos jours. Toujours.

Companion in the Bios ecosystem — specialized capture surface for MS and neurological symptom tracking. Pushes computed events to Bios's metric bus for cross-correlation.

See [MANIFESTO.md](MANIFESTO.md) for the project ethos and [docs/ECOSYSTEM.md](docs/ECOSYSTEM.md) for how Fil relates to the rest of the Bios suite.

## Install

### Option 1: Direct download

Grab the latest signed APK from [Releases](https://github.com/thdelmas/Fil/releases/latest). Verify SHA-256 against the published `.sha256` file, then sideload. "Install unknown apps" must be enabled.

### Option 2: Obtainium (recommended for updates)

[Obtainium](https://github.com/ImranR98/Obtainium) tracks GitHub Releases and auto-updates. Add this repo URL:

```
https://github.com/thdelmas/Fil
```

### Signing identity

All apps in the Bios ecosystem share one signing key. Cert SHA-256:

```
D4:18:F5:1B:E9:D0:28:5D:0B:A8:27:4B:0E:E9:67:8F:F9:DB:DC:1D:32:D5:97:3C:ED:F3:23:59:3F:55:46:33
```

Compare against `apksigner verify --print-certs Fil-vX.Y.Z.apk` before trusting an install.

## Release flow

Push a tag `vX.Y.Z` from `main` → GitHub Actions builds and signs the APK, publishes a Release with auto-generated notes. See [.github/workflows/release.yml](.github/workflows/release.yml).
