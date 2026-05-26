package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "last_inputs")
data class LastInputEntity(
    @PrimaryKey val id: Int = 1,
    val weight: String = "",
    val weightUnit: String = "kg",
    val rate: String = "",
    val rateType: String = "per kg",
    val totalAmount: String = "",
    val reverseType: String = "W_R_T", // Default to Weight + Rate -> Total
    val darkModePreference: String = "system", // "system", "dark", "light"
    val languagePreference: String = "en" // "en", "hi", "es", "bn"
)
