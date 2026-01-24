// --- CORRECCIÓN DE WARNING 1: Anotación correcta ---
@file:SuppressLint("UnsafeOptInUsageError")

package com.aquiles.crosschapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.aquiles.crosschapp.presentation.VideoSplashScreen
import com.aquiles.crosschapp.presentation.home.AdminManageBenchmarksScreen
import com.aquiles.crosschapp.presentation.auth.GymFinderScreen
import com.aquiles.crosschapp.presentation.auth.LoginScreen
import com.aquiles.crosschapp.presentation.auth.RegisterScreen
import com.aquiles.crosschapp.presentation.common.AppBackground
import com.aquiles.crosschapp.presentation.home.*
import com.aquiles.crosschapp.presentation.messages.MessageArchiveScreen
import com.aquiles.crosschapp.presentation.navigation.AppBottomNavigationBar
import com.aquiles.crosschapp.presentation.navigation.BottomNavItem
import com.aquiles.crosschapp.presentation.viewmodel.*
import com.aquiles.crosschapp.ui.theme.CrossChAppTheme
import com.aquiles.crosschapp.presentation.home.AdminPaymentConfigScreen // IMPORTANTE: Importar la nueva pantalla
import java.time.LocalDate

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("Permissions", "POST_NOTIFICATIONS permission granted.")
        } else {
            Log.d("Permissions", "POST_NOTIFICATIONS permission denied.")
        }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        askNotificationPermission()
        val shouldOpenNotifications = intent.getBooleanExtra("open_notifications_screen", false)
        setContent {
            CrossChAppTheme {
                MainApp(shouldOpenNotifications = shouldOpenNotifications)
            }
        }
    }
}

@Composable
fun MainApp(shouldOpenNotifications: Boolean = false) {
    val navController = rememberNavController()

    LaunchedEffect(shouldOpenNotifications) {
        if (shouldOpenNotifications) {
            navController.navigate("notifications_screen")
        }
    }

    val currentUser by UserSession.currentUser.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val startDestination = if (currentUser != null) "main_graph" else "auth_graph"
    val routesWithBottomBar = setOf(
        BottomNavItem.Home.route,
        BottomNavItem.Schedule.route.substringBefore("?"),
        BottomNavItem.Wods.route,
        BottomNavItem.Performance.route,
        BottomNavItem.Profile.route
    )
    val shouldShowBottomBar = routesWithBottomBar.any { currentRoute?.startsWith(it) == true }

    Scaffold(
        bottomBar = {
            if (shouldShowBottomBar) {
                val bottomNavItems = listOf(
                    BottomNavItem.Home, BottomNavItem.Schedule, BottomNavItem.Wods,
                    BottomNavItem.Performance, BottomNavItem.Profile
                )
                AppBottomNavigationBar(navController = navController, items = bottomNavItems)
            }
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        AppBackground {
            AppNavigationHost(
                navController = navController,
                startDestination = startDestination,
                innerPadding = innerPadding,
                performanceViewModel = viewModel()
            )
        }
    }
}


@Composable
fun AppNavigationHost(
    navController: NavHostController,
    startDestination: String,
    innerPadding: PaddingValues,
    performanceViewModel: PerformanceViewModel
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        authGraph(navController)
        mainGraph(navController, innerPadding, performanceViewModel)
        composable("video_splash_screen") {
            VideoSplashScreen(
                onVideoEnded = {
                    navController.navigate("main_graph") {
                        popUpTo("video_splash_screen") { inclusive = true }
                    }
                }
            )
        }
    }
}

fun NavGraphBuilder.authGraph(navController: NavHostController) {
    navigation(startDestination = "login_screen", route = "auth_graph") {
        composable("login_screen") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("video_splash_screen") { popUpTo("auth_graph") { inclusive = true } }
                },
                onNavigateToRegister = { navController.navigate("gym_finder_screen") }
            )
        }

        composable("gym_finder_screen") {
            GymFinderScreen(
                onGymSelected = { gymId ->
                    navController.navigate("register_screen/$gymId")
                }
            )
        }

        composable(
            route = "register_screen/{gymId}",
            arguments = listOf(navArgument("gymId") { type = NavType.StringType })
        ) { backStackEntry ->
            val gymId = backStackEntry.arguments?.getString("gymId")

            RegisterScreen(
                gymId = gymId,
                onRegisterSuccess = {
                    navController.navigate("video_splash_screen") { popUpTo("auth_graph") { inclusive = true } }
                },
                onNavigateToLogin = { navController.popBackStack("login_screen", inclusive = false) }
            )
        }
    }
}

