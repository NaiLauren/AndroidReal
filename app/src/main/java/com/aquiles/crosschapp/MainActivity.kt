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
import androidx.compose.ui.Modifier
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.aquiles.crosschapp.presentation.VideoSplashScreen
import com.aquiles.crosschapp.presentation.home.AdminManageBenchmarksScreen
import com.aquiles.crosschapp.presentation.home.GamificationRulesScreen
import com.aquiles.crosschapp.presentation.auth.GymFinderScreen
import com.aquiles.crosschapp.presentation.auth.LoginScreen
import com.aquiles.crosschapp.presentation.auth.RegisterScreen
import com.aquiles.crosschapp.presentation.common.AppBackground
import com.aquiles.crosschapp.presentation.home.*
import com.aquiles.crosschapp.presentation.social.SocialFeedScreen // NUEVA PANTALLA
import com.aquiles.crosschapp.presentation.messages.MessageArchiveScreen
import com.aquiles.crosschapp.presentation.navigation.AppBottomNavigationBar
import com.aquiles.crosschapp.presentation.navigation.BottomNavItem
import com.aquiles.crosschapp.presentation.viewmodel.*
import com.aquiles.crosschapp.ui.theme.CrossChAppTheme
import com.aquiles.crosschapp.presentation.home.AdminPaymentConfigScreen // IMPORTANTE: Importar la nueva pantalla
import com.aquiles.crosschapp.presentation.home.AdminCompetitionManagerScreen
import com.aquiles.crosschapp.presentation.home.AdminCompetitionDetailScreen
import com.aquiles.crosschapp.presentation.home.OwnerCreateCompetitionScreen
import com.aquiles.crosschapp.presentation.competition.StudentCompetitionDetailScreen
import java.time.LocalDate

import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze

val LocalHazeState = compositionLocalOf<HazeState?> { null }

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

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "crosschapp_default_channel"
            val channelName = "Notificaciones Generales"
            val notificationManager = getSystemService(android.app.NotificationManager::class.java)
            val existingChannel = notificationManager.getNotificationChannel(channelId)
            
            if (existingChannel == null) {
                val channel = android.app.NotificationChannel(
                    channelId, channelName, android.app.NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Avisos importantes de la app"
                    enableVibration(true)
                    enableLights(true)
                }
                notificationManager.createNotificationChannel(channel)
            }
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
        ensureNotificationChannel() // Asegurar canal existente desde el inicio
        askNotificationPermission()
        val shouldOpenNotifications = intent.getBooleanExtra("open_notifications_screen", false)
        setContent {
            val currentGym by UserSession.currentGym.collectAsState()
            
            // Convertir Hex String a Color (Robust)
            val primaryColor = remember(currentGym?.primaryColor) {
                try {
                    var hex = currentGym?.primaryColor?.trim()
                    if (!hex.isNullOrBlank()) {
                        if (!hex.startsWith("#")) hex = "#$hex"
                        if (hex.length == 4) { // #F00 -> #FF0000
                             val r = hex[1]; val g = hex[2]; val b = hex[3]
                             hex = "#$r$r$g$g$b$b"
                        }
                        Color(android.graphics.Color.parseColor(hex))
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    null
                }
            }

            CrossChAppTheme(overridePrimaryColor = primaryColor) {
                MainApp(shouldOpenNotifications = shouldOpenNotifications)
            }
        }
    }
}

