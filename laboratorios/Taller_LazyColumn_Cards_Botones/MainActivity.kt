package com.example.tallercompose

/*
 * Taller: Uso de Jetpack Compose (Listas, Estados y Navegación)
 * Estudiante: Fernando Jiménez
 * Cédula: 20-24-7669
 */

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    CountryApp()
                }
            }
        }
    }
}

// COMPONENTE PRINCIPAL (Maneja la navegación entre pantallas y el estado global)
@Composable
fun CountryApp() {
    var showFavoritesScreen by remember { mutableStateOf(false) }
    val favoriteCountries = remember { mutableStateListOf<String>() }

    if (showFavoritesScreen) {
        // EXTRA: Segunda pantalla
        FavoriteListScreen(
            favorites = favoriteCountries,
            onBack = { showFavoritesScreen = false }
        )
    } else {
        // Pantalla Principal
        CountryListScreen(
            favoriteCountries = favoriteCountries,
            onGoToFavorites = { showFavoritesScreen = true }
        )
    }
}

// PANTALLA PRINCIPAL
@Composable
fun CountryListScreen(
    favoriteCountries: MutableList<String>,
    onGoToFavorites: () -> Unit
) {
    val context = LocalContext.current

    // 1. MODIFICAR LA LISTA: Países originales + Centroamérica + resto de Latinoamérica
    val initialCountries = listOf(
        // Originales de la guía
        "Panamá", "México", "Argentina", "Chile", "Colombia",
        "España", "Italia", "Francia", "Japón", "Brasil",
        // Centroamérica
        "Belice", "Costa Rica", "El Salvador", "Guatemala", "Honduras", "Nicaragua",
        // Resto de Latinoamérica
        "Bolivia", "Cuba", "Ecuador", "Paraguay", "Perú",
        "Puerto Rico", "República Dominicana", "Uruguay", "Venezuela"
    )

    val countries = remember { mutableStateListOf(*initialCountries.toTypedArray()) }
    var newCountryText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

        // 1b. PERMITIR QUE EL USUARIO ESCRIBA UNO NUEVO
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newCountryText,
                onValueChange = { newCountryText = it },
                label = { Text("Escribe un nuevo país") },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                if (newCountryText.isNotBlank()) {
                    countries.add(newCountryText.trim())
                    newCountryText = ""
                }
            }) {
                Text("Agregar")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Botón para ir a la segunda pantalla
        Button(
            onClick = onGoToFavorites,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
        ) {
            Text("Ver mis Favoritos (${favoriteCountries.size})")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // LazyColumn principal
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(countries) { country ->
                CountryCard(
                    country = country,
                    onDetailsClick = {
                        // 3a. ACCIÓN BOTÓN DETALLES: Toast
                        Toast.makeText(context, "Detalles de: $country", Toast.LENGTH_SHORT).show()
                    },
                    onFavoriteClick = {
                        // 3b. ACCIÓN BOTÓN FAVORITOS: Guardar en lista
                        if (!favoriteCountries.contains(country)) {
                            favoriteCountries.add(country)
                            Toast.makeText(context, "$country agregado a favoritos", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "$country ya está en favoritos", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }
}

// 2. PERSONALIZAR EL CARD
@Composable
fun CountryCard(
    country: String,
    onDetailsClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        // Cambiar bordes
        border = BorderStroke(2.dp, Color(0xFF1E88E5)),
        // Cambiar colores
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE3F2FD)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cambiar Tipografía
            Text(
                text = country,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF0D47A1)
            )

            // Botones
            Column(horizontalAlignment = Alignment.End) {
                Button(onClick = onDetailsClick) {
                    Text("Detalles")
                }
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedButton(onClick = onFavoriteClick) {
                    Text("Favorito")
                }
            }
        }
    }
}

// 4. EXTRA: CREAR UNA SEGUNDA PANTALLA
@Composable
fun FavoriteListScreen(
    favorites: List<String>,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Button(onClick = onBack) {
            Text("Volver a la lista")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Mis Países Favoritos",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFE65100)
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (favorites.isEmpty()) {
            Text("Aún no has agregado ningún país a favoritos.")
        } else {
            // Segundo LazyColumn
            LazyColumn {
                items(favorites) { country ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9C4)),
                        border = BorderStroke(1.dp, Color(0xFFFBC02D))
                    ) {
                        Text(
                            text = "- $country",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}