fun NavGraphBuilder.mainGraph(
    navController: NavHostController,
    innerPadding: PaddingValues,
    performanceViewModel: PerformanceViewModel
) {
    navigation(startDestination = BottomNavItem.Home.route, route = "main_graph") {
        val onLogout: () -> Unit = {
            UserSession.endSession()
            navController.navigate("auth_graph") {
                popUpTo(0) { inclusive = true }
            }
        }

        composable(BottomNavItem.Home.route) {
            HomeScreen(
                innerPadding = innerPadding,
                homeViewModel = viewModel(),
                adminViewModel = viewModel(),
                onNavigateToAdminCreditRequests = { navController.navigate("admin_credit_requests") },
                onNavigateToNotifications = { navController.navigate("notifications_screen") },
                onNavigateToMessageArchive = { navController.navigate("message_archive") }
            )
        }

        composable("message_archive") {
            MessageArchiveScreen(navController = navController)
        }

        composable(BottomNavItem.Profile.route) {
            ProfileScreen(
                innerPadding = innerPadding,
                profileViewModel = viewModel(),
                onEditProfileClicked = { navController.navigate("edit_profile_screen") },
                onNavigateToRequestCredits = { navController.navigate("request_credits_screen") },
                onNavigateToAdminDashboard = { navController.navigate("admin_dashboard_screen") },
                onLogout = onLogout
            )
        }
        composable(
            route = "class_details/{classId}",
            arguments = listOf(navArgument("classId") { type = NavType.StringType })
        ) { backStackEntry ->
            val classId = backStackEntry.arguments?.getString("classId")!!
            ClassDetailsScreen(
                innerPadding = innerPadding,
                navController = navController,
                classId = classId,
                scheduleViewModel = viewModel(),
                adminViewModel = viewModel()
            )
        }
        composable(
            route = "${BottomNavItem.Schedule.route}?date={date}",
            arguments = listOf(navArgument("date") { type = NavType.StringType; nullable = true })
        ) { backStackEntry ->
            ScheduleScreen(
                innerPadding = innerPadding,
                scheduleViewModel = viewModel(),
                initialDateStr = backStackEntry.arguments?.getString("date"),
                onClassClick = { classId: String -> navController.navigate("class_details/$classId") },
                onNavigateToRequestCredits = { navController.navigate("request_credits_screen") }
            )
        }
        composable(BottomNavItem.Wods.route) {
            WodsScreen(
                innerPadding = innerPadding,
                adminViewModel = viewModel(),
                scheduleViewModel = viewModel(),
                performanceViewModel = performanceViewModel,
                onNavigateToClassDetail = { classId: String -> navController.navigate("class_details/$classId") },
                onNavigateToScheduleAtDate = { date: LocalDate -> navController.navigate(BottomNavItem.Schedule.route + "?date=" + date.toString()) },
                onNavigateToWodHistory = { /* A implementar */ },
                onNavigateToRequestCredits = { navController.navigate("request_credits_screen") }
            )
        }
        composable(BottomNavItem.Performance.route) {
            PerformanceScreen(
                innerPadding = innerPadding,
                performanceViewModel = performanceViewModel
            )
        }
        composable("edit_profile_screen") {
            EditProfileScreen(
                innerPadding = innerPadding,
                profileViewModel = viewModel(),
                onNavigateBack = { navController.popBackStack() },
                onLogout = onLogout
            )
        }
        composable("request_credits_screen") {
            RequestCreditsScreen(
                innerPadding = innerPadding,
                creditsViewModel = viewModel(),
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("admin_dashboard_screen") {
            AdminDashboardScreen(
                innerPadding = innerPadding,
                navController = navController
            )
        }
        composable("admin_credit_requests") {
            AdminCreditRequestsScreen(
                innerPadding = innerPadding,
                navController = navController,
                adminViewModel = viewModel()
            )
        }
        composable("admin_manage_users") {
            AdminManageUsersScreen(
                innerPadding = innerPadding,
                navController = navController,
                adminViewModel = viewModel()
            )
        }
        composable("admin_reports_screen") {
            AdminReportsScreen(
                innerPadding = innerPadding,
                navController = navController
            )
        }
        composable("admin_manage_packs_screen") {
            AdminManagePacksScreen(
                innerPadding = innerPadding,
                navController = navController
            )
        }

        // --- NUEVA RUTA: CONFIGURACIÓN DE PAGO ---
        composable("admin_payment_config") {
            AdminPaymentConfigScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        // -----------------------------------------

        composable("admin_manage_benchmarks") {
            AdminManageBenchmarksScreen(
                innerPadding = innerPadding,
                navController = navController,
                adminViewModel = viewModel()
            )
        }
        composable(
            route = "admin_user_details/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId")
            if (userId != null) {
                AdminUserDetailsScreen(
                    innerPadding = innerPadding,
                    navController = navController,
                    userId = userId,
                    adminViewModel = viewModel()
                )
            } else {
                navController.popBackStack()
            }
        }
        composable("admin_manage_classes") {
            AdminManageClassesScreen(
                innerPadding = innerPadding,
                navController = navController,
                adminViewModel = viewModel(),
                onNavigateToCreateClass = { navController.navigate("create_edit_class_screen") },
                onNavigateToEditClass = { classId: String -> navController.navigate("create_edit_class_screen?classId=$classId") },
                onNavigateToClassDetails = { classId: String -> navController.navigate("class_details/$classId") }
            )
        }
        composable("admin_manage_schedules") {
            AdminManageSchedulesScreen(
                innerPadding = innerPadding,
                navController = navController,
                adminViewModel = viewModel()
            )
        }
        
        // --- NEW: THEMED & PLANNER ROUTES ---
        composable("admin_gym_settings") {
            AdminGymSettingsScreen(
                navController = navController,
                adminViewModel = viewModel(),
                innerPadding = innerPadding
            )
        }
        composable("admin_schedule_planner") {
            AdminSchedulePlannerScreen(
                navController = navController,
                innerPadding = innerPadding
            )
        }
        // ------------------------------------
        
        composable(
            route = "create_edit_class_screen?classId={classId}",
            arguments = listOf(navArgument("classId") { type = NavType.StringType; nullable = true })
        ) { backStackEntry ->
            CreateEditClassScreen(
                innerPadding = innerPadding,
                navController = navController,
                adminViewModel = viewModel(),
                classId = backStackEntry.arguments?.getString("classId")
            )
        }
        composable("notifications_screen") {
            NotificationsScreen(navController = navController)
        }
        composable(
            route = "create_edit_wod_screen?wodId={wodId}",
            arguments = listOf(navArgument("wodId") { type = NavType.StringType; nullable = true })
        ) { backStackEntry ->
            CreateEditWodScreen(
                innerPadding = innerPadding,
                navController = navController,
                adminViewModel = viewModel(),
                wodId = backStackEntry.arguments?.getString("wodId"),
                profileViewModel = viewModel()
            )
        }

        composable(
            route = "admin_send_message/{userId}/{userName}",
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType },
                navArgument("userName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            val userName = backStackEntry.arguments?.getString("userName") ?: ""
            AdminSendMessageScreen(
                navController = navController,
                userId = userId,
                userName = userName
            )
        }

        composable(
            route = "admin_broadcast_message/{userIds}",
            arguments = listOf(navArgument("userIds") { type = NavType.StringType })
        ) { backStackEntry ->
            val userIdsString = backStackEntry.arguments?.getString("userIds") ?: ""
            val userIdsList = userIdsString.split(',').filter { it.isNotBlank() }

            AdminBroadcastMessageScreen(
                navController = navController,
                userIds = userIdsList
            )
        }
    }
}