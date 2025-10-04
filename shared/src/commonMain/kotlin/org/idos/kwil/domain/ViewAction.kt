package org.idos.kwil.domain.generated

import org.idos.kwil.domain.PositionalParams
import org.idos.kwil.domain.PositionalTypes

/**
 * Base interface for all generated VIEW action descriptors.
 *
 * View actions are read-only queries that don't modify database state.
 *
 * @param I Input parameter type for the action
 * @param O Output row type for the action
 */
interface ViewAction<in I, out O> {
    /** The namespace this action belongs to (e.g., "main") */
    val namespace: String

    /** The name of the action as defined in the Kwil schema */
    val name: String

    /**
     * The expected parameter types in the order they should be passed to the action.
     * This should match the parameter order in the database schema.
     */
    val positionalTypes: PositionalTypes

    /**
     * Converts the strongly-typed input into a list of positional parameters.
     */
    fun toPositionalParams(input: I): PositionalParams
}
