package com.sergio.myapplication.ui.screens

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QRScreen(navController: NavController) {

    var textoQR by remember { mutableStateOf("") }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var scanResult by remember { mutableStateOf("") }
    var tabSeleccionado by remember { mutableStateOf(0) } // 0 = Generar, 1 = Escanear

    // Launcher para escanear QR
    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            scanResult = result.contents
        }
    }

    fun generarQR(texto: String) {
        if (texto.isBlank()) return
        try {
            val hints = mapOf(EncodeHintType.MARGIN to 1)
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(texto, BarcodeFormat.QR_CODE, 512, 512, hints)
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
                        Text(
                            "Generar QR",
                            color = if (tabSeleccionado == 0) Color(0xFF5b9bd5) else Color(0xFF4a7ab5)
                        )
                    },
                    icon = {
                        Icon(
                            Icons.Default.QrCode,
                            null,
                            tint = if (tabSeleccionado == 0) Color(0xFF5b9bd5) else Color(0xFF4a7ab5)
                        )
                    }
                )
                Tab(
                    selected = tabSeleccionado == 1,
                    onClick = { tabSeleccionado = 1 },
                    text = {
                        Text(
                            "Escanear QR",
                            color = if (tabSeleccionado == 1) Color(0xFF5b9bd5) else Color(0xFF4a7ab5)
                        )
                    },
                    icon = {
                        Icon(
                            Icons.Default.QrCodeScanner,
                            null,
                            tint = if (tabSeleccionado == 1) Color(0xFF5b9bd5) else Color(0xFF4a7ab5)
                        )
                    }
                )
            }

            when (tabSeleccionado) {

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
                            text = "Introduce el nombre del material para generar su código QR",
                            color = Color(0xFF4a7ab5),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )

                        OutlinedTextField(
                            value = textoQR,
                            onValueChange = {
                                textoQR = it
                                qrBitmap = null
                            },
                            label = { Text("Nombre del material") },
                            placeholder = { Text("Ej: Harina de Pescado Super Prime", color = Color(0xFF4a7ab5)) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF5b9bd5),
                                unfocusedBorderColor = Color(0xFF1e3a5f),
                                focusedLabelColor = Color(0xFF5b9bd5),
                                unfocusedLabelColor = Color(0xFF4a7ab5)
                            ),
                            singleLine = true,
                            trailingIcon = {
                                if (textoQR.isNotEmpty()) {
                                    IconButton(onClick = {
                                        textoQR = ""
                                        qrBitmap = null
                                    }) {
                                        Icon(Icons.Default.Clear, null, tint = Color(0xFF4a7ab5))
                                    }
                                }
                            }
                        )

                        Button(
                            onClick = { generarQR(textoQR) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1a4a8a)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            enabled = textoQR.isNotBlank()
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
                                        text = textoQR,
                                        color = Color(0xFFe8f4ff),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))

                                    Box(
                                        modifier = Modifier
                                            .background(
                                                Color.White,
                                                RoundedCornerShape(12.dp)
                                            )
                                            .padding(16.dp)
                                    ) {
                                        Image(
                                            bitmap = bitmap.asImageBitmap(),
                                            contentDescription = "Código QR de $textoQR",
                                            modifier = Modifier.size(220.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Puedes hacer una captura de pantalla para guardar el QR",
                                        color = Color(0xFF4a7ab5),
                                        fontSize = 11.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

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
                            text = "Escanea el código QR de un material para ver su información",
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
                            modifier = Modifier
                                .size(160.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1a4a4a)
                            ),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.QrCodeScanner,
                                    null,
                                    tint = Color.White,
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Escanear",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        if (scanResult.isNotEmpty()) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0f1e35)),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            null,
                                            tint = Color(0xFF4a9a40),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Material identificado",
                                            color = Color(0xFF4a9a40),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    HorizontalDivider(color = Color(0xFF1e3a5f))
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = scanResult,
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
                                            onClick = { scanResult = "" },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.outlinedButtonColors(),
                                            border = androidx.compose.foundation.BorderStroke(
                                                0.5.dp, Color(0xFF1e3a5f)
                                            )
                                        ) {
                                            Text("Limpiar", color = Color(0xFF4a7ab5), fontSize = 13.sp)
                                        }
                                        Button(
                                            onClick = {
                                                navController.navigate("stock")
                                            },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF1a4a8a)
                                            )
                                        ) {
                                            Text("Ver stock", color = Color.White, fontSize = 13.sp)
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