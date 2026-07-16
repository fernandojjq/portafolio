package com.example.banca_en_linea.ui.movimientos

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.banca_en_linea.data.remote.dto.CuentaDto
import com.example.banca_en_linea.data.remote.dto.MovimientoDto
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovimientosScreen(
    viewModel: MovimientosViewModel,
    onVolver: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var modoPrivacidad by remember { mutableStateOf(false) }
    var mostrarQrDialog by remember { mutableStateOf(false) }

    var mostrarDialogoCierre by remember { mutableStateOf(false) }
    var cuentaDestinoSeleccionada by remember { mutableStateOf<CuentaDto?>(null) }
    var cerrandoCuenta by remember { mutableStateOf(false) }

    // Sensor de proximidad
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        var lastToastTime: Long = 0

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return
                val distance = event.values[0]
                val maxRange = proximitySensor?.maximumRange ?: 5f
                
                // Si la distancia es menor a 5cm o al rango máximo, está tapado
                val cerca = distance < 5f || distance < maxRange / 2f
                if (cerca != modoPrivacidad) {
                    modoPrivacidad = cerca
                    if (cerca) {
                        val now = System.currentTimeMillis()
                        if (now - lastToastTime > 3000) {
                            lastToastTime = now
                            Toast.makeText(context, "Modo privacidad activado (Saldos ocultos)", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (proximitySensor != null) {
            sensorManager.registerListener(listener, proximitySensor, SensorManager.SENSOR_DELAY_UI)
        }

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historial de Cuenta") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                state.cargando -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.error != null -> Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(state.error!!, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = viewModel::cargar) {
                        Text("Reintentar")
                    }
                }
                state.cuenta != null -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    CabeceraCuenta(
                        cuenta = state.cuenta!!,
                        modoPrivacidad = modoPrivacidad,
                        onMostrarQr = { mostrarQrDialog = true },
                        onCerrarCuenta = {
                            cuentaDestinoSeleccionada = null
                            mostrarDialogoCierre = true
                        }
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Transacciones Recientes",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(Modifier.height(8.dp))
                    if (state.movimientos.isEmpty()) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxWidth().weight(1f)
                        ) {
                            Text(
                                "No hay transacciones registradas",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(state.movimientos, key = { it.id }) { movimiento ->
                                TarjetaMovimiento(movimiento = movimiento, modoPrivacidad = modoPrivacidad)
                            }
                        }
                    }
                }
            }
        }
    }

    if (mostrarDialogoCierre && state.cuenta != null) {
        val saldoMayorACero = state.cuenta!!.saldo > 0.0
        val tieneOtras = state.otrasCuentas.isNotEmpty()

        androidx.compose.material3.AlertDialog(
            onDismissRequest = { if (!cerrandoCuenta) mostrarDialogoCierre = false },
            title = { Text("Cerrar Cuenta") },
            text = {
                Column {
                    when {
                        saldoMayorACero && !tieneOtras -> {
                            Text(
                                text = "Esta cuenta tiene un saldo de B/. ${state.cuenta!!.saldo}. " +
                                       "No puedes cerrarla porque no tienes ninguna otra cuenta activa a donde " +
                                       "transferir los fondos.\n\nPor favor, abre otra cuenta primero.",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        saldoMayorACero && tieneOtras -> {
                            Text(
                                text = "Esta cuenta tiene un saldo de B/. ${state.cuenta!!.saldo}. " +
                                       "Para poder cerrarla, debes seleccionar una cuenta de destino activa para transferir la totalidad de los fondos:"
                            )
                            Spacer(Modifier.height(16.dp))
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.height(150.dp)
                            ) {
                                items(state.otrasCuentas) { otra ->
                                    val seleccionada = cuentaDestinoSeleccionada?.id == otra.id
                                    Card(
                                        onClick = { cuentaDestinoSeleccionada = otra },
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (seleccionada) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        ),
                                        border = if (seleccionada) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(Modifier.padding(12.dp)) {
                                            Text(
                                                text = if (otra.tipo == "AHORRO") "Ahorros" else "Corriente",
                                                style = MaterialTheme.typography.titleSmall
                                            )
                                            Text(
                                                text = otra.numeroCuenta,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        else -> {
                            Text(text = "¿Estás seguro de que deseas cerrar esta cuenta? Esta operación es definitiva.")
                        }
                    }
                }
            },
            confirmButton = {
                val habilitado = !saldoMayorACero || (tieneOtras && cuentaDestinoSeleccionada != null)
                if (habilitado) {
                    Button(
                        enabled = !cerrandoCuenta,
                        onClick = {
                            cerrandoCuenta = true
                            viewModel.cerrarCuenta(cuentaDestinoSeleccionada?.id) { exito, errorMsg ->
                                cerrandoCuenta = false
                                if (exito) {
                                    mostrarDialogoCierre = false
                                    Toast.makeText(context, "¡Cuenta cerrada exitosamente!", Toast.LENGTH_SHORT).show()
                                    onVolver()
                                } else {
                                    Toast.makeText(context, errorMsg ?: "Error al cerrar la cuenta", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    ) {
                        if (cerrandoCuenta) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Confirmar Cierre")
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !cerrandoCuenta,
                    onClick = { mostrarDialogoCierre = false }
                ) {
                    Text(if (saldoMayorACero && !tieneOtras) "Entendido" else "Cancelar")
                }
            }
        )
    }

    if (mostrarQrDialog && state.cuenta != null) {
        var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
        var cargandoQr by remember { mutableStateOf(true) }

        LaunchedEffect(state.cuenta!!.numeroCuenta) {
            cargandoQr = true
            qrBitmap = descargarQr(state.cuenta!!.numeroCuenta)
            cargandoQr = false
        }

        androidx.compose.material3.AlertDialog(
            onDismissRequest = { mostrarQrDialog = false },
            title = { Text("Mi Código QR", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Cuenta: ${state.cuenta!!.numeroCuenta}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(Modifier.height(16.dp))
                    if (cargandoQr) {
                        CircularProgressIndicator(modifier = Modifier.size(50.dp))
                    } else if (qrBitmap != null) {
                        Image(
                            bitmap = qrBitmap!!.asImageBitmap(),
                            contentDescription = "Código QR de la cuenta",
                            modifier = Modifier.size(200.dp)
                        )
                    } else {
                        Text(
                            "No se pudo cargar el código QR.\nVerifica tu conexión a Internet.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Comparte este código con otra persona para recibir transferencias directas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            },
            confirmButton = {
                Button(onClick = { mostrarQrDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }
}

// Cabecera que muestra el resumen y acciones de la cuenta actual
@Composable
private fun CabeceraCuenta(
    cuenta: CuentaDto,
    modoPrivacidad: Boolean,
    onMostrarQr: () -> Unit,
    onCerrarCuenta: () -> Unit
) {
    val formato = NumberFormat.getCurrencyInstance(Locale.US)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    // Tipo de Cuenta (Ahorros o Corriente)
                    Text(
                        text = if (cuenta.tipo == "AHORRO") "CUENTA DE AHORROS" else "CUENTA CORRIENTE",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    // Número de cuenta formateado con prefijo descriptivo
                    Text(
                        text = "Nº Cuenta: ${cuenta.numeroCuenta}",
                        modifier = Modifier.padding(top = 2.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Botón para desplegar el diálogo modal del código QR de cobros
                    androidx.compose.material3.OutlinedButton(
                        onClick = onMostrarQr,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Código QR")
                    }
                    Spacer(Modifier.width(8.dp))
                    // Botón para solicitar el cierre de la cuenta
                    IconButton(onClick = onCerrarCuenta) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Cerrar Cuenta",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            // Indicador del saldo disponible
            Text(
                text = "Saldo disponible",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(Modifier.height(2.dp))
            // Monto monetario formateado (se oculta si el sensor de proximidad está activo)
            Text(
                text = if (modoPrivacidad) "$ ••••" else formato.format(cuenta.saldo),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// Representa cada movimiento/transacción histórica individual en la lista
@Composable
private fun TarjetaMovimiento(movimiento: MovimientoDto, modoPrivacidad: Boolean) {
    val formatoMonto = NumberFormat.getCurrencyInstance(Locale.US)
    val esCredito = movimiento.tipo == "CREDITO"

    // Colores e indicadores de signo según el tipo de movimiento (Crédito=Verde/Débito=Rojo)
    val colorMonto = if (esCredito) Color(0xFF2E7D32) else Color(0xFFC62828)
    val signo = if (esCredito) "+" else "-"

    // Parseo y formateo amigable de la fecha de la transacción
    val fechaFormateada = try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        val formateador = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US)
        parser.parse(movimiento.fecha)?.let { formateador.format(it) } ?: movimiento.fecha
    } catch (e: Exception) {
        movimiento.fecha
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Lado Izquierdo: Descripción, Fecha y Referencia
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = movimiento.descripcion,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = fechaFormateada,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(Modifier.height(2.dp))
                // Referencia única bancaria para auditorías e integridad de base de datos
                Text(
                    text = "Ref: ${movimiento.referencia}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Spacer(Modifier.width(16.dp))
            // Lado Derecho: Monto de la transacción y Saldo resultante acumulado
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (modoPrivacidad) "$ ••••" else "$signo${formatoMonto.format(movimiento.monto).replace("$", "")}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = colorMonto
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (modoPrivacidad) "Saldo: B/. ••••" else "Saldo: ${formatoMonto.format(movimiento.saldoResultante)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

private suspend fun descargarQr(datos: String): Bitmap? {
    return withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.qrserver.com/v1/create-qr-code/?size=500x500&data=$datos")
            val connection = url.openConnection()
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            val input = connection.getInputStream()
            BitmapFactory.decodeStream(input)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
