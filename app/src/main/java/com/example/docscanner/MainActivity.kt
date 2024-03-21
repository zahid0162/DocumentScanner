package com.example.docscanner

import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.Intent.createChooser
import android.content.IntentSender
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.FileProvider
import com.example.docscanner.ui.theme.DocScannerTheme
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import java.io.File

class MainActivity : ComponentActivity() {
    private lateinit var scannerLauncher: ActivityResultLauncher<IntentSenderRequest>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scannerLauncher =
            registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
                handleActivityResult(result)
            }
        setContent {
            DocScannerTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(){ a,b,c ->
                     onScanButtonClicked(a,b,c)
                    }
                }
            }
        }
    }

    private fun handleActivityResult(activityResult: ActivityResult) {
        val resultCode = activityResult.resultCode
        val result = GmsDocumentScanningResult.fromActivityResultIntent(activityResult.data)
        if (resultCode == Activity.RESULT_OK && result != null) {

            val pages = result.pages

            result.pdf?.uri?.path?.let { path ->
                val externalUri = FileProvider.getUriForFile(this, packageName + ".provider", File(path))
                val shareIntent =
                    Intent(Intent.ACTION_SEND).apply {
                        putExtra(Intent.EXTRA_STREAM, externalUri)
                        type = "application/pdf"
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                startActivity(createChooser(shareIntent,"Shared_Info"))
            }
        } else if (resultCode == Activity.RESULT_CANCELED) {
//            resultInfo.text = getString(R.string.error_scanner_cancelled)
        } else {
//            resultInfo.text = getString(R.string.error_default_message)
        }
    }

    private fun onScanButtonClicked(enableGalleryImport: Boolean, pageLimit: Int, selectedMode:String) {

        val options =
            GmsDocumentScannerOptions.Builder()
                .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_BASE)
                .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_PDF)
                .setGalleryImportAllowed(enableGalleryImport)

        when (selectedMode) {
            "Full" -> options.setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            "Base" -> options.setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_BASE)
            "Base with Filler" ->
                options.setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_BASE_WITH_FILTER)
            else -> Log.e(TAG, "Unknown selectedMode: $selectedMode")
        }

        val pageLimitInputText = pageLimit
        if (pageLimitInputText>0) {
            try {
                val pageLimit = pageLimitInputText.toInt()
                options.setPageLimit(pageLimit)
            } catch (e: Throwable) {

                return
            }
        }

        GmsDocumentScanning.getClient(options.build())
            .getStartScanIntent(this)
            .addOnSuccessListener { intentSender: IntentSender ->
                scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
            }
            .addOnFailureListener() { e: Exception ->

            }
    }
}





@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    DocScannerTheme {
        Greeting("Android")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(onScanClick: (Boolean,Int,String) -> Unit ) {
    var isExpanded by remember {
        mutableStateOf(false)
    }
    var isGallery by remember {
        mutableStateOf(true)
    }
    var mode by remember {
        mutableStateOf("Full")
    }
    var pageLimit by remember {
        mutableStateOf("")
    }
    Column {
        Row {
            Text(text = "Scanner Feature Mode", modifier = Modifier.padding(16.dp))
            ExposedDropdownMenuBox(expanded = isExpanded, onExpandedChange = { isExpanded = it }) {
                TextField(
                    value = mode, onValueChange = {}, readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded)
                    },
                    colors = ExposedDropdownMenuDefaults.textFieldColors(),
                    modifier = Modifier.menuAnchor()

                )
                ExposedDropdownMenu(
                    expanded = isExpanded,
                    onDismissRequest = { isExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text(text = "Full") }, onClick = {
                        mode = "Full"
                        isExpanded = false
                    })
                    DropdownMenuItem(
                        text = { Text(text = "Base") }, onClick = {
                            mode = "Base"
                            isExpanded = false
                        })
                    DropdownMenuItem(
                        text = { Text(text = "Base with Filler") }, onClick = {
                            mode = "Base with Filler"
                            isExpanded = false
                        })

                }

            }
        }
        Row {
            Text(text = "Page limit per Scan: ", modifier = Modifier.padding(16.dp))
            Checkbox(checked = isGallery, onCheckedChange = {isGallery = it})
        }
        Row {
            Text(text = "Enable Gallery Import: ", modifier = Modifier.padding(16.dp))
            TextField(value = pageLimit, onValueChange = {pageLimit = it}, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number) )
        }
        Button(onClick = { onScanClick(isGallery,if (pageLimit.isBlank()) 10 else pageLimit.toInt(),mode) }) {
            Text(text = "Scan")
            
        }
    }
}