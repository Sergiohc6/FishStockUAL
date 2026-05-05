package com.sergio.myapplication.data

import android.content.Context
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.api.services.sheets.v4.model.ValueRange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GoogleSheetsService(private val context: Context) {

    companion object {
        const val SPREADSHEET_ID = "1l2cfFSg4EoGgK3v7uyy4zgbtkh2IUsw0IayG8LN6qpU"
        val SCOPES = listOf(SheetsScopes.SPREADSHEETS)
    }

    fun getCredential(accountName: String): GoogleAccountCredential {
        return GoogleAccountCredential
            .usingOAuth2(context, SCOPES)
            .apply { selectedAccountName = accountName }
    }

    fun getService(accountName: String): Sheets {
        return Sheets.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            getCredential(accountName)
        )
            .setApplicationName("FishStockUAL")
            .build()
    }

    suspend fun readSheet(accountName: String, sheetName: String): List<List<Any>> =
        withContext(Dispatchers.IO) {
            val service = getService(accountName)
            val response = service.spreadsheets().values()
                .get(SPREADSHEET_ID, sheetName)
                .execute()
            response.getValues() ?: emptyList()
        }

    suspend fun readMateriales(
        accountName: String,
        sheetName: String
    ): List<Material> = withContext(Dispatchers.IO) {
        val rows = readSheet(accountName, sheetName)
        rows.drop(3).mapNotNull { row ->
            if (row.size >= 5) {
                val nombre = row.getOrNull(0)?.toString() ?: return@mapNotNull null
                if (nombre.isBlank()) return@mapNotNull null
                val stock = row.getOrNull(4)?.toString()?.toDoubleOrNull() ?: 0.0
                val ajuste = row.getOrNull(1)?.toString()?.toDoubleOrNull() ?: 0.0
                val entradas = row.getOrNull(2)?.toString()?.toDoubleOrNull() ?: 0.0
                val salidas = row.getOrNull(3)?.toString()?.toDoubleOrNull() ?: 0.0
                val precio = row.getOrNull(11)?.toString()?.toDoubleOrNull()
                val proveedor = row.getOrNull(12)?.toString() ?: ""
                val ubicacion = row.getOrNull(13)?.toString() ?: ""
                val observaciones = row.getOrNull(14)?.toString() ?: ""
                Material(
                    nombre = nombre,
                    categoria = sheetName,
                    stock = stock,
                    ajuste = ajuste,
                    entradas = entradas,
                    salidas = salidas,
                    precio = precio,
                    proveedor = proveedor,
                    ubicacion = ubicacion,
                    observaciones = observaciones
                )
            } else null
        }
    }

    suspend fun addEntrada(
        accountName: String,
        fecha: String,
        tipo: String,
        descripcion: String,
        cantidad: Double,
        proveedor: String,
        lote: String,
        observaciones: String
    ) = withContext(Dispatchers.IO) {
        val service = getService(accountName)
        val nuevaFila = listOf(listOf(
            fecha, tipo, descripcion, cantidad,
            proveedor, lote, observaciones, accountName
        ))
        val body = ValueRange().setValues(nuevaFila)
        service.spreadsheets().values()
            .append(SPREADSHEET_ID, "ENTRADAS!A:H", body)
            .setValueInputOption("USER_ENTERED")
            .setInsertDataOption("INSERT_ROWS")
            .execute()
    }

    suspend fun addSalida(
        accountName: String,
        fecha: String,
        tipo: String,
        descripcion: String,
        cantidad: Double,
        proyecto: String,
        observaciones: String
    ) = withContext(Dispatchers.IO) {
        val service = getService(accountName)
        val nuevaFila = listOf(listOf(
            fecha, tipo, descripcion, cantidad,
            proyecto, observaciones, accountName
        ))
        val body = ValueRange().setValues(nuevaFila)
        service.spreadsheets().values()
            .append(SPREADSHEET_ID, "SALIDAS!A:G", body)
            .setValueInputOption("USER_ENTERED")
            .setInsertDataOption("INSERT_ROWS")
            .execute()
    }

    suspend fun calcularStockReal(
        accountName: String,
        categoria: String
    ): Map<String, Double> = withContext(Dispatchers.IO) {

        val service = getService(accountName)

        val rowsCategoria = service.spreadsheets().values()
            .get(SPREADSHEET_ID, categoria)
            .execute()
            .getValues() ?: emptyList()

        val ajustes = mutableMapOf<String, Double>()
        rowsCategoria.drop(4).forEach { row ->
            val nombre = row.getOrNull(0)?.toString() ?: return@forEach
            if (nombre.isBlank()) return@forEach
            ajustes[nombre] = row.getOrNull(1)?.toString()
                ?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
        }

        val rowsEntradas = service.spreadsheets().values()
            .get(SPREADSHEET_ID, "ENTRADAS")
            .execute()
            .getValues() ?: emptyList()

        val totalEntradas = mutableMapOf<String, Double>()
        rowsEntradas.drop(2).forEach { row ->
            val descripcion = row.getOrNull(2)?.toString() ?: return@forEach
            if (descripcion.isBlank()) return@forEach
            val cantidad = row.getOrNull(3)?.toString()
                ?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
            totalEntradas[descripcion] = (totalEntradas[descripcion] ?: 0.0) + cantidad
        }

        val rowsSalidas = service.spreadsheets().values()
            .get(SPREADSHEET_ID, "SALIDAS")
            .execute()
            .getValues() ?: emptyList()

        val totalSalidas = mutableMapOf<String, Double>()
        rowsSalidas.drop(2).forEach { row ->
            val descripcion = row.getOrNull(2)?.toString() ?: return@forEach
            if (descripcion.isBlank()) return@forEach
            val cantidad = row.getOrNull(3)?.toString()
                ?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
            totalSalidas[descripcion] = (totalSalidas[descripcion] ?: 0.0) + cantidad
        }

        val stockReal = mutableMapOf<String, Double>()
        ajustes.forEach { (nombre, ajuste) ->
            val entradas = totalEntradas[nombre] ?: 0.0
            val salidas = totalSalidas[nombre] ?: 0.0
            stockReal[nombre] = ajuste + entradas - salidas
        }

        stockReal
    }

    suspend fun readProveedores(accountName: String): List<Proveedor> =
        withContext(Dispatchers.IO) {
            val rows = readSheet(accountName, "PROVEEDORES")
            rows.drop(2).mapNotNull { row ->
                if (row.size >= 2) {
                    Proveedor(
                        id = row.getOrNull(0)?.toString() ?: "",
                        nombre = row.getOrNull(1)?.toString() ?: "",
                        empresa = row.getOrNull(2)?.toString() ?: "",
                        pais = row.getOrNull(5)?.toString() ?: "",
                        web = row.getOrNull(6)?.toString() ?: "",
                        email = row.getOrNull(7)?.toString() ?: "",
                        telefono = row.getOrNull(8)?.toString() ?: "",
                        contacto = row.getOrNull(9)?.toString() ?: ""
                    )
                } else null
            }
        }
}