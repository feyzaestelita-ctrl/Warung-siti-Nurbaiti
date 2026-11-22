package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.util.WhatsAppHelper
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Warung Nurbaiti", appName)
    }

    @Test
    fun `format Indonesian WhatsApp numbers correctly`() {
        assertEquals("6281234567890", WhatsAppHelper.formatIndonesianPhone("0812-3456-7890"))
        assertEquals("6281234567890", WhatsAppHelper.formatIndonesianPhone("+62 812 3456 7890"))
        assertEquals("6281234567890", WhatsAppHelper.formatIndonesianPhone("81234567890"))
    }
}
