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
    val createdAt:  Long = System.currentTimeMillis()
){
    constructor() : this("", "", "", "", "","","", 0)
}