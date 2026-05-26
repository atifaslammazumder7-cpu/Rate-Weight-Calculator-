package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.CalculationEntity
import com.example.data.CalculationRepository
import com.example.data.LastInputEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CalculatorViewModel(
    application: Application,
    private val repository: CalculationRepository
) : AndroidViewModel(application) {

    // Tab Selection: "calculator", "history", "settings"
    private val _currentTab = MutableStateFlow("calculator")
    val currentTab: StateFlow<String> = _currentTab.asStateFlow()

    // Mode: "W_R_T" (Weight+Rate->Total), "T_R_W" (Total+Rate->Weight), "T_W_R" (Total+Weight->Rate)
    private val _reverseMode = MutableStateFlow("W_R_T")
    val reverseMode: StateFlow<String> = _reverseMode.asStateFlow()

    // Active Input Field: "weight", "rate", "total"
    private val _focusedField = MutableStateFlow("weight")
    val focusedField: StateFlow<String> = _focusedField.asStateFlow()

    // Inputs as String to preserve cursor and formatting
    private val _weightInput = MutableStateFlow("")
    val weightInput: StateFlow<String> = _weightInput.asStateFlow()

    private val _weightUnit = MutableStateFlow("kg") // "kg", "gram", "quintal", "ton"
    val weightUnit: StateFlow<String> = _weightUnit.asStateFlow()

    private val _rateInput = MutableStateFlow("")
    val rateInput: StateFlow<String> = _rateInput.asStateFlow()

    private val _rateType = MutableStateFlow("per kg") // "per kg", "per gram", "per quintal", "per ton", "custom"
    val rateType: StateFlow<String> = _rateType.asStateFlow()

    private val _totalInput = MutableStateFlow("")
    val totalInput: StateFlow<String> = _totalInput.asStateFlow()

    // Preferences
    private val _darkModePreference = MutableStateFlow("system") // "system", "dark", "light"
    val darkModePreference: StateFlow<String> = _darkModePreference.asStateFlow()

    private val _languagePreference = MutableStateFlow("en") // "en", "hi", "es", "bn"
    val languagePreference: StateFlow<String> = _languagePreference.asStateFlow()

    // History Flow
    val historyList: StateFlow<List<CalculationEntity>> = repository.allCalculations
        .stateInViewModel(emptyList())

    // UI Toast Messages or Single Events
    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    private val decimalFormat = DecimalFormat("#,##0.######")
    private val rawDecimalFormat = DecimalFormat("0.######")

    init {
        // Load last inputs
        viewModelScope.launch {
            val last = repository.lastInputsFlow.first()
            if (last != null) {
                _weightInput.value = last.weight
                _weightUnit.value = last.weightUnit
                _rateInput.value = last.rate
                _rateType.value = last.rateType
                _totalInput.value = last.totalAmount
                _reverseMode.value = last.reverseType
                _darkModePreference.value = last.darkModePreference
                _languagePreference.value = last.languagePreference
                
                // Set default focus based on mode
                _focusedField.value = when (last.reverseType) {
                    "W_R_T" -> "weight"
                    "T_R_W" -> "total"
                    else -> "total"
                }

                autoCalculate(isInit = true)
            } else {
                // Prepopulate standard defaults
                _weightUnit.value = "kg"
                _rateType.value = "per kg"
                _reverseMode.value = "W_R_T"
                _focusedField.value = "weight"
            }
        }
    }

    fun setTab(tab: String) {
        _currentTab.value = tab
    }

    fun setFocusedField(field: String) {
        _focusedField.value = field
    }

    fun setReverseMode(mode: String) {
        _reverseMode.value = mode
        // Set focus to the primary input of that mode
        when (mode) {
            "W_R_T" -> {
                _focusedField.value = "weight"
            }
            "T_R_W" -> {
                _focusedField.value = "total"
            }
            "T_W_R" -> {
                _focusedField.value = "total"
            }
        }
        autoCalculate()
        saveInputsState()
    }

    fun loadCalculation(entity: CalculationEntity) {
        _weightInput.value = entity.weight
        _weightUnit.value = entity.weightUnit
        _rateInput.value = entity.rate
        _rateType.value = entity.rateType
        _totalInput.value = entity.totalAmount
        _reverseMode.value = entity.reverseType
        _focusedField.value = when (entity.reverseType) {
            "W_R_T" -> "weight"
            "T_R_W" -> "total"
            else -> "total"
        }
        autoCalculate()
        saveInputsState()
    }

    fun setWeightUnit(unit: String) {
        _weightUnit.value = unit
        // Side-effect: change default rate type corresponding to unit to match most likely business scenario
        if (_reverseMode.value == "W_R_T" || _reverseMode.value == "T_R_W") {
            _rateType.value = when (unit) {
                "kg" -> "per kg"
                "gram" -> "per gram"
                "quintal" -> "per quintal"
                "ton" -> "per ton"
                else -> "per kg"
            }
        }
        autoCalculate()
        saveInputsState()
    }

    fun setRateType(type: String) {
        _rateType.value = type
        autoCalculate()
        saveInputsState()
    }

    fun updateDarkMode(preference: String) {
        _darkModePreference.value = preference
        saveInputsState()
    }

    fun updateLanguage(preference: String) {
        _languagePreference.value = preference
        saveInputsState()
    }

    // Keypad press handler
    fun onKeyPress(key: String) {
        val activeField = _focusedField.value
        val currentValue = when (activeField) {
            "weight" -> _weightInput.value
            "rate" -> _rateInput.value
            "total" -> _totalInput.value
            else -> ""
        }

        val newValue = when (key) {
            "DEL" -> {
                if (currentValue.isNotEmpty()) currentValue.dropLast(1) else ""
            }
            "C" -> {
                ""
            }
            "." -> {
                if (currentValue.contains(".")) currentValue 
                else if (currentValue.isEmpty()) "0." 
                else "$currentValue."
            }
            else -> {
                // key is a digit 0-9
                if (currentValue == "0") key else currentValue + key
            }
        }

        when (activeField) {
            "weight" -> _weightInput.value = newValue
            "rate" -> _rateInput.value = newValue
            "total" -> _totalInput.value = newValue
        }

        autoCalculate()
        saveInputsState()
    }

    // Clear all inputs in calculator screen
    fun clearAll() {
        _weightInput.value = ""
        _rateInput.value = ""
        _totalInput.value = ""
        
        // Reset focus
        _focusedField.value = when (_reverseMode.value) {
            "W_R_T" -> "weight"
            "T_R_W" -> "total"
            else -> "total"
        }
        saveInputsState()
    }

    private fun getWeightMultiplier(unit: String): Double {
        return when (unit) {
            "kg" -> 1.0
            "gram" -> 0.001
            "quintal" -> 100.0
            "ton" -> 1000.0
            else -> 1.0
        }
    }

    private fun getRateMultiplier(type: String): Double {
        return when (type) {
            "per kg" -> 1.0
            "per gram" -> 1000.0
            "per quintal" -> 0.01
            "per ton" -> 0.001
            "custom" -> 1.0
            else -> 1.0
        }
    }

    // Core business calculation
    private fun autoCalculate(isInit: Boolean = false) {
        val mode = _reverseMode.value
        val weightStr = _weightInput.value
        val rateStr = _rateInput.value
        val totalStr = _totalInput.value

        try {
            when (mode) {
                "W_R_T" -> {
                    // Calculate Total = Weight * Rate with conversions
                    val weightDouble = weightStr.toDoubleOrNull()
                    val rateDouble = rateStr.toDoubleOrNull()
                    if (weightDouble != null && rateDouble != null) {
                        val total = if (_rateType.value == "custom") {
                            weightDouble * rateDouble
                        } else {
                            val weightInKg = weightDouble * getWeightMultiplier(_weightUnit.value)
                            val rateInBase = rateDouble * getRateMultiplier(_rateType.value)
                            weightInKg * rateInBase
                        }
                        _totalInput.value = formatResult(total)
                    } else if (!isInit) {
                        _totalInput.value = ""
                    }
                }
                "T_R_W" -> {
                    // Calculate Weight = Total / Rate with conversions
                    val totalDouble = totalStr.toDoubleOrNull()
                    val rateDouble = rateStr.toDoubleOrNull()
                    if (totalDouble != null && rateDouble != null && rateDouble > 0) {
                        val weight = if (_rateType.value == "custom") {
                            totalDouble / rateDouble
                        } else {
                            val rateInBase = rateDouble * getRateMultiplier(_rateType.value)
                            val weightInKg = totalDouble / rateInBase
                            weightInKg / getWeightMultiplier(_weightUnit.value)
                        }
                        _weightInput.value = formatResult(weight)
                    } else if (!isInit) {
                        _weightInput.value = ""
                    }
                }
                "T_W_R" -> {
                    // Calculate Rate = Total / Weight with conversions
                    val totalDouble = totalStr.toDoubleOrNull()
                    val weightDouble = weightStr.toDoubleOrNull()
                    if (totalDouble != null && weightDouble != null && weightDouble > 0) {
                        val rate = if (_rateType.value == "custom") {
                            totalDouble / weightDouble
                        } else {
                            val weightInKg = weightDouble * getWeightMultiplier(_weightUnit.value)
                            val rateInBase = totalDouble / weightInKg
                            rateInBase / getRateMultiplier(_rateType.value)
                        }
                        _rateInput.value = formatResult(rate)
                    } else if (!isInit) {
                        _rateInput.value = ""
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore format exceptions in auto-calculation to avoid disruption
        }
    }

    private fun formatResult(value: Double): String {
        return if (value.isNaN() || value.isInfinite()) {
            ""
        } else {
            rawDecimalFormat.format(value)
        }
    }

    // Trigger explicit save to database history after validation
    fun executeCalculate(
        weightValidateMsg: String = "Please enter weight",
        rateValidateMsg: String = "Please enter rate",
        totalValidateMsg: String = "Please enter total amount"
    ) {
        val mode = _reverseMode.value
        val weightStr = _weightInput.value
        val rateStr = _rateInput.value
        val totalStr = _totalInput.value

        val weightVal = weightStr.toDoubleOrNull()
        val rateVal = rateStr.toDoubleOrNull()
        val totalVal = totalStr.toDoubleOrNull()

        // Validate
        when (mode) {
            "W_R_T" -> {
                if (weightVal == null) {
                    emitToast(weightValidateMsg)
                    return
                }
                if (rateVal == null) {
                    emitToast(rateValidateMsg)
                    return
                }
            }
            "T_R_W" -> {
                if (totalVal == null) {
                    emitToast(totalValidateMsg)
                    return
                }
                if (rateVal == null || rateVal <= 0) {
                    emitToast(rateValidateMsg)
                    return
                }
            }
            "T_W_R" -> {
                if (totalVal == null) {
                    emitToast(totalValidateMsg)
                    return
                }
                if (weightVal == null || weightVal <= 0) {
                    emitToast(weightValidateMsg)
                    return
                }
            }
        }

        // Complete final auto calculation just in case
        autoCalculate()

        // Re-read calculated values to commit
        val finalWeight = _weightInput.value
        val finalRate = _rateInput.value
        val finalTotal = _totalInput.value

        if (finalWeight.isNotEmpty() && finalRate.isNotEmpty() && finalTotal.isNotEmpty()) {
            viewModelScope.launch {
                val calculation = CalculationEntity(
                    weight = finalWeight,
                    weightUnit = _weightUnit.value,
                    rate = finalRate,
                    rateType = _rateType.value,
                    totalAmount = finalTotal,
                    reverseType = mode
                )
                repository.insertCalculation(calculation)
                emitToast("Calculation saved to History!")
            }
        }
    }

    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch {
            repository.deleteCalculationById(id)
            emitToast("Calculation deleted")
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearAllCalculations()
            emitToast("Entire history cleared")
        }
    }

    // Async state preservation helper
    private fun saveInputsState() {
        viewModelScope.launch {
            val lastState = LastInputEntity(
                weight = _weightInput.value,
                weightUnit = _weightUnit.value,
                rate = _rateInput.value,
                rateType = _rateType.value,
                totalAmount = _totalInput.value,
                reverseType = _reverseMode.value,
                darkModePreference = _darkModePreference.value,
                languagePreference = _languagePreference.value
            )
            repository.saveLastInputs(lastState)
        }
    }

    private fun emitToast(msg: String) {
        viewModelScope.launch {
            _toastEvent.emit(msg)
        }
    }

    // Export calculation history as a CSV file to download or share
    fun exportHistoryToCSV(fileDir: File): File? {
        val list = historyList.value
        if (list.isEmpty()) {
            emitToast("History is empty to export!")
            return null
        }

        return try {
            val fileName = "rate_weight_history_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(Date())}.csv"
            val file = File(fileDir, fileName)
            file.bufferedWriter().use { writer ->
                // Header (support international business format)
                writer.write("ID,Date,Weight,Unit,Rate,Rate Type,Total Amount,Calculation Type\n")
                
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                list.forEach { item ->
                    val dateStr = dateFormat.format(Date(item.timestamp))
                    val calcType = when (item.reverseType) {
                        "W_R_T" -> "Weight + Rate -> Total"
                        "T_R_W" -> "Total + Rate -> Weight"
                        else -> "Total + Weight -> Rate"
                    }
                    writer.write("${item.id},\"$dateStr\",${item.weight},${item.weightUnit},${item.rate},\"${item.rateType}\",${item.totalAmount},\"$calcType\"\n")
                }
            }
            emitToast("History exported to $fileName")
            file
        } catch (e: Exception) {
            emitToast("Export failed: ${e.localizedMessage}")
            null
        }
    }

    // Helper to turn Flow into StateFlow cleanly
    private fun <T> kotlinx.coroutines.flow.Flow<T>.stateInViewModel(initialValue: T): StateFlow<T> {
        return this.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = initialValue
        )
    }
}

class ViewModelFactory(
    private val application: Application,
    private val repository: CalculationRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CalculatorViewModel::class.java)) {
            return CalculatorViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
