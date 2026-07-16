package com.example.banca_en_linea.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.banca_en_linea.data.remote.dto.CuentaDto
import com.example.banca_en_linea.data.repository.BancaRepository
import com.example.banca_en_linea.data.repository.Resultado
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado que maneja la pantalla principal del Dashboard bancario.
 * @property nombreCliente Nombre del titular de la cuenta para el saludo.
 * @property cuentas Listado de cuentas bancarias activas.
 * @property cargando Indica si los datos se están recuperando de la API.
 * @property error Detalle del error si ocurre algún fallo de comunicación.
 */
data class DashboardUiState(
    val nombreCliente: String = "",
    val cuentas: List<CuentaDto> = emptyList(),
    val cargando: Boolean = true,
    val error: String? = null,
)

/**
 * ViewModel que conecta la lógica del Dashboard con la capa de datos.
 */
class DashboardViewModel(private val repository: BancaRepository) : ViewModel() {

    // Estado reactivo interno y su flujo de salida inmutable
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    /**
     * Carga y actualiza de forma asíncrona la información del perfil y del listado de cuentas.
     * Nota: La carga inicial NO va en init{} sino en un LaunchedEffect de la pantalla.
     * Razón: El ViewModel sobrevive a la navegación (queda en el back stack), por lo que
     * init{} solo correría una vez y los saldos quedarían desactualizados al volver de una transferencia.
     */
    fun cargar() {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, error = null) }

            // Se solicitan los datos en secuencia por simplicidad
            val perfil = repository.obtenerPerfil()
            val cuentas = repository.obtenerCuentas()

            when {
                perfil is Resultado.Error -> _uiState.update {
                    it.copy(cargando = false, error = perfil.mensaje)
                }
                cuentas is Resultado.Error -> _uiState.update {
                    it.copy(cargando = false, error = cuentas.mensaje)
                }
                perfil is Resultado.Exito && cuentas is Resultado.Exito -> _uiState.update {
                    it.copy(
                        cargando = false,
                        nombreCliente = perfil.datos.nombre,
                        cuentas = cuentas.datos,
                    )
                }
            }
        }
    }

    /**
     * Elimina las credenciales y el token JWT almacenado localmente.
     */
    fun cerrarSesion() = repository.cerrarSesion()

    /**
     * Solicita la apertura de una nueva cuenta activa para el usuario.
     * @param tipo Tipo de la nueva cuenta ("AHORRO" o "CORRIENTE").
     * @param onResultado Callback que notifica al UI sobre el éxito o fallo de la apertura.
     */
    fun crearNuevaCuenta(tipo: String, onResultado: (Boolean) -> Unit) {
        viewModelScope.launch {
            when (val res = repository.crearCuenta(tipo)) {
                is Resultado.Exito -> {
                    // Refrescar saldos y lista del dashboard inmediatamente al crear
                    cargar()
                    onResultado(true)
                }
                is Resultado.Error -> {
                    onResultado(false)
                }
            }
        }
    }
}
