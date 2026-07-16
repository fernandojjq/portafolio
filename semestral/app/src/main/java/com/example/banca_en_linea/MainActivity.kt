package com.example.banca_en_linea

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.example.banca_en_linea.ui.navigation.UtpbNavGraph
import com.example.banca_en_linea.ui.theme.Banca_En_LineaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // El repositorio vive en la Application (sobrevive rotaciones y
        // recreaciones de la Activity).
        val repository = (application as UtpbApplication).repository

        setContent {
            Banca_En_LineaTheme {
                val navController = rememberNavController()
                UtpbNavGraph(
                    navController = navController,
                    repository = repository,
                )
            }
        }
    }
}
