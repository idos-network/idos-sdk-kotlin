package org.idos.app.di

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
import org.idos.enclave.AndroidMetadataStorage
import org.idos.enclave.EnclaveOrchestrator
import org.idos.enclave.MetadataStorage
import org.idos.enclave.crypto.AndroidEncryption
import org.idos.enclave.crypto.Encryption
import org.idos.signer.KeyType
import org.idos.signer.Signer
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

const val STAGING_URL = "https://nodes.staging.idos.network"
const val STAGING_CHAIN_ID = "idos-staging"

val networkModule =
    module {
        single { Json { ignoreUnknownKeys = true } }
        single { ApiClient() }
        single { DataProvider(STAGING_URL, get(), STAGING_CHAIN_ID) }
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
        single<UserRepository> { UserRepositoryImpl(get(), get(), get()) }
    }

val viewModelModule =
    module {
        viewModel { DashboardViewModel(get()) }
        viewModel { LoginViewModel(get(), get()) }
        viewModel { CredentialsViewModel(get(), get()) }
        viewModel { WalletsViewModel(get()) }
        viewModel { SettingsViewModel(get()) }
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
        single<Encryption> { AndroidEncryption(androidContext()) }
        single { EnclaveOrchestrator.create(get(), get()) }
        single { KeyManager(androidContext()) }
        single<Signer> { EthSigner(get(), get()) }
    }

val appModule =
    listOf(
        networkModule,
        dataModule,
        repositoryModule,
        viewModelModule,
        securityModule,
        navigationModule,
    )
