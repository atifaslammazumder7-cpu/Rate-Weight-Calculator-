package com.example

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AppDatabase
import com.example.data.CalculationRepository
import com.example.ui.CalculatorScreen
import com.example.ui.CalculatorViewModel
import com.example.ui.HistoryScreen
import com.example.ui.SettingsScreen
import com.example.ui.ViewModelFactory
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Local SQLite Room database & Repository
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = CalculationRepository(database.calculationDao())

        setContent {
            // Instantiate ViewModel with factory to pass repository dependency
            val viewModel: CalculatorViewModel by viewModels {
                ViewModelFactory(application, repository)
            }

            val language by viewModel.languagePreference.collectAsStateWithLifecycle()
            val darkMode by viewModel.darkModePreference.collectAsStateWithLifecycle()

            // Resolve modern application theme preference (System, Dark, Light)
            val isDarkTheme = when (darkMode) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }

            // LocalizedApp handles intercepting of the context configuration for strings
            LocalizedApp(language = language) {
                MyApplicationTheme(darkTheme = isDarkTheme) {
                    
                    // Root Scaffold with window insets support
                    Scaffold(
                        modifier = Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(WindowInsets.safeDrawing),
                        bottomBar = {
                            BottomNavBar(viewModel = viewModel)
                        }
                    ) { innerPadding ->
                        
                        // Observe state parameters
                        val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
                        val context = LocalContext.current

                        // Handle reactive single-event Toast notifications
                        LaunchedEffect(key1 = viewModel) {
                            viewModel.toastEvent.collectLatest { msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            // Load screens with animations
                            when (currentTab) {
                                "calculator" -> {
                                    CalculatorScreen(
                                        viewModel = viewModel,
                                        onShareClick = { text -> shareText(context, text) }
                                    )
                                }
                                "history" -> {
                                    HistoryScreen(
                                        viewModel = viewModel,
                                        onShareClick = { text -> shareText(context, text) }
                                    )
                                }
                                "settings" -> {
                                    SettingsScreen(
                                        viewModel = viewModel
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modern Intent-based Text sharing utility
    private fun shareText(context: Context, text: String) {
        try {
            val intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, text)
                type = "text/plain"
            }
            context.startActivity(Intent.createChooser(intent, "Share Rate & Weight Calculation"))
        } catch (e: Exception) {
            Toast.makeText(context, "Sharing failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun LocalizedApp(
    language: String,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val localizedContext = remember(language) {
        val locale = java.util.Locale(language)
        java.util.Locale.setDefault(locale)
        val configuration = android.content.res.Configuration(context.resources.configuration)
        configuration.setLocale(locale)
        context.createConfigurationContext(configuration)
    }

    CompositionLocalProvider(LocalContext provides localizedContext) {
        content()
    }
}

@Composable
fun BottomNavBar(
    viewModel: CalculatorViewModel
) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()

    NavigationBar(
        modifier = Modifier.testTag("bottom_nav_bar"),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        val itemColors = androidx.compose.material3.NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.primary,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
            unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )

        // Tab 1: Calculator
        NavigationBarItem(
            selected = currentTab == "calculator",
            onClick = { viewModel.setTab("calculator") },
            colors = itemColors,
            icon = {
                Icon(
                    imageVector = if (currentTab == "calculator") Icons.Default.Calculate else Icons.Outlined.Calculate,
                    contentDescription = stringResource(R.string.calculator_tab)
                )
            },
            label = { 
                Text(
                    text = stringResource(R.string.calculator_tab),
                    fontWeight = if (currentTab == "calculator") FontWeight.Bold else FontWeight.Medium
                ) 
            },
            modifier = Modifier.testTag("nav_calculator")
        )

        // Tab 2: History
        NavigationBarItem(
            selected = currentTab == "history",
            onClick = { viewModel.setTab("history") },
            colors = itemColors,
            icon = {
                Icon(
                    imageVector = if (currentTab == "history") Icons.Default.History else Icons.Outlined.History,
                    contentDescription = stringResource(R.string.history_tab)
                )
            },
            label = { 
                Text(
                    text = stringResource(R.string.history_tab),
                    fontWeight = if (currentTab == "history") FontWeight.Bold else FontWeight.Medium
                ) 
            },
            modifier = Modifier.testTag("nav_history")
        )

        // Tab 3: Settings
        NavigationBarItem(
            selected = currentTab == "settings",
            onClick = { viewModel.setTab("settings") },
            colors = itemColors,
            icon = {
                Icon(
                    imageVector = if (currentTab == "settings") Icons.Default.Settings else Icons.Outlined.Settings,
                    contentDescription = stringResource(R.string.settings_tab)
                )
            },
            label = { 
                Text(
                    text = stringResource(R.string.settings_tab),
                    fontWeight = if (currentTab == "settings") FontWeight.Bold else FontWeight.Medium
                ) 
            },
            modifier = Modifier.testTag("nav_settings")
        )
    }
}
