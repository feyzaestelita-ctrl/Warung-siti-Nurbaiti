package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.data.model.OrderEntity
import java.net.URLEncoder

object WhatsAppHelper {

    fun formatIndonesianPhone(raw: String): String {
        val digits = raw.filter { it.isDigit() }
        return when {
            digits.startsWith("0") -> "62" + digits.drop(1)
            digits.startsWith("62") -> digits
            digits.startsWith("8") -> "62$digits"
            else -> digits
        }
    }

    fun openWhatsAppChat(context: Context, phoneNumber: String, message: String): Boolean {
        val formatted = formatIndonesianPhone(phoneNumber)
        if (formatted.length < 9) return false
        val encoded = URLEncoder.encode(message, "UTF-8")
        val uri = Uri.parse("https://wa.me/$formatted?text=$encoded")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun buildOwnerOrderMessage(order: OrderEntity): String {
        return buildString {
            appendLine("*Pesanan Baru - Warung Nurbaiti*")
            appendLine("Kode: ${order.orderCode}")
            appendLine("Nama Pemesan: ${order.customerName}")
            appendLine("No. WhatsApp: ${order.whatsappNumber}")
            appendLine("Metode Pembayaran: ${order.paymentMethod}")
            appendLine()
            appendLine("*Bahan yang Dipesan:*")
            appendLine(order.orderedIngredients)
            if (order.additionalNotes.isNotBlank()) {
                appendLine()
                appendLine("*Catatan Tambahan:*")
                appendLine(order.additionalNotes)
            }
        }
    }
}
