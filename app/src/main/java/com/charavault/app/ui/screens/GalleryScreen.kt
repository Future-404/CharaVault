package com.charavault.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
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
    pendingImportUris: List<Uri>,
    onImportClick: () -> Unit,
    onImportConfirmed: (List<Uri>, List<String>) -> Unit,
    onImportCancelled: () -> Unit,
    onExportSingleCardClick: (CardEntity) -> Unit,
    onExportAllZipClick: () -> Unit
) {
    val groupedCards by viewModel.groupedCards.collectAsState()
    val existingTags by viewModel.existingTags.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    var selectedCardForDetail by remember { mutableStateOf<CardEntity?>(null) }
    var showGithubAboutDialog by remember { mutableStateOf(false) }
    var showTopBarMenu by remember { mutableStateOf(false) }

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
                actions = {
                    // Unified Overflow Menu Button
                    Box {
                        IconButton(onClick = { showTopBarMenu = true }) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = "More Options",
                                tint = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showTopBarMenu,
                            onDismissRequest = { showTopBarMenu = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            // Submenu 1: Export All Zip Archive
                            DropdownMenuItem(
                                text = { Text("📦 备份全量 Zip 导出", fontSize = 13.sp, color = Color.White) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Filled.FolderZip,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = {
                                    showTopBarMenu = false
                                    onExportAllZipClick()
                                }
                            )

                            // Submenu 2: About & GitHub Repository
                            DropdownMenuItem(
                                text = { Text("🐙 关于与 GitHub 源码", fontSize = 13.sp, color = Color.White) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Filled.Code,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = {
                                    showTopBarMenu = false
                                    showGithubAboutDialog = true
                                }
                            )
                        }
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
                icon = { Icon(Icons.Filled.Add, contentDescription = "Batch Import") },
                text = { Text("批量导入") },
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
                            onCardClick = { card -> selectedCardForDetail = card },
                            onReorderCards = { reorderedIds -> viewModel.reorderCards(reorderedIds) }
                        )
                    }
                }
            }
        }

        // Batch Import Category Selection Modal
        if (pendingImportUris.isNotEmpty()) {
            BatchImportCategorySelectDialog(
                count = pendingImportUris.size,
                existingTags = existingTags,
                onConfirm = { selectedTags -> onImportConfirmed(pendingImportUris, selectedTags) },
                onDismiss = onImportCancelled
            )
        }

        // GitHub About Modal
        if (showGithubAboutDialog) {
            GithubAboutDialog(onDismiss = { showGithubAboutDialog = false })
        }

        // Detail Dialog Modal
        selectedCardForDetail?.let { card ->
            CardDetailDialog(
                card = card,
                existingTags = existingTags,
                onDismiss = { selectedCardForDetail = null },
                onExportSingleCard = {
                    onExportSingleCardClick(card)
                },
                onUpdateTags = { newTags ->
                    viewModel.updateCardTags(card.id, newTags)
                },
                onUpdateFullCard = { updatedV3 ->
                    viewModel.updateFullCardData(card.id, updatedV3)
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
fun BatchImportCategorySelectDialog(
    count: Int,
    existingTags: List<String>,
    onConfirm: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    val tagList = remember { mutableStateListOf<String>() }
    var customTagInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("批量导入角色卡 ($count 张)", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("即将进行合规检查并导入 $count 张角色卡，请选择或新建归属的分类货架：", fontSize = 13.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(10.dp))

                // Custom Tag Input Row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = customTagInput,
                        onValueChange = { customTagInput = it },
                        placeholder = { Text("新建分类名称...", fontSize = 12.sp) },
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

                // Display Existing User Categories for quick selection
                if (existingTags.isNotEmpty()) {
                    Text("快捷加入已有的分类：", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        existingTags.forEach { existing ->
                            val isSelected = tagList.contains(existing)
                            AssistChip(
                                onClick = {
                                    if (isSelected) tagList.remove(existing) else tagList.add(existing)
                                },
                                label = { Text((if (isSelected) "✓ " else "+ ") + existing, fontSize = 11.sp) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    labelColor = if (isSelected) Color.White else Color.White.copy(alpha = 0.85f)
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                if (tagList.isNotEmpty()) {
                    Text("当前指定分类: ${tagList.joinToString(", ")}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                } else {
                    Text("提示: 未选择时将自动提取卡片自带标签或归为'未分类'", fontSize = 11.sp, color = Color.Gray)
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(tagList.toList()) }) {
                Text("开始合规检查并导入")
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
fun GithubAboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Code, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("关于 CharaVault", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column {
                Text("CharaVault 🎴 是一款轻量、精致且高颜值的个人角色卡本地珍藏馆应用。", fontSize = 13.sp, color = Color.White.copy(alpha = 0.85f))
                Spacer(modifier = Modifier.height(12.dp))
                Text("开发者 GitHub 徽章：", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                        .clickable {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Future-404/CharaVault"))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "https://github.com/Future-404/CharaVault", Toast.LENGTH_LONG).show()
                            }
                        }
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Code, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Future-404 / CharaVault", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("点击访问 GitHub 仓库源码", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("关闭")
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
                text = "点击右下角按钮多选批量导入你的 PNG/V3 角色卡吧！",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}
