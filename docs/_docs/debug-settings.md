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
| `debugDirectory` | String | `null` | Directory path where debug output files will be written |
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

```scala
import mill._
import mill.scalalib._

object myproject extends ScalaModule {
  def scalaVersion = "3.7.4"
  
  def mvnDeps = Seq(
    mvn"io.github.halotukozak::alpaca:0.0.2"
  )
  
  override def scalacOptions = Seq(
    "-Xmacro-settings:debugDirectory=./debug",
    "-Xmacro-settings:enableVerboseNames=true",
    "-Xmacro-settings:compilationTimeout=120s",
    "-Xmacro-settings:trace=stdout",
    "-Xmacro-settings:debug=file",
  )
}
```

**Tip:** You can combine multiple settings in a single flag using commas:

```scala
override def scalacOptions = Seq(
  "-Xmacro-settings:debugDirectory=./debug,enableVerboseNames=true,trace=stdout"
)
```

### SBT

Add debug settings to your `build.sbt`:

```sbt
scalaVersion := "3.7.4"

libraryDependencies += "io.github.halotukozak" %% "alpaca" % "0.0.2"

scalacOptions ++= Seq(
  "-Xmacro-settings:debugDirectory=./debug",
  "-Xmacro-settings:enableVerboseNames=true",
  "-Xmacro-settings:compilationTimeout=120s",
  "-Xmacro-settings:trace=stdout",
  "-Xmacro-settings:debug=file"
)
```

Or combine them:

```sbt
scalacOptions += "-Xmacro-settings:debugDirectory=./debug,enableVerboseNames=true,trace=stdout"
```

### Scala CLI

Use the `--scala-opt` flag to pass debug settings:

```bash
scala-cli run MyLexer.scala \
  --scala 3.7.4 \
  --dep "io.github.halotukozak::alpaca:0.0.2" \
  --scala-opt "-Xmacro-settings:debugDirectory=./debug" \
  --scala-opt "-Xmacro-settings:enableVerboseNames=true"
```

Or add directives in your Scala file:

```scala
//> using scala "3.7.4"
//> using dep "io.github.halotukozak::alpaca:0.0.2"
//> using options "-Xmacro-settings:debugDirectory=./debug"
//> using options "-Xmacro-settings:enableVerboseNames=true"

import alpaca.*

// Your code here
```

## Common Use Cases

### Basic Debugging

Enable basic debug output to see what's happening during macro expansion:

```scala
// Mill
override def scalacOptions = Seq(
  "-Xmacro-settings:debugDirectory=./debug,debug=stdout"
)
```

### Detailed Tracing

For comprehensive debugging with all trace information:

```scala
// Mill
override def scalacOptions = Seq(
  "-Xmacro-settings:debugDirectory=./debug,trace=stdout,debug=stdout,info=stdout"
)
```

### File-Based Logging

Direct all debug output to files instead of console:

```scala
// Mill
override def scalacOptions = Seq(
  "-Xmacro-settings:debugDirectory=./debug,trace=file,debug=file,info=file"
)
```

This creates log files named after your source files (e.g., `MyLexer.scala.log`) in the debug directory.

### Verbose Names for Debugging

Enable verbose names to make generated code more readable:

```scala
// Mill
override def scalacOptions = Seq(
  "-Xmacro-settings:enableVerboseNames=true"
)
```

### Increasing Compilation Timeout

For complex parsers that require more time to compile:

```scala
// Mill
override def scalacOptions = Seq(
  "-Xmacro-settings:compilationTimeout=300s"
)
```

## Troubleshooting

### Debug Files Not Created

If debug files aren't being created:
1. Verify the `debugDirectory` path is valid and writable
2. Ensure you've enabled at least one log level with `file` output
3. Check that you have at least one setting that generates output (e.g., `debug=file`)

### Compilation Timeouts

If you're experiencing compilation timeouts:
1. Increase the `compilationTimeout` setting
2. Simplify your lexer/parser rules if possible
3. Check for infinite loops or excessive recursion in your grammar

### Too Much Output

If you're overwhelmed by debug output:
1. Disable trace/debug/info levels: `trace=disabled,debug=disabled,info=disabled`
2. Keep only warnings and errors: `warn=stdout,error=stdout` (this is the default)
3. Use file output instead of stdout for detailed logs: `debug=file`

## Example: Complete Debugging Setup

Here's a complete example for debugging a complex parser:

```scala
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
    "-Xmacro-settings:debugDirectory=./debug",
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
- Creates a `./debug` directory for output
- Enables verbose names in generated code
- Sets a 3-minute compilation timeout
- Logs detailed trace/debug/info to files
- Displays warnings and errors on the console

## Notes

- Debug settings only affect compile-time behavior; they have no impact on runtime performance
- The debug directory is created automatically if it doesn't exist
- Log files are buffered and flushed periodically (every 4 seconds)
- All log files are closed automatically when the JVM shuts down
- Debug output can be quite verbose; use file output for detailed logging
