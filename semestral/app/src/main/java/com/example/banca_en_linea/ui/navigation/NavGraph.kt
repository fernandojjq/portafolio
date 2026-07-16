package com.example.banca_en_linea.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.banca_en_linea.data.repository.BancaRepository
import com.example.banca_en_linea.ui.dashboard.DashboardScreen
import com.example.banca_en_linea.ui.dashboard.DashboardViewModel
import com.example.banca_en_linea.ui.login.LoginScreen
import com.example.banca_en_linea.ui.login.LoginViewModel
import com.example.banca_en_linea.ui.registro.RegistroScreen
import com.example.banca_en_linea.ui.registro.RegistroViewModel
import com.example.banca_en_linea.ui.movimientos.MovimientosScreen
import com.example.banca_en_linea.ui.movimientos.MovimientosViewModel
import com.example.banca_en_linea.ui.transferencia.TransferenciaScreen
import com.example.banca_en_linea.ui.transferencia.TransferenciaViewModel

/**
 * Rutas de navegación como constantes: un typo en un string de ruta es un
 * crash en runtime, así que las centralizamos aquí.
 */
object Rutas {
    const val LOGIN = "login"
    const val REGISTRO = "registro"
    const val DASHBOARD = "dashboard"
    const val TRANSFERENCIA = "transferencia"
    const val MOVIMIENTOS = "movimientos/{cuentaId}"
}

/**
 * Factory genérica: como no usamos Hilt, así le pasamos el repositorio a los
 * ViewModels por constructor (que es lo que los hace testeables).
 */
class BancaViewModelFactory(
    private val repository: BancaRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(LoginViewModel::class.java) ->
            LoginViewModel(repository) as T
        modelClass.isAssignableFrom(RegistroViewModel::class.java) ->
            RegistroViewModel(repository) as T
        modelClass.isAssignableFrom(DashboardViewModel::class.java) ->
            DashboardViewModel(repository) as T
        modelClass.isAssignableFrom(TransferenciaViewModel::class.java) ->
            TransferenciaViewModel(repository) as T
        else -> throw IllegalArgumentException("ViewModel desconocido: ${modelClass.name}")
    }
}

@Composable
fun UtpbNavGraph(
    navController: NavHostController,
    repository: BancaRepository,
) {
    val factory = BancaViewModelFactory(repository)

    // Si ya hay sesión guardada, saltamos el login directamente.
    val destinoInicial = if (repository.haySesion()) Rutas.DASHBOARD else Rutas.LOGIN

    NavHost(navController = navController, startDestination = destinoInicial) {

        composable(Rutas.LOGIN) {
            LoginScreen(
                viewModel = viewModel(factory = factory),
                onLoginExitoso = {
                    navController.navigate(Rutas.DASHBOARD) {
                        popUpTo(Rutas.LOGIN) { inclusive = true }
                    }
                },
                onNavegarARegistro = {
                    navController.navigate(Rutas.REGISTRO)
                }
            )
        }

        composable(Rutas.REGISTRO) {
            RegistroScreen(
                viewModel = viewModel(factory = factory),
                onRegistroExitoso = {
                    navController.navigate(Rutas.DASHBOARD) {
                        popUpTo(Rutas.LOGIN) { inclusive = true }
                    }
                },
                onVolverAlLogin = {
                    navController.popBackStack()
                }
            )
        }

        composable(Rutas.DASHBOARD) {
            DashboardScreen(
                viewModel = viewModel(factory = factory),
                onCerrarSesion = {
                    navController.navigate(Rutas.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNuevaTransferencia = {
                    navController.navigate(Rutas.TRANSFERENCIA)
                },
                onVerMovimientos = { cuentaId ->
                    navController.navigate("movimientos/$cuentaId")
                }
            )
        }

        composable(
            route = Rutas.MOVIMIENTOS,
            arguments = listOf(navArgument("cuentaId") { type = NavType.LongType })
        ) { backStackEntry ->
            val cuentaId = backStackEntry.arguments?.getLong("cuentaId") ?: 0L
            val vm: MovimientosViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return MovimientosViewModel(cuentaId, repository) as T
                    }
                }
            )
            MovimientosScreen(viewModel = vm, onVolver = { navController.popBackStack() })
        }

        composable(Rutas.TRANSFERENCIA) {
            TransferenciaScreen(
                viewModel = viewModel(factory = factory),
                onVolver = { navController.popBackStack() },
            )
        }
    }
}
