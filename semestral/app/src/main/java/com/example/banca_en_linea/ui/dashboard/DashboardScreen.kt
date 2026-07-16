package com.example.banca_en_linea.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.banca_en_linea.R
import com.example.banca_en_linea.data.remote.dto.CuentaDto
import java.text.NumberFormat
import java.util.Locale
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.DisposableEffect
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onCerrarSesion: () -> Unit,
    onNuevaTransferencia: () -> Unit,
    onVerMovimientos: (Long) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var modoPrivacidad by remember { mutableStateOf(false) }

    var mostrarDialogoCreacion by remember { mutableStateOf(false) }
    var tipoCuentaSeleccionada by remember { mutableStateOf("AHORRO") }
    var creandoCuenta by remember { mutableStateOf(false) }

    val context = LocalContext.current
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        var lastToastTime: Long = 0

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return
                val distance = event.values[0]
                val maxRange = proximitySensor?.maximumRange ?: 5f
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

    // Se ejecuta cada vez que el dashboard ENTRA a composición — incluida la
    // vuelta desde la pantalla de transferencia. Así los saldos se refrescan
    // solos después de transferir, sin que el usuario tenga que hacer nada.
    LaunchedEffect(Unit) {
        viewModel.cargar()
    }

    Scaffold(
        floatingActionButton = {
            // Botón circular inferior derecho → nueva transferencia
            FloatingActionButton(
                onClick = onNuevaTransferencia,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(
                    Icons.Default.Payments,
                    contentDescription = stringResource(R.string.transferencia_nueva),
                )
            }
        },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                actions = {
                    TextButton(onClick = {
                        viewModel.cerrarSesion()
                        onCerrarSesion()
                    }) {
                        Text(
                            stringResource(R.string.dashboard_cerrar_sesion),
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                // Estado: cargando
                state.cargando -> CircularProgressIndicator(Modifier.align(Alignment.Center))

                // Estado: error con opción de reintentar
                state.error != null -> Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(state.error!!, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = viewModel::cargar) {
                        Text(stringResource(R.string.reintentar))
                    }
                }

                // Estado: contenido
                else -> LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        Text(
                            text = stringResource(R.string.dashboard_saludo, state.nombreCliente),
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.dashboard_mis_cuentas),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                    items(state.cuentas, key = { it.id }) { cuenta ->
                        TarjetaCuenta(
                            cuenta = cuenta,
                            modoPrivacidad = modoPrivacidad,
                            onClick = { onVerMovimientos(cuenta.id) }
                        )
                    }
                    item {
                        Card(
                            onClick = { mostrarDialogoCreacion = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.Transparent,
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            )
                        ) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Abrir Cuenta",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "Abrir nueva cuenta",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                    item {
                        Spacer(Modifier.height(12.dp))
                        DonutChart(cuentas = state.cuentas, modoPrivacidad = modoPrivacidad)
                    }
                }
            }
        }
    }

    if (mostrarDialogoCreacion) {
        AlertDialog(
            onDismissRequest = { if (!creandoCuenta) mostrarDialogoCreacion = false },
            title = { Text("Abrir Nueva Cuenta") },
            text = {
                Column {
                    Text("Selecciona el tipo de cuenta que deseas aperturar:")
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        val isAhorro = tipoCuentaSeleccionada == "AHORRO"
                        Card(
                            onClick = { tipoCuentaSeleccionada = "AHORRO" },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isAhorro) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            border = if (isAhorro) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Ahorros", style = MaterialTheme.typography.titleMedium)
                            }
                        }

                        val isCorriente = tipoCuentaSeleccionada == "CORRIENTE"
                        Card(
                            onClick = { tipoCuentaSeleccionada = "CORRIENTE" },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCorriente) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            border = if (isCorriente) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Corriente", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !creandoCuenta,
                    onClick = {
                        creandoCuenta = true
                        viewModel.crearNuevaCuenta(tipoCuentaSeleccionada) { exito ->
                            creandoCuenta = false
                            if (exito) {
                                mostrarDialogoCreacion = false
                                Toast.makeText(context, "¡Cuenta creada con éxito!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Error al crear la cuenta", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) {
                    if (creandoCuenta) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Aperturar")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !creandoCuenta,
                    onClick = { mostrarDialogoCreacion = false }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

// Tarjeta de cuenta individual para el listado del Dashboard
@Composable
private fun TarjetaCuenta(
    cuenta: CuentaDto,
    modoPrivacidad: Boolean,
    onClick: () -> Unit
) {
    val formato = NumberFormat.getCurrencyInstance(Locale.US)

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            // Tipo de Cuenta (Ahorros o Corriente)
            Text(
                text = if (cuenta.tipo == "AHORRO")
                    stringResource(R.string.cuenta_tipo_ahorro)
                else
                    stringResource(R.string.cuenta_tipo_corriente),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            // Identificador con el número de cuenta formateado
            Text(
                text = "Nº Cuenta: ${cuenta.numeroCuenta}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.height(12.dp))
            // Etiqueta del saldo
            Text(
                text = "Saldo disponible",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(Modifier.height(2.dp))
            // Monto monetario formateado (se oculta si el sensor de proximidad activa modoPrivacidad)
            Text(
                text = if (modoPrivacidad) "$ ••••" else formato.format(cuenta.saldo),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun DonutChart(
    cuentas: List<CuentaDto>,
    modoPrivacidad: Boolean,
    modifier: Modifier = Modifier
) {
    if (cuentas.isEmpty()) return

    val cuentasAgrupadas = cuentas.groupBy { it.tipo }.map { (tipo, lista) ->
        CuentaDto(
            id = if (tipo == "AHORRO") 1L else 2L,
            numeroCuenta = tipo,
            tipo = tipo,
            saldo = lista.sumOf { it.saldo },
            moneda = "USD"
        )
    }

    val total = cuentasAgrupadas.sumOf { it.saldo }
    if (total <= 0.0) return

    val colores = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.error
    )

    var animationPlayed by remember { mutableStateOf(false) }
    LaunchedEffect(key1 = true) {
        animationPlayed = true
    }
    val scaleFactor by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(durationMillis = 1000)
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Distribución de Fondos",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(120.dp)
                ) {
                    Canvas(modifier = Modifier.size(100.dp)) {
                        var startAngle = -90f
                        cuentasAgrupadas.forEachIndexed { index, cuenta ->
                            val sweepAngle = ((cuenta.saldo / total).toFloat() * 360f) * scaleFactor
                            drawArc(
                                color = colores.getOrElse(index) { Color.Gray },
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                style = Stroke(width = 12.dp.toPx())
                            )
                            startAngle += sweepAngle
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Total",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val formato = NumberFormat.getCurrencyInstance(Locale.US)
                        Text(
                            text = if (modoPrivacidad) "$ ••••" else formato.format(total),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(Modifier.width(16.dp))

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    cuentasAgrupadas.forEachIndexed { index, cuenta ->
                        val color = colores.getOrElse(index) { Color.Gray }
                        val porcentaje = (cuenta.saldo / total) * 100
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(color, shape = CircleShape)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "${if (cuenta.tipo == "AHORRO") "Ahorros" else "Corriente"}: ${String.format(Locale.US, "%.1f", porcentaje)}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}
