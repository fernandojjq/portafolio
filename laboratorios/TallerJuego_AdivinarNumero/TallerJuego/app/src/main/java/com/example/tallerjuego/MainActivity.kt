package com.example.tallerjuego

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    JuegoAdivinanza()
                }
            }
        }
    }
}

@Composable
fun JuegoAdivinanza() {
    var numeroSecreto by remember { mutableStateOf((1..100).random()) }
    var input by remember { mutableStateOf("") }
    var mensajes by remember { mutableStateOf("Adivina un número del 1 al 100") }
    var colorMensaje by remember { mutableStateOf(Color.Black) }
    var intentos by remember { mutableIntStateOf(3) }
    var tiempo by remember { mutableLongStateOf(0L) }
    var juegoActivo by remember { mutableStateOf(true) }
    val ranking = remember { mutableStateListOf<String>() }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    LaunchedEffect(juegoActivo) {
        if (juegoActivo) {
            tiempo = 0
            while (juegoActivo) {
                delay(1000L)
                tiempo++
            }
        }
    }

    fun validar() {
        val guess = input.toIntOrNull()
        if (guess == null || input.isEmpty()) {
            mensajes = "Ingresa un número válido."
            colorMensaje = Color.Red
        } else if (guess !in 1..100) {
            mensajes = "¡Fuera de rango (1-100)!"
            colorMensaje = Color.Red
        } else {
            intentos--
            if (guess == numeroSecreto) {
                mensajes = "¡CORRECTO! Tiempo: ${tiempo}s"
                colorMensaje = Color(0xFF008000)
                ranking.add("Tiempo: ${tiempo}s | Intentos: ${3 - intentos}")
                juegoActivo = false
            } else if (intentos > 0) {
                mensajes = if (guess < numeroSecreto) "El número es MAYOR" else "El número es MENOR"
                colorMensaje = Color.Blue
            } else {
                mensajes = "¡PERDISTE! Era el $numeroSecreto"
                colorMensaje = Color.Red
                juegoActivo = false
            }
            input = ""
            // IMPORTANTE: Devolvemos el foco al campo para que no se cierre el teclado
            focusRequester.requestFocus()
        }
    }

    // Diseño centrado y con mejor espacio
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Intentos restantes: $intentos", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("Tiempo transcurrido: $tiempo seg", fontSize = 16.sp)

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = input,
            onValueChange = { if (it.length <= 3) input = it },
            label = { Text("Tu número") },
            modifier = Modifier.focusRequester(focusRequester).fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { validar() }),
            enabled = juegoActivo
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { validar() }, enabled = juegoActivo, modifier = Modifier.fillMaxWidth()) {
            Text("Validar Intento")
        }

        Text(mensajes, color = colorMensaje, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp), fontSize = 18.sp)

        if (!juegoActivo) {
            Button(onClick = {
                numeroSecreto = (1..100).random()
                intentos = 3
                juegoActivo = true
                mensajes = "Nuevo juego"
                colorMensaje = Color.Black
                focusRequester.requestFocus()
            }) { Text("Reiniciar Juego") }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text("Ranking:", style = MaterialTheme.typography.titleMedium)
        LazyColumn(modifier = Modifier.fillMaxWidth().height(150.dp)) {
            items(ranking) { partida -> Text("• $partida") }
        }
    }
}