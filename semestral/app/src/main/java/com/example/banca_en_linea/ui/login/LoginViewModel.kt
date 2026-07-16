package com.example.banca_en_linea.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.banca_en_linea.data.repository.BancaRepository
import com.example.banca_en_linea.data.repository.Resultado
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado único e inmutable de la pantalla de login.
 * Un solo data class en vez de varios LiveData sueltos: la UI siempre ve un
 * snapshot consistente y es trivial de testear.
 */
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val cargando: Boolean = false,
    val error: String? = null,
    val loginExitoso: Boolean = false,
) {
    // El botón solo se habilita con campos llenos y sin request en vuelo.
    val puedeEnviar: Boolean get() = email.isNotBlank() && password.isNotBlank() && !cargando
}

class LoginViewModel(private val repository: BancaRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(valor: String) {
        _uiState.update { it.copy(email = valor, error = null) }
    }

    fun onPasswordChange(valor: String) {
        _uiState.update { it.copy(password = valor, error = null) }
    }

    fun iniciarSesion() {
        val estado = _uiState.value
        if (!estado.puedeEnviar) return

        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, error = null) }

            when (val resultado = repository.login(estado.email.trim(), estado.password)) {
                is Resultado.Exito -> _uiState.update {
                    it.copy(cargando = false, loginExitoso = true)
                }
                is Resultado.Error -> _uiState.update {
                    it.copy(cargando = false, error = resultado.mensaje)
                }
            }
        }
    }

    /** La UI lo llama después de navegar, para no re-navegar en recomposiciones. */
    fun consumirNavegacion() {
        _uiState.update { it.copy(loginExitoso = false) }
    }
}
