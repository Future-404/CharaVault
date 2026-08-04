package com.charavault.app

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.charavault.app.data.local.CardEntity
import com.charavault.app.data.parser.ExportManager
import com.charavault.app.ui.screens.GalleryScreen
import com.charavault.app.ui.theme.CharaVaultTheme
import com.charavault.app.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private var pendingImportUris by mutableStateOf<List<Uri>>(emptyList())
    private var pendingExportSingleCard by mutableStateOf<CardEntity?>(null)

    // Launcher for selecting multiple PNG files at once to import
    private val batchFilePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            pendingImportUris = uris
        }
    }

    // Launcher for saving a single card PNG
    private val exportSingleCardLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("image/png")
    ) { targetUri ->
        val card = pendingExportSingleCard
        if (targetUri != null && card != null) {
            lifecycleScope.launch {
                val success = ExportManager.exportSingleCardToUri(this@MainActivity, card, targetUri)
                val msg = if (success) "🎉 导出角色卡 [${card.name}] 成功！" else "❌ 导出失败"
                Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                pendingExportSingleCard = null
            }
        }
    }

    // Launcher for exporting all cards into a Zip archive
    private val exportZipArchiveLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { zipUri ->
        if (zipUri != null) {
            lifecycleScope.launch {
                val cards = viewModel.allCards.value
                val count = ExportManager.exportAllCardsToZip(this@MainActivity, cards, zipUri)
                Toast.makeText(this@MainActivity, "📦 成功导出 $count 张角色卡到 Zip 压缩包！", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CharaVaultTheme {
                GalleryScreen(
                    viewModel = viewModel,
                    pendingImportUris = pendingImportUris,
                    onImportClick = {
                        batchFilePickerLauncher.launch("image/png")
                    },
                    onImportConfirmed = { uris, selectedTags ->
                        viewModel.importCardUrisBatch(uris, selectedTags) { result ->
                            val sb = StringBuilder()
                            if (result.successCount > 0) {
                                sb.append("🎉 成功导入 ${result.successCount} 张角色卡！\n")
                            }
                            if (result.duplicateCount > 0) {
                                sb.append("🛡️ 自动拦截 ${result.duplicateCount} 张重复卡片\n")
                            }
                            if (result.failedCount > 0) {
                                sb.append("⚠️ 过滤 ${result.failedCount} 张非合规文件")
                            }
                            Toast.makeText(this@MainActivity, sb.toString().trim(), Toast.LENGTH_LONG).show()
                        }
                        pendingImportUris = emptyList()
                    },
                    onImportCancelled = {
                        pendingImportUris = emptyList()
                    },
                    onExportSingleCardClick = { card ->
                        pendingExportSingleCard = card
                        val sanitizeName = card.name.replace("[\\\\/:*?\"<>|]".toRegex(), "_")
                        exportSingleCardLauncher.launch("${sanitizeName}.png")
                    },
                    onExportAllZipClick = {
                        val defaultZipName = ExportManager.generateBackupZipFileName()
                        exportZipArchiveLauncher.launch(defaultZipName)
                    }
                )
            }
        }
    }
}
