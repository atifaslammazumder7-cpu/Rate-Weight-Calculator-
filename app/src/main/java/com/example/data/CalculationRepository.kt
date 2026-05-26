package com.example.data

import kotlinx.coroutines.flow.Flow

class CalculationRepository(private val calculationDao: CalculationDao) {
    val allCalculations: Flow<List<CalculationEntity>> = calculationDao.getAllCalculations()
    val lastInputsFlow: Flow<LastInputEntity?> = calculationDao.getLastInputsFlow()

    suspend fun insertCalculation(calculation: CalculationEntity) =
        calculationDao.insertCalculation(calculation)

    suspend fun deleteCalculationById(id: Long) =
        calculationDao.deleteCalculationById(id)

    suspend fun clearAllCalculations() =
        calculationDao.clearAllCalculations()

    suspend fun getLastInputs(): LastInputEntity? =
        calculationDao.getLastInputs()

    suspend fun saveLastInputs(lastInputs: LastInputEntity) =
        calculationDao.saveLastInputs(lastInputs)
}
