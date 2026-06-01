package com.sergio.myapplication.ui.screens

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MainScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    var username by remember { mutableStateOf("") }
    var showLogoutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val uid = auth.currentUser?.uid ?: return@LaunchedEffect
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                username = doc.getString("username") ?: ""
            }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor = Color(0xFF0f1e35),
            title = {
                Text("Cerrar sesión", color = Color(0xFFe8f4ff), fontWeight = FontWeight.Medium)
            },
            text = {
                Text("¿Seguro que quieres cerrar sesión?", color = Color(0xFF4a7ab5))
            },
            confirmButton = {
                TextButton(onClick = {
                    auth.signOut()
                    navController.navigate("login") {
                        popUpTo("main") { inclusive = true }
                    }
                }) {
                    Text("Salir", color = Color(0xFFe24b4a))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancelar", color = Color(0xFF5b9bd5))
                }
            }
        )
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
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Piensos Experimentales",
                            color = Color(0xFFe8f4ff),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (username.isNotEmpty()) {
                            Text(
                                text = "Hola, $username",
                                color = Color(0xFF4a7ab5),
                                fontSize = 12.sp
                            )
                        }
                        Text(
                            text = SimpleDateFormat("EEEE, d MMM yyyy", Locale("es"))
                                .format(Date()),
                            color = Color(0xFF2d5080),
                            fontSize = 11.sp
                        )
                    }
                    Row {
                        IconButton(onClick = { navController.navigate("perfil") }) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "Perfil",
                                tint = Color(0xFF4a7ab5)
                            )
                        }
                        IconButton(onClick = { showLogoutDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Logout,
                                contentDescription = "Cerrar sesión",
                                tint = Color(0xFF4a7ab5)
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Panel principal",
                    color = Color(0xFFe8f4ff),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )

                // Fila 1
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    MenuCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Inventory,
                        title = "Stock",
                        subtitle = "Ver materiales",
                        color = Color(0xFF1a4a8a),
                        onClick = { navController.navigate("stock") }
                    )
                    MenuCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.MoveToInbox,
                        title = "Entradas",
                        subtitle = "Registrar entrada",
                        color = Color(0xFF1a5a3a),
                        onClick = { navController.navigate("entradas") }
                    )
                }

                // Fila 2
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    MenuCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Output,
                        title = "Salidas",
                        subtitle = "Registrar salida",
                        color = Color(0xFF5a2a1a),
                        onClick = { navController.navigate("salidas") }
                    )
                    MenuCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.QrCode,
                        title = "Códigos QR",
                        subtitle = "Escanear / Generar",
                        color = Color(0xFF1a4a4a),
                        onClick = { navController.navigate("qr") }
                    )
                }

                // Fila 3
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    MenuCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.People,
                        title = "Proveedores",
                        subtitle = "Ver proveedores",
                        color = Color(0xFF4a3a1a),
                        onClick = { navController.navigate("proveedores") }
                    )
                    MenuCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.History,
                        title = "Historial",
                        subtitle = "Entradas y salidas",
                        color = Color(0xFF1a3a5a),
                        onClick = { navController.navigate("historial") }
                    )
                }

                // Fila 4
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    MenuCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.BarChart,
                        title = "Estadísticas",
                        subtitle = "Ver gráficas",
                        color = Color(0xFF1a3a5a),
                        onClick = { navController.navigate("estadisticas") }
                    )
                    MenuCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.AccountCircle,
                        title = "Mi perfil",
                        subtitle = "Ver perfil",
                        color = Color(0xFF2a1a4a),
                        onClick = { navController.navigate("perfil") }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun MenuCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color),
        shape = RoundedCornerShape(16.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFFe8f4ff),
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = title,
                color = Color(0xFFe8f4ff),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                color = Color(0xFFb0c8e8),
                fontSize = 11.sp
            )
        }
    }
}