package com.example

import com.example.data.model.OrderEntity
import com.example.util.WhatsAppHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun formatIndonesianPhone_normalizesPrefixes() {
        assertEquals("6281234567890", WhatsAppHelper.formatIndonesianPhone("0812-3456-7890"))
        assertEquals("6281234567890", WhatsAppHelper.formatIndonesianPhone("+62 812 3456 7890"))
        assertEquals("6281234567890", WhatsAppHelper.formatIndonesianPhone("81234567890"))
    }

    @Test
    fun buildOwnerOrderMessage_includesAllOrderFields() {
        val order = OrderEntity(
            id = 1L,
            orderCode = "WN-1234",
            customerName = "Siti Aminah",
            whatsappNumber = "081234567890",
            orderedIngredients = "Beras 5 kg, Minyak 2 L",
            additionalNotes = "Antar pagi",
            paymentMethod = "QRIS"
        )
        val message = WhatsAppHelper.buildOwnerOrderMessage(order)
        assertTrue(message.contains("WN-1234"))
        assertTrue(message.contains("Siti Aminah"))
        assertTrue(message.contains("081234567890"))
        assertTrue(message.contains("Beras 5 kg, Minyak 2 L"))
        assertTrue(message.contains("Antar pagi"))
        assertTrue(message.contains("QRIS"))
    }
}
