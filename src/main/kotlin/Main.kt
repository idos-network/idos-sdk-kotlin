package org.idos

import org.idos.kwil.KwilActionClient
import org.idos.kwil.actions.getAccessGrantsOwned
import org.idos.kwil.actions.getAttributes
import org.idos.kwil.actions.getCredentials
import org.idos.kwil.actions.getUser
import org.idos.kwil.actions.getWallets
import org.idos.kwil.actions.hasProfile
import org.idos.kwil.actions.revokeAccessGrant
import org.idos.kwil.signer.EthSigner
import org.kethereum.bip32.toKey
import org.kethereum.bip39.model.MnemonicWords
import org.kethereum.bip39.toSeed

// TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
suspend fun main() {
    val words = "SAFE WORDS REPLACE"
    val mnemonic = MnemonicWords(words)
    val seed = mnemonic.toSeed("")

    val key = seed.toKey("m/44'/60'/0'/0/47")
    val ecKeyPair = key.keyPair

    val signer = EthSigner(ecKeyPair)

    println("Init:")
    val client = KwilActionClient("https://nodes.staging.idos.network", signer, "idos-staging")
    val response = client.chainInfo()
    println(response)

    val response3 = client.ping("Test")
    println(response3)

    println()
    println()

    // val response4 = client.getActions("default");
    // println(response4)

    val hasProfile = hasProfile(client, "0xBf847E2565E3767232C18B5723e957053275B28F")
    println("Has profile?")
    println(hasProfile)

    println()
    println()
    println()

    println("Authorized getWallets for this wallet:")
    val wallets = getWallets(client)
    println(wallets)

    println()
    println()
    println()

    /*println("Authorized getCredentials for this wallet:")
    val credentials = getCredentials(client)
    println(credentials)

    println()
    var profile = getUser(client)
    println(profile)


    println("Access grants:")
    var accessGrants = getAccessGrantsOwned(client)
    println(accessGrants)

    println("Revoking access grant:")
    var grant = accessGrants.first()
    if (grant != null) {
        val rag = revokeAccessGrant(client, grant.id);
        println(rag)
    }*/

    println("Get attributes:")
    var attributes = getAttributes(client)
    println(attributes)
}
