package com.example.fencing_project.data.model

import java.util.UUID

data class Opponent(
    val roomId: Long=0,
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val weaponHand: String = "",
    val weaponType: String = "",
    val comment: String = "",
    val avatarUrl: String = "",
    val createdBy: String = "",
    val createdAt:  Long = System.currentTimeMillis(),

    val totalBouts: Int = 0,
    val userWins: Int = 0,
    val opponentWins: Int = 0,
    val draws: Int = 0,
    val totalUserScore: Int = 0,
    val totalOpponentScore: Int = 0,
    val lastBoutDate: Long? = null
){
    constructor() : this(0,"", "", "", "", "","","", 0,
        0,0,0,0,0,0,null)
}