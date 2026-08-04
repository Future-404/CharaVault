package com.charavault.app

import android.net.Uri
import android.os.Bundle
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
    private var pendingImportUri by mutableStateOf<Uri?>(null)

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { pendingImportUri = it }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CharaVaultTheme {
                GalleryScreen(
                    viewModel = viewModel,
                    pendingImportUri = pendingImportUri,
                    onImportClick = {
                        filePickerLauncher.launch("image/png")
                    },
                    onImportConfirmed = { uri, selectedTags ->
                        viewModel.importCardUri(uri, selectedTags)
                        pendingImportUri = null
                    },
                    onImportCancelled = {
                        pendingImportUri = null
                    }
                )
            }
        }
    }
}
