package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CalculationDao {
    @Query("SELECT * FROM calculations ORDER BY timestamp DESC")
    fun getAllCalculations(): Flow<List<CalculationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalculation(calculation: CalculationEntity)

    @Query("DELETE FROM calculations WHERE id = :id")
    suspend fun deleteCalculationById(id: Long)

    @Query("DELETE FROM calculations")
    suspend fun clearAllCalculations()

    @Query("SELECT * FROM last_inputs WHERE id = 1")
    fun getLastInputsFlow(): Flow<LastInputEntity?>

    @Query("SELECT * FROM last_inputs WHERE id = 1")
    suspend fun getLastInputs(): LastInputEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveLastInputs(lastInputs: LastInputEntity)
}
