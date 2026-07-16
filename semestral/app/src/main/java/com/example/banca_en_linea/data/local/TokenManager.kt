package com.example.banca_en_linea.data.local

import android.content.Context
import androidx.core.content.edit

/**
 * Guarda y expone los tokens JWT.
 *
 * Nota de seguridad: usamos SharedPreferences en MODE_PRIVATE (solo legible
 * por esta app). La antigua EncryptedSharedPreferences (androidx.security-crypto)
 * fue deprecada por Google; en una app bancaria real los tokens se cifrarían
 * con una llave del Android Keystore o se usaría DataStore + Tink.
 * Para este mock, MODE_PRIVATE es un compromiso razonable y sin dependencias extra.
 */
class TokenManager(context: Context) {

    private val prefs = context.getSharedPreferences("utpb_session", Context.MODE_PRIVATE)

    var accessToken: String?
        get() = prefs.getString(KEY_ACCESS, null)
        set(value) = prefs.edit { putString(KEY_ACCESS, value) }

    var refreshToken: String?
        get() = prefs.getString(KEY_REFRESH, null)
        set(value) = prefs.edit { putString(KEY_REFRESH, value) }

    fun haySesion(): Boolean = !accessToken.isNullOrBlank()

    /** Borra todo al cerrar sesión. */
    fun limpiar() = prefs.edit { clear() }

    private companion object {
        const val KEY_ACCESS = "access_token"
        const val KEY_REFRESH = "refresh_token"
    }
}
