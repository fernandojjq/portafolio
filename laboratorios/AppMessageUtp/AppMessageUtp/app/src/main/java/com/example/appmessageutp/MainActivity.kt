package com.example.appmessageutp

/*
 * Taller #2: Enviar mensaje a Whatsapp
 * Estudiante: Fernando Jiménez
 * Cédula: 20-24-7669
 */

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    WhatsAppScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsAppScreen() {
    val context = LocalContext.current
    var phoneNumber by remember { mutableStateOf("+507") }
    var message by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Filled.Email,
                contentDescription = "Logo App",
                tint = Color(0xFF6200EA),
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "SendMessageWhatssapp",
                color = Color(0xFF6200EA),
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        }

        Spacer(modifier = Modifier.height(60.dp))

        Text(
            text = "Enviar Mensaje a WhatsApp",
            color = Color(0xFF009688),
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = phoneNumber,
            onValueChange = { phoneNumber = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFEBEBEB),
                unfocusedContainerColor = Color(0xFFEBEBEB),
                unfocusedIndicatorColor = Color.Gray,
                focusedIndicatorColor = Color(0xFF6200EA)
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        TextField(
            value = message,
            onValueChange = { message = it },
            placeholder = { Text("Ingrese su mensaje") },
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFEBEBEB),
                unfocusedContainerColor = Color(0xFFEBEBEB),
                unfocusedIndicatorColor = Color.Gray,
                focusedIndicatorColor = Color(0xFF6200EA)
            )
        )

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = {
                val uriString = java.lang.String.format(
                    "https://api.whatsapp.com/send?phone=%s&text=%s",
                    phoneNumber,
                    message
                )
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString))
                context.startActivity(intent)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF5E35B1)
            )
        ) {
            Text(
                text = "Enviar Mensaje a WhatsApp",
                color = Color.White,
                fontSize = 16.sp
            )
        }
    }
}