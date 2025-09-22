package org.idos.kwil.actions.generated

import org.idos.kwil.transaction.NamedParams
import org.idos.kwil.transaction.PositionalTypes
import org.idos.kwil.utils.SchemaField

/**
 * Base interface for all generated action descriptors.
 *
 * @param I Input parameter type for the action
 * @param O Output row type for the action
 */
interface GeneratedAction<in I, out O> {
    /** The namespace this action belongs to (e.g., "main") */
    val namespace: String

    /** The name of the action as defined in the Kwil schema */
    val name: String

    /**
     * The expected parameter types in the order they should be passed to the action.
     * This should match the parameter order in the database schema.
     */
    val positionalTypes: List<SchemaField>

    /**
     * Converts the strongly-typed input into a map of named parameters
     * that can be used with the underlying [KwilActionClient].
     */
    fun toNamedParams(input: I): NamedParams
}