@Composable
fun MainApp(shouldOpenNotifications: Boolean = false) {
    val navController = rememberNavController()
    // --- NUEVO: Estado para control de chequeo de sesión ---
    var isCheckingSession by remember { mutableStateOf(true) }
    val authViewModel: AuthViewModel = viewModel()
    val hazeState = remember { HazeState() }
    // --------------------------------------------------------

    LaunchedEffect(shouldOpenNotifications) {
        if (shouldOpenNotifications) {
            navController.navigate("notifications_screen")
        }
    }

    // --- NUEVO: Lógica de Restauración de Sesión ---
    LaunchedEffect(Unit) {
        // Verificar si hay sesión en Auth pero no en UserSession (Memory Wipe)
        authViewModel.checkActiveSession { result ->
            isCheckingSession = false
        }
    }
    // -----------------------------------------------

    val currentUser by UserSession.currentUser.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    // --- LÓGICA DE SPLASH SCREEN SINCRONIZADO ---
    // El splash termina cuando: 1) El video termina (o no hay video) Y 2) La sesión se ha verificado
    var isSplashFinished by remember { mutableStateOf(false) }

    if (!isSplashFinished) {
        VideoSplashScreen(
            canProceed = !isCheckingSession, // Esperamos a que termine el check de sesión
            onVideoEnded = { 
                isSplashFinished = true 
            }
        )
        return // No mostramos el resto de la app hasta que el splash termine
    }
    // ----------------------------------------------

    val startDestination = if (currentUser != null) "main_graph" else "auth_graph"
    val routesWithBottomBar = setOf(
        BottomNavItem.Home.route,
        BottomNavItem.Schedule.route.substringBefore("?"),
        BottomNavItem.Wods.route,
        BottomNavItem.Performance.route,
        BottomNavItem.Profile.route,
        "admin_dashboard_screen"
    )
    val shouldShowBottomBar = routesWithBottomBar.any { currentRoute?.startsWith(it) == true }

    CompositionLocalProvider(LocalHazeState provides hazeState) {
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
            AppBackground(modifier = Modifier.haze(state = hazeState)) {
                AppNavigationHost(
                    navController = navController,
                    startDestination = startDestination,
                    innerPadding = innerPadding,
                    performanceViewModel = viewModel()
                )
            }
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
                onNavigateToNotifications = { navController.navigate("notifications_screen") },
                onNavigateToMessageArchive = { navController.navigate("message_archive") },
                onNavigateToCreateNotice = { navController.navigate("create_notice") },
                onNavigateToCompetition = { competitionId -> navController.navigate("student_competition/$competitionId") },
                onNavigateToWall = { navController.navigate("wall_of_results") }
            )
        }

        composable("wall_of_results") {
            WallOfResultsScreen(navController = navController)
        }

        composable("message_archive") {
            MessageArchiveScreen(navController = navController)
        }

        composable(BottomNavItem.Social.route) {
            SocialFeedScreen(
                navController = navController,
                benchmarkFeedViewModel = viewModel(),
                innerPadding = innerPadding
            )
        }

        composable(BottomNavItem.Profile.route) {
            val profileVm: ProfileViewModel = viewModel()
            ProfileScreen(
                innerPadding = innerPadding,
                profileViewModel = profileVm,
                onEditProfileClicked = { navController.navigate("edit_profile_screen") },
                onNavigateToRequestCredits = { navController.navigate("request_credits_screen") },
                onNavigateToAdminDashboard = { navController.navigate("admin_dashboard_screen") },
                onNavigateToAchievements = { navController.navigate("achievements_screen") },
                onNavigateToRules = { navController.navigate("gamification_rules_screen") },
                onNavigateToHistory = { navController.navigate("xp_history_screen") },
                onLogout = onLogout
            )
        }
        composable(
            route = "class_details/{classId}",
            arguments = listOf(navArgument("classId") { type = NavType.StringType })
        ) { backStackEntry ->
            val classId = backStackEntry.arguments?.getString("classId")!!
            val user by UserSession.currentUser.collectAsState()
            if (user != null) {
                ClassDetailsScreen(
                    navController = navController,
                    classId = classId,
                    scheduleViewModel = viewModel(),
                    adminViewModel = viewModel(),
                    currentUser = user!!
                )
            }
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
                onNavigateToRequestCredits = { navController.navigate("request_credits_screen") },
                onNavigateToBenchmarks = { navController.navigate("benchmarks_screen") },
                onNavigateToChallengeRanking = { challengeId -> navController.navigate("challenge_ranking/$challengeId") }
            )
        }
        composable(BottomNavItem.Performance.route) {
            PerformanceScreen(
                innerPadding = innerPadding,
                performanceViewModel = performanceViewModel,
                onNavigateToLeaderboard = { navController.navigate("leaderboard_screen") }
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

        composable("admin_validate_challenges") {
            AdminValidateChallengesScreen(
                innerPadding = innerPadding,
                navController = navController,
                adminViewModel = viewModel()
            )
        }
        composable("admin_credit_requests") {
            AdminCreditRequestsScreen(
                innerPadding = innerPadding,
                navController = navController,
                adminViewModel = viewModel()
            )
        }
        composable(
            route = "admin_manage_users?setupStep={setupStep}",
            arguments = listOf(navArgument("setupStep") { type = NavType.StringType; nullable = true })
        ) { backStackEntry ->
            AdminManageUsersScreen(
                innerPadding = innerPadding,
                navController = navController,
                adminViewModel = viewModel(),
                setupStepKey = backStackEntry.arguments?.getString("setupStep")
            )
        }
        composable("admin_reports_screen") {
            AdminReportsScreen(
                innerPadding = innerPadding,
                navController = navController
            )
        }
        composable(
            route = "admin_manage_packs_screen?setupStep={setupStep}",
            arguments = listOf(navArgument("setupStep") { type = NavType.StringType; nullable = true })
        ) { backStackEntry ->
            AdminManagePacksScreen(
                innerPadding = innerPadding,
                navController = navController,
                setupStepKey = backStackEntry.arguments?.getString("setupStep")
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
                adminViewModel = viewModel(),
                onlyGlobal = false
            )
        }

        composable(
            route = "admin_manage_challenges?onlyGlobal={onlyGlobal}",
            arguments = listOf(navArgument("onlyGlobal") { type = NavType.BoolType; defaultValue = false })
        ) { backStackEntry ->
            val onlyGlobalArg = backStackEntry.arguments?.getBoolean("onlyGlobal") ?: false
            AdminManageChallengesScreen(
                innerPadding = innerPadding,
                navController = navController,
                adminViewModel = viewModel(),
                onlyGlobal = onlyGlobalArg
            )
        }
        
        composable("leaderboard_screen") {
            LeaderboardScreen(
                 innerPadding = innerPadding,
                 navController = navController,
                 leaderboardViewModel = viewModel()
            )
        }
        
        composable(
            route = "challenge_ranking/{challengeId}",
            arguments = listOf(navArgument("challengeId") { type = NavType.StringType })
        ) { backStackEntry ->
            val challengeId = backStackEntry.arguments?.getString("challengeId") ?: ""
            GlobalChallengeRankingScreen(
                innerPadding = innerPadding,
                navController = navController,
                challengeId = challengeId,
                adminViewModel = viewModel()
            )
        }
        composable("benchmarks_screen") {
            com.aquiles.crosschapp.presentation.benchmarks.BenchmarksScreen(
                adminViewModel = viewModel(),
                performanceViewModel = performanceViewModel,
                onBack = { navController.popBackStack() }
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
        composable(
            route = "admin_manage_schedules?setupStep={setupStep}",
            arguments = listOf(navArgument("setupStep") { type = NavType.StringType; nullable = true })
        ) { backStackEntry ->
            AdminManageSchedulesScreen(
                innerPadding = innerPadding,
                navController = navController,
                adminViewModel = viewModel(),
                setupStepKey = backStackEntry.arguments?.getString("setupStep")
            )
        }
        
        composable("admin_manage_activity_images") {
            AdminActivityImagesScreen(
                navController = navController,
                adminViewModel = viewModel()
            )
        }
        
        // --- NEW: THEMED & PLANNER ROUTES ---
        composable(
            route = "admin_gym_settings?setupStep={setupStep}",
            arguments = listOf(navArgument("setupStep") { type = NavType.StringType; nullable = true })
        ) { backStackEntry ->
            AdminGymSettingsScreen(
                navController = navController,
                adminViewModel = viewModel(),
                innerPadding = innerPadding,
                setupStepKey = backStackEntry.arguments?.getString("setupStep")
            )
        }
        composable("admin_schedule_planner") {
            AdminSchedulePlannerScreen(
                navController = navController,
                innerPadding = innerPadding
            )
        }
        composable("gamification_rules_screen") {
            GamificationRulesScreen(
                onDismiss = { navController.popBackStack() }
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

        // --- NUEVA RUTA: CREAR NOVEDAD ---
        composable("create_notice") {
            CreateNoticeScreen(
                navController = navController,
                noticeViewModel = viewModel()
            )
        }
        
        composable(
            route = "admin_news_screen?setupStep={setupStep}",
            arguments = listOf(navArgument("setupStep") { type = NavType.StringType; nullable = true })
        ) { backStackEntry ->
            AdminNewsScreen(
                innerPadding = innerPadding,
                navController = navController,
                noticeViewModel = viewModel(),
                setupStepKey = backStackEntry.arguments?.getString("setupStep")
            )
        }
        // -------------------------------

        // --- TORNEOS Y COMPETENCIAS ---
        composable(
            route = "admin_competition_manager?setupStep={setupStep}",
            arguments = listOf(navArgument("setupStep") { type = NavType.StringType; nullable = true })
        ) { backStackEntry ->
            AdminCompetitionManagerScreen(
                navController = navController,
                setupStepKey = backStackEntry.arguments?.getString("setupStep")
            )
        }
        
        composable(
            route = "admin_competition_detail/{competitionId}",
            arguments = listOf(navArgument("competitionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val competitionId = backStackEntry.arguments?.getString("competitionId")
            if (competitionId != null) {
                AdminCompetitionDetailScreen(
                    navController = navController,
                    competitionId = competitionId
                )
            }
        }
        
        composable("owner_create_competition") {
            OwnerCreateCompetitionScreen(navController = navController)
        }
        // ------------------------------
        
        // ------------------------------

        composable(
            route = "student_competition/{competitionId}",
            arguments = listOf(navArgument("competitionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val competitionId = backStackEntry.arguments?.getString("competitionId")
            if (competitionId != null) {
                StudentCompetitionDetailScreen(
                    navController = navController,
                    competitionId = competitionId
                )
            }
        }
        
        // --- RUTA: HISTORIAL XP ---
        composable("xp_history_screen") {
            com.aquiles.crosschapp.presentation.ui.gamification.XpHistoryScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        // --- RUTA: ÁLBUM DE LOGROS ---
        composable("achievements_screen") {
            val user by UserSession.currentUser.collectAsState()
            val profileVm: ProfileViewModel = viewModel()
            val attendanceState by profileVm.attendanceHistoryState.collectAsState()
            val attendanceCount = if (attendanceState is AttendanceHistoryState.Success)
                (attendanceState as AttendanceHistoryState.Success).records.size
            else 0
            if (user != null) {
                com.aquiles.crosschapp.presentation.ui.gamification.AchievementsScreen(
                    user = user!!,
                    totalClassesAttended = attendanceCount,
                    onBackClick = { navController.popBackStack() },
                    onNavigateToRules = { navController.navigate("gamification_rules_screen") }
                )
            }
        }
        // --------------------------------
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