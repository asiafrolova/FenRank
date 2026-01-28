package com.example.fencing_project.data.model

import java.util.Date

data class Bout(
    val id: String = "",
    val opponentId: String = "",
    val authorId: String = "",
    val userScore: Int = 0,
    val opponentScore: Int = 0,
    val date: Long = System.currentTimeMillis(),
    val comment: String = ""
){
    constructor() : this("", "", "", 0, 0, 0, "")
}