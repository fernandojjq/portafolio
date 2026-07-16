package com.example.banca_en_linea

import android.app.Application
import com.example.banca_en_linea.data.local.TokenManager
import com.example.banca_en_linea.data.remote.ApiClient
import com.example.banca_en_linea.data.repository.BancaRepository
import com.example.banca_en_linea.data.repository.BancaRepositoryFake
import com.example.banca_en_linea.data.repository.BancaRepositoryRemoto

/**
 * Inyección de dependencias manual (patrón "AppContainer" de la guía oficial
 * de Android). Elegimos esto sobre Hilt para mantener el build simple; si la
 * app crece, migrar a Hilt es mecánico porque las dependencias ya fluyen por
 * constructor.
 */
class UtpbApplication : Application() {

    lateinit var repository: BancaRepository
        private set

    override fun onCreate() {
        super.onCreate()

        repository = if (USAR_API_REAL) {
            val tokenManager = TokenManager(this)
            BancaRepositoryRemoto(ApiClient.crear(tokenManager), tokenManager)
        } else {
            BancaRepositoryFake()
        }
    }

    companion object {
        /**
         * Interruptor fake/real. En `false` la app funciona sola con datos
         * demo (demo@utpb.com / 123456). Cámbialo a `true` cuando el API
         * FastAPI esté corriendo en tu PC (uvicorn en el puerto 8000).
         */
        const val USAR_API_REAL = true
    }
}
