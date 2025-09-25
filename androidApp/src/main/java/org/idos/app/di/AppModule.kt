package org.idos.app.di

import org.idos.app.data.ApiClient
import org.idos.app.data.repository.CredentialsRepository
import org.idos.app.data.repository.CredentialsRepositoryImpl
import org.idos.app.data.repository.WalletRepository
import org.idos.app.data.repository.WalletRepositoryImpl
import org.idos.app.security.KeyManager
import org.idos.app.ui.app.IdosAppViewModel
import org.idos.app.ui.screens.credentials.CredentialsViewModel
import org.idos.app.ui.screens.mnemonic.MnemonicViewModel
import org.idos.app.ui.screens.settings.SettingsViewModel
import org.idos.app.ui.screens.wallets.WalletsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val networkModule = module {
    single { ApiClient() }
}

val repositoryModule = module {
    single<CredentialsRepository> { CredentialsRepositoryImpl(get()) }
    single<WalletRepository> { WalletRepositoryImpl(get()) }
}

val viewModelModule = module {
    viewModel { IdosAppViewModel(get()) }
    viewModel { CredentialsViewModel(get()) }
    viewModel { WalletsViewModel(get()) }
    viewModel { SettingsViewModel() }
    viewModel { MnemonicViewModel(get()) }
}

val securityModule = module {
    single { KeyManager(androidContext()) }
}

val appModule = listOf(
    networkModule,
    repositoryModule,
    viewModelModule,
    securityModule
)
