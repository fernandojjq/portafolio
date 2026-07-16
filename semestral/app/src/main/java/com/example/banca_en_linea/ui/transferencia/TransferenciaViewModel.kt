package com.example.banca_en_linea.ui.transferencia

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.banca_en_linea.data.remote.dto.CuentaDto
import com.example.banca_en_linea.data.remote.dto.TransferenciaResponse
import com.example.banca_en_linea.data.remote.dto.CuentaBusquedaDto
import com.example.banca_en_linea.data.repository.BancaRepository
import com.example.banca_en_linea.data.repository.Resultado
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TransferenciaUiState(
    val cuentas: List<CuentaDto> = emptyList(),      // cuentas propias (origen)
    val origen: CuentaDto? = null,                   // seleccionada en el dropdown
    val destino: String = "",
    val monto: String = "",                          // String: el usuario teclea texto
    val descripcion: String = "",
    val cargandoCuentas: Boolean = true,
    val enviando: Boolean = false,
    val error: String? = null,
    val comprobante: TransferenciaResponse? = null,  // != null → mostrar recibo
) {
    val puedeEnviar: Boolean
        get() = origen != null && destino.isNotBlank() && monto.isNotBlank() && !enviando
}

class TransferenciaViewModel(private val repository: BancaRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(TransferenciaUiState())
    val uiState: StateFlow<TransferenciaUiState> = _uiState.asStateFlow()

    init {
        cargarCuentas()
    }

    fun cargarCuentas() {
        viewModelScope.launch {
            _uiState.update { it.copy(cargandoCuentas = true, error = null) }
            when (val r = repository.obtenerCuentas()) {
                is Resultado.Exito -> _uiState.update {
                    // Preseleccionamos la primera cuenta: menos taps para el caso común
                    it.copy(cargandoCuentas = false, cuentas = r.datos,
                            origen = r.datos.firstOrNull())
                }
                is Resultado.Error -> _uiState.update {
                    it.copy(cargandoCuentas = false, error = r.mensaje)
                }
            }
        }
    }

    fun onOrigenChange(cuenta: CuentaDto) = _uiState.update { it.copy(origen = cuenta, error = null) }
    fun onDestinoChange(v: String) = _uiState.update { it.copy(destino = v, error = null) }
    fun onDescripcionChange(v: String) = _uiState.update { it.copy(descripcion = v) }

    fun onMontoChange(v: String) {
        // Filtramos en el ViewModel (no solo con keyboardType, que es una
        // sugerencia al teclado, no una validación): dígitos y un solo punto.
        val limpio = v.replace(',', '.')
        if (limpio.isEmpty() || limpio.matches(Regex("""\d{0,10}(\.\d{0,2})?"""))) {
            _uiState.update { it.copy(monto = limpio, error = null) }
        }
    }

    /** Devuelve el monto validado o null si no es un número > 0. */
    private fun montoValido(): Double? =
        _uiState.value.monto.toDoubleOrNull()?.takeIf { it > 0 }

    fun transferir(mensajeMontoInvalido: String) {
        val estado = _uiState.value
        if (!estado.puedeEnviar) return

        val monto = montoValido()
        if (monto == null) {
            _uiState.update { it.copy(error = mensajeMontoInvalido) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(enviando = true, error = null) }
            val r = repository.transferir(
                origen = estado.origen!!.numeroCuenta,
                destino = estado.destino.trim(),
                monto = monto,
                descripcion = estado.descripcion.ifBlank { null },
            )
            when (r) {
                is Resultado.Exito -> _uiState.update {
                    it.copy(enviando = false, comprobante = r.datos)
                }
                is Resultado.Error -> _uiState.update {
                    it.copy(enviando = false, error = r.mensaje)
                }
            }
        }
    }

    /** Resetea el formulario para hacer otra transferencia sin salir. */
    fun nuevaTransferencia() = _uiState.update {
        it.copy(comprobante = null, destino = "", monto = "", descripcion = "", error = null)
    }

    fun procesarResultadoQr(valorQr: String) {
        val limpia = valorQr.trim()
        val numeroCuenta = if (limpia.startsWith("http") || limpia.contains("://")) {
            val match = Regex("""UTPB-\d{4}-\d{4}""").find(limpia)
            match?.value ?: limpia
        } else {
            limpia
        }

        viewModelScope.launch {
            _uiState.update { it.copy(enviando = true, error = null) }
            when (val r = repository.buscarCuentaPorNumero(numeroCuenta)) {
                is Resultado.Exito -> {
                    _uiState.update {
                        it.copy(
                            enviando = false,
                            destino = r.datos.numeroCuenta,
                            descripcion = "Transferencia a ${r.datos.nombreTitular}",
                            error = null
                        )
                    }
                }
                is Resultado.Error -> {
                    _uiState.update {
                        it.copy(
                            enviando = false,
                            error = "Error al leer QR: ${r.mensaje}"
                        )
                    }
                }
            }
        }
    }
}
