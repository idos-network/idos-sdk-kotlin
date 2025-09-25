package org.idos.app.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.idos.app.data.ApiClient
import org.idos.app.data.model.Credential

interface CredentialsRepository {
    fun getCredentials(): Flow<List<Credential>>
}

class CredentialsRepositoryImpl(
    private val apiClient: ApiClient
) : CredentialsRepository {

    override fun getCredentials(): Flow<List<Credential>> = flow { emit(apiClient.fetchCredentials()) }
}
