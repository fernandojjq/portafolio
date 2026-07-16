package com.example.banca_en_linea.ui.movimientos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.banca_en_linea.data.remote.dto.CuentaDto
import com.example.banca_en_linea.data.remote.dto.MovimientoDto
import com.example.banca_en_linea.data.repository.BancaRepository
import com.example.banca_en_linea.data.repository.Resultado
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Representa el estado de la pantalla de movimientos del usuario.
 * @property cuenta Información general de la cuenta seleccionada.
 * @property movimientos Lista de transacciones asociadas a la cuenta.
 * @property otrasCuentas Lista de otras cuentas activas del usuario (usadas para transferencias de cierre).
 * @property cargando Indica si hay una llamada de red en proceso.
 * @property error Mensaje de error a mostrar si falla alguna operación.
 */
data class MovimientosUiState(
    val cuenta: CuentaDto? = null,
    val movimientos: List<MovimientoDto> = emptyList(),
    val otrasCuentas: List<CuentaDto> = emptyList(),
    val cargando: Boolean = false,
    val error: String? = null,
)

/**
 * ViewModel encargado de la lógica de negocio para la pantalla de transacciones históricas.
 */
class MovimientosViewModel(
    private val cuentaId: Long,
    private val repository: BancaRepository,
) : ViewModel() {

    // Estado reactivo interno y público de la UI
    private val _uiState = MutableStateFlow(MovimientosUiState())
    val uiState: StateFlow<MovimientosUiState> = _uiState.asStateFlow()

    init {
        cargar()
    }

    /**
     * Carga de forma asíncrona la información de la cuenta y su historial de movimientos.
     */
    fun cargar() {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, error = null) }

            // 1. Obtener la lista de cuentas para extraer los detalles de la cuenta actual
            val cuentasRes = repository.obtenerCuentas()
            val cuenta = when (cuentasRes) {
                is Resultado.Exito -> cuentasRes.datos.firstOrNull { it.id == cuentaId }
                is Resultado.Error -> null
            }

            // 2. Filtrar las cuentas alternativas para casos de cierre de cuenta
            val otras = when (cuentasRes) {
                is Resultado.Exito -> cuentasRes.datos.filter { it.id != cuentaId }
                is Resultado.Error -> emptyList()
            }

            if (cuenta == null) {
                _uiState.update {
                    it.copy(cargando = false, error = "No se pudo cargar la información de la cuenta")
                }
                return@launch
            }

            // 3. Obtener el historial completo de movimientos desde la API
            when (val movsRes = repository.obtenerMovimientos(cuentaId)) {
                is Resultado.Exito -> _uiState.update {
                    it.copy(cuenta = cuenta, movimientos = movsRes.datos, otrasCuentas = otras, cargando = false)
                }
                is Resultado.Error -> _uiState.update {
                    it.copy(cuenta = cuenta, error = movsRes.mensaje, otrasCuentas = otras, cargando = false)
                }
            }
        }
    }

    /**
     * Solicita al servidor el cierre definitivo de la cuenta actual.
     * @param destinoId ID de la cuenta destino que recibirá los fondos restantes (si la cuenta origen tiene saldo).
     * @param onCompletado Callback invocado al terminar el proceso indicando éxito o error.
     */
    fun cerrarCuenta(destinoId: Long?, onCompletado: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            when (val res = repository.cerrarCuenta(cuentaId, destinoId)) {
                is Resultado.Exito -> onCompletado(true, null)
                is Resultado.Error -> onCompletado(false, res.mensaje)
            }
        }
    }
}
