package com.example.purrytify.views;

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import com.example.purrytify.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder

class ShareSheet (private var uri: String) : BottomSheetDialogFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.share, container, false)

        // Generate QR
        val qrImageView = view.findViewById<ImageView>(R.id.qr_image_view)
        val barcodeEncoder = BarcodeEncoder()
        val bitmap = barcodeEncoder.encodeBitmap(uri, BarcodeFormat.QR_CODE, 400, 400)
        qrImageView.setImageBitmap(bitmap)

        // LINE share
        view.findViewById<LinearLayout>(R.id.btn_line).setOnClickListener {
            shareViaApp("jp.naver.line.android", uri)
        }

        // WhatsApp share
        view.findViewById<LinearLayout>(R.id.btn_whatsapp).setOnClickListener {
            shareViaApp("com.whatsapp", uri)
        }

        // Instagram story share
        view.findViewById<LinearLayout>(R.id.btn_stories).setOnClickListener {
            shareViaApp("com.instagram.android", uri)
        }

        // X share
        view.findViewById<LinearLayout>(R.id.btn_x).setOnClickListener {
            shareViaApp("com.twitter.android", uri)
        }

        // Copy link
        view.findViewById<LinearLayout>(R.id.btn_copy_link).setOnClickListener {
            val clipboard = this.requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Song URI", uri))
            Toast.makeText(context, "Link copied!", Toast.LENGTH_SHORT).show()
        }

        return view
    }

    private fun shareViaApp(packageName: String, text: String) {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_TEXT, text)
        intent.setPackage(packageName)
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "App not installed", Toast.LENGTH_SHORT).show()
        }
    }
}
