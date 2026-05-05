package com.sergio.myapplication.data

data class Material(
    val nombre: String,
    val categoria: String,
    val stock: Double,
    val ajuste: Double = 0.0,
    val entradas: Double = 0.0,
    val salidas: Double = 0.0,
    val precio: Double? = null,
    val proveedor: String = "",
    val ubicacion: String = "",
    val observaciones: String = ""
)

data class Proveedor(
    val id: String,
    val nombre: String,
    val empresa: String = "",
    val pais: String = "",
    val web: String = "",
    val email: String = "",
    val telefono: String = "",
    val contacto: String = ""
)

data class Formulacion(
    val nombre: String,
    val ingredientes: List<Pair<String, Double>> = emptyList()
)