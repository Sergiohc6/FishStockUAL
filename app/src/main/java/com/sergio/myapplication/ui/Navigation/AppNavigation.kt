package com.sergio.myapplication.ui.Navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sergio.myapplication.ui.screens.EntradasScreen
import com.sergio.myapplication.ui.screens.HistorialScreen
import com.sergio.myapplication.ui.screens.LoginScreen
import com.sergio.myapplication.ui.screens.MainScreen
import com.sergio.myapplication.ui.screens.PerfilScreen
import com.sergio.myapplication.ui.screens.ProveedoresScreen
import com.sergio.myapplication.ui.screens.QRScreen
import com.sergio.myapplication.ui.screens.RegisterScreen
import com.sergio.myapplication.ui.screens.SalidasScreen
import com.sergio.myapplication.ui.screens.StockScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(navController = navController)
        }
        composable("register") {
            RegisterScreen(navController = navController)
        }
        composable("main") {
            MainScreen(navController = navController)
        }
        composable("stock") {
            StockScreen(navController = navController)
        }
        composable("proveedores") {
            ProveedoresScreen(navController = navController)
        }
        composable("entradas") {
            EntradasScreen(navController = navController)
        }
        composable("salidas") {
            SalidasScreen(navController = navController)
        }
        composable("qr") {
            QRScreen(navController = navController)
        }
        composable("perfil") {
            PerfilScreen(navController = navController)
        }
        composable("historial") {
            HistorialScreen(navController = navController)
        }
    }
}