package org.idos.kwil.domain

import org.idos.kwil.serialization.KwilType

/**
 * Access modifiers for KWIL actions.
 */
enum class AccessModifier {
    PUBLIC,
    VIEW,
}

/**
 * Named parameters (key-value map).
 *
 * More readable for developers: `{ $name: "Alice", $age: 25 }`
 */
typealias NamedParams = Map<String, Any?>

/**
 * Named type information (key-type map).
 *
 * Example: `{ $name: KwilType.Text(), $age: KwilType.Int() }`
 */
typealias NamedTypes = Map<String, KwilType>

/**
 * Positional parameters (ordered list).
 *
 * Wire format for KWIL: `["Alice", 25, 1.25]`
 */
typealias PositionalParams = List<Any?>

/**
 * Positional type information (ordered list).
 *
 * Type-safe sealed classes: `[KwilType.Text(), KwilType.Int(), KwilType.Numeric(3, 2)]`
 */
typealias PositionalTypes = List<KwilType>
