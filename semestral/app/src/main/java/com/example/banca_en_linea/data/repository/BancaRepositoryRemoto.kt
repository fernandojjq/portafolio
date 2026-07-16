package com.example.banca_en_linea.data.repository

import com.example.banca_en_linea.data.local.TokenManager
import com.example.banca_en_linea.data.remote.ApiService
import com.example.banca_en_linea.data.remote.dto.ApiError
import com.example.banca_en_linea.data.remote.dto.ClienteDto
import com.example.banca_en_linea.data.remote.dto.CuentaDto
import com.example.banca_en_linea.data.remote.dto.LoginRequest
import com.example.banca_en_linea.data.remote.dto.MovimientoDto
import com.example.banca_en_linea.data.remote.dto.TransferenciaRequest
import com.example.banca_en_linea.data.remote.dto.TransferenciaResponse
import com.example.banca_en_linea.data.remote.dto.CuentaBusquedaDto
import com.example.banca_en_linea.data.remote.dto.RegistroRequest
import com.example.banca_en_linea.data.remote.dto.CrearCuentaRequest
import com.google.gson.Gson
import retrofit2.HttpException
import java.io.IOException

/**
 * Implementación real contra el API FastAPI.
 *
 * Patrón central: [llamar] envuelve toda llamada de red y traduce las tres
 * familias de fallo a un [Resultado.Error] con código estable:
 *  - HttpException (4xx/5xx): intenta parsear el {codigo, mensaje} del API.
 *  - IOException: sin red / servidor caído.
 *  - Cualquier otra: bug — se reporta como ERROR_DESCONOCIDO en vez de crashear.
 */
class BancaRepositoryRemoto(
    private val api: ApiService,
    private val tokenManager: TokenManager,
    private val gson: Gson = Gson(),
) : BancaRepository {

    private suspend fun <T> llamar(bloque: suspend () -> T): Resultado<T> {
        return try {
            Resultado.Exito(bloque())
        } catch (e: HttpException) {
            val error = try {
                gson.fromJson(e.response()?.errorBody()?.string(), ApiError::class.java)
            } catch (_: Exception) {
                null
            }
            Resultado.Error(
                codigo = error?.codigo ?: "HTTP_${e.code()}",
                mensaje = error?.mensaje ?: "Error del servidor (${e.code()})",
            )
        } catch (e: IOException) {
            Resultado.Error("SIN_CONEXION", "No se pudo conectar con el banco")
        } catch (e: Exception) {
            Resultado.Error("ERROR_DESCONOCIDO", e.message ?: "Error inesperado")
        }
    }

    override suspend fun login(email: String, password: String): Resultado<Unit> {
        val resultado = llamar { api.login(LoginRequest(email, password)) }
        return when (resultado) {
            is Resultado.Exito -> {
                // Persistimos los tokens: el authInterceptor los usará en
                // todas las llamadas siguientes.
                tokenManager.accessToken = resultado.datos.accessToken
                tokenManager.refreshToken = resultado.datos.refreshToken
                Resultado.Exito(Unit)
            }
            is Resultado.Error -> resultado
        }
    }

    override suspend fun obtenerPerfil(): Resultado<ClienteDto> =
        llamar { api.obtenerPerfil() }

    override suspend fun obtenerCuentas(): Resultado<List<CuentaDto>> =
        llamar { api.obtenerCuentas() }

    override suspend fun obtenerMovimientos(cuentaId: Long, page: Int): Resultado<List<MovimientoDto>> =
        llamar { api.obtenerMovimientos(cuentaId, page) }

    override suspend fun transferir(
        origen: String,
        destino: String,
        monto: Double,
        descripcion: String?,
    ): Resultado<TransferenciaResponse> =
        llamar { api.transferir(TransferenciaRequest(origen, destino, monto, descripcion)) }

    override suspend fun buscarCuentaPorNumero(numero: String): Resultado<CuentaBusquedaDto> =
        llamar { api.buscarCuentaPorNumero(numero) }

    override suspend fun registrarse(
        nombre: String,
        apellido: String,
        email: String,
        cedula: String,
        contrasena: String,
        tipoCuenta: String,
    ): Resultado<Unit> {
        val resultado = llamar {
            api.registrarse(
                RegistroRequest(nombre, apellido, email, cedula, contrasena, tipoCuenta)
            )
        }
        return when (resultado) {
            is Resultado.Exito -> {
                tokenManager.accessToken = resultado.datos.accessToken
                tokenManager.refreshToken = resultado.datos.refreshToken
                Resultado.Exito(Unit)
            }
            is Resultado.Error -> resultado
        }
    }

    override fun cerrarSesion() = tokenManager.limpiar()

    override suspend fun crearCuenta(tipoCuenta: String): Resultado<CuentaDto> =
        llamar { api.crearCuenta(CrearCuentaRequest(tipoCuenta)) }

    override suspend fun cerrarCuenta(cuentaId: Long, destinoCuentaId: Long?): Resultado<Unit> =
        llamar { api.cerrarCuenta(cuentaId, destinoCuentaId) }

    override fun haySesion(): Boolean = tokenManager.haySesion()
}
