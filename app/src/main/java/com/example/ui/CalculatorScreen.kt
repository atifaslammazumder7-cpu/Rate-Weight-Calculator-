package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.SleekIndigo
import com.example.ui.theme.DarkPrimary

@Composable
fun CalculatorScreen(
    viewModel: CalculatorViewModel,
    modifier: Modifier = Modifier,
    onShareClick: (String) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val reverseMode by viewModel.reverseMode.collectAsState()
    val focusedField by viewModel.focusedField.collectAsState()
    
    val weightInput by viewModel.weightInput.collectAsState()
    val weightUnit by viewModel.weightUnit.collectAsState()
    
    val rateInput by viewModel.rateInput.collectAsState()
    val rateType by viewModel.rateType.collectAsState()
    
    val totalInput by viewModel.totalInput.collectAsState()

    // Blinking cursor simulation
    val infiniteTransition = rememberInfiniteTransition(label = "cursor")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursorAlpha"
    )

    // Translation resource validations
    val weightValidation = stringResource(R.string.enter_weight)
    val rateValidation = stringResource(R.string.enter_rate)
    val totalValidation = stringResource(R.string.enter_total)
    val copiedTextToast = stringResource(R.string.copied_to_clipboard)

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // Branding and Title Panel
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("app_title")
            )
            Text(
                text = stringResource(R.string.tagline),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        // Segmented Control for Calculations Direction
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val modeList = listOf(
                    Triple("W_R_T", "W + R → T", "Weight × Rate"),
                    Triple("T_R_W", "T + R → W", "Total ÷ Rate"),
                    Triple("T_W_R", "T + W → R", "Total ÷ Weight")
                )
                
                modeList.forEach { (modeKey, label, formula) ->
                    val isSelected = reverseMode == modeKey
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.surface 
                                else Color.Transparent
                            )
                            .then(
                                if (isSelected) Modifier.border(
                                    BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)),
                                    RoundedCornerShape(12.dp)
                                ) else Modifier
                            )
                            .clickable { viewModel.setReverseMode(modeKey) }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                            Text(
                                text = formula,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 9.sp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            }
        }

        // Active Cards Stack
        // 1. Weight block
        val isWeightOutput = reverseMode == "T_R_W"
        val isWeightFocused = focusedField == "weight" && !isWeightOutput
        InputCard(
            title = stringResource(R.string.weight_label),
            value = if (weightInput.isEmpty() && isWeightOutput) "—" else weightInput,
            unit = weightUnit,
            isOutput = isWeightOutput,
            isFocused = isWeightFocused,
            cursorAlpha = alpha,
            hint = stringResource(R.string.enter_weight),
            onCardClick = { if (!isWeightOutput) viewModel.setFocusedField("weight") },
            unitContent = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("kg", "gram", "quintal", "ton").forEach { unit ->
                        val isSelected = weightUnit == unit
                        val translatedLabel = when (unit) {
                            "kg" -> stringResource(R.string.unit_kg)
                            "gram" -> stringResource(R.string.unit_gram)
                            "quintal" -> stringResource(R.string.unit_quintal)
                            "ton" -> stringResource(R.string.unit_ton)
                            else -> unit
                        }
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                    else Color.Transparent
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                    CircleShape
                                )
                                .clickable { viewModel.setWeightUnit(unit) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = translatedLabel.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                            )
                        }
                    }
                }
            }
        )

        // 2. Rate block
        val isRateOutput = reverseMode == "T_W_R"
        val isRateFocused = focusedField == "rate" && !isRateOutput
        InputCard(
            title = stringResource(R.string.rate_label),
            value = if (rateInput.isEmpty() && isRateOutput) "—" else rateInput,
            unit = rateType,
            isOutput = isRateOutput,
            isFocused = isRateFocused,
            cursorAlpha = alpha,
            hint = stringResource(R.string.enter_rate),
            onCardClick = { if (!isRateOutput) viewModel.setFocusedField("rate") },
            unitContent = {
                // Rate Pill Row (Single-line scrolling or flow)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val types = listOf("per kg", "per gram", "per quintal", "per ton", "custom")
                    types.forEach { type ->
                        val isSelected = rateType == type
                        val translatedLabel = when (type) {
                            "per kg" -> stringResource(R.string.rate_per_kg)
                            "per gram" -> stringResource(R.string.rate_per_gram)
                            "per quintal" -> stringResource(R.string.rate_per_quintal)
                            "per ton" -> stringResource(R.string.rate_per_ton)
                            "custom" -> stringResource(R.string.rate_custom)
                            else -> type
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                    else Color.Transparent
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                    CircleShape
                                )
                                .clickable { viewModel.setRateType(type) }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = translatedLabel.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                maxLines = 1,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                            )
                        }
                    }
                }
            }
        )

        // 3. Output result block of formula
        val isTotalOutput = reverseMode == "W_R_T"
        val isTotalFocused = focusedField == "total" && !isTotalOutput
        val activeTotalValue = if (totalInput.isEmpty() && isTotalOutput) "—" else totalInput
        
        InputCard(
            title = stringResource(R.string.total_label),
            value = activeTotalValue,
            unit = "",
            isOutput = isTotalOutput,
            isFocused = isTotalFocused,
            cursorAlpha = alpha,
            hint = stringResource(R.string.enter_total),
            onCardClick = { if (!isTotalOutput) viewModel.setFocusedField("total") },
            unitContent = {
                // Action tools: Copy & Share
                AnimatedVisibility (visible = activeTotalValue.isNotEmpty() && activeTotalValue != "—") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(activeTotalValue))
                                android.widget.Toast.makeText(context, copiedTextToast, android.widget.Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.testTag("copy_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = stringResource(R.string.copy),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(
                            onClick = {
                                val message = "Rate&Weight Calculation:\nWeight: $weightInput $weightUnit\nRate: $rateInput $rateType\nTotal: $totalInput"
                                onShareClick(message)
                            },
                            modifier = Modifier.testTag("share_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = stringResource(R.string.share),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        )

        // Action Toolbar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Clear All
            Button(
                onClick = { viewModel.clearAll() },
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
                    .testTag("clear_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                    contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                ),
                shape = CircleShape
            ) {
                Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.clear_all).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            // Big Calculate / Save Button (Fully rounded like Design FAB)
            Button(
                onClick = {
                    viewModel.executeCalculate(
                        weightValidateMsg = weightValidation,
                        rateValidateMsg = rateValidation,
                        totalValidateMsg = totalValidation
                    )
                },
                modifier = Modifier
                    .weight(1.5f)
                    .height(54.dp)
                    .testTag("calculate_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = CircleShape
            ) {
                Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.calculate).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }
        }

        // Custom Keypad Component
        CustomKeypad(
            onKeySelect = { key ->
                viewModel.onKeyPress(key)
            }
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun InputCard(
    title: String,
    value: String,
    unit: String,
    isOutput: Boolean,
    isFocused: Boolean,
    cursorAlpha: Float,
    hint: String,
    onCardClick: () -> Unit,
    unitContent: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(
                width = if (isFocused) 1.5.dp else 1.dp,
                color = when {
                    isFocused -> MaterialTheme.colorScheme.primary
                    isOutput -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                },
                shape = RoundedCornerShape(24.dp)
            )
            .clickable(enabled = !isOutput) { onCardClick() },
        colors = CardDefaults.cardColors(
            containerColor = when {
                isOutput -> MaterialTheme.colorScheme.primary.copy(alpha = 0.03f)
                isFocused -> MaterialTheme.colorScheme.surface
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isFocused) 2.dp else 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isOutput) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                    if (isOutput) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "RESULT",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 8.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
                
                if (unit.isNotEmpty() && !isOutput) {
                    Text(
                        text = unit.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 0.5.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Text Entry and Cursor Alignment
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (value.isEmpty() && !isOutput) {
                        Text(
                            text = hint,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Light,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f),
                            maxLines = 1
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = value,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = if (isOutput) FontWeight.Black else FontWeight.Light,
                            color = if (isOutput) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            fontFamily = FontFamily.SansSerif,
                            maxLines = 1,
                            letterSpacing = (-0.5).sp
                        )
                        if (isFocused) {
                            Box(
                                modifier = Modifier
                                    .alpha(cursorAlpha)
                                    .size(width = 2.dp, height = 32.dp)
                                    .background(MaterialTheme.colorScheme.primary)
                                    .padding(start = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            unitContent()
        }
    }
}

@Composable
fun CustomKeypad(
    onKeySelect: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val rows = listOf(
                listOf("7", "8", "9", "DEL"),
                listOf("4", "5", "6", "C"),
                listOf("1", "2", "3", "."),
                listOf("0")
            )

            rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { key ->
                        val isZero = key == "0"
                        val weight = if (isZero) 3f else 1f
                        
                        Box(
                            modifier = Modifier
                                .weight(weight)
                                .height(58.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    when (key) {
                                        "DEL" -> MaterialTheme.colorScheme.error.copy(alpha = 0.08f)
                                        "C" -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                                        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)
                                    }
                                )
                                .clickable { onKeySelect(key) }
                                .testTag("keypad_$key"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = key,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = when (key) {
                                    "DEL" -> MaterialTheme.colorScheme.error
                                    "C" -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                    
                    // If the row is the last row (containing "0" with weight 3), let's fill the extra empty space
                    if (row.size == 1 && row[0] == "0") {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}


