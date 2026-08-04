package com.charavault.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.charavault.app.data.local.CardEntity
import com.charavault.app.data.model.CardData
import com.charavault.app.data.model.CharacterCardV3
import kotlinx.serialization.json.Json

@Composable
fun CardEditorDialog(
    card: CardEntity,
    onDismiss: () -> Unit,
    onSave: (CharacterCardV3) -> Unit
) {
    val context = LocalContext.current
    val jsonParser = remember { Json { ignoreUnknownKeys = true; isLenient = true } }

    val originalV3: CharacterCardV3 = remember(card.rawJsonData) {
        try {
            jsonParser.decodeFromString<CharacterCardV3>(card.rawJsonData)
        } catch (e: Exception) {
            CharacterCardV3(
                data = CardData(
                    name = card.name,
                    creator = card.creator,
                    description = card.description,
                    personality = card.personality,
                    scenario = card.scenario,
                    firstMes = card.firstMes,
                    systemPrompt = card.systemPrompt
                )
            )
        }
    }

    var name by remember { mutableStateOf(originalV3.data.name) }
    var creator by remember { mutableStateOf(originalV3.data.creator) }
    var description by remember { mutableStateOf(originalV3.data.description) }
    var personality by remember { mutableStateOf(originalV3.data.personality) }
    var scenario by remember { mutableStateOf(originalV3.data.scenario) }
    var firstMes by remember { mutableStateOf(originalV3.data.firstMes) }
    var systemPrompt by remember { mutableStateOf(originalV3.data.systemPrompt) }
    var postHistory by remember { mutableStateOf(originalV3.data.postHistoryInstructions) }
    var creatorNotes by remember { mutableStateOf(originalV3.data.creatorNotes) }

    val alternateGreetings = remember {
        mutableStateListOf<String>().apply { addAll(originalV3.data.alternateGreetings) }
    }

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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "✏️ 编辑角色卡数据",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable Form Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // 1. Basic Info Section
                    Text("1. 基本信息", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("角色名称 (Name)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = creator,
                        onValueChange = { creator = it },
                        label = { Text("作者 (Creator)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 2. Character Profile & Details
                    Text("2. 设定与背景", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("背景与描述 (Description)") },
                        modifier = Modifier.fillMaxWidth().height(140.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = personality,
                        onValueChange = { personality = it },
                        label = { Text("性格特征 (Personality)") },
                        modifier = Modifier.fillMaxWidth().height(100.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = scenario,
                        onValueChange = { scenario = it },
                        label = { Text("对话场景 (Scenario)") },
                        modifier = Modifier.fillMaxWidth().height(100.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 3. Greetings & Openings
                    Text("3. 欢迎语与开场白", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = firstMes,
                        onValueChange = { firstMes = it },
                        label = { Text("默认开场白 (First Message)") },
                        modifier = Modifier.fillMaxWidth().height(140.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("备选欢迎语 (${alternateGreetings.size} 条)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        TextButton(onClick = { alternateGreetings.add("") }) {
                            Icon(Icons.Filled.Add, contentDescription = "Add", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("添加欢迎语", fontSize = 12.sp)
                        }
                    }

                    alternateGreetings.forEachIndexed { index, greeting ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("备选欢迎语 #${index + 1}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                    IconButton(
                                        onClick = { alternateGreetings.removeAt(index) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Remove", tint = Color.Red, modifier = Modifier.size(16.dp))
                                    }
                                }
                                OutlinedTextField(
                                    value = greeting,
                                    onValueChange = { alternateGreetings[index] = it },
                                    modifier = Modifier.fillMaxWidth().height(100.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 4. System Prompts & Notes
                    Text("4. 系统提示词与指令", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = systemPrompt,
                        onValueChange = { systemPrompt = it },
                        label = { Text("System Prompt") },
                        modifier = Modifier.fillMaxWidth().height(100.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = postHistory,
                        onValueChange = { postHistory = it },
                        label = { Text("Post History Instructions") },
                        modifier = Modifier.fillMaxWidth().height(80.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = creatorNotes,
                        onValueChange = { creatorNotes = it },
                        label = { Text("作者留言 (Creator Notes)") },
                        modifier = Modifier.fillMaxWidth().height(80.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Save Action Button
                Button(
                    onClick = {
                        if (name.isBlank()) {
                            Toast.makeText(context, "角色名称不能为空", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        val updatedData = originalV3.data.copy(
                            name = name.trim(),
                            creator = creator.trim(),
                            description = description,
                            personality = personality,
                            scenario = scenario,
                            firstMes = firstMes,
                            alternateGreetings = alternateGreetings.filter { it.isNotBlank() },
                            systemPrompt = systemPrompt,
                            postHistoryInstructions = postHistory,
                            creatorNotes = creatorNotes
                        )

                        val updatedV3 = originalV3.copy(data = updatedData)
                        onSave(updatedV3)
                        Toast.makeText(context, "🎉 角色卡数据修改保存成功！", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("保存角色卡修改")
                }
            }
        }
    }
}
