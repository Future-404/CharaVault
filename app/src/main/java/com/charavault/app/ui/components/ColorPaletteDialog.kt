package com.charavault.app.ui.components

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.charavault.app.ui.theme.ThemeMode
import com.charavault.app.ui.theme.accessibleAccentColor
import com.charavault.app.ui.theme.accentContentColor

private val PresetColors = listOf(
    Pair("魅紫", Color(0xFF8B5CF6)),
    Pair("霓虹粉", Color(0xFFEC4899)),
    Pair("极光青", Color(0xFF06B6D4)),
    Pair("翡翠绿", Color(0xFF10B981)),
    Pair("琥珀橙", Color(0xFFF59E0B)),
    Pair("烈焰红", Color(0xFFEF4444)),
    Pair("靛蓝", Color(0xFF6366F1)),
    Pair("纯白", Color(0xFFFFFFFF)),
    Pair("深灰", Color(0xFF64748B)),
    Pair("纯黑", Color(0xFF000000))
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorPaletteDialog(
    currentAccentHex: String,
    currentThemeMode: ThemeMode,
    onDismiss: () -> Unit,
    onSaveTheme: (hex: String, mode: ThemeMode) -> Unit
) {
    var selectedColorHex by remember(currentAccentHex) { mutableStateOf(currentAccentHex) }
    var selectedThemeMode by remember(currentThemeMode) { mutableStateOf(currentThemeMode) }

    val initialHsv = FloatArray(3).apply {
        try {
            AndroidColor.colorToHSV(AndroidColor.parseColor(selectedColorHex), this)
        } catch (e: Exception) {
            AndroidColor.colorToHSV(AndroidColor.parseColor("#8B5CF6"), this)
        }
    }

    var hue by remember(currentAccentHex) { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember(currentAccentHex) { mutableFloatStateOf(initialHsv[1]) }
    var value by remember(currentAccentHex) { mutableFloatStateOf(initialHsv[2]) }

    fun getCalculatedColor(): Color {
        val hsv = floatArrayOf(hue, saturation, value)
        return Color(AndroidColor.HSVToColor(hsv))
    }

    fun getCalculatedHex(): String {
        val argb = getCalculatedColor().toArgb()
        return String.format("#%06X", 0xFFFFFF and argb)
    }

    val activeColor = getCalculatedColor()
    val systemInDark = isSystemInDarkTheme()
    val currentThemeIsDark = when (currentThemeMode) {
        ThemeMode.SYSTEM -> systemInDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val accessibleActiveColor = accessibleAccentColor(activeColor, currentThemeIsDark)
    val activeContentColor = accentContentColor(accessibleActiveColor)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Palette,
                    contentDescription = null,
                    tint = accessibleActiveColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("主题调色室 (Theme Studio)", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Section 0: Theme Mode Selection (System / Light / Dark)
                Text("外观模式 (Dark / Light)：", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ThemeMode.values().forEach { mode ->
                        val isSelected = selectedThemeMode == mode
                        AssistChip(
                            onClick = { selectedThemeMode = mode },
                            label = { Text(mode.label, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (isSelected) accessibleActiveColor else MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = if (isSelected) activeContentColor else MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section 1: Quick Presets Swatches
                Text("快捷预设色彩：", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    PresetColors.forEach { (name, color) ->
                        val isSelected = selectedColorHex.equals(String.format("#%06X", 0xFFFFFF and color.toArgb()), ignoreCase = true)
                        val isWhiteSwatch = color == Color.White
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) (if (isWhiteSwatch) accessibleActiveColor else Color.White) else (if (isWhiteSwatch) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.3f)),
                                    shape = CircleShape
                                )
                                .clickable {
                                    val hsv = FloatArray(3)
                                    AndroidColor.colorToHSV(color.toArgb(), hsv)
                                    hue = hsv[0]
                                    saturation = hsv[1]
                                    value = hsv[2]
                                    selectedColorHex = String.format("#%06X", 0xFFFFFF and color.toArgb())
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = name,
                                    tint = accentContentColor(color),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section 2: Full Spectrum Rainbow Hue Slider
                Text("全光谱万色盘 (0° ~ 360°)：", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(4.dp))
                
                val rainbowBrush = remember {
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red
                        )
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(rainbowBrush)
                )

                Slider(
                    value = hue,
                    onValueChange = {
                        hue = it
                        selectedColorHex = getCalculatedHex()
                    },
                    valueRange = 0f..360f,
                    colors = SliderDefaults.colors(
                        thumbColor = accessibleActiveColor,
                        activeTrackColor = Color.Transparent,
                        inactiveTrackColor = Color.Transparent
                    )
                )

                // Section 3: Saturation Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("饱和鲜艳度", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    Slider(
                        value = saturation,
                        onValueChange = {
                            saturation = it
                            selectedColorHex = getCalculatedHex()
                        },
                        valueRange = 0.1f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = accessibleActiveColor,
                            activeTrackColor = accessibleActiveColor,
                            inactiveTrackColor = accessibleActiveColor.copy(alpha = 0.25f)
                        ),
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                    )
                }

                // Section 4: Hex Code & Real-Time Preview
                Spacer(modifier = Modifier.height(6.dp))
                Text("HEX 颜色代码与效果预览：", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = selectedColorHex,
                        onValueChange = { input ->
                            selectedColorHex = input
                            try {
                                val parsed = AndroidColor.parseColor(input)
                                val hsv = FloatArray(3)
                                AndroidColor.colorToHSV(parsed, hsv)
                                hue = hsv[0]
                                saturation = hsv[1]
                                value = hsv[2]
                            } catch (e: Exception) {
                                // ignore invalid typing until valid hex
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accessibleActiveColor,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    IconButton(
                        onClick = {
                            hue = 258f
                            saturation = 0.62f
                            value = 0.96f
                            selectedColorHex = "#8B5CF6"
                        }
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = "重置默认", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Real-Time Mini UI Component Preview Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(activeColor)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("CharaVault 样式预览", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text("分类高亮 · 按钮 · 模式", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                        }

                        AssistChip(
                            onClick = { },
                            label = { Text("预览标签", fontSize = 11.sp) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = accessibleActiveColor,
                                labelColor = activeContentColor
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSaveTheme(selectedColorHex, selectedThemeMode) },
                colors = ButtonDefaults.buttonColors(containerColor = accessibleActiveColor)
            ) {
                Text("应用此配色与模式", color = activeContentColor)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = accessibleActiveColor)
            }
        }
    )
}
