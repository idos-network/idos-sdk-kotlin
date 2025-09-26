package org.idos.app.di

import `EnclaveFactory.android`
import androidx.lifecycle.SavedStateHandle
import org.idos.app.data.ApiClient
import org.idos.app.data.DataProvider
import org.idos.app.data.repository.CredentialsRepository
import org.idos.app.data.repository.CredentialsRepositoryImpl
import org.idos.app.data.repository.WalletRepository
import org.idos.app.data.repository.WalletRepositoryImpl
import org.idos.app.navigation.NavigationManager
import org.idos.app.security.EthSigner
import org.idos.app.security.KeyManager
import org.idos.app.ui.app.IdosAppViewModel
import org.idos.app.ui.screens.credentials.CredentialDetailViewModel
import org.idos.app.ui.screens.credentials.CredentialsViewModel
import org.idos.app.ui.screens.mnemonic.MnemonicViewModel
import org.idos.app.ui.screens.settings.SettingsViewModel
import org.idos.app.ui.screens.wallets.WalletsViewModel
import org.idos.enclave.AndroidEncryption
import org.idos.kwil.rpc.UuidString
import org.idos.kwil.signer.BaseSigner
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

const val STAGING_URL = "https://nodes.staging.idos.network"
const val STAGING_CHAIN_ID = "idos-staging"

val networkModule =
    module {
        single { ApiClient() }
        single { DataProvider(STAGING_URL, get(), STAGING_CHAIN_ID) }
    }

val repositoryModule =
    module {
        single<CredentialsRepository> { CredentialsRepositoryImpl(get()) }
        single<WalletRepository> { WalletRepositoryImpl(get()) }
    }

val viewModelModule =
    module {
        viewModel { IdosAppViewModel(get()) }
        viewModel { CredentialsViewModel(get(), get()) }
        viewModel { WalletsViewModel(get()) }
        viewModel { SettingsViewModel() }
        viewModel { MnemonicViewModel(get()) }
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
        single { KeyManager(androidContext()) }
        single { AndroidEncryption(androidContext()) }
        single<BaseSigner> { EthSigner(get()) }
    }

val appModule =
    listOf(
        networkModule,
        repositoryModule,
        viewModelModule,
        securityModule,
        navigationModule,
    )
