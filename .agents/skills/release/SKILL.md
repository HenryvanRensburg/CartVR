---
name: release
description: >-
  Automate CartVR APK releases via GitHub Actions. Use this skill whenever the user invokes /release or asks to release, publish, bump the version, push, or ship a new CartVR update to GitHub Releases.
---

# CartVR Release Procedure

Automate version bumping, committing, pushing, and tagging `v<versionName>` so `.github/workflows/release.yml` builds and signs `CartVR.apk` for CartVR's in-app updater.

## Rules

* **Do NOT run local Gradle builds or self-test.** GitHub Actions builds and signs the APK remotely.
* **Never stage secrets or IDE state:** Do not commit `keystore.properties`, `*.jks`, or `.idea/`.
* **Windows PowerShell compatibility:** Chain commands with `; if ($LASTEXITCODE -eq 0) { ... }` (never `&&`, which fails in PowerShell 5.1).

## Steps

### 1. Pre-flight Check
Run a single command to inspect pending changes and existing version tags:
```powershell
git status -s; git tag --sort=-v:refname
```

### 2. Verify or Bump Version (`app/build.gradle.kts`)
Read `app/build.gradle.kts` around `versionCode` and `versionName`:
* If the user requested a specific version (e.g., `/release 1.4.0`), set `versionName` to that value and increment `versionCode` by `1`.
* Else if tag `v<versionName>` **already exists** in Git, increment the patch number (`X.Y.Z` -> `X.Y.(Z+1)`) and increment `versionCode` by `1`.
* Else (`v<versionName>` does not exist yet), keep the already-bumped `versionCode` and `versionName`.

### 3. Stage, Commit, Push, and Tag
Stage only project/release files (for example `app`, `gradle`, `docs`, `.github`, `.agents`, `README.md` — never `.idea/`, `keystore.properties`, or `*.jks`), verify `git status -s`, then commit, push `main`, tag, and push the tag:

```powershell
git add <changed-paths>; if ($LASTEXITCODE -eq 0) { git status -s }; if ($LASTEXITCODE -eq 0) { git diff --cached --quiet; if ($LASTEXITCODE -ne 0) { git commit -m "Release v<versionName>: <short summary>" } }; if ($LASTEXITCODE -eq 0) { git push }; if ($LASTEXITCODE -eq 0) { git tag v<versionName> }; if ($LASTEXITCODE -eq 0) { git push origin v<versionName> }
```

### 4. Handoff to User
Report the commit hash, pushed tag `v<versionName>`, and tell the user:
* GitHub Actions takes ~3–4 minutes to build and sign `CartVR.apk`: https://github.com/HenryvanRensburg/CartVR/actions
* Once green, the release appears at https://github.com/HenryvanRensburg/CartVR/releases and CartVR will prompt to update automatically when opened (or via **Settings → Check for Updates**).
