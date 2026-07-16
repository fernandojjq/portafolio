package com.example.calculadorautp

/*
 * Taller #1: Calculadora
 * Estudiante: Fernando Jiménez
 * Cédula: 20-24-7669
 */

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    CalculadoraScreen()
                }
            }
        }
    }
}

@Composable
fun CalculadoraScreen() {
    // Variables para guardar los números que el usuario escribe y el resultado
    var num1 by remember { mutableStateOf("") }
    var num2 by remember { mutableStateOf("") }
    var resultado by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        // Título de la aplicación
        Text(
            text = "CALCULADORA",
            fontSize = 24.sp,
            fontWeight = FontWeight.Normal,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(50.dp))

        // Fila para Num 1
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Num 1", modifier = Modifier.weight(1f))
            OutlinedTextField(
                value = num1,
                onValueChange = { num1 = it },
                modifier = Modifier.weight(2f),
                singleLine = true,
                // Configuramos el teclado para que solo muestre números
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Fila para Num 2
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Num 2", modifier = Modifier.weight(1f))
            OutlinedTextField(
                value = num2,
                onValueChange = { num2 = it },
                modifier = Modifier.weight(2f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Fila para Resultado (Es de solo lectura)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Resultado", modifier = Modifier.weight(1f))
            OutlinedTextField(
                value = resultado,
                onValueChange = {}, // No hace nada porque es solo lectura
                readOnly = true,    // Bloquea la escritura manual
                modifier = Modifier.weight(2f),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Botón Azul de Sumar
        Button(
            onClick = {
                // Lógica matemática: convertimos el texto a número y lo sumamos
                // Si el usuario deja la caja vacía, tomamos un 0 por defecto para evitar que la app se cierre
                val valor1 = num1.toDoubleOrNull() ?: 0.0
                val valor2 = num2.toDoubleOrNull() ?: 0.0
                val suma = valor1 + valor2

                // Convertimos el resultado de vuelta a texto para mostrarlo en la caja
                // Si es un número entero (ej. 5.0) le quitamos el decimal para que se vea más limpio (5)
                resultado = if (suma % 1.0 == 0.0) {
                    suma.toInt().toString()
                } else {
                    suma.toString()
                }
            },
            modifier = Modifier
                .width(200.dp)
                .height(50.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF0055D4) // Color azul similar a la imagen
            )
        ) {
            Text(
                text = "SUMAR",
                color = Color.White,
                fontSize = 16.sp
            )
        }
    }
}