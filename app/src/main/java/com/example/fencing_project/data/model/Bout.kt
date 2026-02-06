package com.example.fencing_project.data.model

import java.util.UUID

data class Bout(
    val roomId: Long=0,
    val id: String = UUID.randomUUID().toString(),
    val opponentId: String = "",
    val roomOpponentId: Long = 0,
    val authorId: String = "",
    val userScore: Int = 0,
    val opponentScore: Int = 0,
    val date: Long = System.currentTimeMillis(),
    val comment: String = ""
){
    constructor() : this(0,"", "",0, "", 0, 0, 0, "")
}