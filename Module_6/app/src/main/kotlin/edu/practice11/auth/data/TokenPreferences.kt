package edu.practice11.auth.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import edu.practice11.auth.domain.TokenStore
import kotlinx.coroutines.flow.first

private val Context.tokens by preferencesDataStore(name = "tokens")

class TokenPreferences(context: Context) : TokenStore {
    private val store = context.applicationContext.tokens
    private val key = stringPreferencesKey("accessToken")

    override suspend fun read(): String? = store.data.first()[key]

    override suspend fun save(token: String) {
        store.edit { it[key] = token }
    }

    override suspend fun clear() {
        store.edit { it.remove(key) }
    }
}
