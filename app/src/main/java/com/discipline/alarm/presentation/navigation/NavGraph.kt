package com.discipline.alarm.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.discipline.alarm.presentation.alarm.AlarmEditScreen
import com.discipline.alarm.presentation.alarm.AlarmEditViewModel
import com.discipline.alarm.presentation.home.HomeScreen
import com.discipline.alarm.presentation.home.HomeViewModel
import com.discipline.alarm.presentation.mission.MissionRegisterScreen
import com.discipline.alarm.presentation.mission.MissionRegisterViewModel
import com.discipline.alarm.presentation.permissions.PermissionsScreen
import com.discipline.alarm.presentation.permissions.PermissionsViewModel

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            val viewModel: HomeViewModel = hiltViewModel()
            HomeScreen(
                viewModel = viewModel,
                onNavigateToEditAlarm = { alarmId ->
                    navController.navigate(Screen.AlarmEdit.createRoute(alarmId))
                },
                onNavigateToMissions = {
                    navController.navigate(Screen.MissionRegister.createRoute(null))
                },
                onNavigateToPermissions = {
                    navController.navigate(Screen.Permissions.route)
                }
            )
        }

        composable(
            route = Screen.AlarmEdit.route,
            arguments = listOf(
                navArgument("alarmId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val alarmId = backStackEntry.arguments?.getString("alarmId")?.toLongOrNull()
            val viewModel: AlarmEditViewModel = hiltViewModel()
            AlarmEditScreen(
                alarmId = alarmId,
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToCreateMission = {
                    navController.navigate(Screen.MissionRegister.createRoute(null))
                }
            )
        }

        composable(
            route = Screen.MissionRegister.route,
            arguments = listOf(
                navArgument("missionId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            val viewModel: MissionRegisterViewModel = hiltViewModel()
            MissionRegisterScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onMissionCreated = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Permissions.route) {
            val viewModel: PermissionsViewModel = hiltViewModel()
            PermissionsScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
