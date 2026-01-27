package com.example.fencing_project.data.model

data class Opponent(
    val id: String = "",
    val name: String = "",
    val weaponHand: String = "", // "Правая", "Левая"
    val weaponType: String = "", // "Рапира", "Сабля", "Шпага"
    val comment: String = "",
    val photoUrl: String = "",
    val createdBy: String = "" // UID создателя
)