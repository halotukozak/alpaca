# Debug Settings

Alpaca provides compile-time debug settings that help you troubleshoot and understand macro expansion, lexer, and parser behavior during compilation.

## Overview

Debug settings are configured using the Scala compiler's `-Xmacro-settings` flag. These settings control:
- Debug output directory
- Compilation timeout
- Verbose naming
- Log levels and outputs

## Available Settings

### Core Settings

| Setting | Type | Default | Description |
|---------|------|---------|-------------|
| `debugDirectory` | String | `null` | **Absolute** directory path where debug output files will be written. For Mill, use `$moduleDir/debug`; for SBT, use an absolute path or `${baseDirectory.value}/debug` |
| `compilationTimeout` | Duration | `90s` | Maximum time allowed for macro compilation before timeout |
| `enableVerboseNames` | Boolean | `false` | Enable verbose naming in generated code for better debugging |

### Log Level Settings

Each log level can be configured with one of three output modes:

| Log Level | Default Output | Description |
|-----------|----------------|-------------|
| `trace` | `disabled` | Most detailed logging for fine-grained debugging |
| `debug` | `disabled` | Debug-level messages during macro expansion |
| `info` | `disabled` | Informational messages |
| `warn` | `stdout` | Warning messages (printed to console by default) |
| `error` | `stdout` | Error messages (printed to console by default) |

**Output Modes:**
- `stdout` - Print to standard output (console)
- `file` - Write to a log file in the debug directory
- `disabled` - Suppress output for this level

## Configuration

### Mill

Add debug settings to your `scalacOptions` in `build.mill`:

```scala sc:nocompile
import mill._
import mill.scalalib._

object myproject extends ScalaModule {
  def scalaVersion = "3.7.4"

  def mvnDeps = Seq(
    mvn"io.github.halotukozak::alpaca:0.0.2"
  )

  override def scalacOptions = Seq(
    s"-Xmacro-settings:debugDirectory=$moduleDir/debug",
    "-Xmacro-settings:enableVerboseNames=true",
    "-Xmacro-settings:compilationTimeout=120s",
    "-Xmacro-settings:trace=stdout",
    "-Xmacro-settings:debug=file",
  )
}
```

**Tip:** You can combine multiple settings in a single flag using commas:

```scala sc:nocompile
override def scalacOptions = Seq(
  s"-Xmacro-settings:debugDirectory=$moduleDir/debug,enableVerboseNames=true,trace=stdout"
)
```

**Note:** `debugDirectory` must be an absolute path. In Mill, use `$moduleDir` to reference the module directory.

### SBT

Add debug settings to your `build.sbt`:

```sbt
scalaVersion := "3.7.4"

libraryDependencies += "io.github.halotukozak" %% "alpaca" % "0.0.2"

scalacOptions ++= Seq(
  s"-Xmacro-settings:debugDirectory=${baseDirectory.value}/debug",
  "-Xmacro-settings:enableVerboseNames=true",
  "-Xmacro-settings:compilationTimeout=120s",
  "-Xmacro-settings:trace=stdout",
  "-Xmacro-settings:debug=file"
)
```

Or combine them:

```sbt
scalacOptions += s"-Xmacro-settings:debugDirectory=${baseDirectory.value}/debug,enableVerboseNames=true,trace=stdout"
```

**Note:** `debugDirectory` must be an absolute path. In SBT, use `${baseDirectory.value}` to reference the project directory.

### Scala CLI

Use the `--scala-opt` flag to pass debug settings:

```bash
scala-cli run MyLexer.scala \
  --scala 3.7.4 \
  --dep "io.github.halotukozak::alpaca:0.0.2" \
  --scala-opt "-Xmacro-settings:debugDirectory=/absolute/path/to/debug" \
  --scala-opt "-Xmacro-settings:enableVerboseNames=true"
```

Or add directives in your Scala file:

```scala sc:nocompile
//> using scala "3.7.4"
//> using dep "io.github.halotukozak::alpaca:0.0.2"
//> using options "-Xmacro-settings:debugDirectory=/absolute/path/to/debug"
//> using options "-Xmacro-settings:enableVerboseNames=true"

import alpaca.*

// Your code here
```

**Note:** `debugDirectory` must be an absolute path. Use an absolute path like `/Users/username/project/debug` or `/home/user/project/debug`.

## Example: Complete Debugging Setup

Here's a complete example for debugging a complex parser:

```scala sc:nocompile
// build.mill
import mill._
import mill.scalalib._

object myparser extends ScalaModule {
  def scalaVersion = "3.7.4"

  def mvnDeps = Seq(
    mvn"io.github.halotukozak::alpaca:0.0.2"
  )

  override def scalacOptions = Seq(
    // Enable all debug features
    s"-Xmacro-settings:debugDirectory=$moduleDir/debug",
    "-Xmacro-settings:enableVerboseNames=true",
    "-Xmacro-settings:compilationTimeout=180s",

    // Log everything to files
    "-Xmacro-settings:trace=file",
    "-Xmacro-settings:debug=file",
    "-Xmacro-settings:info=file",

    // Keep warnings and errors on console
    "-Xmacro-settings:warn=stdout",
    "-Xmacro-settings:error=stdout"
  )
}
```

This configuration:
- Creates a `debug` directory in your module directory for output
- Enables verbose names in generated code
- Sets a 3-minute compilation timeout
- Logs detailed trace/debug/info to files
- Displays warnings and errors on the console

**Finding log files:** When using `file` output mode, log files are created in the `debugDirectory` with names based on your source files (e.g., `MyLexer.scala.log`). The full path will be `$moduleDir/debug/MyLexer.scala.log` for Mill projects.

## Notes

- **`debugDirectory` must be an absolute path.** Use build tool variables like `$moduleDir` (Mill) or `${baseDirectory.value}` (SBT) to construct the path
- Debug settings only affect compile-time behavior; they have no impact on runtime performance
- The debug directory is created automatically if it doesn't exist
- Log files are buffered and flushed periodically (every 4 seconds)
- All log files are closed automatically when the JVM shuts down
- Debug output can be quite verbose; use file output for detailed logging
- Log files are named after your source files (e.g., `MyLexer.scala.log`) and placed in the `debugDirectory`
