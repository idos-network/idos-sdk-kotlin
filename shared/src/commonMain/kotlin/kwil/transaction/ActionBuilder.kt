package org.idos.kwil.transaction

import org.idos.kwil.KwilActionClient
import org.idos.kwil.rpc.Base64String
import org.idos.kwil.rpc.HexString
import org.idos.kwil.rpc.Message
import org.idos.kwil.rpc.PayloadMsg
import org.idos.kwil.rpc.PayloadMsgOptions
import org.idos.kwil.rpc.PayloadType
import org.idos.kwil.rpc.TransactionBase64
import org.idos.kwil.serialization.DataInfo
import org.idos.kwil.serialization.toByteArray
import org.idos.kwil.signer.BaseSigner
import org.idos.kwil.signer.SignatureType
import org.idos.kwil.utils.ParamsTypes
import org.idos.kwil.utils.encodeValueType

// https://github.com/trufnetwork/kwil-js/blob/main/src/transaction/action.ts#L93

private val TXN_BUILD_IN_PROGRESS: List<Any?> = emptyList()

data class ActionOptions(
    val actionName: String,
    val namespace: String,
    val description: String? = null,
    val actionInputs: List<PositionalParams>,
    val types: List<DataInfo> = emptyList(),
    val signature: String = "",
    val challenge: String = "",
    var chainId: String,
    var nonce: Int? = null,
)

