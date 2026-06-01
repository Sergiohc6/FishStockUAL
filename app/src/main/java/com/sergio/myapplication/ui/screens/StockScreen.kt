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
import com.sergio.myapplication.data.GoogleSheetsService
import com.sergio.myapplication.data.Material
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockScreen(navController: NavController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val sheetsService = remember { GoogleSheetsService(context) }
    val scope = rememberCoroutineScope()

    val categorias = listOf("HARINAS", "ACEITES", "ADITIVOS", "MACROALGAS", "MICROALGAS", "OTROS")

    var materiales by remember { mutableStateOf<List<Material>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }
    var categoriaSeleccionada by remember { mutableStateOf("HARINAS") }
    var pendingAuthIntent by remember { mutableStateOf<Intent?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var mostrarBuscador by remember { mutableStateOf(false) }
    var soloStockCritico by remember { mutableStateOf(false) }

    // Stock mínimo por defecto en kg
    val stockMinimo = 5.0

    val colorCategoria = mapOf(
        "HARINAS" to Color(0xFF1a4a8a),
        "ACEITES" to Color(0xFF1a5a3a),
        "ADITIVOS" to Color(0xFF5a2a1a),
        "MACROALGAS" to Color(0xFF1a4a4a),
        "MICROALGAS" to Color(0xFF3a1a5a),
        "OTROS" to Color(0xFF4a3a1a)
    )

    val authLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        pendingAuthIntent = null
        scope.launch {
            isLoading = true
            errorMessage = ""
            try {
                val accountName = com.google.firebase.auth.FirebaseAuth
                    .getInstance().currentUser?.email ?: ""
                val rows = sheetsService.readSheet(accountName, categoriaSeleccionada)
                val stockReal = sheetsService.calcularStockReal(accountName, categoriaSeleccionada)
                materiales = rows.drop(4).mapNotNull { row ->
                    val nombre = row.getOrNull(0)?.toString() ?: return@mapNotNull null
                    if (nombre.isBlank()) return@mapNotNull null
                    val stock = stockReal[nombre] ?: 0.0
                    val proveedor = row.getOrNull(12)?.toString() ?: ""
                    val ubicacion = row.getOrNull(13)?.toString() ?: ""
                    val observaciones = row.getOrNull(14)?.toString() ?: ""
                    Material(
                        nombre = nombre,
                        categoria = categoriaSeleccionada,
                        stock = stock,
                        proveedor = proveedor,
                        ubicacion = ubicacion,
                        observaciones = observaciones
                    )
                }
            } catch (e: Exception) {
                errorMessage = "Error: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(pendingAuthIntent) {
        pendingAuthIntent?.let { authLauncher.launch(it) }
    }

    fun cargarMateriales(categoria: String) {
        scope.launch {
            isLoading = true
            errorMessage = ""
            try {
                val accountName = com.google.firebase.auth.FirebaseAuth
                    .getInstance().currentUser?.email ?: ""
                val rows = sheetsService.readSheet(accountName, categoria)
                val stockReal = sheetsService.calcularStockReal(accountName, categoria)
                materiales = rows.drop(4).mapNotNull { row ->
                    val nombre = row.getOrNull(0)?.toString() ?: return@mapNotNull null
                    if (nombre.isBlank()) return@mapNotNull null
                    val stock = stockReal[nombre] ?: 0.0
                    val proveedor = row.getOrNull(12)?.toString() ?: ""
                    val ubicacion = row.getOrNull(13)?.toString() ?: ""
                    val observaciones = row.getOrNull(14)?.toString() ?: ""
                    Material(
                        nombre = nombre,
                        categoria = categoria,
                        stock = stock,
                        proveedor = proveedor,
                        ubicacion = ubicacion,
                        observaciones = observaciones
                    )
                }
            } catch (e: UserRecoverableAuthIOException) {
                pendingAuthIntent = e.intent
            } catch (e: Exception) {
                errorMessage = "Error: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(categoriaSeleccionada) {
        cargarMateriales(categoriaSeleccionada)
    }

    // Filtrar materiales por búsqueda y stock crítico
    val materialesFiltrados = materiales.filter { material ->
        val coincideBusqueda = searchQuery.isEmpty() ||
                material.nombre.contains(searchQuery, ignoreCase = true) ||
                material.proveedor.contains(searchQuery, ignoreCase = true)
        val coincideStock = !soloStockCritico || material.stock <= stockMinimo
        coincideBusqueda && coincideStock
    }

    // Contar materiales con stock crítico
    val stockCriticoCount = materiales.count { it.stock <= stockMinimo }

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
                        text = "Stock",
                        color = Color(0xFFe8f4ff),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row {
                        // Botón buscador
                        IconButton(onClick = {
                            mostrarBuscador = !mostrarBuscador
                            if (!mostrarBuscador) searchQuery = ""
                        }) {
                            Icon(
                                if (mostrarBuscador) Icons.Default.SearchOff else Icons.Default.Search,
                                null,
                                tint = if (mostrarBuscador) Color(0xFF5b9bd5) else Color(0xFF4a7ab5)
                            )
                        }
                        IconButton(onClick = { cargarMateriales(categoriaSeleccionada) }) {
                            Icon(Icons.Default.Refresh, null, tint = Color(0xFF5b9bd5))
                        }
                    }
                }
            }

            // Buscador expandible
            if (mostrarBuscador) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar material o proveedor...", color = Color(0xFF4a7ab5)) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = Color(0xFF5b9bd5)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, null, tint = Color(0xFF4a7ab5))
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0f1e35))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF5b9bd5),
                        unfocusedBorderColor = Color(0xFF1e3a5f)
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Alerta de stock crítico
            if (stockCriticoCount > 0 && !isLoading) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF3a1a1a)),
                    shape = RoundedCornerShape(10.dp),
                    onClick = { soloStockCritico = !soloStockCritico }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Warning,
                                null,
                                tint = Color(0xFFe24b4a),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "$stockCriticoCount materiales con stock crítico ( ≤ ${stockMinimo.toInt()} kg) ",
                                color = Color(0xFFe24b4a),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Text(
                            text = if (soloStockCritico) "Ver todos" else "Ver",
                            color = Color(0xFFe24b4a),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            ScrollableTabRow(
                selectedTabIndex = categorias.indexOf(categoriaSeleccionada),
                containerColor = Color(0xFF0f1e35),
                contentColor = Color(0xFF5b9bd5),
                edgePadding = 8.dp
            ) {
                categorias.forEach { categoria ->
                    Tab(
                        selected = categoriaSeleccionada == categoria,
                        onClick = {
                            categoriaSeleccionada = categoria
                            searchQuery = ""
                            soloStockCritico = false
                        },
                        text = {
                            Text(
                                text = categoria,
                                fontSize = 12.sp,
                                color = if (categoriaSeleccionada == categoria)
                                    Color(0xFF5b9bd5)
                                else
                                    Color(0xFF4a7ab5)
                            )
                        }
                    )
                }
            }

            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color(0xFF5b9bd5))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Cargando $categoriaSeleccionada...", color = Color(0xFF4a7ab5), fontSize = 13.sp)
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
                                onClick = { cargarMateriales(categoriaSeleccionada) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1a4a8a))
                            ) {
                                Text("Reintentar", color = Color(0xFFe8f4ff))
                            }
                        }
                    }
                }

                materialesFiltrados.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.SearchOff,
                                null,
                                tint = Color(0xFF4a7ab5),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (searchQuery.isNotEmpty())
                                    "No se encontró \"$searchQuery\""
                                else
                                    "No hay materiales en $categoriaSeleccionada",
                                color = Color(0xFF4a7ab5),
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${materialesFiltrados.size} materiales",
                                    color = Color(0xFF4a7ab5),
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "Stock total: ${"%.1f".format(materialesFiltrados.sumOf { it.stock })} kg",
                                    color = Color(0xFF4a7ab5),
                                    fontSize = 12.sp
                                )
                            }
                        }

                        items(materialesFiltrados) { material ->
                            MaterialCard(
                                material = material,
                                color = colorCategoria[categoriaSeleccionada] ?: Color(0xFF1a4a8a),
                                stockMinimo = stockMinimo
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MaterialCard(material: Material, color: Color, stockMinimo: Double = 5.0) {
    var expanded by remember { mutableStateOf(false) }
    val esCritico = material.stock <= stockMinimo && material.stock > 0
    val esVacio = material.stock <= 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0f1e35)),
        shape = RoundedCornerShape(12.dp),
        onClick = { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Icono de alerta si stock crítico
                    if (esCritico) {
                        Icon(
                            Icons.Default.Warning,
                            null,
                            tint = Color(0xFFf0a030),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = material.nombre,
                        color = Color(0xFFe8f4ff),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Box(
                    modifier = Modifier
                        .background(
                            color = when {
                                esVacio -> Color(0xFF3a1a1a)
                                esCritico -> Color(0xFF3a2a0a)
                                else -> color
                            },
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${"%.1f".format(material.stock)} kg",
                        color = when {
                            esVacio -> Color(0xFFe24b4a)
                            esCritico -> Color(0xFFf0a030)
                            else -> Color(0xFFe8f4ff)
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFF1e3a5f))
                Spacer(modifier = Modifier.height(12.dp))

                if (material.proveedor.isNotBlank()) {
                    DetalleRow("Proveedor", material.proveedor)
                }
                if (material.ubicacion.isNotBlank()) {
                    DetalleRow("Ubicación", material.ubicacion)
                }
                if (material.observaciones.isNotBlank()) {
                    DetalleRow("Obs.", material.observaciones)
                }
                if (esCritico) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "⚠ Stock por debajo del mínimo recomendado (${stockMinimo.toInt()} kg)",
                        color = Color(0xFFf0a030),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
fun DetalleRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Text(text = "$label: ", color = Color(0xFF4a7ab5), fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Text(text = value, color = Color(0xFF8ab8e8), fontSize = 12.sp)
    }
}