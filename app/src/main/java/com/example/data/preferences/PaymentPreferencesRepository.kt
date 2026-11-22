package com.example.data.preferences

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

private val Context.warungDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "warung_nurbaiti_payment_prefs"
)

data class PaymentConfig(
    val qrisImagePath: String? = null,
    val qrisUpdatedAt: Long = 0L,
    val qrisMerchantName: String = "Warung Nurbaiti",
    val qrisNotes: String = "Scan kode QRIS di atas menggunakan e-wallet atau m-banking Anda, lalu simpan bukti pembayaran.",
    val ownerWhatsappNumber: String = "",
    val isQrisEnabled: Boolean = true,
    val isCodEnabled: Boolean = true
)

class PaymentPreferencesRepository(private val context: Context) {

    private object Keys {
        val QRIS_IMAGE_PATH = stringPreferencesKey("qris_image_path")
        val QRIS_UPDATED_AT = longPreferencesKey("qris_updated_at")
        val QRIS_MERCHANT_NAME = stringPreferencesKey("qris_merchant_name")
        val QRIS_NOTES = stringPreferencesKey("qris_notes")
        val OWNER_WHATSAPP = stringPreferencesKey("owner_whatsapp_number")
        val IS_QRIS_ENABLED = booleanPreferencesKey("is_qris_enabled")
        val IS_COD_ENABLED = booleanPreferencesKey("is_cod_enabled")
    }

    val paymentConfig: Flow<PaymentConfig> = context.warungDataStore.data.map { prefs ->
        val path = prefs[Keys.QRIS_IMAGE_PATH]
        val validPath = path?.takeIf { File(it).exists() }
        PaymentConfig(
            qrisImagePath = validPath,
            qrisUpdatedAt = prefs[Keys.QRIS_UPDATED_AT] ?: 0L,
            qrisMerchantName = prefs[Keys.QRIS_MERCHANT_NAME] ?: "Warung Nurbaiti",
            qrisNotes = prefs[Keys.QRIS_NOTES]
                ?: "Scan kode QRIS di atas menggunakan e-wallet atau m-banking Anda, lalu simpan bukti pembayaran.",
            ownerWhatsappNumber = prefs[Keys.OWNER_WHATSAPP] ?: "",
            isQrisEnabled = prefs[Keys.IS_QRIS_ENABLED] ?: true,
            isCodEnabled = prefs[Keys.IS_COD_ENABLED] ?: true
        )
    }

    suspend fun saveQrisImageFromUri(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val destFile = File(context.filesDir, "owner_qris_image.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext false

            val now = System.currentTimeMillis()
            context.warungDataStore.edit { prefs ->
                prefs[Keys.QRIS_IMAGE_PATH] = destFile.absolutePath
                prefs[Keys.QRIS_UPDATED_AT] = now
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun removeQrisImage() {
        withContext(Dispatchers.IO) {
            val destFile = File(context.filesDir, "owner_qris_image.jpg")
            if (destFile.exists()) {
                destFile.delete()
            }
        }
        context.warungDataStore.edit { prefs ->
            prefs.remove(Keys.QRIS_IMAGE_PATH)
            prefs[Keys.QRIS_UPDATED_AT] = System.currentTimeMillis()
        }
    }

    suspend fun updatePaymentSettings(
        merchantName: String,
        qrisNotes: String,
        ownerWhatsapp: String,
        isQrisEnabled: Boolean,
        isCodEnabled: Boolean
    ) {
        context.warungDataStore.edit { prefs ->
            prefs[Keys.QRIS_MERCHANT_NAME] = merchantName.ifBlank { "Warung Nurbaiti" }
            prefs[Keys.QRIS_NOTES] = qrisNotes.ifBlank {
                "Scan kode QRIS di atas menggunakan e-wallet atau m-banking Anda, lalu simpan bukti pembayaran."
            }
            prefs[Keys.OWNER_WHATSAPP] = ownerWhatsapp.trim()
            prefs[Keys.IS_QRIS_ENABLED] = isQrisEnabled
            prefs[Keys.IS_COD_ENABLED] = isCodEnabled
        }
    }
}
