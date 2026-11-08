package com.example.service

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.util.EnumMap

object QrisService {

  private const val MERCHANT_NAME = "DAPUR KULINER NUSANTARA"
  private const val MERCHANT_CITY = "JAKARTA"
  private const val MERCHANT_NMID = "ID1020261028001"

  /**
   * Generates a standard EMVCo-compliant QRIS payload string
   */
  fun generateQrisPayload(
    amount: Double,
    invoiceNumber: String
  ): String {
    val roundedAmount = amount.toLong().toString()
    // Simplified standard Indonesian QRIS payload with merchant tags:
    // 00: Format Indicator (01)
    // 01: Point of Initiation (12 = Dynamic QR)
    // 26: Merchant Info (00=ID.CO.QRIS.WWW, 01=NMID, 02=Criteria G)
    // 52: Merchant Category Code (5812 = Food & Beverage)
    // 53: Transaction Currency (360 = IDR)
    // 54: Transaction Amount
    // 58: Country Code (ID)
    // 59: Merchant Name
    // 60: Merchant City
    // 62: Additional Data (01=Invoice)
    // 63: CRC16 Checksum
    val base = StringBuilder()
      .append("000201")
      .append("010212") // Dynamic QR
      .append(formatEmvTag("26", formatEmvTag("00", "ID.CO.QRIS.WWW") + formatEmvTag("01", MERCHANT_NMID) + formatEmvTag("02", "UMI")))
      .append("52045812") // F&B Merchant
      .append("5303360")  // IDR
      .append(formatEmvTag("54", roundedAmount))
      .append("5802ID")
      .append(formatEmvTag("59", MERCHANT_NAME.take(25)))
      .append(formatEmvTag("60", MERCHANT_CITY))
      .append(formatEmvTag("62", formatEmvTag("01", invoiceNumber)))
      .append("6304")

    val crc = calculateCrc16(base.toString().toByteArray())
    return base.toString() + String.format("%04X", crc)
  }

  private fun formatEmvTag(tag: String, value: String): String {
    val length = String.format("%02d", value.length)
    return "$tag$length$value"
  }

  private fun calculateCrc16(bytes: ByteArray): Int {
    var crc = 0xFFFF
    val polynomial = 0x1021
    for (b in bytes) {
      for (i in 0 until 8) {
        val bit = (b.toInt() shr (7 - i) and 1) == 1
        val c15 = (crc shr 15 and 1) == 1
        crc = crc shl 1
        if (c15 xor bit) crc = crc xor polynomial
      }
    }
    return crc and 0xFFFF
  }

  /**
   * Generates an ImageBitmap representing the QR Code
   */
  fun generateQrBitmap(content: String, size: Int = 512): ImageBitmap {
    val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
      put(EncodeHintType.CHARACTER_SET, "UTF-8")
      put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M)
      put(EncodeHintType.MARGIN, 2)
    }

    val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
    val width = bitMatrix.width
    val height = bitMatrix.height
    val pixels = IntArray(width * height)

    val black = 0xFF1A1A1A.toInt()
    val white = 0xFFFFFFFF.toInt()

    for (y in 0 until height) {
      val offset = y * width
      for (x in 0 until width) {
        pixels[offset + x] = if (bitMatrix.get(x, y)) black else white
      }
    }

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    return bitmap.asImageBitmap()
  }
}
