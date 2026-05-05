package com.sergio.myapplication.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.foundation.text.KeyboardOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.sergio.myapplication.ui.theme.outlinedTextFieldColors

@Composable
fun RegisterScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0a1628))
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "FishStock UAL",
                color = Color(0xFFe8f4ff),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Crear cuenta",
                color = Color(0xFF4a7ab5),
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0f1e35)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(0.5.dp, Color(0xFF1e3a5f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {

                    Text(
                        text = "Registro",
                        color = Color(0xFFe8f4ff),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Nombre completo
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Nombre completo") },
                        leadingIcon = { Icon(Icons.Default.Person, null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = outlinedTextFieldColors()
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Email
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Correo electrónico") },
                        leadingIcon = { Icon(Icons.Default.Email, null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = outlinedTextFieldColors()
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Username
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Nombre de usuario") },
                        leadingIcon = { Icon(Icons.Default.AlternateEmail, null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = outlinedTextFieldColors()
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Contraseña
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Contraseña") },
                        leadingIcon = { Icon(Icons.Default.Lock, null) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Default.Visibility
                                    else Icons.Default.VisibilityOff, null
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible)
                            VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = outlinedTextFieldColors()
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Confirmar contraseña
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirmar contraseña") },
                        leadingIcon = { Icon(Icons.Default.Lock, null) },
                        trailingIcon = {
                            IconButton(onClick = { confirmVisible = !confirmVisible }) {
                                Icon(
                                    if (confirmVisible) Icons.Default.Visibility
                                    else Icons.Default.VisibilityOff, null
                                )
                            }
                        },
                        visualTransformation = if (confirmVisible)
                            VisualTransformation.None else PasswordVisualTransformation(),
                        // Borde verde si coinciden
                        isError = confirmPassword.isNotEmpty() && confirmPassword != password,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = if (confirmPassword.isNotEmpty() && confirmPassword == password)
                            outlinedTextFieldColors(focusedBorderColor = Color(0xFF4a9a40))
                        else
                            outlinedTextFieldColors()
                    )

                    if (errorMessage.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage,
                            color = Color(0xFFe24b4a),
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            errorMessage = validateRegister(
                                fullName, email, username, password, confirmPassword
                            )
                            if (errorMessage.isNotEmpty()) return@Button

                            isLoading = true
                            registerUser(
                                db, auth, email, password, fullName, username,
                                onSuccess = {
                                    navController.navigate("main") {
                                        popUpTo("register") { inclusive = true }
                                    }
                                },
                                onError = { msg ->
                                    errorMessage = msg
                                    isLoading = false
                                }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1a5a3a)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Crear cuenta", color = Color(0xFFe8f4ff))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("¿Ya tienes cuenta? ", color = Color(0xFF4a7ab5), fontSize = 13.sp)
                        Text(
                            text = "Inicia sesión",
                            color = Color(0xFF5b9bd5),
                            fontSize = 13.sp,
                            textDecoration = TextDecoration.Underline,
                            modifier = Modifier.clickable { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}

// Validaciones separadas de la UI
private fun validateRegister(
    fullName: String, email: String, username: String,
    password: String, confirmPassword: String
): String {
    if (fullName.isEmpty() || email.isEmpty() || username.isEmpty()
        || password.isEmpty() || confirmPassword.isEmpty())
        return "Rellena todos los campos"
    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches())
        return "El correo no es válido"
    if (password.length < 6)
        return "La contraseña debe tener al menos 6 caracteres"
    if (password != confirmPassword)
        return "Las contraseñas no coinciden"
    return ""
}

private fun registerUser(
    db: FirebaseFirestore, auth: FirebaseAuth,
    email: String, password: String,
    fullName: String, username: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    db.collection("users").whereEqualTo("username", username).get()
        .addOnSuccessListener { docs ->
            if (!docs.isEmpty) {
                onError("Ese nombre de usuario ya existe")
                return@addOnSuccessListener
            }
            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    val uid = result.user?.uid ?: return@addOnSuccessListener
                    val userData = hashMapOf(
                        "uid" to uid,
                        "fullName" to fullName,
                        "username" to username,
                        "email" to email,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                    db.collection("users").document(uid).set(userData)
                        .addOnSuccessListener { onSuccess() }
                        .addOnFailureListener { onError("Error al guardar datos") }
                }
                .addOnFailureListener { e ->
                    onError(e.message ?: "Error al crear cuenta")
                }
        }
        .addOnFailureListener { onError("Error de conexión") }
}