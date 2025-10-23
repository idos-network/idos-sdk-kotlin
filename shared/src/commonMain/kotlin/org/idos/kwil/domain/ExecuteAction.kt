package org.idos.kwil.domain

/**
 * Base interface for all generated execute action descriptors.
 *
 * Execute actions modify database state and are mined on the blockchain.
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
     * Converts the strongly-typed input into a list of positional parameters.
     */
    fun toPositionalParams(input: List<I>): List<PositionalParams>
}
