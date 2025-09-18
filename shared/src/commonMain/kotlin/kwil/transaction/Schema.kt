package org.idos.kwil.transaction

import org.idos.kwil.utils.DataInfo

enum class AccessModifier {
    PUBLIC,
    VIEW,
}

// Instead of implementation of various of param types
// let's do the most obvious one as an input, they are more "readable" for devs.
// https://github.com/trufnetwork/kwil-js/blob/main/src/core/action.ts#L211
//
// inputs = { $name: "Alice", $age: 25, $height: 1.25 }
typealias NamedParams = Map<String, Any?>

// types = { $name: DataInfo.Text, $age: DataInfo.Int8, $height: DataInfo.Numeric(3, 2) }
typealias NamedTypes = Map<String, DataInfo>

// But for requests we have to use positional params:
//
// const body = {
//  inputs: ["Alice", 25, 1.25],
//  types: [DataType.Text, DataType.Int8, DataType.Numeric(3, 2)]
// }
typealias PositionalParams = List<Any?>
typealias PositionalTypes = List<DataInfo>

data class UnencodedActionPayload<T>(
    val dbid: String,
    val action: String,
    var arguments: T?,
)

data class EncodedValue(
    val type: DataInfo,
    val data: List<ByteArray>,
)

data class ValidatedAction(
    val actionName: String,
    val modifiers: List<AccessModifier>,
    val encodedActionInputs: List<List<EncodedValue>>,
)
