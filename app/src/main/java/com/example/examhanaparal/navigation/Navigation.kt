package com.example.examhanaparal.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.examhanaparal.screens.*

// ── Route constants
object Routes {
    const val LOGIN         = "login"
    const val PROFILE_SETUP = "profile_setup"
    const val MAIN          = "main"
    const val CREATE_GROUP  = "create_group"
    const val GROUP_DETAIL  = "group_detail/{groupId}"
    const val ADMIN         = "admin"
    const val USER_PROFILE  = "user_profile"

    fun groupDetail(groupId: String) = "group_detail/$groupId"
}

@Composable
fun HanapAralNavGraph(navController: NavHostController, startDestination: String) {
    NavHost(navController = navController, startDestination = startDestination) {

        composable(Routes.LOGIN) {
            LoginScreen(navController)
        }

        composable(Routes.PROFILE_SETUP) {
            ProfileSetupScreen(navController)
        }

        composable(Routes.MAIN) {
            MainScreen(navController)
        }

        composable(Routes.CREATE_GROUP) {
            CreateGroupScreen(navController)
        }

        composable(
            route     = Routes.GROUP_DETAIL,
            arguments = listOf(navArgument("groupId") { type = NavType.StringType })
        ) { backStack ->
            val groupId = backStack.arguments?.getString("groupId") ?: ""
            GroupDetailScreen(navController, groupId)
        }

        composable(Routes.ADMIN) {
            AdminScreen(navController)
        }

        composable(Routes.USER_PROFILE) {
            UserProfileScreen(navController)
        }
    }
}
