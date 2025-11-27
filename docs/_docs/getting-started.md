# Alpaca

## Internal Utilities

Alpaca provides several internal utilities that are used throughout the library for macro expansion and type manipulation.

### Empty

A type class for creating empty instances of types.

`Empty[T]` provides a way to create default instances of Product types (case classes) by using their default parameter values. It extends `Function0` to act as a factory.

```scala
private[alpaca] trait Empty[T] extends (() => T)
```

The `Empty` companion object provides automatic derivation for any Product type with default parameters. This macro-based derivation uses the default values of constructor parameters to create a factory for the type.

**Usage:**
```scala
case class Config(name: String = "default", count: Int = 0)

// Automatically derives an Empty instance
val empty = summon[Empty[Config]]
val instance: Config = empty() // Config("default", 0)
```

### ReplaceRefs

A TreeMap that replaces symbol references in a tree.

`ReplaceRefs` is used during macro expansion to transform trees by replacing references to specific symbols with replacement terms. This is useful for adapting code from one context to another.

```scala
private[internal] final class ReplaceRefs[Q <: Quotes](using val quotes: Q)
```

**Usage:**
Given a sequence of `(symbol to find, term to replace with)` pairs, this creates a TreeMap that will substitute all references to the found symbols with the corresponding replacement terms.

```scala
val replaceRefs = ReplaceRefs()
val treeMap = replaceRefs((findSymbol, replaceTerm))
val transformedTree = treeMap.transformTree(originalTree)(owner)
```

### CreateLambda

A helper for creating lambda expressions during macro expansion.

`CreateLambda` provides a way to construct function expressions programmatically by specifying how to build the function body given the parameter symbols.

```scala
private[internal] final class CreateLambda[Q <: Quotes](using val quotes: Q)
```

**Usage:**
```scala
val createLambda = CreateLambda()
val lambdaExpr: Expr[Int => Int] = createLambda[Int => Int] {
  case (sym, List(arg)) =>
    // Build the body tree using the method symbol and argument trees
    buildBody(arg)
}
```

### Copyable

A type class for creating copies of case class instances.

`Copyable[T]` provides a function to copy an instance of type T. It is primarily used internally for creating modified copies of context objects.

```scala
@implicitNotFound("${T} should be a case class.")
private[alpaca] trait Copyable[T] extends (T => T)
```

The `Copyable` companion object provides automatic derivation for any Product type (case class) using `Mirror.ProductOf`.

**Usage:**
```scala
case class User(name: String, age: Int)

// Automatically derives a Copyable instance
val copyable = summon[Copyable[User]]
val user = User("Alice", 30)
val copied: User = copyable(user) // User("Alice", 30)
```

### Default

A type class providing default values for types.

`Default[T]` provides a way to obtain default values for various types. It is used internally to provide fallback values when needed.

```scala
private[internal] trait Default[+T] extends (() => T)
```

The `Default` companion object provides given instances for common types:

- `Default[Unit]` - returns `()`
- `Default[Boolean]` - returns `false`
- `Default[Int]` - returns `0`
- `Default[String]` - returns `""`
- `Default[Nothing]` - throws `NoSuchElementException`
- `Default[Expr[T]]` - returns `'{ ??? }` (requires Quotes)
- `Default[List[T]]` - returns `Nil` (requires Quotes)
- `Default[quotes.reflect.Tree]` - returns `'{ ??? }.asTerm` (requires Quotes)
- `Default[quotes.reflect.TypeRepr]` - returns `TypeRepr.of[Nothing]` (requires Quotes)
- `Default[T <: Tuple]` - returns a tuple with default values for each element

**Usage:**
```scala
val defaultInt = summon[Default[Int]]
val value: Int = defaultInt() // 0

val defaultString = summon[Default[String]]
val str: String = defaultString() // ""
```
