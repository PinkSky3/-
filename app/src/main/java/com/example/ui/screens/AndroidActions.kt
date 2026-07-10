package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast

internal fun copyToClipboard(context: Context, text: String) {
    try {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("hot_search_link", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "\u5DF2\u590D\u5236\u5230\u526A\u8D34\u677F", Toast.LENGTH_SHORT).show()
    } catch (_: Exception) {
        Toast.makeText(context, "\u590D\u5236\u5931\u8D25", Toast.LENGTH_SHORT).show()
    }
}

internal fun shareText(context: Context, text: String) {
    try {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "\u53D1\u9001\u70ED\u641C\u81F3"))
    } catch (_: Exception) {
        Toast.makeText(context, "\u5206\u4EAB\u5931\u8D25", Toast.LENGTH_SHORT).show()
    }
}
