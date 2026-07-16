package com.example.banca_en_linea.ui.login

import com.example.banca_en_linea.data.repository.BancaRepositoryFake
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests del LoginViewModel usando el repositorio fake real de la app.
 * viewModelScope usa Dispatchers.Main, que no existe en la JVM de tests,
 * así que lo reemplazamos con un dispatcher de prueba controlable.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var viewModel: LoginViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = LoginViewModel(BancaRepositoryFake())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `login exitoso con credenciales demo`() = runTest(dispatcher) {
        viewModel.onEmailChange("demo@utpb.com")
        viewModel.onPasswordChange("123456")

        viewModel.iniciarSesion()
        dispatcher.scheduler.advanceUntilIdle() // ejecuta la coroutine y el delay fake

        val estado = viewModel.uiState.value
        assertTrue(estado.loginExitoso)
        assertFalse(estado.cargando)
        assertNull(estado.error)
    }

    @Test
    fun `login falla con credenciales incorrectas`() = runTest(dispatcher) {
        viewModel.onEmailChange("otro@correo.com")
        viewModel.onPasswordChange("mala")

        viewModel.iniciarSesion()
        dispatcher.scheduler.advanceUntilIdle()

        val estado = viewModel.uiState.value
        assertFalse(estado.loginExitoso)
        assertNotNull(estado.error)
    }

    @Test
    fun `no envia con campos vacios`() = runTest(dispatcher) {
        // Sin llenar campos, puedeEnviar debe ser false y iniciarSesion no hace nada.
        assertFalse(viewModel.uiState.value.puedeEnviar)

        viewModel.iniciarSesion()
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.cargando)
        assertFalse(viewModel.uiState.value.loginExitoso)
    }

    @Test
    fun `escribir limpia el error anterior`() = runTest(dispatcher) {
        viewModel.onEmailChange("otro@correo.com")
        viewModel.onPasswordChange("mala")
        viewModel.iniciarSesion()
        dispatcher.scheduler.advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.error)

        // Al corregir el campo, el mensaje de error debe desaparecer.
        viewModel.onPasswordChange("123456")
        assertNull(viewModel.uiState.value.error)
        assertEquals("123456", viewModel.uiState.value.password)
    }
}
