package org.idos.app.di

import android.content.pm.ApplicationInfo
import androidx.lifecycle.SavedStateHandle
import kotlinx.serialization.json.Json
import org.idos.app.data.ApiClient
import org.idos.app.data.DataProvider
import org.idos.app.data.StorageManager
import org.idos.app.data.repository.CredentialsRepository
import org.idos.app.data.repository.CredentialsRepositoryImpl
import org.idos.app.data.repository.UserRepository
import org.idos.app.data.repository.UserRepositoryImpl
import org.idos.app.data.repository.WalletRepository
import org.idos.app.data.repository.WalletRepositoryImpl
import org.idos.app.logging.TimberLogAdapter
import org.idos.app.navigation.NavigationManager
import org.idos.app.security.EthSigner
import org.idos.app.security.KeyManager
import org.idos.app.ui.dashboard.DashboardViewModel
import org.idos.app.ui.screens.credentials.CredentialDetailViewModel
import org.idos.app.ui.screens.credentials.CredentialsViewModel
import org.idos.app.ui.screens.login.LoginViewModel
import org.idos.app.ui.screens.mnemonic.MnemonicViewModel
import org.idos.app.ui.screens.settings.SettingsViewModel
import org.idos.app.ui.screens.wallets.WalletsViewModel
import org.idos.crypto.BouncyCastleKeccak256
import org.idos.crypto.Keccak256Hasher
import org.idos.enclave.AndroidMetadataStorage
import org.idos.enclave.EnclaveOrchestrator
import org.idos.enclave.MetadataStorage
import org.idos.enclave.crypto.AndroidEncryption
import org.idos.enclave.crypto.Encryption
import org.idos.enclave.mpc.MpcConfig
import org.idos.logging.HttpLogLevel
import org.idos.logging.IdosLogConfig
import org.idos.logging.SdkLogLevel
import org.idos.signer.Signer
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

private const val STAGING_URL = "https://nodes.playground.idos.network"
private const val STAGING_CHAIN_ID = "kwil-testnet"
private const val PG_PARTISIA_URL = "https://partisia-reader-node.playground.idos.network:8080"
private const val PG_CONTRACT = "0223996d84146dbf310dd52a0e1d103e91bb8402b3"
private val MPC_CONFIG = MpcConfig(PG_PARTISIA_URL, PG_CONTRACT, 6, 4, 2)

/**
 * Logging configuration module.
 *
 * Provides SDK logging configuration based on build type.
 * Separate module for easy testing and override.
 */
val loggingModule =
    module {
        single {
            // Determine if app is in debug mode
            val context = androidContext()
            val isDebuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

            // Configure SDK logging with DSL-style builder
            IdosLogConfig.build {
                httpLogLevel = if (isDebuggable) HttpLogLevel.INFO else HttpLogLevel.NONE
                sdkLogLevel = if (isDebuggable) SdkLogLevel.DEBUG else SdkLogLevel.INFO

                // Route all SDK logs to Timber with "idOS-" prefix
                callbackSink(tagPrefix = "idOS-", callback = TimberLogAdapter::log)

                // Example: Add Crashlytics for errors in production
                // if (!isDebuggable) {
                //     callbackSink { level, tag, message ->
                //         if (level == SdkLogLevel.ERROR) {
                //             FirebaseCrashlytics.getInstance().log("[$tag] $message")
                //         }
                //     }
                // }
            }
        }
    }

val networkModule =
    module {
        single { Json { ignoreUnknownKeys = true } }
        single { ApiClient() }
        single { DataProvider(STAGING_URL, get(), STAGING_CHAIN_ID, get()) }
    }

val dataModule =
    module {
        single { StorageManager(androidContext(), get()) }
        single<MetadataStorage> { AndroidMetadataStorage(androidContext()) }
    }

val repositoryModule =
    module {
        single<CredentialsRepository> { CredentialsRepositoryImpl(get()) }
        single<WalletRepository> { WalletRepositoryImpl(get()) }
        single<UserRepository> { UserRepositoryImpl(get(), get(), get(), get()) }
    }

val viewModelModule =
    module {
        viewModel { DashboardViewModel(get()) }
        viewModel { LoginViewModel(get(), get()) }
        viewModel { CredentialsViewModel(get(), get()) }
        viewModel { WalletsViewModel(get()) }
        viewModel { SettingsViewModel(get(), get()) }
        viewModel { MnemonicViewModel(get(), get()) }
        viewModel { (savedStateHandle: SavedStateHandle) ->
            CredentialDetailViewModel(
                credentialsRepository = get(),
                navigationManager = get(),
                get(),
                get(),
                savedStateHandle,
            )
        }
    }

val navigationModule =
    module {
        single { NavigationManager() }
    }

val securityModule =
    module {
        single<Keccak256Hasher> { BouncyCastleKeccak256() }
        single<Encryption> { AndroidEncryption(androidContext()) }
        single<Signer> { EthSigner(get(), get()) }
        single { KeyManager(androidContext()) }
        single {
            EnclaveOrchestrator.create(
                get(),
                get(),
                MPC_CONFIG,
                get(),
                get(),
            )
        }
    }

val appModule =
    listOf(
        loggingModule,
        networkModule,
        dataModule,
        repositoryModule,
        viewModelModule,
        securityModule,
        navigationModule,
    )
