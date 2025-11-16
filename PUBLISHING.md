# Publishing Guide

This document describes how to publish the Alpaca library to Maven Central.

## Prerequisites

Before publishing, ensure you have:

1. **GPG Key**: A GPG key pair for signing artifacts
   ```bash
   gpg --gen-key
   gpg --list-keys
   ```

2. **Sonatype Account**: An account on [Sonatype OSSRH](https://central.sonatype.org/)
   - Request access to the `io.github.halotukozak` group ID

3. **Credentials Configuration**: Set up your credentials in `~/.mill/credentials`:
   ```properties
   SONATYPE_USERNAME=your-username
   SONATYPE_PASSWORD=your-password
   PGP_KEY_PASSWORD=your-gpg-passphrase
   ```

## Publishing Steps

### 1. Verify the Build

First, ensure everything compiles and tests pass:

```bash
./mill compile
./mill test
./mill docJar
```

### 2. Generate Publishing Artifacts

Build the artifacts that will be published:

```bash
# Generate JAR files
./mill jar

# Generate source JAR
./mill sourceJar

# Generate documentation JAR
./mill docJar

# Generate POM file
./mill publishPomSettings
```

### 3. Publish to Maven Central

#### Local Publishing (for testing)

```bash
# Publish to local Maven repository
./mill publishLocal
```

#### Publish to Sonatype

```bash
# Publish and sign artifacts
./mill publish \
  --sonatypeCreds $SONATYPE_USERNAME:$SONATYPE_PASSWORD \
  --gpgArgs --passphrase=$PGP_KEY_PASSWORD,--batch,--yes,-a,-b \
  --publishArtifacts __.publishArtifacts \
  --readTimeout 600000 \
  --awaitTimeout 600000 \
  --release true
```

### 4. Release Process

After publishing to Sonatype:

1. **Staging**: Artifacts are uploaded to a staging repository
2. **Verification**: Sonatype performs automated checks
3. **Release**: If checks pass, the release is promoted to Maven Central
4. **Sync**: After a few hours, the artifacts appear in Maven Central

## Version Management

The version is defined in `build.mill`:

```scala
def alpacaVersion = "0.1.0"
```

When releasing:
1. Update the version number
2. Commit the change: `git commit -am "Release version X.Y.Z"`
3. Tag the release: `git tag -a vX.Y.Z -m "Release version X.Y.Z"`
4. Push the tag: `git push origin vX.Y.Z`
5. Publish to Maven Central

## Post-Release

After successful publication:

1. Update the version in `build.mill` to the next snapshot version (e.g., "0.2.0-SNAPSHOT")
2. Update the README.md with the new version number
3. Create a GitHub release with release notes

## Troubleshooting

### Authentication Issues

If you encounter authentication issues:
- Verify your Sonatype credentials
- Check that your GPG key is properly configured
- Ensure the key is uploaded to a public keyserver: `gpg --keyserver keyserver.ubuntu.com --send-keys YOUR_KEY_ID`

### Validation Errors

If artifacts fail validation:
- Ensure all required metadata is present in `pomSettings`
- Verify that artifacts are properly signed
- Check that the POM file is valid XML

## Resources

- [Mill Documentation - Publishing](https://mill-build.com/mill/scalalib/Publishing_Modules.html)
- [Sonatype OSSRH Guide](https://central.sonatype.org/publish/publish-guide/)
- [Maven Central Requirements](https://central.sonatype.org/publish/requirements/)
