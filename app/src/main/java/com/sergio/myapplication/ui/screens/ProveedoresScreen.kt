package com.sergio.myapplication.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.firebase.auth.FirebaseAuth
import com.sergio.myapplication.data.GoogleSheetsService
import com.sergio.myapplication.data.Proveedor
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProveedoresScreen(navController: NavController) {
    val context = LocalContext.current
    val sheetsService = remember { GoogleSheetsService(context) }
    val scope = rememberCoroutineScope()

    var proveedores by remember { mutableStateOf<List<Proveedor>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var pendingAuthIntent by remember { mutableStateOf<android.content.Intent?>(null) }

    val authLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        pendingAuthIntent = null
    }

    LaunchedEffect(pendingAuthIntent) {
        pendingAuthIntent?.let { authLauncher.launch(it) }
    }

    fun cargarProveedores() {
        scope.launch {
            isLoading = true
            errorMessage = ""
            try {
                val accountName = FirebaseAuth.getInstance().currentUser?.email ?: ""
                val rows = sheetsService.readSheet(accountName, "PROVEEDORES")
                proveedores = rows.drop(2).mapNotNull { row ->
                    val nombre = row.getOrNull(1)?.toString() ?: return@mapNotNull null
                    if (nombre.isBlank()) return@mapNotNull null
                    Proveedor(
                        id = row.getOrNull(0)?.toString() ?: "",
                        nombre = nombre,
                        empresa = row.getOrNull(2)?.toString() ?: "",
                        pais = row.getOrNull(5)?.toString() ?: "",
                        web = row.getOrNull(6)?.toString() ?: "",
                        email = row.getOrNull(7)?.toString() ?: "",
                        telefono = row.getOrNull(8)?.toString() ?: "",
                        contacto = row.getOrNull(9)?.toString() ?: ""
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

    LaunchedEffect(Unit) {
        cargarProveedores()
    }

    val proveedoresFiltrados = proveedores.filter {
        it.nombre.contains(searchQuery, ignoreCase = true) ||
                it.empresa.contains(searchQuery, ignoreCase = true)
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
                        text = "Proveedores",
                        color = Color(0xFFe8f4ff),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { cargarProveedores() }) {
                        Icon(Icons.Default.Refresh, null, tint = Color(0xFF5b9bd5))
                    }
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar proveedor...", color = Color(0xFF4a7ab5)) },
                leadingIcon = {
                    Icon(Icons.Default.Search, null, tint = Color(0xFF5b9bd5))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF5b9bd5),
                    unfocusedBorderColor = Color(0xFF1e3a5f)
                ),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

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
                                text = "Cargando proveedores...",
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
                            Icon(
                                Icons.Default.Warning,
                                null,
                                tint = Color(0xFFe24b4a),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = errorMessage,
                                color = Color(0xFFe24b4a),
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { cargarProveedores() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1a4a8a)
                                )
                            ) {
                                Text("Reintentar", color = Color(0xFFe8f4ff))
                            }
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Text(
                                text = "${proveedoresFiltrados.size} proveedores",
                                color = Color(0xFF4a7ab5),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }

                        items(proveedoresFiltrados) { proveedor ->
                            ProveedorCard(proveedor = proveedor)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProveedorCard(proveedor: Proveedor) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }

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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = proveedor.nombre,
                        color = Color(0xFFe8f4ff),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    if (proveedor.empresa.isNotBlank()) {
                        Text(
                            text = proveedor.empresa,
                            color = Color(0xFF4a7ab5),
                            fontSize = 12.sp
                        )
                    }
                }
                if (proveedor.pais.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF1a4a8a), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = proveedor.pais,
                            color = Color(0xFFe8f4ff),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFF1e3a5f))
                Spacer(modifier = Modifier.height(12.dp))

                if (proveedor.contacto.isNotBlank()) {
                    DetalleRowProveedor("Contacto", proveedor.contacto)
                }

                if (proveedor.telefono.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tel: ${proveedor.telefono}",
                            color = Color(0xFF8ab8e8),
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                val intent = Intent(
                                    Intent.ACTION_DIAL,
                                    Uri.parse("tel:${proveedor.telefono}")
                                )
                                context.startActivity(intent)
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Phone, null,
                                tint = Color(0xFF5b9bd5),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                if (proveedor.email.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = proveedor.email,
                            color = Color(0xFF8ab8e8),
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                val intent = Intent(
                                    Intent.ACTION_SENDTO,
                                    Uri.parse("mailto:${proveedor.email}")
                                )
                                context.startActivity(intent)
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Email, null,
                                tint = Color(0xFF5b9bd5),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                if (proveedor.web.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = proveedor.web,
                            color = Color(0xFF8ab8e8),
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                val url = if (proveedor.web.startsWith("http"))
                                    proveedor.web else "https://${proveedor.web}"
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Language, null,
                                tint = Color(0xFF5b9bd5),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetalleRowProveedor(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Text(
            text = "$label: ",
            color = Color(0xFF4a7ab5),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            color = Color(0xFF8ab8e8),
            fontSize = 12.sp
        )
    }
}