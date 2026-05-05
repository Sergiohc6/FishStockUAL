package com.sergio.myapplication.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.firebase.auth.FirebaseAuth
import com.sergio.myapplication.data.GoogleSheetsService
import kotlinx.coroutines.launch

data class MovimientoHistorial(
    val fecha: String,
    val tipo: String,
    val descripcion: String,
    val cantidad: Double,
    val extra: String,
    val usuario: String,
    val esEntrada: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialScreen(navController: NavController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val sheetsService = remember { GoogleSheetsService(context) }
    val scope = rememberCoroutineScope()

    var movimientos by remember { mutableStateOf<List<MovimientoHistorial>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }
    var filtro by remember { mutableStateOf("TODOS") }
    var pendingAuthIntent by remember { mutableStateOf<Intent?>(null) }

    val authLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { pendingAuthIntent = null }

    LaunchedEffect(pendingAuthIntent) {
        pendingAuthIntent?.let { authLauncher.launch(it) }
    }

    fun cargarHistorial() {
        scope.launch {
            isLoading = true
            errorMessage = ""
            try {
                val accountName = FirebaseAuth.getInstance().currentUser?.email ?: ""
                val lista = mutableListOf<MovimientoHistorial>()

                // Leer entradas
                val rowsEntradas = sheetsService.readSheet(accountName, "ENTRADAS")
                rowsEntradas.drop(2).forEach { row ->
                    val descripcion = row.getOrNull(2)?.toString() ?: return@forEach
                    if (descripcion.isBlank()) return@forEach
                    lista.add(
                        MovimientoHistorial(
                            fecha = row.getOrNull(0)?.toString() ?: "",
                            tipo = row.getOrNull(1)?.toString() ?: "",
                            descripcion = descripcion,
                            cantidad = row.getOrNull(3)?.toString()
                                ?.replace(",", ".")?.toDoubleOrNull() ?: 0.0,
                            extra = row.getOrNull(4)?.toString() ?: "",
                            usuario = row.getOrNull(7)?.toString() ?: "",
                            esEntrada = true
                        )
                    )
                }

                // Leer salidas
                val rowsSalidas = sheetsService.readSheet(accountName, "SALIDAS")
                rowsSalidas.drop(2).forEach { row ->
                    val descripcion = row.getOrNull(2)?.toString() ?: return@forEach
                    if (descripcion.isBlank()) return@forEach
                    lista.add(
                        MovimientoHistorial(
                            fecha = row.getOrNull(0)?.toString() ?: "",
                            tipo = row.getOrNull(1)?.toString() ?: "",
                            descripcion = descripcion,
                            cantidad = row.getOrNull(3)?.toString()
                                ?.replace(",", ".")?.toDoubleOrNull() ?: 0.0,
                            extra = row.getOrNull(4)?.toString() ?: "",
                            usuario = row.getOrNull(6)?.toString() ?: "",
                            esEntrada = false
                        )
                    )
                }

                movimientos = lista.sortedByDescending { it.fecha }

            } catch (e: UserRecoverableAuthIOException) {
                pendingAuthIntent = e.intent
            } catch (e: Exception) {
                errorMessage = "Error: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        cargarHistorial()
    }

    val movimientosFiltrados = when (filtro) {
        "ENTRADAS" -> movimientos.filter { it.esEntrada }
        "SALIDAS" -> movimientos.filter { !it.esEntrada }
        else -> movimientos
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0a1628))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0f1e35))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color(0xFF5b9bd5))
                    }
                    Text(
                        text = "Historial",
                        color = Color(0xFFe8f4ff),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { cargarHistorial() }) {
                        Icon(Icons.Default.Refresh, null, tint = Color(0xFF5b9bd5))
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0f1e35))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("TODOS", "ENTRADAS", "SALIDAS").forEach { opcion ->
                    FilterChip(
                        selected = filtro == opcion,
                        onClick = { filtro = opcion },
                        label = {
                            Text(
                                text = opcion,
                                fontSize = 12.sp,
                                color = if (filtro == opcion) Color.White else Color(0xFF4a7ab5)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = when (opcion) {
                                "ENTRADAS" -> Color(0xFF1a5a3a)
                                "SALIDAS" -> Color(0xFF5a2a1a)
                                else -> Color(0xFF1a4a8a)
                            },
                            containerColor = Color(0xFF0a1628)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = filtro == opcion,
                            borderColor = Color(0xFF1e3a5f),
                            selectedBorderColor = Color.Transparent
                        )
                    )
                }
            }

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color(0xFF5b9bd5))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Cargando historial...",
                                color = Color(0xFF4a7ab5),
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                errorMessage.isNotEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(Icons.Default.Warning, null, tint = Color(0xFFe24b4a), modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(text = errorMessage, color = Color(0xFFe24b4a), fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { cargarHistorial() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1a4a8a))
                            ) {
                                Text("Reintentar", color = Color(0xFFe8f4ff))
                            }
                        }
                    }
                }

                movimientosFiltrados.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No hay movimientos registrados",
                            color = Color(0xFF4a7ab5),
                            fontSize = 13.sp
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Text(
                                text = "${movimientosFiltrados.size} movimientos",
                                color = Color(0xFF4a7ab5),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        items(movimientosFiltrados) { movimiento ->
                            MovimientoCard(movimiento = movimiento)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MovimientoCard(movimiento: MovimientoHistorial) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0f1e35)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = if (movimiento.esEntrada) Color(0xFF1a3a2a) else Color(0xFF3a1a1a),
                        shape = RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (movimiento.esEntrada) Icons.Default.Add else Icons.Default.Remove,
                    contentDescription = null,
                    tint = if (movimiento.esEntrada) Color(0xFF4a9a40) else Color(0xFFe24b4a),
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = movimiento.descripcion,
                    color = Color(0xFFe8f4ff),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = movimiento.tipo, color = Color(0xFF4a7ab5), fontSize = 11.sp)
                    if (movimiento.extra.isNotBlank()) {
                        Text(text = "·", color = Color(0xFF4a7ab5), fontSize = 11.sp)
                        Text(text = movimiento.extra, color = Color(0xFF4a7ab5), fontSize = 11.sp)
                    }
                }
                if (movimiento.fecha.isNotBlank()) {
                    Text(text = movimiento.fecha, color = Color(0xFF2d5080), fontSize = 11.sp)
                }
                if (movimiento.usuario.isNotBlank()) {
                    Text(
                        text = movimiento.usuario,
                        color = Color(0xFF2d5080),
                        fontSize = 11.sp
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (movimiento.esEntrada) "+" else "-"}${"%.2f".format(movimiento.cantidad)}",
                    color = if (movimiento.esEntrada) Color(0xFF4a9a40) else Color(0xFFe24b4a),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(text = "kg", color = Color(0xFF4a7ab5), fontSize = 11.sp)
            }
        }
    }
}