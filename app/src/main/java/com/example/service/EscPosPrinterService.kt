package com.example.service

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import com.example.data.model.OrderWithItems
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class BluetoothDeviceInfo(
  val name: String,
  val address: String,
  val isConnected: Boolean = false
)

object EscPosPrinterService {

  private const val TAG = "EscPosPrinter"
  private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
  private const val PAPER_WIDTH_58MM = 32 // 32 characters per line for 58mm paper

  data class MerchantInfo(
    val name: String = "DAPUR KULINER NUSANTARA",
    val address: String = "Jl. Merdeka No. 45, Rasa Sedap",
    val phone: String = "0812-3456-7890",
    val footerMessage: String = "Terima kasih atas kunjungan Anda!\nKritik & Saran WA: 0812-3456-7890"
  )

  /**
   * Generates formatted monospace text receipt matching 32-column 58mm thermal paper
   */
  fun formatPlainTextReceipt(
    orderWithItems: OrderWithItems,
    merchant: MerchantInfo = MerchantInfo()
  ): String {
    val order = orderWithItems.order
    val items = orderWithItems.items
    val sb = StringBuilder()
    val dateFmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val formattedDate = dateFmt.format(Date(order.timestamp))

    fun center(text: String, width: Int = PAPER_WIDTH_58MM): String {
      if (text.length >= width) return text.take(width)
      val pad = (width - text.length) / 2
      return " ".repeat(pad) + text
    }

    fun row(left: String, right: String, width: Int = PAPER_WIDTH_58MM): String {
      val maxLeft = width - right.length - 1
      val trimmedLeft = if (left.length > maxLeft) left.take(maxLeft) else left
      val spaces = width - trimmedLeft.length - right.length
      return trimmedLeft + " ".repeat(maxOf(1, spaces)) + right
    }

    val divider = "-".repeat(PAPER_WIDTH_58MM)
    val doubleDivider = "=".repeat(PAPER_WIDTH_58MM)

    sb.appendLine(center(merchant.name))
    sb.appendLine(center(merchant.address))
    sb.appendLine(center("Telp: ${merchant.phone}"))
    sb.appendLine(doubleDivider)
    sb.appendLine(row("No: ${order.invoiceNumber}", order.orderType))
    sb.appendLine(row("Tgl: $formattedDate", "Kasir: ${order.cashierName}"))
    sb.appendLine(row("Pelanggan: ${order.customerName}", ""))
    sb.appendLine(divider)

    for (item in items) {
      sb.appendLine(item.menuItemName)
      val detail = "${item.quantity} x ${formatRupiah(item.unitPrice)}"
      val subtotal = formatRupiah(item.subtotal)
      sb.appendLine(row("  $detail", subtotal))
      if (item.notes.isNotBlank()) {
        sb.appendLine("  * ${item.notes}")
      }
    }

    sb.appendLine(divider)
    sb.appendLine(row("Subtotal:", formatRupiah(order.totalAmount)))
    if (order.discountAmount > 0) {
      sb.appendLine(row("Diskon:", "-${formatRupiah(order.discountAmount)}"))
    }
    if (order.taxAmount > 0) {
      sb.appendLine(row("Pajak:", formatRupiah(order.taxAmount)))
    }
    sb.appendLine(row("TOTAL:", formatRupiah(order.finalAmount)))
    sb.appendLine(divider)

    sb.appendLine(row("Metode Bayar:", order.paymentMethod))
    if (order.paymentMethod == "TUNAI") {
      sb.appendLine(row("Bayar Tunai:", formatRupiah(order.cashReceived)))
      sb.appendLine(row("Kembalian:", formatRupiah(order.cashChange)))
    } else {
      sb.appendLine(row("Ref QRIS:", order.qrisReference.ifBlank { "QRIS-APPROVED" }))
      sb.appendLine(row("Status:", "LUNAS (VERIFIED)"))
    }

    sb.appendLine(doubleDivider)
    merchant.footerMessage.split("\n").forEach { line ->
      sb.appendLine(center(line))
    }
    sb.appendLine()
    sb.appendLine(center("*** SIMPAN STRUK INI ***"))

    return sb.toString()
  }

