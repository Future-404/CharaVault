package com.charavault.app.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.charavault.app.data.local.CardEntity
import com.charavault.app.ui.components.CategoryRow
import com.charavault.app.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    viewModel: MainViewModel,
    pendingImportUri: Uri?,
    onImportClick: () -> Unit,
    onImportConfirmed: (Uri, List<String>) -> Unit,
    onImportCancelled: () -> Unit
) {
    val groupedCards by viewModel.groupedCards.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    var selectedCardForDetail by remember { mutableStateOf<CardEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "CharaVault 🎴",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "个人角色卡珍藏馆",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onImportClick,
                icon = { Icon(Icons.Filled.Add, contentDescription = "Import") },
                text = { Text("导入角色卡") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Input Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.searchQuery.value = it },
                placeholder = { Text("搜索角色卡名称、作者、性格...", fontSize = 13.sp, color = Color.Gray) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search", tint = Color.Gray) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            // Content Feed
            if (groupedCards.isEmpty()) {
                EmptyStateView(onImportClick = onImportClick)
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 88.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(groupedCards, key = { it.title }) { group ->
                        CategoryRow(
                            group = group,
                            onCardClick = { card -> selectedCardForDetail = card }
                        )
                    }
                }
            }
        }

        // Import Category Selection Modal
        pendingImportUri?.let { uri ->
            ImportCategorySelectDialog(
                uri = uri,
                onConfirm = { selectedTags -> onImportConfirmed(uri, selectedTags) },
                onDismiss = onImportCancelled
            )
        }

        // Detail Dialog Modal
        selectedCardForDetail?.let { card ->
            CardDetailDialog(
                card = card,
                onDismiss = { selectedCardForDetail = null },
                onFavoriteToggle = {
                    viewModel.toggleFavorite(card)
                    selectedCardForDetail = card.copy(isFavorite = !card.isFavorite)
                },
                onUpdateTags = { newTags ->
                    viewModel.updateCardTags(card.id, newTags)
                },
                onDelete = {
                    viewModel.deleteCard(card)
                    selectedCardForDetail = null
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ImportCategorySelectDialog(
    uri: Uri,
    onConfirm: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    val tagList = remember { mutableStateListOf<String>() }
    var customTagInput by remember { mutableStateOf("") }
    val presetCategories = listOf("赛博朋克", "奇幻魔法", "日常恋爱", "助手工具", "原神", "二次元", "特例系")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择导入分类", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("即将导入角色卡，请选择其归属的分类货架：", fontSize = 13.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(10.dp))

                // Custom Tag Input Row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = customTagInput,
                        onValueChange = { customTagInput = it },
                        placeholder = { Text("输入分类标签...", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (customTagInput.isNotBlank() && !tagList.contains(customTagInput.trim())) {
                                tagList.add(customTagInput.trim())
                                customTagInput = ""
                            }
                        }
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Add")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("快捷勾选预设分类：", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetCategories.forEach { preset ->
                        val isSelected = tagList.contains(preset)
                        AssistChip(
                            onClick = {
                                if (isSelected) tagList.remove(preset) else tagList.add(preset)
                            },
                            label = { Text((if (isSelected) "✓ " else "+ ") + preset, fontSize = 11.sp) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = if (isSelected) Color.White else Color.White.copy(alpha = 0.8f)
                            )
                        )
                    }
                }

                if (tagList.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("当前分配分类: ${tagList.joinToString(", ")}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                } else {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("提示: 未选择时将优先保留卡片自带标签或归为'未分类'", fontSize = 11.sp, color = Color.Gray)
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(tagList.toList()) }) {
                Text("确定导入")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun EmptyStateView(onImportClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Collections,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "还没有角色卡呢",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "点击右下角按钮导入你的第一张 PNG/V3 角色卡吧！",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}
