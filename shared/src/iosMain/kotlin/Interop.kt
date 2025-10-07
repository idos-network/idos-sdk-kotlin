package org.idos

sealed class ResultInterop<out T : Any> {
    data class Success<out T : Any>(
        val value: T,
    ) : ResultInterop<T>()

    data class Failure(
        val error: Throwable,
    ) : ResultInterop<Nothing>()
}

fun <T : Any> Result<T>.interop(): ResultInterop<T> =
    fold(
        onSuccess = { ResultInterop.Success(it) },
        onFailure = { ResultInterop.Failure(it) },
    )

typealias HexStringValue = org.idos.kwil.types.HexString
typealias UuidStringValue = org.idos.kwil.types.UuidString

data class HexString(
    val value: String,
)

data class UuidString(
    val value: String,
)

fun HexStringValue.interop() = HexString(value)

fun UuidStringValue.interop() = UuidString(value)

fun Result<HexStringValue>.interop(): ResultInterop<HexString> = map { it.interop() }.interop()

fun Result<UuidStringValue>.interop(): ResultInterop<UuidString> = map { it.interop() }.interop()

suspend fun <T : Any> HexString.flatMap(result: suspend (HexStringValue) -> Result<T>): Result<T> =
    runCatching { HexStringValue.withoutPrefix(this.value) }
        .fold(
            onSuccess = { result(it) },
            onFailure = { Result.failure(it) },
        )

suspend fun <T : Any> UuidString.flatMap(result: suspend (UuidStringValue) -> Result<T>): Result<T> =
    runCatching { UuidStringValue(this.value) }
        .fold(
            onSuccess = { result(it) },
            onFailure = { Result.failure(it) },
        )

suspend fun <T : Any> Pair<UuidString, UuidString>.flatMap(
    result: suspend (Pair<UuidStringValue, UuidStringValue>) -> Result<T>,
): Result<T> =
    runCatching { Pair(UuidStringValue(this.first.value), UuidStringValue(this.second.value)) }
        .fold(
            onSuccess = { result(it) },
            onFailure = { Result.failure(it) },
        )
