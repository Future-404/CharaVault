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
import com.charavault.app.ui.screens.GalleryScreen
import com.charavault.app.ui.theme.CharaVaultTheme
import com.charavault.app.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private var pendingImportUris by mutableStateOf<List<Uri>>(emptyList())

    // ActivityResultLauncher for selecting multiple PNG files at once
    private val batchFilePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            pendingImportUris = uris
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
                            val msg = if (result.failedCount == 0) {
                                "🎉 成功导入 ${result.successCount} 张合规角色卡！"
                            } else {
                                "✅ 成功导入 ${result.successCount} 张，⚠️ 过滤掉 ${result.failedCount} 张非合规文件"
                            }
                            Toast.makeText(this@MainActivity, msg, Toast.LENGTH_LONG).show()
                        }
                        pendingImportUris = emptyList()
                    },
                    onImportCancelled = {
                        pendingImportUris = emptyList()
                    }
                )
            }
        }
    }
}
