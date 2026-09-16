package com.example

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val sharedPrefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
            var themeMode by remember { mutableStateOf(sharedPrefs.getString("theme_mode", "light") ?: "light") }
            
            val currentColors = when (themeMode) {
                "dark" -> DarkNeuColors
                "sepia" -> SepiaNeuColors
                else -> LightNeuColors
            }

            MyApplicationTheme {
                CompositionLocalProvider(LocalNeuColors provides currentColors) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        val navController = rememberNavController()
                        NavHost(navController = navController, startDestination = "home") {
                            composable("home") {
                                HomeView(
                                    onNavigateToReader = { fileName ->
                                        navController.navigate("reader/${Uri.encode(fileName)}")
                                    }
                                )
                            }
                            composable(
                                route = "reader/{fileName}",
                                arguments = listOf(navArgument("fileName") { type = NavType.StringType })
                            ) { backStackEntry ->
                                val fileName = backStackEntry.arguments?.getString("fileName") ?: return@composable
                                ReaderView(
                                    fileName = Uri.decode(fileName),
                                    onBack = { navController.popBackStack() },
                                    currentTheme = themeMode,
                                    onThemeChange = { newTheme ->
                                        themeMode = newTheme
                                        sharedPrefs.edit().putString("theme_mode", newTheme).apply()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
