package dev.clawdboard.ui

import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import dev.clawdboard.core.Repository
import dev.clawdboard.core.txt

@Composable
fun ScanButton(repo: Repository, onResult: (Boolean, String) -> Unit) {
    val context = LocalContext.current
    OutlinedButton(onClick = {
        val options = GmsBarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
        runCatching { GmsBarcodeScanning.getClient(context, options) }.getOrNull()
            ?.startScan()
            ?.addOnSuccessListener { code ->
                val ok = code.rawValue?.let { repo.pairRemote(it) } == true
                onResult(ok, if (ok) txt.scanOk else txt.scanBad)
            }
            ?.addOnFailureListener { onResult(false, txt.scanUnavailable) }
            ?: onResult(false, txt.scanUnavailable)
    }) { Text(txt.scanQr, color = C.text) }
}
