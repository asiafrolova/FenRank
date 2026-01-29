package com.example.fencing_project.data.model

import java.util.Date

data class Opponent(
    val id: String = "",
    val name: String = "",
    val weaponHand: String = "", // "Правая", "Левая"
    val weaponType: String = "", // "Рапира", "Сабля", "Шпага"
    val comment: String = "",
    val avatarUrl: String = "",
    val createdBy: String = "", // UID создателя
    val createdAt:  Long = System.currentTimeMillis(),

    // Добавляем поля для статистики
    val totalBouts: Int = 0,      // Всего боев с этим соперником
    val userWins: Int = 0,        // Победы пользователя
    val opponentWins: Int = 0,    // Победы соперника
    val draws: Int = 0,           // Ничьи
    val totalUserScore: Int = 0,  // Всего набранных очков пользователем
    val totalOpponentScore: Int = 0, // Всего набранных очков соперником
    val lastBoutDate: Long? = null // Дата последнего боя
){
    constructor() : this("", "", "", "", "","","", 0,
        0,0,0,0,0,0,null)
}