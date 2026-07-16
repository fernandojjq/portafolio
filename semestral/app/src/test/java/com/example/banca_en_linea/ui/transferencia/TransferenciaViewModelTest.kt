package com.example.banca_en_linea.ui.transferencia

import com.example.banca_en_linea.data.repository.BancaRepositoryFake
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private const val MSG_MONTO_INVALIDO = "monto inválido"

@OptIn(ExperimentalCoroutinesApi::class)
class TransferenciaViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var viewModel: TransferenciaViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = TransferenciaViewModel(BancaRepositoryFake())
        dispatcher.scheduler.advanceUntilIdle() // deja terminar cargarCuentas() del init
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `carga cuentas y preselecciona la primera`() = runTest(dispatcher) {
        val estado = viewModel.uiState.value
        assertEquals(2, estado.cuentas.size)
        assertEquals("UTPB-0001-4523", estado.origen?.numeroCuenta)
    }

    @Test
    fun `transferencia exitosa genera comprobante`() = runTest(dispatcher) {
        viewModel.onDestinoChange("UTPB-0002-8817")
        viewModel.onMontoChange("100.50")
        viewModel.transferir(MSG_MONTO_INVALIDO)
        dispatcher.scheduler.advanceUntilIdle()

        val estado = viewModel.uiState.value
        assertNotNull(estado.comprobante)
        assertEquals("COMPLETADA", estado.comprobante?.estado)
        assertNull(estado.error)
    }

    @Test
    fun `saldo insuficiente muestra error sin comprobante`() = runTest(dispatcher) {
        viewModel.onDestinoChange("UTPB-0002-8817")
        viewModel.onMontoChange("999999")
        viewModel.transferir(MSG_MONTO_INVALIDO)
        dispatcher.scheduler.advanceUntilIdle()

        val estado = viewModel.uiState.value
        assertNull(estado.comprobante)
        assertNotNull(estado.error)
    }

    @Test
    fun `monto no numerico es rechazado antes de llamar al repo`() = runTest(dispatcher) {
        viewModel.onDestinoChange("UTPB-0002-8817")
        // onMontoChange filtra letras, así que forzamos un valor "raro" válido
        // para el filtro pero inválido como número: solo un punto.
        viewModel.onMontoChange(".")
        viewModel.transferir(MSG_MONTO_INVALIDO)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(MSG_MONTO_INVALIDO, viewModel.uiState.value.error)
    }

    @Test
    fun `el filtro de monto ignora letras`() = runTest(dispatcher) {
        viewModel.onMontoChange("12.50")
        viewModel.onMontoChange("12.50abc")   // intento inválido: se ignora
        assertEquals("12.50", viewModel.uiState.value.monto)
    }

    @Test
    fun `nueva transferencia resetea el formulario`() = runTest(dispatcher) {
        viewModel.onDestinoChange("UTPB-0002-8817")
        viewModel.onMontoChange("50")
        viewModel.transferir(MSG_MONTO_INVALIDO)
        dispatcher.scheduler.advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.comprobante)

        viewModel.nuevaTransferencia()
        val estado = viewModel.uiState.value
        assertNull(estado.comprobante)
        assertEquals("", estado.destino)
        assertEquals("", estado.monto)
        // La cuenta origen se conserva: es lo que el usuario esperaría
        assertTrue(estado.origen != null)
    }
}
