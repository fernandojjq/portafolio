package com.example.navegacionutp

/*
 * Reto: Sistema de Navegación Jetpack Compose
 * Ingeniero Desarrollador: Fernando Jiménez
 * Cédula: 20-24-7669
 */

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument

// --- COLORES PERSONALIZADOS ---
val SpainRed = Color(0xFFAA151B)
val SpainYellow = Color(0xFFF1BF00)
val LightYellowBg = Color(0xFFFFFDE7)

// 1. DEFINICIÓN DE RUTAS
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Profile : Screen("profile")
    object Settings : Screen("settings")
    object Details : Screen("details/{info}")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                MainAppContainer()
            }
        }
    }
}

@Composable
fun MainAppContainer() {
    val navController = rememberNavController()

    Scaffold(
        // Menú en la barra inferior (Bottom Navigation)
        bottomBar = { AppBottomNavigation(navController) }
    ) { innerPadding ->

        // 2. NAVHOST CON ANIMACIONES Y 4 PANTALLAS
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding),
            // Animaciones de transición (Criterio de evaluación)
            enterTransition = { slideInHorizontally(animationSpec = tween(500)) { 1000 } + fadeIn() },
            exitTransition = { slideOutHorizontally(animationSpec = tween(500)) { -1000 } + fadeOut() }
        ) {
            // Pantalla 1: Home
            composable(Screen.Home.route) {
                GenericScreen(
                    titulo = "INICIO",
                    icono = Icons.Default.Home,
                    rutaDestino = Screen.Profile.route,
                    navController = navController
                )
            }
            // Pantalla 2: Perfil
            composable(Screen.Profile.route) {
                GenericScreen(
                    titulo = "MI PERFIL",
                    icono = Icons.Default.Person,
                    rutaDestino = Screen.Settings.route,
                    navController = navController
                )
            }
            // Pantalla 3: Configuración
            composable(Screen.Settings.route) {
                // Desde Configuración enviamos un PARAMETRO a Detalles
                GenericScreen(
                    titulo = "CONFIGURACIÓN",
                    icono = Icons.Default.Settings,
                    rutaDestino = "details/Aprobado_Con_100",
                    navController = navController
                )
            }
            // Pantalla 4: Detalles (Recibe parámetros)
            composable(
                route = Screen.Details.route,
                arguments = listOf(navArgument("info") { type = NavType.StringType })
            ) { backStackEntry ->
                val parametroRecibido = backStackEntry.arguments?.getString("info") ?: "Sin datos"
                DetailsScreen(parametroRecibido, navController)
            }
        }
    }
}

// 3. BARRA INFERIOR CORREGIDA (Evita que se trabe)
@Composable
fun AppBottomNavigation(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(containerColor = SpainRed) {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home", color = SpainYellow) },
            selected = currentRoute == Screen.Home.route,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = SpainRed,
                unselectedIconColor = SpainYellow,
                indicatorColor = SpainYellow
            ),
            onClick = {
                navController.navigate(Screen.Home.route) {
                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Person, contentDescription = "Perfil") },
            label = { Text("Perfil", color = SpainYellow) },
            selected = currentRoute == Screen.Profile.route,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = SpainRed,
                unselectedIconColor = SpainYellow,
                indicatorColor = SpainYellow
            ),
            onClick = {
                navController.navigate(Screen.Profile.route) {
                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = "Ajustes") },
            label = { Text("Ajustes", color = SpainYellow) },
            selected = currentRoute == Screen.Settings.route,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = SpainRed,
                unselectedIconColor = SpainYellow,
                indicatorColor = SpainYellow
            ),
            onClick = {
                navController.navigate(Screen.Settings.route) {
                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )
    }
}

// 4. PLANTILLA REUTILIZABLE PARA PANTALLAS (Código Limpio)
@Composable
fun GenericScreen(titulo: String, icono: ImageVector, rutaDestino: String, navController: NavHostController) {
    Column(
        modifier = Modifier.fillMaxSize().background(LightYellowBg).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Criterio: Texto con el nombre del Ingeniero
        Text(text = "Ingeniero Fernando Jiménez", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SpainRed)

        Spacer(modifier = Modifier.height(20.dp))
        Text(text = titulo, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)

        Spacer(modifier = Modifier.height(40.dp))

        // Criterio: Una Imagen (Uso de Iconos escalados)
        Icon(icono, contentDescription = null, modifier = Modifier.size(150.dp), tint = SpainRed)

        Spacer(modifier = Modifier.height(50.dp))

        // Criterio: Super Botón
        Button(
            onClick = { navController.navigate(rutaDestino) },
            modifier = Modifier.fillMaxWidth().height(65.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SpainYellow)
        ) {
            Text("SUPER BOTÓN", color = SpainRed, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// 5. PANTALLA DE DETALLES (Muestra el parámetro recibido)
@Composable
fun DetailsScreen(info: String, navController: NavHostController) {
    Column(
        modifier = Modifier.fillMaxSize().background(LightYellowBg).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Criterio: Texto con el nombre del Ingeniero
        Text(text = "Ingeniero Fernando Jiménez", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SpainRed)

        Spacer(modifier = Modifier.height(20.dp))
        Text(text = "PANTALLA DETALLES", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)

        Spacer(modifier = Modifier.height(30.dp))

        // Criterio: Imagen
        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(100.dp), tint = SpainRed)

        Spacer(modifier = Modifier.height(30.dp))

        // MOSTRAR PARÁMETRO
        Card(
            colors = CardDefaults.cardColors(containerColor = SpainYellow),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Dato Recibido: $info",
                modifier = Modifier.padding(20.dp),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = SpainRed
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Criterio: Super Botón (Para regresar)
        Button(
            onClick = { navController.popBackStack() },
            modifier = Modifier.fillMaxWidth().height(65.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SpainRed)
        ) {
            Text("SUPER BOTÓN (VOLVER)", color = SpainYellow, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}