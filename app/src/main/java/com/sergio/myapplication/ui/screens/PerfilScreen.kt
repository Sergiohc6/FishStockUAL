package com.sergio.myapplication.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun PerfilScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val user = auth.currentUser

    var fullName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var showLogoutDialog by remember { mutableStateOf(false) }

    val photoUrl = user?.photoUrl

    LaunchedEffect(Unit) {
        val uid = user?.uid ?: return@LaunchedEffect
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                fullName = doc.getString("fullName") ?: ""
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
                        text = "Mi perfil",
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
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                Spacer(modifier = Modifier.height(16.dp))

                // Avatar con foto de Google o iniciales
                if (photoUrl != null) {
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = "Foto de perfil",
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1a4a8a)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (fullName.isNotEmpty())
                                fullName.split(" ")
                                    .take(2)
                                    .map { it.firstOrNull()?.uppercaseChar() ?: "" }
                                    .joinToString("")
                            else
                                user?.email?.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                            color = Color(0xFFe8f4ff),
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = fullName.ifEmpty { user?.displayName ?: "Usuario" },
                    color = Color(0xFFe8f4ff),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                if (username.isNotEmpty()) {
                    Text(
                        text = "@$username",
                        color = Color(0xFF4a7ab5),
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0f1e35)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {

                        Text(
                            text = "Información de cuenta",
                            color = Color(0xFFe8f4ff),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Email, null, tint = Color(0xFF5b9bd5), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = "Correo electrónico", color = Color(0xFF4a7ab5), fontSize = 11.sp)
                                Text(text = user?.email ?: "", color = Color(0xFFe8f4ff), fontSize = 14.sp)
                            }
                        }

                        HorizontalDivider(color = Color(0xFF1e3a5f))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Person, null, tint = Color(0xFF5b9bd5), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = "Nombre completo", color = Color(0xFF4a7ab5), fontSize = 11.sp)
                                Text(
                                    text = fullName.ifEmpty { "No especificado" },
                                    color = Color(0xFFe8f4ff),
                                    fontSize = 14.sp
                                )
                            }
                        }

                        HorizontalDivider(color = Color(0xFF1e3a5f))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Shield, null, tint = Color(0xFF5b9bd5), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = "Método de acceso", color = Color(0xFF4a7ab5), fontSize = 11.sp)
                                Text(
                                    text = if (user?.providerData?.any { it.providerId == "google.com" } == true)
                                        "Google" else "Email y contraseña",
                                    color = Color(0xFFe8f4ff),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = { showLogoutDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3a1a1a)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Logout, null, tint = Color(0xFFe24b4a), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cerrar sesión", color = Color(0xFFe24b4a), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}