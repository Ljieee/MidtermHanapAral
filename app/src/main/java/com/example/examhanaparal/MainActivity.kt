package com.example.examhanaparal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.example.examhanaparal.navigation.HanapAralNavGraph
import com.example.examhanaparal.navigation.Routes
import com.example.examhanaparal.ui.theme.ExamHanapAralTheme
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ExamHanapAralTheme {
                val navController = rememberNavController()
                val currentUser = FirebaseAuth.getInstance().currentUser
                
                val startDestination = if (currentUser != null) {
                    Routes.MAIN
                } else {
                    Routes.LOGIN
                }
                
                HanapAralNavGraph(
                    navController = navController,
                    startDestination = startDestination
                )
            }
        }
    }
}
