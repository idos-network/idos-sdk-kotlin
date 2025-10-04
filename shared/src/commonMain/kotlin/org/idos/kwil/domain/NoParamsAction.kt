package org.idos.kwil.domain

import org.idos.kwil.domain.generated.ViewAction
import org.idos.kwil.domain.PositionalParams
import org.idos.kwil.domain.PositionalTypes

/**
 * Sentinel object representing absence of parameters for no-params actions.
 */
object Empty

/**
 * Base class for view actions that don't require input parameters.
 *
 * @param O Output row type for the action
 */
abstract class NoParamsAction<out O> : ViewAction<Empty, O> {
    override val positionalTypes: PositionalTypes = emptyList()

    override fun toPositionalParams(input: Empty): PositionalParams = emptyList()
}
