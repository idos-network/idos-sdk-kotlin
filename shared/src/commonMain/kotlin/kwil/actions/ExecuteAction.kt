package org.idos.kwil.actions

import org.idos.kwil.transaction.PositionalParams
import org.idos.kwil.transaction.PositionalTypes

/**
 * Base interface for all generated write action descriptors.
 *
 * @param I Input parameter type for the action
 */
interface ExecuteAction<in I> {
    /** The namespace this action belongs to (e.g., "main") */
    val namespace: String

    /** The name of the action as defined in the Kwil schema */
    val name: String

    /** The description of the action as defined in the Kwil schema */
    val description: String

    /**
     * The expected parameter types in the order they should be passed to the action.
     * This should match the parameter order in the database schema.
     */
    val positionalTypes: PositionalTypes

    /**
     * Converts the strongly-typed input into a list of positional parameters
     * that can be used with the underlying [KwilActionClient].
     */
    fun toPositionalParams(input: I): PositionalParams
}
