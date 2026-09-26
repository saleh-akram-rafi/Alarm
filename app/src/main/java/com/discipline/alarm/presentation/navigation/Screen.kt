package com.discipline.alarm.presentation.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object AlarmEdit : Screen("alarm_edit?alarmId={alarmId}") {
        fun createRoute(alarmId: Long?): String =
            if (alarmId != null) "alarm_edit?alarmId=$alarmId" else "alarm_edit"
    }
    object MissionRegister : Screen("mission_register?missionId={missionId}") {
        fun createRoute(missionId: Long? = null): String =
            if (missionId != null) "mission_register?missionId=$missionId" else "mission_register"
    }
    object Permissions : Screen("permissions")
    object Settings : Screen("settings")
}
