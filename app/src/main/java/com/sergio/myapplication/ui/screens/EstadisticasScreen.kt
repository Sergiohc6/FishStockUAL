package com.sergio.myapplication.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.firebase.auth.FirebaseAuth
import com.sergio.myapplication.data.GoogleSheetsService
import kotlinx.coroutines.launch

data class ResumenMovimiento(
    val descripcion: String,
    val totalEntradas: Double,
    val totalSalidas: Double,
    val stockActual: Double
)

@Composable
fun EstadisticasScreen(navController: NavController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val sheetsService = remember { GoogleSheetsService(context) }
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }
    var pendingAuthIntent by remember { mutableStateOf<Intent?>(null) }

    // Datos calculados
    var totalEntradas by remember { mutableStateOf(0) }
    var totalSalidas by remember { mutableStateOf(0) }
    var totalMateriales by remember { mutableStateOf(0) }
    var materialesCriticos by remember { mutableStateOf(0) }
    var topMateriales by remember { mutableStateOf<List<ResumenMovimiento>>(emptyList()) }
    var entradasPorCategoria by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var salidasPorCategoria by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }

    val authLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { pendingAuthIntent = null }

    LaunchedEffect(pendingAuthIntent) {
        pendingAuthIntent?.let { authLauncher.launch(it) }
    }

    fun cargarEstadisticas() {
        scope.launch {
            isLoading = true
            errorMessage = ""
            try {
                val accountName = FirebaseAuth.getInstance().currentUser?.email ?: ""
                val categorias = listOf("HARINAS", "ACEITES", "ADITIVOS", "MACROALGAS", "MICROALGAS", "OTROS")

                // Leer entradas
                val rowsEntradas = sheetsService.readSheet(accountName, "ENTRADAS")
                val entradasData = rowsEntradas.drop(2).mapNotNull { row ->
                    val tipo = row.getOrNull(1)?.toString() ?: return@mapNotNull null
                    val cantidad = row.getOrNull(3)?.toString()?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
                    Pair(tipo, cantidad)
                }
                totalEntradas = entradasData.size

                // Leer salidas
                val rowsSalidas = sheetsService.readSheet(accountName, "SALIDAS")
                val salidasData = rowsSalidas.drop(2).mapNotNull { row ->
                    val tipo = row.getOrNull(1)?.toString() ?: return@mapNotNull null
                    val cantidad = row.getOrNull(3)?.toString()?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
                    Pair(tipo, cantidad)
                }
                totalSalidas = salidasData.size

                // Entradas y salidas por categoría
                entradasPorCategoria = entradasData
                    .groupBy { it.first }
                    .mapValues { entry -> entry.value.sumOf { it.second } }

                salidasPorCategoria = salidasData
                    .groupBy { it.first }
                    .mapValues { entry -> entry.value.sumOf { it.second } }

                // Materiales totales y críticos
                var totalMat = 0
                var criticos = 0
                val movimientos = mutableMapOf<String, ResumenMovimiento>()

                // Entradas por material
                val entradasPorMaterial = rowsEntradas.drop(2).mapNotNull { row ->
                    val desc = row.getOrNull(2)?.toString() ?: return@mapNotNull null
                    val cantidad = row.getOrNull(3)?.toString()?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
                    Pair(desc, cantidad)
                }.groupBy { it.first }.mapValues { e -> e.value.sumOf { it.second } }

                // Salidas por material
                val salidasPorMaterial = rowsSalidas.drop(2).mapNotNull { row ->
                    val desc = row.getOrNull(2)?.toString() ?: return@mapNotNull null
                    val cantidad = row.getOrNull(3)?.toString()?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
                    Pair(desc, cantidad)
                }.groupBy { it.first }.mapValues { e -> e.value.sumOf { it.second } }

                // Calcular para cada categoría
                categorias.forEach { categoria ->
                    val rows = sheetsService.readSheet(accountName, categoria)
                    val stockReal = sheetsService.calcularStockReal(accountName, categoria)
                    rows.drop(4).forEach { row ->
                        val nombre = row.getOrNull(0)?.toString() ?: return@forEach
                        if (nombre.isBlank()) return@forEach
                        totalMat++
                        val stock = stockReal[nombre] ?: 0.0
                        if (stock <= 5.0) criticos++

                        val entradas = entradasPorMaterial[nombre] ?: 0.0
                        val salidas = salidasPorMaterial[nombre] ?: 0.0
                        if (entradas > 0 || salidas > 0) {
                            movimientos[nombre] = ResumenMovimiento(
                                descripcion = nombre,
                                totalEntradas = entradas,
                                totalSalidas = salidas,
                                stockActual = stock
                            )
                        }
                    }
                }

                totalMateriales = totalMat
                materialesCriticos = criticos

                // Top 5 materiales con más movimiento
                topMateriales = movimientos.values
                    .sortedByDescending { it.totalEntradas + it.totalSalidas }
                    .take(5)

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
        cargarEstadisticas()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0a1628))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // TopBar
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
                        text = "Estadísticas",
                        color = Color(0xFFe8f4ff),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { cargarEstadisticas() }) {
                        Icon(Icons.Default.Refresh, null, tint = Color(0xFF5b9bd5))
                    }
                }
            }

            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color(0xFF5b9bd5))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Calculando estadísticas...", color = Color(0xFF4a7ab5), fontSize = 13.sp)
                        }
                    }
                }

                errorMessage.isNotEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(Icons.Default.Warning, null, tint = Color(0xFFe24b4a), modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(text = errorMessage, color = Color(0xFFe24b4a), fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { cargarEstadisticas() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1a4a8a))
                            ) {
                                Text("Reintentar", color = Color(0xFFe8f4ff))
                            }
                        }
                    }
                }

                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {

                        // Tarjetas resumen
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ResumenCard(
                                modifier = Modifier.weight(1f),
                                titulo = "Materiales",
                                valor = totalMateriales.toString(),
                                icono = Icons.Default.Inventory,
                                color = Color(0xFF1a4a8a)
                            )
                            ResumenCard(
                                modifier = Modifier.weight(1f),
                                titulo = "Stock crítico",
                                valor = materialesCriticos.toString(),
                                icono = Icons.Default.Warning,
                                color = if (materialesCriticos > 0) Color(0xFF5a2a1a) else Color(0xFF1a3a2a)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ResumenCard(
                                modifier = Modifier.weight(1f),
                                titulo = "Entradas",
                                valor = totalEntradas.toString(),
                                icono = Icons.Default.Add,
                                color = Color(0xFF1a5a3a)
                            )
                            ResumenCard(
                                modifier = Modifier.weight(1f),
                                titulo = "Salidas",
                                valor = totalSalidas.toString(),
                                icono = Icons.Default.Remove,
                                color = Color(0xFF5a2a1a)
                            )
                        }

                        // Gráfica de barras por categoría
                        if (entradasPorCategoria.isNotEmpty() || salidasPorCategoria.isNotEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0f1e35)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Movimientos por categoría",
                                        color = Color(0xFFe8f4ff),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(bottom = 16.dp)
                                    )

                                    val categorias = listOf("HARINAS", "ACEITES", "ADITIVOS", "MACROALGAS", "MICROALGAS", "OTROS")
                                    val maxVal = categorias.maxOfOrNull { cat ->
                                        maxOf(
                                            entradasPorCategoria[cat] ?: 0.0,
                                            salidasPorCategoria[cat] ?: 0.0
                                        )
                                    }?.takeIf { it > 0 } ?: 1.0

                                    categorias.forEach { categoria ->
                                        val entradas = entradasPorCategoria[categoria] ?: 0.0
                                        val salidas = salidasPorCategoria[categoria] ?: 0.0
                                        if (entradas > 0 || salidas > 0) {
                                            BarraCategoria(
                                                categoria = categoria,
                                                entradas = entradas,
                                                salidas = salidas,
                                                maxVal = maxVal
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                        }
                                    }

                                    // Leyenda
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(modifier = Modifier.size(12.dp).background(Color(0xFF1a5a3a), RoundedCornerShape(2.dp)))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Entradas", color = Color(0xFF4a7ab5), fontSize = 11.sp)
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Box(modifier = Modifier.size(12.dp).background(Color(0xFF5a2a1a), RoundedCornerShape(2.dp)))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Salidas", color = Color(0xFF4a7ab5), fontSize = 11.sp)
                                    }
                                }
                            }
                        }

                        // Top 5 materiales con más movimiento
                        if (topMateriales.isNotEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0f1e35)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Top 5 materiales con más movimiento",
                                        color = Color(0xFFe8f4ff),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(bottom = 16.dp)
                                    )

                                    topMateriales.forEachIndexed { index, item ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Número ranking
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .background(Color(0xFF1a4a8a), RoundedCornerShape(6.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "${index + 1}",
                                                    color = Color.White,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = item.descripcion,
                                                    color = Color(0xFFe8f4ff),
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Text(
                                                        text = "+${"%.1f".format(item.totalEntradas)} kg",
                                                        color = Color(0xFF4a9a40),
                                                        fontSize = 11.sp
                                                    )
                                                    Text(
                                                        text = "-${"%.1f".format(item.totalSalidas)} kg",
                                                        color = Color(0xFFe24b4a),
                                                        fontSize = 11.sp
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "${"%.1f".format(item.stockActual)} kg",
                                                color = if (item.stockActual <= 5.0) Color(0xFFe24b4a) else Color(0xFF4a7ab5),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        if (index < topMateriales.size - 1) {
                                            HorizontalDivider(color = Color(0xFF1e3a5f))
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ResumenCard(
    modifier: Modifier = Modifier,
    titulo: String,
    valor: String,
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icono, null, tint = Color.White, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = valor,
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = titulo,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun BarraCategoria(
    categoria: String,
    entradas: Double,
    salidas: Double,
    maxVal: Double
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = categoria,
            color = Color(0xFF4a7ab5),
            fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Barra entradas
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(16.dp)
                    .background(Color(0xFF1e3a5f), RoundedCornerShape(4.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth((entradas / maxVal).toFloat().coerceIn(0f, 1f))
                        .background(Color(0xFF1a5a3a), RoundedCornerShape(4.dp))
                )
            }
            Text(
                text = "${"%.0f".format(entradas)}",
                color = Color(0xFF4a9a40),
                fontSize = 10.sp,
                modifier = Modifier.width(40.dp),
                textAlign = TextAlign.End
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Barra salidas
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(16.dp)
                    .background(Color(0xFF1e3a5f), RoundedCornerShape(4.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth((salidas / maxVal).toFloat().coerceIn(0f, 1f))
                        .background(Color(0xFF5a2a1a), RoundedCornerShape(4.dp))
                )
            }
            Text(
                text = "${"%.0f".format(salidas)}",
                color = Color(0xFFe24b4a),
                fontSize = 10.sp,
                modifier = Modifier.width(40.dp),
                textAlign = TextAlign.End
            )
        }
    }
}