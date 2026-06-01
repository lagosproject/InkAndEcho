# Contributing to Ink & Echo

Thank you for choosing to contribute to **Ink & Echo**! We appreciate your help in making this app better.

---

## 🛠️ Getting Started

1. **Fork the Repository** on GitHub.
2. **Clone your fork** locally:
   ```bash
   git clone https://github.com/<your-username>/InkAndEcho.git
   cd InkAndEcho
   ```
3. **Set up the upstream remote**:
   ```bash
   git remote add upstream https://github.com/lagosproject/InkAndEcho.git
   ```

---

## 🌿 Branching Strategy

We follow a simple branch naming convention:
- **Features:** `feature/short-description`
- **Bugfixes:** `bugfix/short-description`
- **Documentation:** `docs/short-description`

Always create your branch from the `main` branch:
```bash
git checkout main
git pull upstream main
git checkout -b feature/my-cool-feature
```

---

## 💻 Coding Standards

- Write clean, idiomatic Kotlin code using Android Jetpack Compose best practices.
- Maintain formatting matching the Gradle Kotlin style rules.
- Ensure all resources (strings, colors, dimensions) are extracted into resource files (`strings.xml`, etc.) rather than hardcoded in Compose functions.
- Run tests and lint checks locally before committing:
  ```bash
  ./gradlew lintDebug
  ./gradlew testDebugUnitTest
  ```

---

## 🔒 Security Best Practices

* **NO SECRETS:** Double-check that you have not added or committed any private signing keys, passwords, or API tokens.
* Keep `keystore.properties` and local keys outside of git tracking.

---

## 📤 Submitting a Pull Request

1. Push your branch to your GitHub fork:
   ```bash
   git push origin feature/my-cool-feature
   ```
2. Open a Pull Request from your branch to the `main` branch of the upstream repository.
3. Fill out the Pull Request template checklist.
4. A maintainer will review your code. Once approved and checks pass, it will be merged!
