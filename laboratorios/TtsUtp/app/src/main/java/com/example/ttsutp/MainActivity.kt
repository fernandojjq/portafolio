package com.example.ttsutp

/*
 * Taller: Text to Speech (Texto a Voz)
 * Integrantes:
 * - Fernando Jiménez (Cédula: 20-24-7669)
 * - Bryan Law
 */

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    TtsScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TtsScreen() {
    val context = LocalContext.current

    // Estados para almacenar el texto escrito y saber si el motor está listo
    var textToSpeak by remember { mutableStateOf("") }
    var isTtsReady by remember { mutableStateOf(false) }

    // Lista de idiomas/voces disponibles
    val languages = listOf(
        "Español (España)" to Locale("es", "ES"),
        "Español (México)" to Locale("es", "MX"),
        "Inglés (Estados Unidos)" to Locale.US,
        "Francés (Francia)" to Locale.FRANCE,
        "Portugués (Brasil)" to Locale("pt", "BR")
    )

    var selectedLanguageIndex by remember { mutableIntStateOf(0) }
    var expanded by remember { mutableStateOf(false) }

    // Instancia de TextToSpeech de manera segura con el ciclo de vida de Compose
    var ttsInstance by remember { mutableStateOf<TextToSpeech?>(null) }

    DisposableEffect(context) {
        val tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsReady = true
            } else {
                Toast.makeText(context, "Error al iniciar el sintetizador de voz", Toast.LENGTH_SHORT).show()
            }
        }
        ttsInstance = tts

        onDispose {
            tts.stop()
            tts.shutdown()
        }
    }

    // Cambiar de idioma cuando el usuario selecciona otro en la lista
    LaunchedEffect(selectedLanguageIndex, isTtsReady) {
        if (isTtsReady) {
            ttsInstance?.setLanguage(languages[selectedLanguageIndex].second)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // Título de la app
        Text(
            text = "SINTETIZADOR DE VOZ",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E88E5)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Integrantes del grupo impresos en pantalla
        Text(
            text = "Fernando Jiménez (20-24-7669)\nBryan Law",
            fontSize = 14.sp,
            color = Color.Gray,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 1. Selector de Voces/Idiomas (DropdownMenu)
        Text(
            text = "Seleccionar idioma de la voz:",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.align(Alignment.Start)
        )

        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = languages[selectedLanguageIndex].first)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                languages.forEachIndexed { index, pair ->
                    DropdownMenuItem(
                        text = { Text(pair.first) },
                        onClick = {
                            selectedLanguageIndex = index
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Caja de Texto Multilínea (Textarea wrap)
        Text(
            text = "Escribe el texto a reproducir:",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.align(Alignment.Start)
        )

        OutlinedTextField(
            value = textToSpeak,
            onValueChange = { textToSpeak = it },
            placeholder = { Text("Escribe aquí lo que quieras escuchar...") },
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp) // Altura tipo Textarea
                .padding(vertical = 8.dp),
            singleLine = false, // Multilínea habilitado
            maxLines = 10,
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 3. Botón de Ejecutar Audio
        Button(
            onClick = {
                if (textToSpeak.isNotBlank()) {
                    if (isTtsReady) {
                        ttsInstance?.speak(
                            textToSpeak,
                            TextToSpeech.QUEUE_FLUSH,
                            null,
                            "UtpTtsId"
                        )
                    } else {
                        Toast.makeText(context, "El motor de voz no está listo", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Por favor, escribe un texto primero", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "REPRODUCIR AUDIO",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}