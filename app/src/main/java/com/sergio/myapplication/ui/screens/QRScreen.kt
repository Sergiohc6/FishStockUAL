package com.sergio.myapplication.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.firebase.auth.FirebaseAuth
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.sergio.myapplication.data.GoogleSheetsService
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QRScreen(navController: NavController) {
    val context = LocalContext.current
    val sheetsService = remember { GoogleSheetsService(context) }
    val scope = rememberCoroutineScope()

    var categoriaSeleccionada by remember { mutableStateOf("") }
    var materialSeleccionado by remember { mutableStateOf("") }
    var materiales by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoadingMateriales by remember { mutableStateOf(false) }
    var categoriaExpanded by remember { mutableStateOf(false) }
    var materialExpanded by remember { mutableStateOf(false) }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var scanResult by remember { mutableStateOf("") }
    var scanCategoria by remember { mutableStateOf("") }
    var scanMaterial by remember { mutableStateOf("") }
    var tabSeleccionado by remember { mutableStateOf(0) }
    var pendingAuthIntent by remember { mutableStateOf<Intent?>(null) }

    val tiposCategoria = listOf("HARINAS", "ACEITES", "ADITIVOS", "MACROALGAS", "MICROALGAS", "OTROS")

    val authLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { pendingAuthIntent = null }

    LaunchedEffect(pendingAuthIntent) {
        pendingAuthIntent?.let { authLauncher.launch(it) }
    }

    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            scanResult = result.contents
            // Parsear el formato CATEGORIA|NombreMaterial
            if (result.contents.contains("|")) {
                val parts = result.contents.split("|")
                scanCategoria = parts[0].trim()
                scanMaterial = parts[1].trim()
            } else {
                scanCategoria = ""
                scanMaterial = result.contents
            }
        }
    }

    fun cargarMateriales(categoria: String) {
        scope.launch {
            isLoadingMateriales = true
            materialSeleccionado = ""
            qrBitmap = null
            try {
                val accountName = FirebaseAuth.getInstance().currentUser?.email ?: ""
                val rows = sheetsService.readSheet(accountName, categoria)
                materiales = rows.drop(4).mapNotNull { row ->
                    val nombre = row.getOrNull(0)?.toString() ?: return@mapNotNull null
                    if (nombre.isBlank()) return@mapNotNull null
                    nombre
                }
            } catch (e: UserRecoverableAuthIOException) {
                pendingAuthIntent = e.intent
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoadingMateriales = false
            }
        }
    }

    fun generarQR() {
        if (categoriaSeleccionada.isBlank() || materialSeleccionado.isBlank()) return
        try {
            // Formato: CATEGORIA|NombreMaterial
            val contenido = "$categoriaSeleccionada|$materialSeleccionado"
            val hints = mapOf(EncodeHintType.MARGIN to 1)
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(contenido, BarcodeFormat.QR_CODE, 512, 512, hints)
            val bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.RGB_565)
            for (x in 0 until 512) {
                for (y in 0 until 512) {
                    bitmap.setPixel(
                        x, y,
                        if (bitMatrix[x, y]) AndroidColor.BLACK else AndroidColor.WHITE
                    )
                }
            }
            qrBitmap = bitmap
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
                        text = "Códigos QR",
                        color = Color(0xFFe8f4ff),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.size(48.dp))
                }
            }

            TabRow(
                selectedTabIndex = tabSeleccionado,
                containerColor = Color(0xFF0f1e35),
                contentColor = Color(0xFF5b9bd5)
            ) {
                Tab(
                    selected = tabSeleccionado == 0,
                    onClick = { tabSeleccionado = 0 },
                    text = {
                        Text("Generar QR", color = if (tabSeleccionado == 0) Color(0xFF5b9bd5) else Color(0xFF4a7ab5))
                    },
                    icon = {
                        Icon(Icons.Default.QrCode, null, tint = if (tabSeleccionado == 0) Color(0xFF5b9bd5) else Color(0xFF4a7ab5))
                    }
                )
                Tab(
                    selected = tabSeleccionado == 1,
                    onClick = { tabSeleccionado = 1 },
                    text = {
                        Text("Escanear QR", color = if (tabSeleccionado == 1) Color(0xFF5b9bd5) else Color(0xFF4a7ab5))
                    },
                    icon = {
                        Icon(Icons.Default.QrCodeScanner, null, tint = if (tabSeleccionado == 1) Color(0xFF5b9bd5) else Color(0xFF4a7ab5))
                    }
                )
            }

            when (tabSeleccionado) {

                // TAB GENERAR QR
                0 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Selecciona la categoría y el material para generar su QR",
                            color = Color(0xFF4a7ab5),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )

                        // Selector categoría
                        ExposedDropdownMenuBox(
                            expanded = categoriaExpanded,
                            onExpandedChange = { categoriaExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = categoriaSeleccionada,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Categoría") },
                                placeholder = { Text("Selecciona categoría", color = Color(0xFF4a7ab5)) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoriaExpanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF5b9bd5),
                                    unfocusedBorderColor = Color(0xFF1e3a5f),
                                    focusedLabelColor = Color(0xFF5b9bd5),
                                    unfocusedLabelColor = Color(0xFF4a7ab5)
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = categoriaExpanded,
                                onDismissRequest = { categoriaExpanded = false },
                                modifier = Modifier.background(Color(0xFF0f1e35))
                            ) {
                                tiposCategoria.forEach { categoria ->
                                    DropdownMenuItem(
                                        text = { Text(categoria, color = Color(0xFFe8f4ff)) },
                                        onClick = {
                                            categoriaSeleccionada = categoria
                                            categoriaExpanded = false
                                            cargarMateriales(categoria)
                                        }
                                    )
                                }
                            }
                        }

                        // Selector material
                        if (isLoadingMateriales) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(color = Color(0xFF5b9bd5), modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Cargando materiales...", color = Color(0xFF4a7ab5), fontSize = 12.sp)
                            }
                        } else {
                            ExposedDropdownMenuBox(
                                expanded = materialExpanded,
                                onExpandedChange = { if (categoriaSeleccionada.isNotEmpty()) materialExpanded = it }
                            ) {
                                OutlinedTextField(
                                    value = materialSeleccionado,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Material") },
                                    placeholder = {
                                        Text(
                                            if (categoriaSeleccionada.isEmpty()) "Selecciona primero una categoría"
                                            else "Selecciona material",
                                            color = Color(0xFF4a7ab5)
                                        )
                                    },
                                    trailingIcon = {
                                        if (categoriaSeleccionada.isNotEmpty())
                                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = materialExpanded)
                                    },
                                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFF5b9bd5),
                                        unfocusedBorderColor = Color(0xFF1e3a5f),
                                        focusedLabelColor = Color(0xFF5b9bd5),
                                        unfocusedLabelColor = Color(0xFF4a7ab5),
                                        disabledTextColor = Color(0xFF4a7ab5),
                                        disabledBorderColor = Color(0xFF1e3a5f)
                                    ),
                                    enabled = categoriaSeleccionada.isNotEmpty()
                                )
                                if (materiales.isNotEmpty()) {
                                    ExposedDropdownMenu(
                                        expanded = materialExpanded,
                                        onDismissRequest = { materialExpanded = false },
                                        modifier = Modifier.background(Color(0xFF0f1e35))
                                    ) {
                                        materiales.forEach { material ->
                                            DropdownMenuItem(
                                                text = { Text(material, color = Color(0xFFe8f4ff)) },
                                                onClick = {
                                                    materialSeleccionado = material
                                                    materialExpanded = false
                                                    qrBitmap = null
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = { generarQR() },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1a4a8a)),
                            shape = RoundedCornerShape(12.dp),
                            enabled = categoriaSeleccionada.isNotBlank() && materialSeleccionado.isNotBlank()
                        ) {
                            Icon(Icons.Default.QrCode, null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generar QR", color = Color.White, fontSize = 15.sp)
                        }

                        qrBitmap?.let { bitmap ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0f1e35)),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = materialSeleccionado,
                                        color = Color(0xFFe8f4ff),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = categoriaSeleccionada,
                                        color = Color(0xFF4a7ab5),
                                        fontSize = 12.sp
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(Color.White, RoundedCornerShape(12.dp))
                                            .padding(16.dp)
                                    ) {
                                        Image(
                                            bitmap = bitmap.asImageBitmap(),
                                            contentDescription = "QR de $materialSeleccionado",
                                            modifier = Modifier.size(220.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Haz una captura para guardar el QR e imprimirlo",
                                        color = Color(0xFF4a7ab5),
                                        fontSize = 11.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                // TAB ESCANEAR QR
                1 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Escanea el QR de un material para ver su información",
                            color = Color(0xFF4a7ab5),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                val options = ScanOptions().apply {
                                    setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                                    setPrompt("Apunta al código QR del material")
                                    setBeepEnabled(true)
                                    setOrientationLocked(false)
                                }
                                scanLauncher.launch(options)
                            },
                            modifier = Modifier.size(160.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1a4a4a)),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.QrCodeScanner, null, tint = Color.White, modifier = Modifier.size(64.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Escanear", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            }
                        }

                        if (scanResult.isNotEmpty()) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0f1e35)),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4a9a40), modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Material identificado", color = Color(0xFF4a9a40), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    HorizontalDivider(color = Color(0xFF1e3a5f))
                                    Spacer(modifier = Modifier.height(12.dp))

                                    if (scanCategoria.isNotEmpty()) {
                                        Text(text = scanCategoria, color = Color(0xFF4a7ab5), fontSize = 12.sp)
                                    }
                                    Text(
                                        text = scanMaterial.ifEmpty { scanResult },
                                        color = Color(0xFFe8f4ff),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                scanResult = ""
                                                scanCategoria = ""
                                                scanMaterial = ""
                                            },
                                            modifier = Modifier.weight(1f),
                                            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF1e3a5f))
                                        ) {
                                            Text("Limpiar", color = Color(0xFF4a7ab5), fontSize = 13.sp)
                                        }
                                        Button(
                                            onClick = {
                                                // Navegar a entradas con los datos del QR
                                                navController.navigate("entradas?categoria=$scanCategoria&material=$scanMaterial")
                                            },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1a5a3a))
                                        ) {
                                            Text("Entrada", color = Color.White, fontSize = 13.sp)
                                        }
                                        Button(
                                            onClick = {
                                                // Navegar a salidas con los datos del QR
                                                navController.navigate("salidas?categoria=$scanCategoria&material=$scanMaterial")
                                            },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5a2a1a))
                                        ) {
                                            Text("Salida", color = Color.White, fontSize = 13.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}