  /**
   * Generates ESC/POS byte sequence for hardware thermal printer
   */
  fun generateEscPosBytes(
    orderWithItems: OrderWithItems,
    merchant: MerchantInfo = MerchantInfo()
  ): ByteArray {
    val out = ByteArrayOutputStream()

    // ESC @: Init printer
    out.write(byteArrayOf(0x1B, 0x40))

    // Align Center
    out.write(byteArrayOf(0x1B, 0x61, 0x01))
    // Bold On + Double Size for Title
    out.write(byteArrayOf(0x1B, 0x45, 0x01, 0x1D, 0x21, 0x11))
    out.write("${merchant.name}\n".toByteArray(Charsets.ISO_8859_1))

    // Normal Size, Bold Off
    out.write(byteArrayOf(0x1D, 0x21, 0x00, 0x1B, 0x45, 0x00))
    out.write("${merchant.address}\nTelp: ${merchant.phone}\n".toByteArray(Charsets.ISO_8859_1))
    out.write("${"=".repeat(PAPER_WIDTH_58MM)}\n".toByteArray(Charsets.ISO_8859_1))

    // Align Left
    out.write(byteArrayOf(0x1B, 0x61, 0x00))
    val textContent = formatPlainTextReceipt(orderWithItems, merchant)
    // Skip the header part already printed in center bold
    val lines = textContent.split("\n")
    val bodyLines = if (lines.size > 4) lines.drop(4) else lines
    out.write(bodyLines.joinToString("\n").toByteArray(Charsets.ISO_8859_1))

    // Feed 3 lines & Paper Cut (GS V 65 0)
    out.write(byteArrayOf(0x1B, 0x64, 0x03))
    out.write(byteArrayOf(0x1D, 0x56, 0x41, 0x00))

    return out.toByteArray()
  }

  /**
   * Retrieves paired Bluetooth devices
   */
  @SuppressLint("MissingPermission")
  fun getPairedDevices(context: Context): List<BluetoothDeviceInfo> {
    return try {
      val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
      val adapter = bluetoothManager?.adapter ?: BluetoothAdapter.getDefaultAdapter()
      if (adapter != null && adapter.isEnabled) {
        adapter.bondedDevices.map { device ->
          BluetoothDeviceInfo(
            name = device.name ?: "Unknown Printer",
            address = device.address
          )
        }
      } else {
        emptyList()
      }
    } catch (e: Exception) {
      Log.w(TAG, "Cannot get paired devices: ${e.message}")
      emptyList()
    }
  }

  /**
   * Prints bytes directly to Bluetooth printer by MAC address
   */
  @SuppressLint("MissingPermission")
  suspend fun printToBluetoothPrinter(
    context: Context,
    deviceAddress: String,
    data: ByteArray
  ): Result<Unit> = withContext(Dispatchers.IO) {
    var socket: BluetoothSocket? = null
    var out: OutputStream? = null
    try {
      val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
      val adapter = bluetoothManager?.adapter ?: BluetoothAdapter.getDefaultAdapter()
        ?: return@withContext Result.failure(Exception("Bluetooth tidak didukung pada perangkat ini"))

      val device: BluetoothDevice = adapter.getRemoteDevice(deviceAddress)
      socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
      adapter.cancelDiscovery()
      socket.connect()
      out = socket.outputStream
      out.write(data)
      out.flush()
      // Brief sleep for thermal buffer completion
      Thread.sleep(500)
      Result.success(Unit)
    } catch (e: Exception) {
      Log.e(TAG, "Bluetooth printing failed: ${e.message}", e)
      Result.failure(e)
    } finally {
      try { out?.close() } catch (_: Exception) {}
      try { socket?.close() } catch (_: Exception) {}
    }
  }

  private fun formatRupiah(amount: Double): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    return formatter.format(amount).replace("Rp", "Rp ").replace(",00", "")
  }
}
