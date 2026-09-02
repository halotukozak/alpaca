# Releasing the Alpaca IntelliJ plugin

The plugin is published to the [JetBrains Marketplace](https://plugins.jetbrains.com/) from the
[**Publish IntelliJ Plugin**](../.github/workflows/ij-plugin-publish.yml) workflow (`workflow_dispatch`, Actions tab).
Everything below the one-time setup is the per-release checklist.

## One-time setup

### 1. Generate a signing certificate

Marketplace strongly recommends signed plugins. Generate a self-issued chain once (see
the [plugin signing docs](https://plugins.jetbrains.com/docs/intellij/plugin-signing.html)):

```bash
openssl genpkey -aes-256-cbc -algorithm RSA -out private.pem -pkeyopt rsa_keygen_bits:4096
openssl req -key private.pem -new -x509 -days 3650 -out chain.crt \
  -subj "/CN=Alpaca IntelliJ plugin/O=halotukozak"
```

Keep `private.pem`, its passphrase, and `chain.crt` somewhere safe (a password manager).

### 2. Get a Marketplace token

Marketplace → your profile → **My Tokens** → generate a permanent token scoped to plugin upload.

### 3. Add the repository secrets

`Settings → Secrets and variables → Actions` (or a `marketplace` Environment — see the commented
`environment:` line in the workflow):

| Secret                 | Value                             |
|------------------------|-----------------------------------|
| `PUBLISH_TOKEN`        | the Marketplace token from step 2 |
| `CERTIFICATE_CHAIN`    | full contents of `chain.crt`      |
| `PRIVATE_KEY`          | full contents of `private.pem`    |
| `PRIVATE_KEY_PASSWORD` | the passphrase from step 1        |

### 4. First upload (manual, once)

`publishPlugin` can only *update* a plugin that already exists on the Marketplace. For the very first version, build it
locally and upload by hand:

```bash
cd ij-plugin
./gradlew buildPlugin           # -> build/distributions/alpaca-ij-plugin-<version>.zip
```

Upload that zip at <https://plugins.jetbrains.com/plugin/add>, fill in the listing, and wait for JetBrains moderation
(usually a couple of business days). After it is approved and live, every later version goes through the workflow.

## Per-release checklist

1. Bump `version` in [`gradle.properties`](gradle.properties) (plain `x.y.z`, or `x.y.z-eap.N` /
   `x.y.z-beta` for a pre-release channel).
2. `cd ij-plugin && ./gradlew patchChangelog` — moves `[Unreleased]` in `CHANGELOG.md` to a
   `[x.y.z]` section and reseeds an empty `[Unreleased]`. Review the diff.
3. Commit both files, open a PR, merge to `main`.
4. Actions tab → **Publish IntelliJ Plugin** → *Run workflow*. Leave `dry_run` off for a real publish; tick it to only
   build + sign + verify.
5. The workflow runs `verifyPlugin`, then `buildPlugin signPlugin`, uploads the signed zip as a run artifact, and
   finally `publishPlugin`. Stable versions appear after a short re-moderation; pre-release channels are immediate but
   only reach users who added the channel's repository URL.

## Notes

- `sinceBuild` is `252` (2025.2); `untilBuild` is intentionally open — the plugin only uses stable
  `com.intellij.modules.platform` API. Bump `sinceBuild` only when adopting newer platform API.
- The channel is derived from the version suffix in `build.gradle.kts`: no suffix → `default`
  (stable), `-eap.1` → `eap`, `-beta` → `beta`.
- CI (`ij-plugin-test.yml`) already runs `verifyPlugin` on every PR touching `ij-plugin/`, so a green PR means the
  publish workflow's verification step will pass too.
