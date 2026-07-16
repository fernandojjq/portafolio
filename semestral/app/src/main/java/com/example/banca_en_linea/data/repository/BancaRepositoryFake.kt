package com.example.banca_en_linea.data.repository

import com.example.banca_en_linea.data.remote.dto.ClienteDto
import com.example.banca_en_linea.data.remote.dto.CuentaDto
import com.example.banca_en_linea.data.remote.dto.MovimientoDto
import com.example.banca_en_linea.data.remote.dto.TransferenciaResponse
import com.example.banca_en_linea.data.remote.dto.CuentaBusquedaDto
import kotlinx.coroutines.delay

/**
 * Implementación en memoria para desarrollar la UI sin backend.
 * Credenciales demo: demo@utpb.com / 123456
 *
 * El `delay(600)` simula latencia de red: obliga a que la UI maneje bien
 * los estados de carga desde el día uno, no como un parche al final.
 */
class BancaRepositoryFake : BancaRepository {

    private var sesionActiva = false

    private val cliente = ClienteDto(1, "Evaristo", "Álvarez", "demo@utpb.com")

    private val cuentas = mutableListOf(
        CuentaDto(1, "UTPB-0001-4523", "AHORRO", 5240.75, "USD"),
        CuentaDto(2, "UTPB-0002-8817", "CORRIENTE", 1830.20, "USD"),
    )

    private val movimientos = mutableMapOf(
        1L to mutableListOf(
            MovimientoDto(3, "CREDITO", 1200.00, 5240.75, "Depósito de salario", "2026-07-01T09:15:00", "REG-993810"),
            MovimientoDto(2, "DEBITO", 89.50, 4040.75, "Supermercado El Trébol", "2026-06-28T17:42:00", "REG-183710"),
            MovimientoDto(1, "CREDITO", 300.00, 4130.25, "Transferencia recibida", "2026-06-25T11:03:00", "REG-382910"),
        ),
        2L to mutableListOf(
            MovimientoDto(2, "DEBITO", 45.99, 1830.20, "Pago de electricidad", "2026-07-03T08:00:00", "REG-472910"),
            MovimientoDto(1, "DEBITO", 12.75, 1876.19, "Café Verde S.A.", "2026-06-30T14:21:00", "REG-839102"),
        ),
    )

    override suspend fun login(email: String, password: String): Resultado<Unit> {
        delay(600)
        return if (email == "demo@utpb.com" && password == "123456") {
            sesionActiva = true
            Resultado.Exito(Unit)
        } else {
            Resultado.Error("CREDENCIALES_INVALIDAS", "Credenciales incorrectas")
        }
    }

    override suspend fun obtenerPerfil(): Resultado<ClienteDto> {
        delay(400)
        return Resultado.Exito(cliente)
    }

    override suspend fun obtenerCuentas(): Resultado<List<CuentaDto>> {
        delay(600)
        return Resultado.Exito(cuentas.toList())
    }

    override suspend fun obtenerMovimientos(cuentaId: Long, page: Int): Resultado<List<MovimientoDto>> {
        delay(500)
        // Solo la página 0 tiene datos en el fake; páginas siguientes vacías.
        val lista = if (page == 0) movimientos[cuentaId].orEmpty() else emptyList()
        return Resultado.Exito(lista)
    }

    override suspend fun transferir(
        origen: String,
        destino: String,
        monto: Double,
        descripcion: String?,
    ): Resultado<TransferenciaResponse> {
        delay(800)
        // Replicamos las validaciones que hará el servidor real, para que la
        // UI se comporte igual cuando cambiemos de implementación.
        if (monto <= 0) return Resultado.Error("MONTO_INVALIDO", "El monto debe ser mayor a cero")
        val cuentaOrigen = cuentas.find { it.numeroCuenta == origen }
            ?: return Resultado.Error("CUENTA_NO_ENCONTRADA", "Cuenta origen no existe")
        if (cuentaOrigen.saldo < monto) {
            return Resultado.Error("SALDO_INSUFICIENTE", "Saldo insuficiente")
        }
        return Resultado.Exito(
            TransferenciaResponse(
                id = 100,
                referencia = "TRF-${System.currentTimeMillis()}",
                estado = "COMPLETADA",
                fecha = "2026-07-10T12:00:00",
            )
        )
    }

    override suspend fun buscarCuentaPorNumero(numero: String): Resultado<CuentaBusquedaDto> {
        delay(400)
        return when (numero) {
            "UTPB-0001-4523", "UTPB-0002-8817" -> Resultado.Exito(CuentaBusquedaDto(numero, "Evaristo Álvarez"))
            else -> {
                if (numero.contains("wikipedia", ignoreCase = true)) {
                    Resultado.Exito(CuentaBusquedaDto("UTPB-0005-7734", "Luis Herrera"))
                } else if (numero.matches(Regex("""UTPB-\d{4}-\d{4}"""))) {
                    Resultado.Exito(CuentaBusquedaDto(numero, "Fernando Jiménez"))
                } else {
                    Resultado.Error("CUENTA_NO_ENCONTRADA", "La cuenta no existe")
                }
            }
        }
    }

    override suspend fun registrarse(
        nombre: String,
        apellido: String,
        email: String,
        cedula: String,
        contrasena: String,
        tipoCuenta: String,
    ): Resultado<Unit> {
        delay(600)
        sesionActiva = true
        return Resultado.Exito(Unit)
    }

    override fun cerrarSesion() { sesionActiva = false }

    override suspend fun crearCuenta(tipoCuenta: String): Resultado<CuentaDto> {
        delay(500)
        val nueva = CuentaDto(
            id = (3..100).random().toLong(),
            numeroCuenta = "UTPB-0006-${(1000..9999).random()}",
            tipo = tipoCuenta,
            saldo = 500.00,
            moneda = "USD"
        )
        return Resultado.Exito(nueva)
    }

    override suspend fun cerrarCuenta(cuentaId: Long, destinoCuentaId: Long?): Resultado<Unit> {
        delay(500)
        return Resultado.Exito(Unit)
    }

    override fun haySesion(): Boolean = sesionActiva
}
