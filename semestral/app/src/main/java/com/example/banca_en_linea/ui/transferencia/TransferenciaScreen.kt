package com.example.banca_en_linea.ui.transferencia

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.banca_en_linea.R
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferenciaScreen(
    viewModel: TransferenciaViewModel,
    onVolver: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val mensajeQrPronto = stringResource(R.string.transferencia_qr_pronto)
    val mensajeMontoInvalido = stringResource(R.string.transferencia_monto_invalido)
    val context = LocalContext.current
    val scanner = remember {
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        GmsBarcodeScanning.getClient(context, options)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.transferencia_titulo)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.atras),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        when {
            state.cargandoCuentas -> Column(
                Modifier.fillMaxSize().padding(innerPadding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) { CircularProgressIndicator() }

            // Comprobante: reemplaza el formulario cuando la transferencia triunfa
            state.comprobante != null -> ComprobanteExito(
                referencia = state.comprobante!!.referencia,
                onOtra = viewModel::nuevaTransferencia,
                onVolver = onVolver,
                modifier = Modifier.padding(innerPadding),
            )

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()), // teclado abierto no corta campos
            ) {
                Spacer(Modifier.height(16.dp))

                // --- Cuenta origen (dropdown) ---
                var expandido by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expandido,
                    onExpandedChange = { expandido = it },
                ) {
                    OutlinedTextField(
                        value = state.origen?.let { "${it.numeroCuenta}  ($${it.saldo})" } ?: "",
                        onValueChange = {},          // readOnly: solo se elige del menú
                        readOnly = true,
                        label = { Text(stringResource(R.string.transferencia_origen)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandido) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                    )
                    ExposedDropdownMenu(
                        expanded = expandido,
                        onDismissRequest = { expandido = false },
                    ) {
                        state.cuentas.forEach { cuenta ->
                            DropdownMenuItem(
                                text = { Text("${cuenta.numeroCuenta}  ($${cuenta.saldo})") },
                                onClick = {
                                    viewModel.onOrigenChange(cuenta)
                                    expandido = false
                                },
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // --- Cuenta destino + QR ---
                OutlinedTextField(
                    value = state.destino,
                    onValueChange = viewModel::onDestinoChange,
                    label = { Text(stringResource(R.string.transferencia_destino)) },
                    placeholder = { Text("UTPB-0000-0000") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                // Escaneo de código QR mediante Google Code Scanner
                OutlinedButton(
                    onClick = {
                        scanner.startScan()
                            .addOnSuccessListener { barcode ->
                                barcode.rawValue?.let { valorQr ->
                                    viewModel.procesarResultadoQr(valorQr)
                                }
                            }
                            .addOnFailureListener { e ->
                                scope.launch {
                                    snackbarHostState.showSnackbar("Error al escanear QR: ${e.localizedMessage ?: "Cancelado"}")
                                }
                            }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("📷  " + stringResource(R.string.transferencia_qr))
                }

                Spacer(Modifier.height(16.dp))

                // --- Monto ---
                OutlinedTextField(
                    value = state.monto,
                    onValueChange = viewModel::onMontoChange,
                    label = { Text(stringResource(R.string.transferencia_monto)) },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(16.dp))

                // --- Descripción ---
                OutlinedTextField(
                    value = state.descripcion,
                    onValueChange = viewModel::onDescripcionChange,
                    label = { Text(stringResource(R.string.transferencia_descripcion)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                state.error?.let { mensaje ->
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = mensaje,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = { viewModel.transferir(mensajeMontoInvalido) },
                    enabled = state.puedeEnviar,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (state.enviando) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text(stringResource(R.string.transferencia_boton))
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ComprobanteExito(
    referencia: String,
    onOtra: () -> Unit,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
        ) {
            Column(
                Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("✅", style = MaterialTheme.typography.displaySmall)
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.transferencia_exito),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.transferencia_referencia, referencia),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Button(onClick = onVolver, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.transferencia_volver))
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onOtra, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.transferencia_nueva))
        }
    }
}
