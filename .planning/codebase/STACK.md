# Technology Stack

## Core Language & Runtime
- **Language:** Scala 3.8.1
  - **Compiler Options:** Significant use of experimental and preview features (`-experimental`, `-preview`), strict null safety (`-Yexplicit-nulls`), and macro-related flags.
- **Runtime:** JVM 21

## Build System
- **Tool:** Mill 1.1.0-RC2
- **Plugins:**
  - `mill-contrib-scoverage`: Code coverage analysis.
  - `mill-contrib-sonatypecentral`: Publishing to Maven Central.
  - `scala-steward-mill-plugin`: Automated dependency updates.
  - `mill-util-VcsVersion`: Versioning based on Git state.

## Core Libraries
- **Regex Handling:** `com.github.marianobarrios:dregex:0.9.0`
- **Parsing Reference:** `org.jparsec:jparsec:3.1` (likely for comparison or bootstrapping)
- **Logging:** `org.slf4j:slf4j-api:2.0.17`, `org.slf4j:slf4j-simple:2.0.17`
- **Concurrency:** `com.softwaremill.ox::core:1.0.2` (structured concurrency)

## Testing
- **Framework:** ScalaTest 3.2.19
- **Style:** `AnyFunSuite` with `Matchers`
