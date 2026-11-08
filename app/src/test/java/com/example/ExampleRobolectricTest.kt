package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.service.QrisService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("DapurKasir", appName)
  }

  @Test
  fun `verify qris payload generation`() {
    val payload = QrisService.generateQrisPayload(50000.0, "INV-TEST-001")
    assertTrue(payload.startsWith("000201"))
    assertTrue(payload.contains("5812"))
    assertTrue(payload.contains("360"))
    assertTrue(payload.contains("50000"))
    val qrBitmap = QrisService.generateQrBitmap(payload, 200)
    assertNotNull(qrBitmap)
  }
}
