package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class PaymentMethod(val label: String) {
    QRIS("QRIS"),
    COD("COD")
}

enum class OrderStatus(val label: String) {
    BARU("Pesanan Baru"),
    DIPROSES("Sedang Disiapkan"),
    SELESAI("Selesai")
}

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val orderCode: String,
    val customerName: String,
    val whatsappNumber: String,
    val orderedIngredients: String,
    val additionalNotes: String,
    val paymentMethod: String,
    val status: String = OrderStatus.BARU.name,
    val createdAt: Long = System.currentTimeMillis(),
    val isReadByOwner: Boolean = false
)
