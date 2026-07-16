package com.example.banca_en_linea.data.repository

import com.example.banca_en_linea.data.remote.dto.ClienteDto
import com.example.banca_en_linea.data.remote.dto.CuentaDto
import com.example.banca_en_linea.data.remote.dto.MovimientoDto
import com.example.banca_en_linea.data.remote.dto.TransferenciaResponse
import com.example.banca_en_linea.data.remote.dto.CuentaBusquedaDto

/**
 * Resultado tipado de una operación del repositorio.
 * Preferimos esto sobre lanzar excepciones hacia la UI: el ViewModel hace
 * un `when` exhaustivo y el compilador nos obliga a manejar el error.
 */
sealed interface Resultado<out T> {
    data class Exito<T>(val datos: T) : Resultado<T>
    data class Error(val codigo: String, val mensaje: String) : Resultado<Nothing>
}

/**
 * Contrato de datos que consumen los ViewModels.
 * Tiene dos implementaciones:
 *  - [BancaRepositoryFake]: datos en memoria, permite desarrollar la UI sin backend.
 *  - [BancaRepositoryRemoto]: Retrofit contra el API FastAPI.
 * Los ViewModels no saben cuál usan — eso decide el AppContainer.
 */
interface BancaRepository {
    suspend fun login(email: String, password: String): Resultado<Unit>
    suspend fun obtenerPerfil(): Resultado<ClienteDto>
    suspend fun obtenerCuentas(): Resultado<List<CuentaDto>>
    suspend fun obtenerMovimientos(cuentaId: Long, page: Int = 0): Resultado<List<MovimientoDto>>
    suspend fun transferir(
        origen: String,
        destino: String,
        monto: Double,
        descripcion: String?,
    ): Resultado<TransferenciaResponse>

    suspend fun buscarCuentaPorNumero(numero: String): Resultado<CuentaBusquedaDto>

    suspend fun registrarse(
        nombre: String,
        apellido: String,
        email: String,
        cedula: String,
        contrasena: String,
        tipoCuenta: String,
    ): Resultado<Unit>

    fun cerrarSesion()

    suspend fun crearCuenta(tipoCuenta: String): Resultado<CuentaDto>

    suspend fun cerrarCuenta(cuentaId: Long, destinoCuentaId: Long?): Resultado<Unit>

    fun haySesion(): Boolean
}
