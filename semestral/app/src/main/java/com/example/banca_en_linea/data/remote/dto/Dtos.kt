package com.example.banca_en_linea.data.remote.dto

import com.google.gson.annotations.SerializedName

// DTOs = objetos que viajan por la red, espejo 1:1 del JSON del API FastAPI.
// Se mantienen separados de los modelos de UI para que un cambio en el API
// no obligue a tocar las pantallas (solo el mapper en el repositorio).

data class LoginRequest(
    val email: String,
    val password: String,
)

data class LoginResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String,
)

data class ClienteDto(
    val id: Long,
    val nombre: String,
    val apellido: String,
    val email: String,
)

data class CuentaDto(
    val id: Long,
    @SerializedName("numero_cuenta") val numeroCuenta: String,
    val tipo: String,          // "AHORRO" | "CORRIENTE"
    val saldo: Double,
    val moneda: String,        // "USD"
)

data class MovimientoDto(
    val id: Long,
    val tipo: String,          // "DEBITO" | "CREDITO"
    val monto: Double,
    @SerializedName("saldo_resultante") val saldoResultante: Double,
    val descripcion: String,
    val fecha: String,         // ISO-8601; se parsea en la capa de UI
    val referencia: String,
)

data class TransferenciaRequest(
    @SerializedName("cuenta_origen") val cuentaOrigen: String,
    @SerializedName("cuenta_destino") val cuentaDestino: String,
    val monto: Double,
    val descripcion: String?,
)

data class TransferenciaResponse(
    val id: Long,
    val referencia: String,
    val estado: String,        // "COMPLETADA" | "FALLIDA"
    val fecha: String,
)

/** Formato uniforme de error del API: {"codigo": "...", "mensaje": "..."} */
data class ApiError(
    val codigo: String,
    val mensaje: String,
)

data class CuentaBusquedaDto(
    @SerializedName("numero_cuenta") val numeroCuenta: String,
    @SerializedName("nombre_titular") val nombreTitular: String,
)

data class RegistroRequest(
    val nombre: String,
    val apellido: String,
    val email: String,
    val cedula: String,
    val password: String,
    @SerializedName("tipo_cuenta") val tipoCuenta: String,
)

data class CrearCuentaRequest(
    @SerializedName("tipo_cuenta") val tipoCuenta: String,
)
