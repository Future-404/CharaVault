package com.charavault.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.charavault.app.data.model.CharacterCardV3
import com.charavault.app.ui.components.AccordionSection
import com.charavault.app.ui.components.LorebookEntryItem
import com.charavault.app.ui.theme.GoldStar
import kotlinx.serialization.json.Json
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CardDetailDialog(
    card: CardEntity,
    onDismiss: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val jsonParser = remember { Json { ignoreUnknownKeys = true; isLenient = true } }

    // Parse V3 Full JSON
    val cardV3: CharacterCardV3? = remember(card.rawJsonData) {
        try { jsonParser.decodeFromString<CharacterCardV3>(card.rawJsonData) } catch (e: Exception) { null }
    }

    val tags = try { jsonParser.decodeFromString<List<String>>(card.tagsJson) } catch (e: Exception) { emptyList() }
    val alternateGreetings = cardV3?.data?.alternateGreetings ?: emptyList()
    val lorebookEntries = cardV3?.data?.characterBook?.entries ?: emptyList()

    // Accordion Expansion States
    var expandGreetings by remember { mutableStateOf(true) }
    var expandProfile by remember { mutableStateOf(true) }
    var expandLorebook by remember { mutableStateOf(lorebookEntries.size <= 5) }
    var expandSystem by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    // Top Hero Banner & Image
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .clip(RoundedCornerShape(20.dp))
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(File(card.imagePath))
                                .crossfade(true)
                                .build(),
                            contentDescription = card.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Character Name & Author
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = card.name,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "作者: @${card.creator.ifBlank { "匿名" }}",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        IconButton(onClick = onFavoriteToggle) {
                            Icon(
                                imageVector = if (card.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                contentDescription = "Favorite",
                                tint = if (card.isFavorite) GoldStar else Color.Gray,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }

                    // Tags FlowRow
                    if (tags.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            tags.forEach { tag ->
                                AssistChip(
                                    onClick = { },
                                    label = { Text("#$tag", fontSize = 11.sp) },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Global Collapse / Expand All Quick Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                expandGreetings = true
                                expandProfile = true
                                expandLorebook = true
                                expandSystem = true
                            }
                        ) {
                            Text("📂 全部展开", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        }
                        TextButton(
                            onClick = {
                                expandGreetings = false
                                expandProfile = false
                                expandLorebook = false
                                expandSystem = false
                            }
                        ) {
                            Text("📁 全部收起", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                        }
                    }

                    // 1. Accordion Section: 💬 Greetings & Alternate Greetings
                    val totalGreetingsCount = 1 + alternateGreetings.size
                    AccordionSection(
                        title = "💬 欢迎语与开场白",
                        icon = Icons.Filled.Chat,
                        isExpanded = expandGreetings,
                        onToggle = { expandGreetings = !expandGreetings },
                        countBadge = "$totalGreetingsCount 条"
                    ) {
                        Column {
                            // Default First Message
                            if (card.firstMes.isNotBlank()) {
                                GreetingCardItem(
                                    label = "📍 默认开场白",
                                    content = card.firstMes
                                )
                            }

                            // Alternate Greetings
                            alternateGreetings.forEachIndexed { index, greeting ->
                                GreetingCardItem(
                                    label = "📍 备选欢迎语 #${index + 1}",
                                    content = greeting
                                )
                            }
                        }
                    }

                    // 2. Accordion Section: 📝 Profile / Description & Personality
                    AccordionSection(
                        title = "📝 角色设定与背景",
                        icon = Icons.Filled.Person,
                        isExpanded = expandProfile,
                        onToggle = { expandProfile = !expandProfile }
                    ) {
                        Column {
                            if (card.description.isNotBlank()) {
                                SubTitleText("背景与描述 (Description)")
                                ExpandableContentBox(text = card.description)
                            }
                            if (card.personality.isNotBlank()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                SubTitleText("性格特征 (Personality)")
                                ExpandableContentBox(text = card.personality)
                            }
                            if (card.scenario.isNotBlank()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                SubTitleText("对话场景 (Scenario)")
                                ExpandableContentBox(text = card.scenario)
                            }
                        }
                    }

                    // 3. Accordion Section: 📚 Character Book / Lorebook (Secondary expansion)
                    if (lorebookEntries.isNotEmpty()) {
                        AccordionSection(
                            title = "📚 世界书 (Lorebook)",
                            icon = Icons.Filled.Book,
                            isExpanded = expandLorebook,
                            onToggle = { expandLorebook = !expandLorebook },
                            countBadge = "${lorebookEntries.size} 个词条"
                        ) {
                            Column {
                                lorebookEntries.forEachIndexed { index, entry ->
                                    LorebookEntryItem(entry = entry, index = index)
                                }
                            }
                        }
                    }

                    // 4. Accordion Section: ⚙️ System Prompts & Instructions
                    if (card.systemPrompt.isNotBlank() || cardV3?.data?.postHistoryInstructions?.isNotBlank() == true || cardV3?.data?.creatorNotes?.isNotBlank() == true) {
                        AccordionSection(
                            title = "⚙️ 系统提示词与指令",
                            icon = Icons.Filled.Settings,
                            isExpanded = expandSystem,
                            onToggle = { expandSystem = !expandSystem }
                        ) {
                            Column {
                                if (card.systemPrompt.isNotBlank()) {
                                    SubTitleText("System Prompt")
                                    ExpandableContentBox(text = card.systemPrompt)
                                }
                                val postHistory = cardV3?.data?.postHistoryInstructions
                                if (!postHistory.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    SubTitleText("Post History Instructions")
                                    ExpandableContentBox(text = postHistory)
                                }
                                val creatorNotes = cardV3?.data?.creatorNotes
                                if (!creatorNotes.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    SubTitleText("作者留言 (Creator Notes)")
                                    ExpandableContentBox(text = creatorNotes)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Delete Button
                    Button(
                        onClick = onDelete,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("删除角色卡")
                    }
                }

                // Top Right Close Button
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(20.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                }
            }
        }
    }
}

@Composable
fun GreetingCardItem(label: String, content: String) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(content.length < 300) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.5f)),
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
                    color = MaterialTheme.colorScheme.primary
                )

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
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            val displayText = if (isExpanded || content.length < 300) content else content.take(300) + "..."
            Text(
                text = displayText,
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.85f),
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
fun SubTitleText(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White.copy(alpha = 0.7f),
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
fun ExpandableContentBox(text: String) {
    var isExpanded by remember { mutableStateOf(text.length < 500) }
    val displayText = if (isExpanded || text.length < 500) text else text.take(500) + "..."

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Column {
            Text(
                text = displayText,
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.85f),
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
