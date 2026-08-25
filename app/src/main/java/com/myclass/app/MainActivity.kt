package com.myclass.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.myclass.app.ui.screens.dashboard.DashboardScreen
import com.myclass.app.ui.screens.homework.HomeworkScreen
import com.myclass.app.ui.screens.more.MoreScreen
import com.myclass.app.ui.screens.students.StudentsScreen
import com.myclass.app.ui.screens.syllabus.SyllabusScreen
import com.myclass.app.ui.theme.MyClassTheme

object Routes {
    const val DASHBOARD = "dashboard"
    const val HOMEWORK = "homework"
    const val SYLLABUS = "syllabus"
    const val STUDENTS = "students"
    const val MORE = "more"
    const val EXAMS = "exams"
    const val NOTES = "notes"
    const val HOLIDAYS = "holidays"
    const val SETTINGS = "settings"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyClassTheme {
                MyClassApp()
            }
        }
    }
}

private data class BottomDest(val route: String, val label: String, val icon: @Composable () -> Unit)

@Composable
fun MyClassApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val bottomDests = listOf(
        BottomDest(Routes.DASHBOARD, "Home") { Icon(Icons.Filled.Home, contentDescription = null) },
        BottomDest(Routes.HOMEWORK, "Homework") { Icon(Icons.Filled.Assignment, contentDescription = null) },
        BottomDest(Routes.SYLLABUS, "Syllabus") { Icon(Icons.Filled.MenuBook, contentDescription = null) },
        BottomDest(Routes.STUDENTS, "Students") { Icon(Icons.Filled.People, contentDescription = null) },
        BottomDest(Routes.MORE, "More") { Icon(Icons.Filled.MoreHoriz, contentDescription = null) }
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomDests.forEach { dest ->
                    NavigationBarItem(
                        selected = currentRoute == dest.route,
                        onClick = {
                            navController.navigate(dest.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = dest.icon,
                        label = { Text(dest.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.DASHBOARD,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.DASHBOARD) { DashboardScreen() }
            composable(Routes.HOMEWORK) { HomeworkScreen() }
            composable(Routes.SYLLABUS) { SyllabusScreen() }
            composable(Routes.STUDENTS) { StudentsScreen() }
            composable(Routes.MORE) {
                MoreScreen(
                    onOpenExams = { navController.navigate(Routes.EXAMS) },
                    onOpenNotes = { navController.navigate(Routes.NOTES) },
                    onOpenHolidays = { navController.navigate(Routes.HOLIDAYS) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) }
                )
            }
            composable(Routes.EXAMS) {
                com.myclass.app.ui.screens.exams.ExamsScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.NOTES) {
                com.myclass.app.ui.screens.notes.NotesScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.HOLIDAYS) {
                com.myclass.app.ui.screens.holidays.HolidaysScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.SETTINGS) {
                com.myclass.app.ui.screens.settings.SettingsScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
