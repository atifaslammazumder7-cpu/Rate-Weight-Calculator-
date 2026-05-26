package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calculations")
data class CalculationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val weight: String,
    val weightUnit: String,
    val rate: String,
    val rateType: String,
    val totalAmount: String,
    val reverseType: String, // "W_R_T" (Weight+Rate->Total), "T_R_W" (Total+Rate->Weight), "T_W_R" (Total+Weight->Rate)
    val timestamp: Long = System.currentTimeMillis()
)
