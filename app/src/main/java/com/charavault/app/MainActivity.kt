package com.charavault.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.charavault.app.ui.screens.GalleryScreen
import com.charavault.app.ui.theme.CharaVaultTheme
import com.charavault.app.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.importCardUri(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CharaVaultTheme {
                GalleryScreen(
                    viewModel = viewModel,
                    onImportClick = {
                        filePickerLauncher.launch("image/png")
                    }
                )
            }
        }
    }
}
