package com.example.examhanaparal.models

data class UserProfile(
    val uid     : String = "",
    val name    : String = "",
    val course  : String = "",
    val program : String = "",
    val email   : String = "",
    val role    : String = "student"   // "admin" or "student"
)

// ── Study group stored in Firestore under /groups/{id}
data class StudyGroup(
    val id         : String       = "",
    val name       : String       = "",
    val subject    : String       = "",
    val adminUid   : String       = "",
    val members    : List<String> = emptyList(),
    val maxMembers : Long         = 10L
) {
    val memberCount get() = members.size
    val isFull      get() = memberCount >= maxMembers
}