package com.example.purrytify

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.Result
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView

class QRScannerActivity : AppCompatActivity() {

    private lateinit var barcodeView: DecoratedBarcodeView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qrscanner)

        barcodeView = findViewById(R.id.barcode_scanner)
        barcodeView.decodeContinuous(callback)
    }

    private val callback = object : BarcodeCallback {
        override fun barcodeResult(result: BarcodeResult?) {
            result?.text?.let {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = android.net.Uri.parse(it)
                }
                if (intent.resolveActivity(packageManager) != null) {
                    startActivity(intent)
                    barcodeView.pause()
                    finish()
                }
            }
        }

        override fun possibleResultPoints(resultPoints: List<com.google.zxing.ResultPoint>) {}
    }

    override fun onResume() {
        super.onResume()
        barcodeView.resume()
    }

    override fun onPause() {
        super.onPause()
        barcodeView.pause()
    }
}
