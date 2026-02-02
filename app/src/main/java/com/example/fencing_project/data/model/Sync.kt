package com.example.fencing_project.data.model

import com.example.fencing_project.data.model.Opponent
import java.util.UUID

data class Sync (
    val id: String = UUID.randomUUID().toString(),
    val createdAt:  Long = System.currentTimeMillis(),
    val createdBy: String = ""
){
    constructor() : this("",0, "")
}