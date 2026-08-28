package com.example

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.ClassFlowViewModel
import com.example.ui.screens.StudentsScreen
import com.example.ui.screens.SyllabusScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.NotesScreen
import com.example.ui.theme.ClassFlowTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Re-arm the exact restart alarm (and widget refresh alarm) on every app
        // open, then start the tracking service only if we are inside the class
        // notification window.
        com.example.notification.ScheduleSync.syncNow(this)

        // Permanently light status bar so dark icons sit on the off-white theme.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )
        setContent {
            ClassFlowTheme {
                var showSplash by remember { mutableStateOf(value = true) }
                
                if (showSplash) {
                    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
                         Icon(Icons.Default.School, "Logo", modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                    LaunchedEffect(Unit) {
                        kotlinx.coroutines.delay(500)
                        showSplash = false
                    }
                } else {
                    MainAppLayout()
                }
            }
        }
    }
}

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.School,
            contentDescription = "App Logo",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(100.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "Welcome to MyClass",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Your smart teacher diary. Manage your routines, track student attendance, define holidays, and assign homework dynamically.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = onComplete,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Get Started", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun MainAppLayout() {
    val navController = rememberNavController()
    val viewModel: ClassFlowViewModel = viewModel()
    
    val onboardingFinished by viewModel.onboardingFinished.collectAsStateWithLifecycle()
    
    if (onboardingFinished == null) {
        // Still loading preferences
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    } else if (onboardingFinished == false) {
        OnboardingScreen { viewModel.completeOnboarding() }
        return
    }

    val context = LocalContext.current
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { }
        LaunchedEffect(Unit) {
            val permission = "android.permission.POST_NOTIFICATIONS"
            if (androidx.core.content.ContextCompat.checkSelfPermission(context, permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(permission)
            }
        }
    }
    
    // Track the current route to highlight active tabs in bottom navigation
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "home"

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
        bottomBar = {
            CustomBottomNavigationBar(currentRoute) { route ->
                navController.navigate(route) {
                    popUpTo(navController.graph.startDestinationId) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
    ) { _ ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.fillMaxSize()
        ) {
            composable("home") {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToClasses = { navController.navigate("settings") }
                )
            }
            composable("students") {
                StudentsScreen(viewModel = viewModel)
            }
            composable("syllabus") {
                SyllabusScreen(viewModel = viewModel)
            }
            composable("exams") {
                com.example.ui.screens.ExamsScreen(viewModel = viewModel)
            }
            composable("notes") {
                NotesScreen(viewModel = viewModel)
            }
            composable("settings") {
                SettingsScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun CustomBottomNavigationBar(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    val items = remember {
        listOf(
            BottomNavItem("home", "Home", Icons.Filled.Home, Icons.Outlined.Home),
            BottomNavItem("students", "Students", Icons.Filled.People, Icons.Outlined.People),
            BottomNavItem("syllabus", "Syllabus", Icons.Filled.AutoStories, Icons.Outlined.AutoStories),
            BottomNavItem("exams", "Exams", Icons.AutoMirrored.Filled.List, Icons.AutoMirrored.Outlined.List),
            BottomNavItem("notes", "Notes", Icons.Filled.StickyNote2, Icons.Outlined.StickyNote2),
            BottomNavItem("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
        )
    }

    // Elegant semi-transparent floating pill navigation bar
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding() // full edge-to-edge transparent gestural bar support
            .padding(start = 12.dp, end = 12.dp, bottom = 4.dp) // margins to center and float the bar low
            .clip(RoundedCornerShape(50)) // true pill shape
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)) // semi-transparent
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                shape = RoundedCornerShape(50)
            )
            .clickable(enabled = false) {}
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = currentRoute == item.route

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .clickable { onNavigate(item.route) }
                        .padding(4.dp)
                        .testTag("nav_tab_${item.route}"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Active selection chip: 40dp circle behind a 22dp icon
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = item.label,
                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    )
                }
            }
        }
    }
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)