class ActionBuilder(
    private val kwil: KwilActionClient,
    options: ActionOptions,
    var signer: BaseSigner? = null,
    var signatureType: SignatureType? = null,
    var identifier: ByteArray? = null,
) {
    val actionName: String = options.actionName
    val namespace: String = options.namespace
    val description: String? = options.description
    val actionInputs: List<PositionalParams> = options.actionInputs
    val types: List<DataInfo> = options.types
    var signature: String = options.signature
    var challenge: String = options.challenge
    var chainId: String = options.chainId
    var nonce: Int? = options.nonce

    fun addSigner(
        signer: BaseSigner,
        signature: String? = null,
        challenge: String? = null,
    ) {
        this.signer = signer
        this.signatureType = signer.getSignatureType()
        this.identifier = signer.getIdentifier().toByteArray()

        if (signature != null) {
            this.signature = signature
        }

        if (challenge != null) {
            this.challenge = challenge
        }
    }

    suspend fun buildTx(privateMode: Boolean): TransactionBase64 {
        assertNotBuilding()

        // TODO Do we want to cache it?
        // val cachedInputs = actionInputs
        // actionInputs = TXN_BUILD_IN_PROGRESS

        val payload = buildTxPayload(privateMode, actionInputs)

        val signer = requireNotNull(signer) { "signer is required" }
        val identifier = requireNotNull(identifier) { "identifier is required" }
        val signatureType = requireNotNull(signatureType) { "signatureType is required" }
        val description = requireNotNull(description) { "description is required" }

        return PayloadTx
            .createTx(
                kwil,
                PayloadTxOptions(
                    payloadType = PayloadType.EXECUTE_ACTION,
                    payload = payload,
                    signer = signer,
                    signatureType = signatureType,
                    description = description,
                    chainId = chainId,
                    identifier = identifier,
                    nonce = nonce,
                ),
            ).buildTx()
    }

    // https://github.com/trufnetwork/kwil-js/blob/main/src/transaction/action.ts#L138C9-L138C17
    fun buildMsg(privateMode: Boolean): Message {
        assertNotBuilding()

        // TODO: Do we want to cache inputs?...
        // val cachedInputs = actionInputs
        // actionInputs = TXN_BUILD_IN_PROGRESS

        val payload = buildMsgPayload(privateMode, actionInputs)

        val msg =
            PayloadMsg.createMsg(
                payload,
                PayloadMsgOptions(
                    challenge = HexString(challenge),
                    signature = Base64String(signature),
                ),
            )

        if (signer != null) {
            msg.signer = signer
            msg.signatureType = signatureType
            msg.identifier = HexString(requireNotNull(identifier))
        }

        return msg.buildMsg()
    }

    private fun buildTxPayload(
        privateMode: Boolean,
        actionInputs: List<PositionalParams>,
    ): UnencodedActionPayload<List<List<EncodedValue>>> {
        if (privateMode) {
            val arguments = mutableListOf<List<EncodedValue>>()
            for (input in actionInputs) {
                val value = resolveParamTypes(input, types)
                arguments.add(encodeValueType(value))
            }

            return UnencodedActionPayload(
                dbid = namespace,
                action = actionName,
                arguments = arguments.toList(),
            )
        }

        val validated = validatedActionRequest(actionInputs)

        if (validated.modifiers.contains(AccessModifier.VIEW)) {
            throw IllegalStateException(
                "Action / Procedure ${validated.actionName} is a 'view' action. Please use kwil.call().",
            )
        }

        return UnencodedActionPayload(
            dbid = namespace,
            action = actionName,
            arguments = validated.encodedActionInputs,
        )
    }

    // This builds a CALL action
    //
    // https://github.com/trufnetwork/kwil-js/blob/main/src/transaction/action.ts#L219
    //
    private fun buildMsgPayload(
        privateMode: Boolean,
        actionInputs: List<PositionalParams>,
    ): UnencodedActionPayload<MutableList<EncodedValue>> {
        val payload =
            UnencodedActionPayload<MutableList<EncodedValue>>(
                dbid = namespace,
                action = actionName,
                arguments = mutableListOf(),
            )

        // In private mode, we cannot validate the action inputs as we cannot run the selectQuery to get the schema.
        if (privateMode) {
            val actionValues = if (actionInputs.isEmpty()) emptyList() else actionInputs[0]
            val value = resolveParamTypes(actionValues, types)
            payload.arguments?.addAll(encodeValueType(value))
            return payload
        }

        // / If we have access to the schema, we can validate the action inputs
        val validated = validatedActionRequest(actionInputs)

        if (validated.encodedActionInputs.size > 1) {
            throw IllegalStateException("Cannot pass more than one input to the call endpoint.")
        }

        // TODO: This is weird
        // if (!validated.modifiers.contains(AccessModifier.VIEW)) {
        //    throw IllegalStateException("Action ${validated.actionName} is not a view only action. Please use kwil.execute().")
        // }

        payload.arguments?.addAll(validated.encodedActionInputs.first())
        return payload
    }

    private fun validatedActionRequest(actionInputs: List<PositionalParams>): ValidatedAction {
        // We don't care about NamedParams, so only the 2nd part of the function is required
        // https://github.com/trufnetwork/kwil-js/blob/main/src/transaction/action.ts#L323

        val encValue = mutableListOf<List<EncodedValue>>()

        actionInputs.forEach { input ->
            // TODO: This is weird...
            val value = resolveParamTypes(input, types)
            encValue.add(encodeValueType(value))
        }

        return ValidatedAction(
            actionName = actionName,
            modifiers = emptyList(),
            encodedActionInputs = encValue,
        )
    }

    //
    // https://github.com/trufnetwork/kwil-js/blob/main/src/core/action.ts#L21C17-L21C34
    //
    private fun resolveParamTypes(
        i: PositionalParams,
        types: List<DataInfo>?,
    ): List<ParamsTypes> {
        val ret = mutableListOf<ParamsTypes>()

        // if no types are provided, return param types with no o property
        if (types === null) {
            for (item in i) {
                ret.add(
                    ParamsTypes(
                        v = item,
                        o = null,
                    ),
                )
            }
        } else {
            // Handle positional params
            i.forEachIndexed { index, item ->
                // assume that the order of the types matches the order of the parameters
                ret.add(
                    ParamsTypes(
                        v = item,
                        o = types[index],
                    ),
                )
            }
        }

        return ret
    }

    private fun assertNotBuilding() {
        if (actionInputs === TXN_BUILD_IN_PROGRESS) {
            throw IllegalStateException("Cannot modify the builder while a transaction is being built.")
        }
    }
}
