package com.sergio.myapplication.ui.Navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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
import com.sergio.myapplication.ui.screens.SplashScreen
import com.sergio.myapplication.ui.screens.EstadisticasScreen


@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            SplashScreen(navController = navController)
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
        composable(
            route = "entradas?categoria={categoria}&material={material}",
            arguments = listOf(
                navArgument("categoria") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("material") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            EntradasScreen(
                navController = navController,
                categoriaInicial = backStackEntry.arguments?.getString("categoria") ?: "",
                materialInicial = backStackEntry.arguments?.getString("material") ?: ""
            )
        }
        composable(
            route = "salidas?categoria={categoria}&material={material}",
            arguments = listOf(
                navArgument("categoria") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("material") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            SalidasScreen(
                navController = navController,
                categoriaInicial = backStackEntry.arguments?.getString("categoria") ?: "",
                materialInicial = backStackEntry.arguments?.getString("material") ?: ""
            )
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
        composable("estadisticas") {
            EstadisticasScreen(navController = navController)
        }
    }
}