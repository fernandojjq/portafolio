package com.example.banca_en_linea.data.remote

import com.example.banca_en_linea.data.remote.dto.ClienteDto
import com.example.banca_en_linea.data.remote.dto.CuentaDto
import com.example.banca_en_linea.data.remote.dto.LoginRequest
import com.example.banca_en_linea.data.remote.dto.LoginResponse
import com.example.banca_en_linea.data.remote.dto.MovimientoDto
import com.example.banca_en_linea.data.remote.dto.TransferenciaRequest
import com.example.banca_en_linea.data.remote.dto.TransferenciaResponse
import com.example.banca_en_linea.data.remote.dto.CuentaBusquedaDto
import com.example.banca_en_linea.data.remote.dto.RegistroRequest
import com.example.banca_en_linea.data.remote.dto.CrearCuentaRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.DELETE
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Contrato Retrofit contra el API FastAPI (v1).
 * Las funciones son `suspend`: Retrofit las ejecuta en su propio dispatcher
 * de IO y se integran directo con coroutines, sin callbacks.
 */
interface ApiService {

    @POST("api/v1/auth/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    @GET("api/v1/clientes/me")
    suspend fun obtenerPerfil(): ClienteDto

    @GET("api/v1/cuentas")
    suspend fun obtenerCuentas(): List<CuentaDto>

    @GET("api/v1/cuentas/{id}/movimientos")
    suspend fun obtenerMovimientos(
        @Path("id") cuentaId: Long,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
    ): List<MovimientoDto>

    @POST("api/v1/transferencias")
    suspend fun transferir(@Body body: TransferenciaRequest): TransferenciaResponse

    @GET("api/v1/cuentas/buscar-por-numero")
    suspend fun buscarCuentaPorNumero(@Query("numero") numero: String): CuentaBusquedaDto

    @POST("api/v1/auth/register")
    suspend fun registrarse(@Body body: RegistroRequest): LoginResponse

    @POST("api/v1/cuentas")
    suspend fun crearCuenta(@Body body: CrearCuentaRequest): CuentaDto

    @DELETE("api/v1/cuentas/{id}")
    suspend fun cerrarCuenta(
        @Path("id") cuentaId: Long,
        @Query("destino_cuenta_id") destinoCuentaId: Long?,
    )
}
