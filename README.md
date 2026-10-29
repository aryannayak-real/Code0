# Code0 — Mobile Coding Studio for Android

**Code0** is a mobile-first coding studio application designed for Android devices. It enables developers to create and manage coding projects, browse and edit source files with syntax highlighting, connect to GitHub, download repositories, leverage AI for understanding and multi-file code modifications, review proposed diffs with conflict detection before applying them, and trigger reproducible Android APK builds via GitHub Actions.

---

## Key Features

1. **Project Management & File Explorer**
   - Built-in starter templates: Android / Kotlin, Python CLI, Web (HTML/CSS/JS), TypeScript / Node, and Empty projects.
   - Real disk-based persistence in app storage (`code0_projects/`).
   - 3 File Explorer Layouts:
     - **Expandable Tree**: Inline expand and collapse.
     - **Traditional Navigation**: Directory-by-directory breadcrumb navigation.
     - **Combined Layout**: Navigable folder structure paired with file list.
   - File search, create, rename, delete, and disk usage tracking.

2. **Mobile-First Code Editor**
   - Line numbers gutter synced with scrolling.
   - Syntax highlighting tokens for Kotlin, Java, Python, JavaScript, TypeScript, HTML, CSS, JSON, and Shell.
   - Soft symbol quick access bar (`{`, `}`, `(`, `)`, `[`, `]`, `;`, `=`, `<`, `>`, `/`, `"`, `'`, `$`, `:`, `_`, `.`).
   - Find & Replace in file.
   - Undo and Redo stack with dirty status indicator (`*`).

3. **AI Studio (Chat & Agent Modes)**
   - **Multiple Providers**: Google Gemini, OpenAI, Anthropic Claude, and custom endpoints.
   - **Chat Mode**: Explain code, discuss architecture, answer questions with project context.
   - **Agent Mode**: Multi-file development agent that inspects real files, plans modifications, and produces proposed changes without overwriting original files.

4. **Change Review, Apply, and Reject Workflow (Stages A to F)**
   - **Stage A (Staging)**: Changes prepared separately in memory/database. Original files are untouched.
   - **Stage B (Completion Banner)**: Clear notification showing affected file count and validation status with `Review`, `Apply`, and `Reject` actions.
   - **Stage C (Diff Review)**: Unified diff showing line numbers, added lines (`+`), and removed lines (`-`), per-file inclusion checkboxes, and conflict warnings.
   - **Stage D (Atomic Apply)**: Pre-apply backup for recovery, atomic file write, and disk validation.
   - **Stage E (Reject)**: Safely discards proposals without touching working tree or creating commits.
   - **Stage F (Undo & Recovery)**: Snapshots stored for applied changes, allowing one-tap reversal with confirmation.

5. **GitHub Integration & Repository Downloads**
   - Connect via Personal Access Token (PAT).
   - Browse authenticated user repositories and search public GitHub repositories.
   - Download complete repositories preserving exact directory hierarchies.
   - Inspect local Git status (modified, untracked, deleted).
   - Push commits to GitHub with **mandatory explicit confirmation dialog** showing target branch, commit message, and files to push.

6. **Build, Test, and Diagnostics**
   - Built-in structure and syntax integrity checker.
   - Verification logs streaming and diagnostics export.
   - Project ZIP export.

7. **Customization & Themes**
   - **Presets**: Charcoal + Green (Default), Black + Orange, White + Grey, Black + Silver.
   - **Appearance Modes**: Dark, Light, System.
   - First-launch setup flow (customizable and skippable).

---

## Security & Secrets

- API keys (Gemini, GitHub PAT, OpenAI) are managed securely through user preferences and BuildConfig.
- Credentials are never logged or exported in diagnostics reports.
- Applying AI proposals locally never automatically commits or pushes to remote Git repositories.

---

## Continuous Integration & Reproducible Builds

Code0 includes a production-ready GitHub Actions workflow in `.github/workflows/android-build.yml`:
- Triggered on push to `main`, pull request to `main`, or manual `workflow_dispatch`.
- Validates Gradle wrapper.
- Sets up JDK 17 (Temurin).
- Executes unit tests: `gradle :app:testDebugUnitTest`.
- Assembles debug APK: `gradle :app:assembleDebug`.
- Automatically uploads `code0-debug-apk` and test report artifacts.

---

## Running Local Unit Tests

Run the comprehensive unit and Robolectric test suites using Gradle:

```bash
gradle :app:testDebugUnitTest
```
