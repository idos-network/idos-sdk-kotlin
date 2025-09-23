package org.idos.kwil.actions.generated

import org.idos.kwil.transaction.PositionalParams
import org.idos.kwil.transaction.PositionalTypes

abstract class NoParamsAction<out O> : ViewAction<Empty, O> {
    override val positionalTypes: PositionalTypes = emptyList()

    override fun toPositionalParams(input: Empty): PositionalParams = emptyList()
}

object Empty
