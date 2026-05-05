package com.sergio.myapplication.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.firebase.auth.FirebaseAuth
import com.sergio.myapplication.data.GoogleSheetsService
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntradasScreen(navController: NavController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val sheetsService = remember { GoogleSheetsService(context) }
    val scope = rememberCoroutineScope()

    var tipo by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var cantidad by remember { mutableStateOf("") }
    var proveedor by remember { mutableStateOf("") }
    var lote by remember { mutableStateOf("") }
    var observaciones by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }
    var isLoadingMateriales by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var pendingAuthIntent by remember { mutableStateOf<Intent?>(null) }

    var tipoExpanded by remember { mutableStateOf(false) }
    var descripcionExpanded by remember { mutableStateOf(false) }
    var materiales by remember { mutableStateOf<List<String>>(emptyList()) }

    val tiposCategoria = listOf("HARINAS", "ACEITES", "ADITIVOS", "MACROALGAS", "MICROALGAS", "OTROS")

    val authLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { pendingAuthIntent = null }

    LaunchedEffect(pendingAuthIntent) {
        pendingAuthIntent?.let { authLauncher.launch(it) }
    }

    // Cargar materiales cuando cambia la categoría
    fun cargarMateriales(categoria: String) {
        scope.launch {
            isLoadingMateriales = true
            descripcion = ""
            try {
                val accountName = FirebaseAuth.getInstance().currentUser?.email ?: ""
                val rows = sheetsService.readSheet(accountName, categoria)
                materiales = rows.drop(4).mapNotNull { row ->
                    val nombre = row.getOrNull(0)?.toString() ?: return@mapNotNull null
                    if (nombre.isBlank()) return@mapNotNull null
                    nombre
                }
            } catch (e: Exception) {
                errorMessage = "Error al cargar materiales: ${e.message}"
            } finally {
                isLoadingMateriales = false
            }
        }
    }

    fun registrarEntrada() {
        if (tipo.isEmpty() || descripcion.isEmpty() || cantidad.isEmpty()) {
            errorMessage = "Tipo, descripción y cantidad son obligatorios"
            return
        }
        val cantidadDouble = cantidad.toDoubleOrNull()
        if (cantidadDouble == null || cantidadDouble <= 0) {
            errorMessage = "La cantidad debe ser un número válido mayor que 0"
            return
        }

        scope.launch {
            isLoading = true
            errorMessage = ""
            successMessage = ""
            try {
                val accountName = FirebaseAuth.getInstance().currentUser?.email ?: ""
                val fecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

                sheetsService.addEntrada(
                    accountName = accountName,
                    fecha = fecha,
                    tipo = tipo,
                    descripcion = descripcion,
                    cantidad = cantidadDouble,
                    proveedor = proveedor,
                    lote = lote,
                    observaciones = observaciones
                )

                successMessage = "Entrada registrada correctamente"
                tipo = ""
                descripcion = ""
                cantidad = ""
                proveedor = ""
                lote = ""
                observaciones = ""
                materiales = emptyList()

            } catch (e: UserRecoverableAuthIOException) {
                pendingAuthIntent = e.intent
            } catch (e: Exception) {
                errorMessage = "Error: ${e.message}"
            } finally {
                isLoading = false
            }
        }
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
                        text = "Registrar Entrada",
                        color = Color(0xFFe8f4ff),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.size(48.dp))
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                // Fecha automática
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0f1e35)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CalendarToday, null,
                            tint = Color(0xFF5b9bd5),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Fecha: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())}",
                            color = Color(0xFFe8f4ff),
                            fontSize = 14.sp
                        )
                    }
                }

                // Selector de categoría
                Text(
                    text = "Categoría *",
                    color = Color(0xFF4a7ab5),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                ExposedDropdownMenuBox(
                    expanded = tipoExpanded,
                    onExpandedChange = { tipoExpanded = it }
                ) {
                    OutlinedTextField(
                        value = tipo,
                        onValueChange = {},
                        readOnly = true,
                        placeholder = { Text("Selecciona categoría", color = Color(0xFF4a7ab5)) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = tipoExpanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF5b9bd5),
                            unfocusedBorderColor = Color(0xFF1e3a5f)
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = tipoExpanded,
                        onDismissRequest = { tipoExpanded = false },
                        modifier = Modifier.background(Color(0xFF0f1e35))
                    ) {
                        tiposCategoria.forEach { categoria ->
                            DropdownMenuItem(
                                text = { Text(categoria, color = Color(0xFFe8f4ff)) },
                                onClick = {
                                    tipo = categoria
                                    tipoExpanded = false
                                    cargarMateriales(categoria)
                                }
                            )
                        }
                    }
                }

                // Selector de material
                Text(
                    text = "Material *",
                    color = Color(0xFF4a7ab5),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )

                if (isLoadingMateriales) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF5b9bd5),
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Cargando materiales...",
                            color = Color(0xFF4a7ab5),
                            fontSize = 12.sp
                        )
                    }
                } else {
                    ExposedDropdownMenuBox(
                        expanded = descripcionExpanded,
                        onExpandedChange = {
                            if (tipo.isNotEmpty()) descripcionExpanded = it
                        }
                    ) {
                        OutlinedTextField(
                            value = descripcion,
                            onValueChange = {},
                            readOnly = true,
                            placeholder = {
                                Text(
                                    if (tipo.isEmpty()) "Selecciona primero una categoría"
                                    else "Selecciona material",
                                    color = Color(0xFF4a7ab5)
                                )
                            },
                            trailingIcon = {
                                if (tipo.isNotEmpty()) {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = descripcionExpanded)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF5b9bd5),
                                unfocusedBorderColor = Color(0xFF1e3a5f),
                                disabledTextColor = Color(0xFF4a7ab5),
                                disabledBorderColor = Color(0xFF1e3a5f)
                            ),
                            enabled = tipo.isNotEmpty()
                        )
                        if (materiales.isNotEmpty()) {
                            ExposedDropdownMenu(
                                expanded = descripcionExpanded,
                                onDismissRequest = { descripcionExpanded = false },
                                modifier = Modifier.background(Color(0xFF0f1e35))
                            ) {
                                materiales.forEach { material ->
                                    DropdownMenuItem(
                                        text = { Text(material, color = Color(0xFFe8f4ff)) },
                                        onClick = {
                                            descripcion = material
                                            descripcionExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Cantidad
                Text(
                    text = "Cantidad (kg) *",
                    color = Color(0xFF4a7ab5),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                OutlinedTextField(
                    value = cantidad,
                    onValueChange = { cantidad = it },
                    placeholder = { Text("0.0", color = Color(0xFF4a7ab5)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF5b9bd5),
                        unfocusedBorderColor = Color(0xFF1e3a5f)
                    ),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    ),
                    singleLine = true
                )

                // Proveedor
                Text(
                    text = "Proveedor",
                    color = Color(0xFF4a7ab5),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                OutlinedTextField(
                    value = proveedor,
                    onValueChange = { proveedor = it },
                    placeholder = { Text("Nombre del proveedor", color = Color(0xFF4a7ab5)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF5b9bd5),
                        unfocusedBorderColor = Color(0xFF1e3a5f)
                    ),
                    singleLine = true
                )

                // Lote
                Text(
                    text = "Lote",
                    color = Color(0xFF4a7ab5),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                OutlinedTextField(
                    value = lote,
                    onValueChange = { lote = it },
                    placeholder = { Text("Número de lote", color = Color(0xFF4a7ab5)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF5b9bd5),
                        unfocusedBorderColor = Color(0xFF1e3a5f)
                    ),
                    singleLine = true
                )

                // Observaciones
                Text(
                    text = "Observaciones",
                    color = Color(0xFF4a7ab5),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                OutlinedTextField(
                    value = observaciones,
                    onValueChange = { observaciones = it },
                    placeholder = { Text("Observaciones opcionales", color = Color(0xFF4a7ab5)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF5b9bd5),
                        unfocusedBorderColor = Color(0xFF1e3a5f)
                    ),
                    maxLines = 4
                )

                if (errorMessage.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF3a1a1a)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, null, tint = Color(0xFFe24b4a), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = errorMessage, color = Color(0xFFe24b4a), fontSize = 13.sp)
                        }
                    }
                }

                if (successMessage.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1a3a1a)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4a9a40), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = successMessage, color = Color(0xFF4a9a40), fontSize = 13.sp)
                        }
                    }
                }

                Button(
                    onClick = { registrarEntrada() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1a5a3a)),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Registrar Entrada", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}