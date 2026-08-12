package com.charavault.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import com.charavault.app.data.parser.ExportFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.charavault.app.data.local.CardEntity
import com.charavault.app.data.model.CharacterBook
import com.charavault.app.data.model.CharacterCardV3
import com.charavault.app.data.parser.CardTokenStats
import com.charavault.app.data.parser.TokenEstimator
import com.charavault.app.ui.components.AccordionSection
import com.charavault.app.ui.components.LorebookEntryItem
import kotlinx.serialization.json.Json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CardDetailDialog(
    card: CardEntity,
    existingTags: List<String>,
    onDismiss: () -> Unit,
    onExportSingleCard: (ExportFormat) -> Unit,
    onUpdateTags: (List<String>) -> Unit,
    onUpdateAvatar: (Uri) -> Unit,
    onUpdateFullCard: (CharacterCardV3) -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val jsonParser = remember { Json { ignoreUnknownKeys = true; isLenient = true } }

    val avatarPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { onUpdateAvatar(it) }
    }

    val parsedCardV3 by produceState<CharacterCardV3?>(
        initialValue = null,
        key1 = card.rawJsonData
    ) {
        val rawJsonData = card.rawJsonData
        value = withContext(Dispatchers.Default) {
            runCatching { jsonParser.decodeFromString<CharacterCardV3>(rawJsonData) }.getOrNull()
        }
    }

    var currentCardV3 by remember(card.rawJsonData) { mutableStateOf<CharacterCardV3?>(null) }
    LaunchedEffect(parsedCardV3) {
        if (currentCardV3 == null) currentCardV3 = parsedCardV3
    }

    val initialTags = remember(card.tagsJson) {
        try { jsonParser.decodeFromString<List<String>>(card.tagsJson) } catch (e: Exception) { listOf("未分类") }
    }
    var currentTags by remember(card.tagsJson) { mutableStateOf(initialTags) }

    val name = currentCardV3?.data?.name ?: card.name
    val creator = currentCardV3?.data?.creator ?: card.creator
    val description = currentCardV3?.data?.description ?: card.description
    val personality = currentCardV3?.data?.personality ?: card.personality
    val scenario = currentCardV3?.data?.scenario ?: card.scenario
    val firstMes = currentCardV3?.data?.firstMes ?: card.firstMes
    val systemPrompt = currentCardV3?.data?.systemPrompt ?: card.systemPrompt
    val postHistory = currentCardV3?.data?.postHistoryInstructions
    val creatorNotes = currentCardV3?.data?.creatorNotes
    val alternateGreetings = currentCardV3?.data?.alternateGreetings ?: emptyList()
    val lorebookEntries = currentCardV3?.data?.characterBook?.entries ?: emptyList()

    val tokenStats by produceState<CardTokenStats?>(
        initialValue = null,
        key1 = currentCardV3
    ) {
        val cardV3 = currentCardV3 ?: return@produceState
        val cacheKey = "${card.id}:${cardV3.hashCode()}"
        value = withContext(Dispatchers.Default) {
            TokenEstimator.calculateCardStatsCached(cacheKey, cardV3)
        }
    }

    var showCategoryEditDialog by remember { mutableStateOf(false) }
    var showBasicInfoEditDialog by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showExportFormatDialog by remember { mutableStateOf(false) }
    var showAvatarOptionsBottomSheet by remember { mutableStateOf(false) }
    var showFullScreenAvatarViewer by remember { mutableStateOf(false) }
    var showTokenStatsDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var editingField by remember { mutableStateOf<EditableField?>(null) }
    var editingAlternateGreetingIndex by remember { mutableStateOf<Int?>(null) }
    var editingLorebookEntryIndex by remember { mutableStateOf<Int?>(null) }
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    val detailListState = rememberLazyListState()
    val scrollScope = rememberCoroutineScope()
    val tabHeaderItemIndex = 1

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                val tabContentMinHeight = maxHeight - 96.dp
                LazyColumn(
                    state = detailListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 48.dp)
                ) {
                    item(key = "card-summary") {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            // Option 3: Centered Streamlined Circular Avatar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp, bottom = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                        Box(
                            modifier = Modifier
                                .size(130.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                .clickable { showAvatarOptionsBottomSheet = true }
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(File(card.imagePath))
                                    .diskCacheKey("${card.id}_${card.fileHash}_${card.updatedAt}")
                                    .memoryCacheKey("${card.id}_${card.fileHash}_${card.updatedAt}")
                                    .crossfade(false)
                                    .build(),
                                contentDescription = card.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    // Centered Name and Creator Header (Clickable to edit)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showBasicInfoEditDialog = true }
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = name,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "作者: @${creator.ifBlank { "匿名" }}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Category Tags Bar (Removed separate TextButton, click flow layout below to edit)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Label,
                            contentDescription = "Category",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "分类标签:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showCategoryEditDialog = true }
                            .padding(vertical = 4.dp)
                    ) {
                        currentTags.forEach { tag ->
                            AssistChip(
                                onClick = { showCategoryEditDialog = true },
                                label = { Text("#$tag", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    labelColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                        if (currentTags.isEmpty() || (currentTags.size == 1 && currentTags.first() == "未分类")) {
                            AssistChip(
                                onClick = { showCategoryEditDialog = true },
                                label = { Text("+ 点击添加分类", fontSize = 12.sp) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    labelColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Token Summary Card (Pure plain text)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showTokenStatsDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tokenStats?.let { "Token 统计: 约 ${it.totalTokens} Tokens" }
                                        ?: "Token 统计: 计算中...",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = tokenStats?.let {
                                        "人设 ${it.profileTokens} · 开场 ${it.greetingTokens} · 高级 ${it.advancedTokens} · 世界书 ${it.lorebookTokens}"
                                    } ?: "正在后台分析卡片内容",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "查看占比",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                            Spacer(modifier = Modifier.height(14.dp))
                        }
                    }

                    stickyHeader(key = "detail-tabs") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(horizontal = 16.dp)
                        ) {
                            TabRow(
                                selectedTabIndex = selectedTab,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.primary
                            ) {
                                listOf("角色设定", "开场与对话", "世界书与高级").forEachIndexed { index, title ->
                                    Tab(
                                        selected = selectedTab == index,
                                        onClick = {
                                            val tabsArePinned = detailListState.firstVisibleItemIndex >= tabHeaderItemIndex
                                            selectedTab = index
                                            if (tabsArePinned) {
                                                scrollScope.launch {
                                                    detailListState.animateScrollToItem(tabHeaderItemIndex)
                                                }
                                            }
                                        },
                                        text = {
                                            Text(
                                                text = title,
                                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 13.sp
                                            )
                                        },
                                        selectedContentColor = MaterialTheme.colorScheme.primary,
                                        unselectedContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }

                    item(key = "tab-content") {
                        Column(
                            modifier = Modifier
                                .heightIn(min = tabContentMinHeight)
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {

                    if (selectedTab == 0) {
                        // 1. Tab 0: 角色设定 (Profile) - Flat Layout, No Nested Accordions
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            var hasProfileContent = false

                            if (description.isNotBlank()) {
                                hasProfileContent = true
                                SubTitleText("背景与描述") { editingField = EditableField.DESCRIPTION }
                                ExpandableContentBox(text = description)
                                Spacer(modifier = Modifier.height(14.dp))
                            }

                            if (personality.isNotBlank()) {
                                hasProfileContent = true
                                SubTitleText("性格特征") { editingField = EditableField.PERSONALITY }
                                ExpandableContentBox(text = personality)
                                Spacer(modifier = Modifier.height(14.dp))
                            }

                            if (scenario.isNotBlank()) {
                                hasProfileContent = true
                                SubTitleText("对话场景") { editingField = EditableField.SCENARIO }
                                ExpandableContentBox(text = scenario)
                                Spacer(modifier = Modifier.height(14.dp))
                            }

                            if (!hasProfileContent) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    TextButton(onClick = { editingField = EditableField.DESCRIPTION }) {
                                        Text("+ 添加背景与描述", fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }

                    if (selectedTab == 1) {
                        // 2. Tab 1: 开场与对话 - Flat Layout
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            if (firstMes.isNotBlank()) {
                                GreetingCardItem(label = "默认开场白", content = firstMes) {
                                    editingField = EditableField.FIRST_MESSAGE
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            alternateGreetings.forEachIndexed { index, greeting ->
                                GreetingCardItem(
                                    label = "备选欢迎语 #${index + 1}",
                                    content = greeting,
                                    onEdit = { editingAlternateGreetingIndex = index }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            if (firstMes.isBlank() && alternateGreetings.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("暂无开场白与欢迎语", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                }
                            }
                        }
                    }

                    if (selectedTab == 2) {
                        // 3. Tab 2: 世界书与高级 - Flat Layout
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            if (lorebookEntries.isNotEmpty()) {
                                SubTitleText("世界书 · 共 ${lorebookEntries.size} 个词条")
                                Spacer(modifier = Modifier.height(6.dp))
                                lorebookEntries.forEachIndexed { index, entry ->
                                    LorebookEntryItem(
                                        entry = entry,
                                        index = index,
                                        onEdit = { editingLorebookEntryIndex = index }
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                            }

                            val hasSystemPrompts = systemPrompt.isNotBlank() ||
                                    postHistory?.isNotBlank() == true ||
                                    creatorNotes?.isNotBlank() == true

                            if (hasSystemPrompts) {
                                SubTitleText("系统提示词与高级指令")
                                Spacer(modifier = Modifier.height(6.dp))

                                if (systemPrompt.isNotBlank()) {
                                    Text("System Prompt", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    ExpandableContentBox(text = systemPrompt)
                                    Spacer(modifier = Modifier.height(10.dp))
                                }

                                if (!postHistory.isNullOrBlank()) {
                                    Text("Post History Instructions", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    ExpandableContentBox(text = postHistory)
                                    Spacer(modifier = Modifier.height(10.dp))
                                }

                                if (!creatorNotes.isNullOrBlank()) {
                                    Text("作者留言", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    ExpandableContentBox(text = creatorNotes)
                                }
                            }

                            if (lorebookEntries.isEmpty() && !hasSystemPrompts) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("暂无世界书与高级指令", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Subtle GitHub Badge at bottom
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Future-404/CharaVault"))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "https://github.com/Future-404/CharaVault", Toast.LENGTH_SHORT).show()
                                }
                            },
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Code, contentDescription = "GitHub", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Future-404 / CharaVault",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                        }
                    }
                }

                // Top FullScreen Navigation Bar (Back Arrow & More Overflow Menu)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Box {
                        IconButton(onClick = { showMoreMenu = true }) {
                            Icon(
                                Icons.Filled.MoreVert,
                                contentDescription = "更多操作",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("导出角色卡") },
                                onClick = {
                                    showMoreMenu = false
                                    showExportFormatDialog = true
                                },
                                leadingIcon = { Icon(Icons.Filled.FileDownload, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("删除角色卡") },
                                onClick = {
                                    showMoreMenu = false
                                    showDeleteConfirmation = true
                                },
                                leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Export Format Select Dialog (PNG vs JSON)
    if (showExportFormatDialog) {
        ExportFormatSelectDialog(
            cardName = name,
            onDismiss = { showExportFormatDialog = false },
            onSelectFormat = { format ->
                showExportFormatDialog = false
                onExportSingleCard(format)
            }
        )
    }

    // Avatar Options Bottom Sheet (Change Avatar vs View Full Image)
    if (showAvatarOptionsBottomSheet) {
        Dialog(
            onDismissRequest = { showAvatarOptionsBottomSheet = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showAvatarOptionsBottomSheet = false }
            ) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { /* Keep taps inside the panel from dismissing it. */ },
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        // Item 1: 更换角色贴图
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showAvatarOptionsBottomSheet = false
                                    avatarPickerLauncher.launch("image/*")
                                }
                                .padding(horizontal = 24.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Image,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "更换角色贴图",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Item 2: 查看高清大图
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showAvatarOptionsBottomSheet = false
                                    showFullScreenAvatarViewer = true
                                }
                                .padding(horizontal = 24.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "查看高清大图",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }

    // FullScreen Avatar Viewer Modal
    if (showFullScreenAvatarViewer) {
        Dialog(
            onDismissRequest = { showFullScreenAvatarViewer = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.92f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        showFullScreenAvatarViewer = false
                    },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(File(card.imagePath))
                        .diskCacheKey("${card.id}_${card.fileHash}_${card.updatedAt}")
                        .memoryCacheKey("${card.id}_${card.fileHash}_${card.updatedAt}")
                        .build(),
                    contentDescription = card.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                )

                IconButton(
                    onClick = { showFullScreenAvatarViewer = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(16.dp)
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "关闭",
                        tint = Color.White
                    )
                }
            }
        }
    }

    // Token Statistics Breakdown Modal
    if (showTokenStatsDialog) {
        TokenStatsDialog(
            stats = tokenStats,
            onDismiss = { showTokenStatsDialog = false }
        )
    }

    // Category Edit Dialog
    if (showCategoryEditDialog) {
        CategoryEditDialog(
            currentTags = currentTags,
            existingTags = existingTags,
            onDismiss = { showCategoryEditDialog = false },
            onSave = { newTags ->
                val effectiveTags = if (newTags.isEmpty()) listOf("未分类") else newTags
                currentTags = effectiveTags
                currentCardV3?.let { original ->
                    val updatedData = original.data.copy(tags = effectiveTags)
                    currentCardV3 = original.copy(data = updatedData)
                }
                onUpdateTags(effectiveTags)
                showCategoryEditDialog = false
            }
        )
    }

    if (showBasicInfoEditDialog) {
        BasicInfoEditorDialog(
            initialName = name,
            initialCreator = creator,
            onDismiss = { showBasicInfoEditDialog = false },
            onSave = { newName, newCreator ->
                currentCardV3?.let { original ->
                    val updatedData = original.data.copy(name = newName, creator = newCreator)
                    val newV3 = original.copy(data = updatedData)
                    currentCardV3 = newV3
                    onUpdateFullCard(newV3)
                }
                showBasicInfoEditDialog = false
            }
        )
    }

    editingAlternateGreetingIndex?.let { index ->
        val initialText = alternateGreetings.getOrNull(index) ?: ""
        FullScreenTextEditorDialog(
            title = "编辑备选欢迎语 #${index + 1}",
            initialValue = initialText,
            onDismiss = { editingAlternateGreetingIndex = null },
            onSave = { newText ->
                currentCardV3?.let { original ->
                    val list = original.data.alternateGreetings.toMutableList()
                    if (index in list.indices) {
                        list[index] = newText
                    }
                    val updatedData = original.data.copy(alternateGreetings = list)
                    val newV3 = original.copy(data = updatedData)
                    currentCardV3 = newV3
                    onUpdateFullCard(newV3)
                }
                editingAlternateGreetingIndex = null
            }
        )
    }

    editingLorebookEntryIndex?.let { index ->
        val entry = lorebookEntries.getOrNull(index)
        if (entry != null) {
            FullScreenLorebookEntryEditorDialog(
                entryIndex = index,
                entry = entry,
                onDismiss = { editingLorebookEntryIndex = null },
                onSave = { comment, keysList, secondaryKeysList, content, enabled, insertionOrder, position, useRegex, constant, selective ->
                    currentCardV3?.let { original ->
                        val updatedEntry = entry.copy(
                            comment = comment,
                            keys = keysList,
                            secondaryKeys = secondaryKeysList,
                            content = content,
                            enabled = enabled,
                            insertionOrder = insertionOrder,
                            position = position,
                            useRegex = useRegex,
                            constant = constant,
                            selective = selective
                        )
                        val book = original.data.characterBook ?: CharacterBook()
                        val updatedEntries = book.entries.toMutableList()
                        if (index in updatedEntries.indices) {
                            updatedEntries[index] = updatedEntry
                        }
                        val updatedBook = book.copy(entries = updatedEntries)
                        val updatedData = original.data.copy(characterBook = updatedBook)
                        val newV3 = original.copy(data = updatedData)
                        currentCardV3 = newV3
                        onUpdateFullCard(newV3)
                    }
                    editingLorebookEntryIndex = null
                }
            )
        }
    }

    editingField?.let { field ->
        val value = when (field) {
            EditableField.NAME -> name
            EditableField.CREATOR -> creator
            EditableField.DESCRIPTION -> description
            EditableField.PERSONALITY -> personality
            EditableField.SCENARIO -> scenario
            EditableField.FIRST_MESSAGE -> firstMes
        }
        FullScreenTextEditorDialog(
            title = "编辑${field.label}",
            initialValue = value,
            onDismiss = { editingField = null },
            onSave = { updatedValue ->
                currentCardV3?.let { original ->
                    val updatedData = when (field) {
                        EditableField.NAME -> original.data.copy(name = updatedValue)
                        EditableField.CREATOR -> original.data.copy(creator = updatedValue)
                        EditableField.DESCRIPTION -> original.data.copy(description = updatedValue)
                        EditableField.PERSONALITY -> original.data.copy(personality = updatedValue)
                        EditableField.SCENARIO -> original.data.copy(scenario = updatedValue)
                        EditableField.FIRST_MESSAGE -> original.data.copy(firstMes = updatedValue)
                    }
                    val newV3 = original.copy(data = updatedData)
                    currentCardV3 = newV3
                    onUpdateFullCard(newV3)
                }
                editingField = null
            }
        )
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("删除角色卡？") },
            text = { Text("删除后将同时移除应用内保存的 PNG 文件，此操作无法撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirmation = false
                    onDelete()
                }) { Text("删除", color = Color(0xFFEF4444)) }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirmation = false }) { Text("取消") } }
        )
    }
}

@Composable
private fun FullScreenTextEditorDialog(
    title: String,
    initialValue: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var text by remember(title, initialValue) { mutableStateOf(initialValue) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // Top Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "取消编辑",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Button(
                        onClick = { onSave(text) },
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("保存", fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Full Screen Text Input Field
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        placeholder = { Text("请输入内容...", fontSize = 14.sp, color = Color.Gray) },
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }

                // Character Counter
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "共 ${text.length} 字",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FullScreenLorebookEntryEditorDialog(
    entryIndex: Int,
    entry: com.charavault.app.data.model.CharacterBookEntry,
    onDismiss: () -> Unit,
    onSave: (
        comment: String,
        keys: List<String>,
        secondaryKeys: List<String>,
        content: String,
        enabled: Boolean,
        insertionOrder: Int,
        position: String,
        useRegex: Boolean,
        constant: Boolean,
        selective: Boolean
    ) -> Unit
) {
    var comment by remember(entryIndex, entry.comment) { mutableStateOf(entry.comment) }
    var keysText by remember(entryIndex, entry.keys) { mutableStateOf(entry.keys.joinToString(", ")) }
    var secondaryKeysText by remember(entryIndex, entry.secondaryKeys) { mutableStateOf(entry.secondaryKeys.joinToString(", ")) }
    var content by remember(entryIndex, entry.content) { mutableStateOf(entry.content) }
    var enabled by remember(entryIndex, entry.enabled) { mutableStateOf(entry.enabled) }

    var insertionOrderText by remember(entryIndex, entry.insertionOrder) { mutableStateOf(entry.insertionOrder.toString()) }
    var position by remember(entryIndex, entry.position) { mutableStateOf(entry.position) }
    var useRegex by remember(entryIndex, entry.useRegex) { mutableStateOf(entry.useRegex) }
    var constant by remember(entryIndex, entry.constant) { mutableStateOf(entry.constant) }
    var selective by remember(entryIndex, entry.selective) { mutableStateOf(entry.selective) }

    var showAdvancedRules by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // Top Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "取消编辑",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "编辑世界书词条 #${entryIndex + 1}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Button(
                        onClick = {
                            val keysList = keysText.split(",", "，")
                                .map { it.trim() }
                                .filter { it.isNotBlank() }
                            val secKeysList = secondaryKeysText.split(",", "，")
                                .map { it.trim() }
                                .filter { it.isNotBlank() }
                            val order = insertionOrderText.toIntOrNull() ?: 100
                            onSave(comment, keysList, secKeysList, content, enabled, order, position, useRegex, constant, selective)
                        },
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("保存", fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Main Layout with vertical scroll
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Enable Switch Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("启用此词条", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        Switch(checked = enabled, onCheckedChange = { enabled = it })
                    }

                    OutlinedTextField(
                        value = comment,
                        onValueChange = { comment = it },
                        label = { Text("词条名称 / 备注") },
                        placeholder = { Text("如：红月祭典、好感度面板...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    OutlinedTextField(
                        value = keysText,
                        onValueChange = { keysText = it },
                        label = { Text(if (useRegex) "触发关键词 (当前为正则表达式，用逗号分隔)" else "触发关键词 (用逗号分隔)") },
                        placeholder = { Text(if (useRegex) "如：/(红月|血月)/i, /好感度\\s*\\d+/" else "如：红月, 祭典, 传说") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("词条详细内容", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                            Text("共 ${content.length} 字", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }

                        OutlinedTextField(
                            value = content,
                            onValueChange = { content = it },
                            placeholder = { Text("请输入词条正文设定...") },
                            modifier = Modifier.fillMaxSize(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }

                    // Collapsible Advanced Rules Toggle Button (Pure Plain Text)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAdvancedRules = !showAdvancedRules }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (showAdvancedRules) "收起高级匹配参数 ▲" else "展开高级匹配参数 ▼",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Collapsible Advanced Rules Section
                    AnimatedVisibility(visible = showAdvancedRules) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text("插入位置", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            
                            // Position Segmented Chips Row
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val positions = listOf(
                                    "before_char" to "角色前",
                                    "after_char" to "角色后",
                                    "an_top" to "附言顶",
                                    "an_bottom" to "附言底"
                                )
                                positions.forEach { (posValue, posLabel) ->
                                    val isSelected = position == posValue
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable { position = posValue }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = posLabel,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = insertionOrderText,
                                onValueChange = { insertionOrderText = it },
                                label = { Text("插入优先级 (默认 100)") },
                                placeholder = { Text("100") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                )
                            )

                            // Use Regex Switch
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("正则匹配", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                    Text("开启后，“触发关键词”框内的文本将直接作为正则表达式解析", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                }
                                Switch(checked = useRegex, onCheckedChange = { useRegex = it })
                            }

                            // Constant Switch
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("常驻注入", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                    Text("无需关键词触发，始终注入到上下文", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                }
                                Switch(checked = constant, onCheckedChange = { constant = it })
                            }

                            // Selective Switch
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("选择性触发", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                    Text("要求同时匹配主关键词与次要关键词", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                }
                                Switch(checked = selective, onCheckedChange = { selective = it })
                            }

                            if (selective) {
                                OutlinedTextField(
                                    value = secondaryKeysText,
                                    onValueChange = { secondaryKeysText = it },
                                    label = { Text("次要触发词 (用逗号分隔)") },
                                    placeholder = { Text("如：好感度 > 50") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = Color.Transparent,
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

private enum class EditableField(val label: String, val multiline: Boolean) {
    NAME("角色名称", false),
    CREATOR("作者", false),
    DESCRIPTION("背景与描述", true),
    PERSONALITY("性格特征", true),
    SCENARIO("对话场景", true),
    FIRST_MESSAGE("默认开场白", true)
}

@Composable
private fun BasicInfoEditorDialog(
    initialName: String,
    initialCreator: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    var creator by remember(initialCreator) { mutableStateOf(initialCreator) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑基本信息") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("角色名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = creator,
                    onValueChange = { creator = it },
                    label = { Text("作者名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSave(name, creator) }) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}



@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryEditDialog(
    currentTags: List<String>,
    existingTags: List<String>,
    onDismiss: () -> Unit,
    onSave: (List<String>) -> Unit
) {
    val tagList = remember { mutableStateListOf<String>().apply { addAll(currentTags.filter { it != "未分类" }) } }
    var newTagInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑角色卡分类标签", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("输入自定义分类名称，或快捷勾选已有的分类：", fontSize = 13.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(10.dp))

                // Custom Input Box
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newTagInput,
                        onValueChange = { newTagInput = it },
                        placeholder = { Text("输入新分类名称...", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (newTagInput.isNotBlank() && !tagList.contains(newTagInput.trim())) {
                                tagList.add(newTagInput.trim())
                                newTagInput = ""
                            }
                        }
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Add")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Current Selected Tags
                Text("已选分类：", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                if (tagList.isEmpty()) {
                    Text("暂未选择分类（保存后将归为'未分类'）", fontSize = 11.sp, color = Color.Gray)
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        tagList.forEach { tag ->
                            AssistChip(
                                onClick = { tagList.remove(tag) },
                                label = { Text("$tag ✕", fontSize = 12.sp) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    labelColor = Color.White
                                )
                            )
                        }
                    }
                }

                // Dynamic Existing User Categories
                if (existingTags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("快捷加入已有的分类：", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
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
                                    labelColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(tagList.toList()) }) {
                Text("保存分类")
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
fun GreetingCardItem(label: String, content: String, onEdit: (() -> Unit)? = null) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(content.length < 300) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    onEdit?.let {
                        IconButton(onClick = it, modifier = Modifier.size(28.dp)) {
                            Icon(
                                Icons.Filled.Edit,
                                contentDescription = "编辑$label",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText(label, content))
                            Toast.makeText(context, "已复制 $label 到剪贴板", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Filled.ContentCopy,
                            contentDescription = "Copy",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            val displayText = if (isExpanded || content.length < 300) content else content.take(300) + "..."
            Text(
                text = displayText,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                lineHeight = 19.sp
            )

            if (content.length >= 300) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isExpanded) "▲ 收起全文" else "▼ 展开全文 (共 ${content.length} 字)",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { isExpanded = !isExpanded }
                        .padding(vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun SubTitleText(title: String, onEdit: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        onEdit?.let {
            IconButton(onClick = it) {
                Icon(Icons.Filled.Edit, contentDescription = "编辑$title", modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun ExpandableContentBox(text: String) {
    var isExpanded by remember { mutableStateOf(text.length < 500) }
    val displayText = if (isExpanded || text.length < 500) text else text.take(500) + "..."

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Column {
            Text(
                text = displayText,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                lineHeight = 19.sp
            )
            if (text.length >= 500) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (isExpanded) "▲ 收起全文" else "▼ 展开全文 (共 ${text.length} 字)",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { isExpanded = !isExpanded }
                        .padding(vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun ExportFormatSelectDialog(
    cardName: String,
    onDismiss: () -> Unit,
    onSelectFormat: (ExportFormat) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.FileDownload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("选择导出格式", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "即将导出角色卡 [$cardName]，请选择保存格式：",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )

                // Option 1: PNG Format
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectFormat(ExportFormat.PNG) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Image, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("1. 导出 PNG 图片格式", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text("嵌入完整人设及世界书 Chunk 元数据", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                    }
                }

                // Option 2: JSON Format
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectFormat(ExportFormat.JSON) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Code, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("2. 导出 JSON 数据格式", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text("SillyTavern / V3 独立文本规范数据", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            }
        }
    )
}

@Composable
private fun TokenStatsDialog(
    stats: CardTokenStats?,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Token 消耗分析仪表盘", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(2.dp))
                Text("分词引擎: Tiktoken (cl100k_base / GPT-4 / Claude)", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
            }
        },
        text = {
            if (stats == null) {
                Text("正在后台分析卡片内容，请稍候…", fontSize = 13.sp)
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val pct4k = (stats.totalTokens * 100 / 4096).coerceIn(0, 100)

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            TokenStatRow("基础人设 (背景/性格/场景)", stats.profileTokens, stats.totalTokens)
                            TokenStatRow("开场对话 (默认/备选欢迎语)", stats.greetingTokens, stats.totalTokens)
                            TokenStatRow("高级指令 (系统提示词/留言)", stats.advancedTokens, stats.totalTokens)
                            TokenStatRow("世界书词条 (${stats.enabledLorebookCount}/${stats.totalLorebookCount} 已启用)", stats.lorebookTokens, stats.totalTokens)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("全卡总计 Token 消耗", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text("${stats.totalTokens} Tokens", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }

                    Text(
                        text = "说明：大约占用 4K 上下文窗口的 ${pct4k}% 空间。未启用的世界书词条不计入消耗。",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("确定", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun TokenStatRow(label: String, tokens: Int, total: Int) {
    val pct = if (total > 0) (tokens * 100 / total) else 0
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
        }
        Text(
            text = "$tokens ($pct%)",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
        )
    }
}
