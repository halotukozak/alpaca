package alpaca
package internal

/**
 * Exports commonly used types and functions from the Scala quoted API.
 *
 * This provides convenient access to macro-related types without
 * requiring explicit imports of scala.quoted members.
 */
export scala.quoted.{Expr, FromExpr, Quotes, ToExpr, Type, Varargs}

/**
 * Exports the Mirror type for type-class derivation.
 */
export scala.deriving.Mirror
