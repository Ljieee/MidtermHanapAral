package com.example.examhanaparal.navigation

import androidx.compose.runtime.Composable

object Routes{
    const val LOGIN = "login"
    const val PROFILE_SETUP = "profile_setup"
    const val MAIN = "main"
    const val CREATE_GROUP = "create_group"
    const val GROUP_DETAIL = "group_detail"
    const val USER_PROFILE = "user_profile"
    const val ADMIN = "admin"

    // Helper function to build a path with an ID
    fun groupDetail(groupId: String) = "group_detail/$groupId"

}

