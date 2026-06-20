package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.EarnViewModel
import com.example.ui.LoginScreen
import com.example.ui.MainAppContainer
import com.example.ui.RegisterScreen
import com.example.ui.Routes
import com.example.ui.SplashScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    
    // Instantiate EarnViewModel
    private val viewModel: EarnViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Supports notch and edge-to-edge transparent drawing
        enableEdgeToEdge()
        
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val navController = rememberNavController()
                    
                    NavHost(
                        navController = navController,
                        startDestination = Routes.SPLASH
                    ) {
                        // 1. Splash Screen Routing
                        composable(route = Routes.SPLASH) {
                            SplashScreen(
                                viewModel = viewModel,
                                onNavigate = { destination ->
                                    navController.navigate(destination) {
                                        // Pop Splash off backstack to avoid returning to it on back press
                                        popUpTo(Routes.SPLASH) { inclusive = true }
                                    }
                                }
                            )
                        }
                        
                        // 2. Login Screen Routing
                        composable(route = Routes.LOGIN) {
                            LoginScreen(
                                viewModel = viewModel,
                                onNavigate = { destination ->
                                    navController.navigate(destination)
                                }
                            )
                        }
                        
                        // 3. Register Screen Routing
                        composable(route = Routes.REGISTER) {
                            RegisterScreen(
                                viewModel = viewModel,
                                onNavigate = { destination ->
                                    navController.navigate(destination) {
                                        popUpTo(Routes.LOGIN) { inclusive = false }
                                    }
                                }
                            )
                        }
                        
                        // 4. Main App Container Dashboard Routing (holds home tab, earn activities tab, refer, withdraw, etc.)
                        composable(route = Routes.MAIN_CONTAINER) {
                            MainAppContainer(
                                viewModel = viewModel,
                                onLogout = {
                                    navController.navigate(Routes.LOGIN) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
