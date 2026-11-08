package com.example.service

import android.util.Log
import com.example.data.model.DailySalesSummary
import com.example.data.model.MenuItemWithStock
import com.example.data.model.OrderWithItems
import com.example.data.model.WebOrder
import com.example.data.model.WebOrderItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.util.Collections

class EmbeddedWebOrderServer(
  private val port: Int = 8080,
  private val onOrderReceived: (WebOrder) -> Unit
) {

  private var serverSocket: ServerSocket? = null
  private var serverJob: Job? = null
  private val scope = CoroutineScope(Dispatchers.IO)
  private var isRunning = false

  var currentMenuItemsProvider: (() -> List<MenuItemWithStock>)? = null
  var currentReportsProvider: (() -> Pair<DailySalesSummary, List<OrderWithItems>>)? = null

  fun start(): Boolean {
    if (isRunning) return true
    return try {
      serverSocket = ServerSocket(port)
      isRunning = true
      serverJob = scope.launch {
        while (isActive && isRunning) {
          try {
            val clientSocket = serverSocket?.accept() ?: break
            launch { handleClient(clientSocket) }
          } catch (e: Exception) {
            if (!isRunning) break
            Log.e(TAG, "Socket accept error: ${e.message}")
          }
        }
      }
      Log.i(TAG, "Embedded Web Server started on port $port")
      true
    } catch (e: Exception) {
      Log.e(TAG, "Failed to start server on port $port: ${e.message}")
      false
    }
  }

  fun stop() {
    isRunning = false
    try {
      serverSocket?.close()
    } catch (_: Exception) {}
    serverJob?.cancel()
    serverSocket = null
    Log.i(TAG, "Embedded Web Server stopped")
  }

  fun isServerRunning(): Boolean = isRunning

  private suspend fun handleClient(socket: Socket) = withContext(Dispatchers.IO) {
    try {
      val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
      val writer = PrintWriter(socket.getOutputStream(), true)

      val requestLine = reader.readLine() ?: return@withContext
      val parts = requestLine.split(" ")
      if (parts.size < 2) return@withContext

      val method = parts[0]
      val path = parts[1]

      var contentLength = 0
      var line = reader.readLine()
      while (!line.isNullOrBlank()) {
        if (line.startsWith("Content-Length:", ignoreCase = true)) {
          contentLength = line.substring(15).trim().toIntOrNull() ?: 0
        }
        line = reader.readLine()
      }

      val body = if (contentLength > 0) {
        val charArray = CharArray(contentLength)
        var totalRead = 0
        while (totalRead < contentLength) {
          val read = reader.read(charArray, totalRead, contentLength - totalRead)
          if (read == -1) break
          totalRead += read
        }
        String(charArray, 0, totalRead)
      } else {
        ""
      }

      when {
        path == "/" || path.startsWith("/index") || path.startsWith("/order") -> {
          val html = generateWebOrderAppHtml()
          sendResponse(writer, 200, "text/html; charset=UTF-8", html)
        }

        path == "/laporan" || path.startsWith("/report") -> {
          val html = generateWebReportsHtml()
          sendResponse(writer, 200, "text/html; charset=UTF-8", html)
        }

        path == "/api/reports" -> {
          val reportData = currentReportsProvider?.invoke()
          val summary = reportData?.first
          val orders = reportData?.second.orEmpty()

          val json = JSONObject().apply {
            put("status", "success")
            put("totalRevenue", summary?.totalRevenue ?: 0.0)
            put("estimatedProfit", summary?.estimatedProfit ?: 0.0)
            put("totalOrders", summary?.totalOrders ?: 0)
            put("totalCash", summary?.totalCash ?: 0.0)
            put("totalQris", summary?.totalQris ?: 0.0)
            put("averageTicket", summary?.averageTicket ?: 0.0)

            val orderArr = JSONArray()
            for (ow in orders.take(50)) {
              val o = ow.order
              val oObj = JSONObject().apply {
                put("invoiceNumber", o.invoiceNumber)
                put("timestamp", o.timestamp)
                put("customerName", o.customerName)
                put("orderType", o.orderType)
                put("finalAmount", o.finalAmount)
                put("paymentMethod", o.paymentMethod)
                put("itemsSummary", ow.items.joinToString(", ") { "${it.quantity}x ${it.menuItemName}" })
              }
              orderArr.put(oObj)
            }
            put("transactions", orderArr)
          }
          sendResponse(writer, 200, "application/json; charset=UTF-8", json.toString())
        }

        path == "/api/menu" -> {
          val menus = currentMenuItemsProvider?.invoke().orEmpty()
          val jsonArray = JSONArray()
          for (itemWithStock in menus) {
            val m = itemWithStock.menuItem
            val obj = JSONObject().apply {
              put("id", m.id)
              put("name", m.name)
              put("category", m.category)
              put("price", m.price)
              put("description", m.description)
              put("availablePortions", itemWithStock.availablePortions)
              put("isOutOfStock", itemWithStock.isOutOfStock)
            }
            jsonArray.put(obj)
          }
          sendResponse(writer, 200, "application/json; charset=UTF-8", jsonArray.toString())
        }

        path == "/api/order" && method.equals("POST", ignoreCase = true) -> {
          val order = parseIncomingOrder(body)
          if (order != null) {
            onOrderReceived(order)
            val resObj = JSONObject().apply {
              put("status", "success")
              put("orderId", order.id)
              put("message", "Pesanan berhasil diterima di kasir!")
              put("totalAmount", order.totalAmount)
            }
            sendResponse(writer, 200, "application/json; charset=UTF-8", resObj.toString())
          } else {
            sendResponse(writer, 400, "application/json", """{"status":"error","message":"Format pesanan tidak valid"}""")
          }
        }

        path == "/api/status" -> {
          sendResponse(writer, 200, "application/json", """{"status":"online","app":"DapurKasir-Host","port":$port}""")
        }

        else -> {
          sendResponse(writer, 404, "text/plain", "Not Found")
        }
      }

      socket.close()
    } catch (e: Exception) {
      Log.e(TAG, "Error handling client request: ${e.message}")
    }
  }

  private fun sendResponse(writer: PrintWriter, statusCode: Int, contentType: String, content: String) {
    val statusText = if (statusCode == 200) "OK" else if (statusCode == 404) "Not Found" else "Bad Request"
    val bytes = content.toByteArray(Charsets.UTF_8)
    writer.print("HTTP/1.1 $statusCode $statusText\r\n")
    writer.print("Content-Type: $contentType\r\n")
    writer.print("Content-Length: ${bytes.size}\r\n")
    writer.print("Access-Control-Allow-Origin: *\r\n")
    writer.print("Connection: close\r\n")
    writer.print("\r\n")
    writer.print(content)
    writer.flush()
  }

  private fun parseIncomingOrder(body: String): WebOrder? {
    return try {
      val json = JSONObject(body)
      val customerName = json.optString("customerName", "Pelanggan Web").ifBlank { "Pelanggan Web" }
      val tableOrAddress = json.optString("tableNumber", "Meja 1").ifBlank { "Meja 1" }
      val orderType = json.optString("orderType", "Dine In")
      val customerPhone = json.optString("customerPhone", "")
      val notes = json.optString("notes", "")
      val paymentPref = json.optString("paymentMethod", "QRIS")

      val itemsArray = json.getJSONArray("items")
      val itemsList = mutableListOf<WebOrderItem>()
      var total = 0.0

      for (i in 0 until itemsArray.length()) {
        val itemObj = itemsArray.getJSONObject(i)
        val menuItemId = itemObj.optLong("menuItemId", 0)
        val menuItemName = itemObj.optString("name", "Item")
        val unitPrice = itemObj.optDouble("price", 0.0)
        val quantity = itemObj.optInt("quantity", 1)
        val itemNotes = itemObj.optString("notes", "")

        if (quantity > 0) {
          val sub = unitPrice * quantity
          total += sub
          itemsList.add(
            WebOrderItem(
              menuItemId = menuItemId,
              menuItemName = menuItemName,
              unitPrice = unitPrice,
              quantity = quantity,
              notes = itemNotes
            )
          )
        }
      }

      if (itemsList.isEmpty()) return null

      val randomId = "WEB-${(1000..9999).random()}"
      WebOrder(
        id = randomId,
        customerName = customerName,
        tableOrAddress = tableOrAddress,
        orderType = orderType,
        customerPhone = customerPhone,
        notes = notes,
        items = itemsList,
        totalAmount = total,
        paymentPreference = paymentPref,
        timestamp = System.currentTimeMillis()
      )
    } catch (e: Exception) {
      Log.e(TAG, "Failed to parse web order: ${e.message}")
      null
    }
  }

  private fun generateWebOrderAppHtml(): String {
    val menus = currentMenuItemsProvider?.invoke().orEmpty()
    val menuJsonBuilder = StringBuilder("[")
    menus.forEachIndexed { idx, itemWithStock ->
      val m = itemWithStock.menuItem
      menuJsonBuilder.append("""{"id":${m.id},"name":"${escapeJson(m.name)}","category":"${escapeJson(m.category)}","price":${m.price},"description":"${escapeJson(m.description)}","available":${itemWithStock.availablePortions},"isOut":${itemWithStock.isOutOfStock}}""")
      if (idx < menus.size - 1) menuJsonBuilder.append(",")
    }
    menuJsonBuilder.append("]")

    return """
<!DOCTYPE html>
<html lang="id">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
  <title>Dapur Kuliner Nusantara - Form Pemesanan Online</title>
  <style>
    :root {
      --primary: #D84315;
      --primary-hover: #BF360C;
      --primary-light: #FFDBCF;
      --secondary: #E65100;
      --green: #2E7D32;
      --bg: #FBF8F6;
      --surface: #FFFFFF;
      --text: #201A18;
      --text-muted: #665550;
      --border: #E8DED8;
      --radius: 16px;
    }
    * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif; }
    body { background-color: var(--bg); color: var(--text); padding-bottom: 120px; }
    header { background: linear-gradient(135deg, var(--primary) 0%, var(--secondary) 100%); color: white; padding: 24px 20px 20px; border-radius: 0 0 24px 24px; }
    .badge-host { display: inline-flex; align-items: center; gap: 6px; background: rgba(255, 255, 255, 0.2); padding: 4px 12px; border-radius: 20px; font-size: 11px; font-weight: 700; margin-bottom: 8px; }
    .badge-host .dot { width: 8px; height: 8px; background: #4CAF50; border-radius: 50%; }
    h1 { font-size: 22px; font-weight: 800; }
    .subtitle { font-size: 13px; opacity: 0.9; margin-top: 4px; }
    .container { max-width: 600px; margin: 0 auto; padding: 16px; }
    .card { background: var(--surface); border-radius: var(--radius); padding: 18px; margin-bottom: 16px; border: 1px solid var(--border); }
    .card-title { font-size: 16px; font-weight: 700; margin-bottom: 12px; color: var(--primary); }
    .form-group { margin-bottom: 14px; }
    label { display: block; font-size: 12px; font-weight: 700; margin-bottom: 6px; color: var(--text-muted); }
    input, select { width: 100%; padding: 12px 14px; border: 1.5px solid var(--border); border-radius: 12px; font-size: 14px; background: #FAFAFA; }
    .form-row { display: flex; gap: 10px; }
    .category-tabs { display: flex; gap: 8px; overflow-x: auto; padding-bottom: 8px; margin-bottom: 12px; }
    .tab-chip { padding: 8px 16px; background: #EFE8E4; border-radius: 20px; font-size: 13px; font-weight: 600; white-space: nowrap; cursor: pointer; border: none; color: var(--text-muted); }
    .tab-chip.active { background: var(--primary); color: white; }
    .menu-grid { display: flex; flex-direction: column; gap: 12px; }
    .menu-item { display: flex; align-items: center; justify-content: space-between; padding: 12px; background: white; border-radius: 14px; border: 1px solid var(--border); }
    .item-info { flex: 1; padding-right: 12px; }
    .item-name { font-size: 15px; font-weight: 700; }
    .item-desc { font-size: 12px; color: var(--text-muted); margin-top: 2px; }
    .item-price { font-size: 14px; font-weight: 800; color: var(--primary); margin-top: 4px; }
    .item-stock { font-size: 11px; font-weight: 700; color: var(--green); margin-top: 2px; }
    .item-stock.out { color: #D32F2F; }
    .qty-controls { display: flex; align-items: center; gap: 8px; }
    .btn-qty { width: 32px; height: 32px; border-radius: 50%; border: none; background: #F0EAE6; font-size: 16px; font-weight: 700; cursor: pointer; display: flex; align-items: center; justify-content: center; }
    .btn-qty.plus { background: var(--primary); color: white; }
    .qty-display { font-size: 14px; font-weight: 700; min-width: 20px; text-align: center; }
    .item-notes-input { margin-top: 8px; padding: 6px 10px; font-size: 12px; border-radius: 8px; border: 1px dashed var(--border); width: 100%; }
    .sticky-bar { position: fixed; bottom: 0; left: 0; right: 0; background: white; padding: 14px 20px; box-shadow: 0 -4px 16px rgba(0,0,0,0.08); display: flex; align-items: center; justify-content: space-between; z-index: 100; border-top: 1px solid var(--border); }
    .total-val { font-size: 18px; font-weight: 800; color: var(--primary); }
    .btn-order { background: var(--primary); color: white; border: none; padding: 14px 24px; border-radius: 14px; font-size: 15px; font-weight: 700; cursor: pointer; }
    .btn-order:disabled { background: #CCC; cursor: not-allowed; }
    .modal { display: none; position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.6); align-items: center; justify-content: center; padding: 20px; z-index: 999; }
    .modal.active { display: flex; }
    .modal-content { background: white; border-radius: 20px; padding: 24px; max-width: 420px; width: 100%; text-align: center; }
  </style>
</head>
<body>
  <header>
    <div class="badge-host"><span class="dot"></span> HOSTING SERVER POS AKTIF</div>
    <h1>Dapur Kuliner Nusantara</h1>
    <div class="subtitle">Form Pemesanan Mandiri Pelanggan (Self-Order Web App)</div>
  </header>
  <div class="container">
    <div class="card">
      <div class="card-title">👤 Data Pemesan</div>
      <div class="form-row">
        <div class="form-group" style="flex: 1.5;">
          <label>Nama Pemesan *</label>
          <input type="text" id="custName" placeholder="Contoh: Budi Santoso" required>
        </div>
        <div class="form-group" style="flex: 1;">
          <label>No. Meja *</label>
          <input type="text" id="tableNo" placeholder="Meja 3" value="Meja 1" required>
        </div>
      </div>
      <div class="form-row">
        <div class="form-group" style="flex: 1;">
          <label>Tipe Pesanan</label>
          <select id="orderType">
            <option value="Dine In">Makan di Tempat (Dine In)</option>
            <option value="Take Away">Bawa Pulang (Take Away)</option>
          </select>
        </div>
        <div class="form-group" style="flex: 1;">
          <label>No. WhatsApp</label>
          <input type="tel" id="custPhone" placeholder="0812...">
        </div>
      </div>
      <div class="form-group">
        <label>Metode Pembayaran</label>
        <select id="paymentPref">
          <option value="QRIS">QRIS Digital (Scan di Meja/Kasir)</option>
          <option value="TUNAI">Bayar Tunai di Kasir</option>
        </select>
      </div>
    </div>
    <div class="category-tabs" id="categoryTabs">
      <button class="tab-chip active" onclick="filterCategory('Semua', this)">Semua</button>
      <button class="tab-chip" onclick="filterCategory('Makanan', this)">Makanan</button>
      <button class="tab-chip" onclick="filterCategory('Minuman', this)">Minuman</button>
      <button class="tab-chip" onclick="filterCategory('Camilan', this)">Camilan</button>
    </div>
    <div class="menu-grid" id="menuList"></div>
  </div>
  <div class="sticky-bar">
    <div>
      <div style="font-size: 12px; color: var(--text-muted);" id="cartCountLabel">0 item dipilih</div>
      <div class="total-val" id="cartTotalLabel">Rp 0</div>
    </div>
    <button class="btn-order" id="btnSubmitOrder" onclick="submitWebOrder()" disabled>Kirim Pesanan</button>
  </div>
  <div class="modal" id="successModal">
    <div class="modal-content">
      <div style="font-size: 40px; margin-bottom: 10px;">✓</div>
      <h2 style="font-size: 20px; font-weight: 800;">Pesanan Diterima!</h2>
      <p style="font-size: 13px; color: var(--text-muted); margin: 8px 0 16px;">Pesanan Anda sedang dipersiapkan di dapur.</p>
      <div style="background: #F5F5F5; padding: 12px; border-radius: 12px; margin-bottom: 16px;">
        <div style="font-size: 12px; color: #777;">ID Pesanan:</div>
        <div style="font-size: 18px; font-weight: 800; color: var(--primary);" id="orderRefDisplay">WEB-0000</div>
        <div style="font-size: 14px; font-weight: 700; margin-top: 4px;" id="orderTotalDisplay">Rp 0</div>
      </div>
      <button class="btn-order" style="width: 100%;" onclick="resetWebForm()">Pesan Menu Lainnya</button>
    </div>
  </div>
  <script>
    const menus = $menuJsonBuilder;
    const cart = {};
    let selectedCategory = 'Semua';
    function formatRp(num) { return 'Rp ' + Math.round(num).toLocaleString('id-ID'); }
    function renderMenu() {
      const container = document.getElementById('menuList');
      container.innerHTML = '';
      const filtered = selectedCategory === 'Semua' ? menus : menus.filter(m => m.category === selectedCategory);
      filtered.forEach(item => {
        const qty = cart[item.id]?.qty || 0;
        const notes = cart[item.id]?.notes || '';
        const isOut = item.isOut || item.available <= 0;
        const div = document.createElement('div');
        div.className = 'menu-item';
        div.innerHTML = `
          <div class="item-info">
            <div class="item-name">${'$'}{item.name}</div>
            <div class="item-desc">${'$'}{item.description || ''}</div>
            <div class="item-price">${'$'}{formatRp(item.price)}</div>
            <div class="item-stock ${'$'}{isOut ? 'out' : ''}">${'$'}{isOut ? 'Habis' : 'Tersedia ' + item.available + ' porsi'}</div>
            ${'$'}{qty > 0 ? `<input type="text" class="item-notes-input" placeholder="Catatan: pedas..." value="${'$'}{notes}" onchange="updateItemNotes(${'$'}{item.id}, this.value)">` : ''}
          </div>
          <div class="qty-controls">
            ${'$'}{qty > 0 ? `<button class="btn-qty" onclick="changeQty(${'$'}{item.id}, -1)">−</button>` : ''}
            ${'$'}{qty > 0 ? `<span class="qty-display">${'$'}{qty}</span>` : ''}
            <button class="btn-qty plus" ${'$'}{isOut ? 'disabled style="opacity:0.4;"' : ''} onclick="changeQty(${'$'}{item.id}, 1)">+</button>
          </div>
        `;
        container.appendChild(div);
      });
      updateCartSummary();
    }
    function changeQty(id, delta) {
      const item = menus.find(m => m.id === id);
      if (!item) return;
      if (!cart[id]) cart[id] = { id: item.id, name: item.name, price: item.price, qty: 0, notes: '' };
      const newQty = cart[id].qty + delta;
      if (newQty <= 0) delete cart[id];
      else {
        if (newQty > item.available) { alert('Maksimal porsi: ' + item.available); return; }
        cart[id].qty = newQty;
      }
      renderMenu();
    }
    function updateItemNotes(id, notes) { if (cart[id]) cart[id].notes = notes; }
    function filterCategory(cat, btn) {
      selectedCategory = cat;
      document.querySelectorAll('.tab-chip').forEach(c => c.classList.remove('active'));
      btn.classList.add('active');
      renderMenu();
    }
    function updateCartSummary() {
      let totalQty = 0, totalPrice = 0;
      Object.values(cart).forEach(it => { totalQty += it.qty; totalPrice += it.price * it.qty; });
      document.getElementById('cartCountLabel').innerText = totalQty + ' item dipilih';
      document.getElementById('cartTotalLabel').innerText = formatRp(totalPrice);
      document.getElementById('btnSubmitOrder').disabled = (totalQty === 0);
    }
    async function submitWebOrder() {
      const custName = document.getElementById('custName').value.trim();
      const tableNo = document.getElementById('tableNo').value.trim();
      const orderType = document.getElementById('orderType').value;
      const custPhone = document.getElementById('custPhone').value.trim();
      const paymentMethod = document.getElementById('paymentPref').value;
      if (!custName || !tableNo) { alert('Mohon isi nama dan nomor meja'); return; }
      const items = Object.values(cart).map(it => ({ menuItemId: it.id, name: it.name, price: it.price, quantity: it.qty, notes: it.notes }));
      const payload = { customerName: custName, tableNumber: tableNo, orderType, customerPhone: custPhone, paymentMethod, items };
      const btn = document.getElementById('btnSubmitOrder');
      btn.disabled = true;
      btn.innerText = 'Mengirim...';
      try {
        const resp = await fetch('/api/order', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) });
        const res = await resp.json();
        if (res.status === 'success') {
          document.getElementById('orderRefDisplay').innerText = res.orderId;
          document.getElementById('orderTotalDisplay').innerText = formatRp(res.totalAmount);
          document.getElementById('successModal').classList.add('active');
        } else { alert('Gagal: ' + res.message); }
      } catch (err) { alert('Kesalahan: ' + err.message); }
      finally { btn.disabled = false; btn.innerText = 'Kirim Pesanan'; }
    }
    function resetWebForm() {
      document.getElementById('successModal').classList.remove('active');
      for (const k in cart) delete cart[k];
      renderMenu();
    }
    renderMenu();
  </script>
</body>
</html>
    """.trimIndent()
  }

  private fun generateWebReportsHtml(): String {
    val reportData = currentReportsProvider?.invoke()
    val summary = reportData?.first
    val orders = reportData?.second.orEmpty()

    val totalRevenue = summary?.totalRevenue ?: 0.0
    val estimatedProfit = summary?.estimatedProfit ?: 0.0
    val totalOrders = summary?.totalOrders ?: 0
    val totalCash = summary?.totalCash ?: 0.0
    val totalQris = summary?.totalQris ?: 0.0

    return """
<!DOCTYPE html>
<html lang="id">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Laporan Transaksi & Laba - DapurKasir Real-Time Database</title>
  <!-- Firebase App & Realtime Database SDK (Bebas Firestore) -->
  <script src="https://www.gstatic.com/firebasejs/9.23.0/firebase-app-compat.js"></script>
  <script src="https://www.gstatic.com/firebasejs/9.23.0/firebase-database-compat.js"></script>
  <style>
    :root {
      --primary: #E65100;
      --primary-light: #FFF3E0;
      --green: #2E7D32;
      --green-light: #E8F5E9;
      --bg: #121320;
      --surface: #1E2034;
      --surface-inner: #151627;
      --text: #FFFFFF;
      --muted: #9FA2BD;
      --border: #2C2E46;
    }
    * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif; }
    body { background-color: var(--bg); color: var(--text); padding: 20px; }
    .container { max-width: 1040px; margin: 0 auto; }
    header { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 14px; margin-bottom: 24px; padding-bottom: 18px; border-bottom: 1px solid var(--border); }
    .header-title h1 { font-size: 22px; font-weight: 800; color: #FFA726; display: flex; align-items: center; gap: 8px; }
    .header-title p { font-size: 13px; color: var(--muted); margin-top: 4px; }
    .badge-sync { display: inline-flex; align-items: center; gap: 8px; background: rgba(46, 125, 50, 0.2); border: 1px solid #81C784; color: #81C784; padding: 6px 14px; border-radius: 20px; font-size: 12px; font-weight: 700; }
    .pulse-dot { width: 8px; height: 8px; background: #81C784; border-radius: 50%; box-shadow: 0 0 8px #81C784; }
    .mode-badge { display: inline-block; background: #E65100; color: #FFF; font-size: 11px; font-weight: 800; padding: 2px 8px; border-radius: 6px; margin-left: 8px; vertical-align: middle; }
    .actions-bar { display: flex; gap: 10px; margin-bottom: 20px; flex-wrap: wrap; }
    .btn { padding: 9px 16px; border-radius: 10px; font-size: 13px; font-weight: 700; cursor: pointer; border: none; display: inline-flex; align-items: center; gap: 6px; }
    .btn-primary { background: #E65100; color: white; }
    .btn-outline { background: var(--surface); border: 1px solid var(--border); color: #FFF; }
    .btn:hover { opacity: 0.9; }
    .metrics-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 16px; margin-bottom: 24px; }
    .metric-card { background: var(--surface); padding: 20px; border-radius: 16px; border: 1px solid var(--border); box-shadow: 0 4px 12px rgba(0,0,0,0.2); }
    .metric-label { font-size: 12.5px; font-weight: 600; color: var(--muted); }
    .metric-value { font-size: 24px; font-weight: 800; color: #FFA726; margin: 8px 0 4px; }
    .metric-value.profit { color: #81C784; }
    .metric-sub { font-size: 11.5px; color: var(--muted); }
    .dashboard-row { display: grid; grid-template-columns: repeat(auto-fit, minmax(320px, 1fr)); gap: 16px; margin-bottom: 24px; }
    .card { background: var(--surface); padding: 20px; border-radius: 16px; border: 1px solid var(--border); box-shadow: 0 4px 12px rgba(0,0,0,0.2); }
    .card-title { font-size: 15px; font-weight: 700; color: #FFF; margin-bottom: 16px; display: flex; justify-content: space-between; align-items: center; }
    .prog-bar-container { margin-bottom: 14px; }
    .prog-label { display: flex; justify-content: space-between; font-size: 12.5px; font-weight: 600; margin-bottom: 6px; color: #C0C2D6; }
    .prog-track { height: 10px; background: #26283E; border-radius: 5px; overflow: hidden; }
    .prog-fill { height: 100%; border-radius: 5px; transition: width 0.3s; }
    table { width: 100%; border-collapse: collapse; margin-top: 10px; font-size: 12.5px; }
    th { text-align: left; padding: 12px 10px; background: #151627; color: var(--muted); font-weight: 700; border-bottom: 1px solid var(--border); }
    td { padding: 12px 10px; border-bottom: 1px solid #23253B; color: #E0E2EC; }
    .badge-method { display: inline-block; padding: 4px 8px; border-radius: 6px; font-size: 11px; font-weight: 700; }
    .badge-tunai { background: rgba(46, 125, 50, 0.25); color: #81C784; border: 1px solid #4CAF50; }
    .badge-qris { background: rgba(230, 81, 0, 0.25); color: #FFB74D; border: 1px solid #FF9800; }
    .badge-status { padding: 3px 8px; border-radius: 6px; font-size: 11px; font-weight: 700; background: #1B5E20; color: #C8E6C9; }
  </style>
</head>
<body>
  <div class="container">
    <header>
      <div class="header-title">
        <h1>
          🔥 Dashboard Laporan Transaksi & Laba
          <span class="mode-badge">REALTIME DATABASE</span>
        </h1>
        <p>DapurKasir POS • Menggunakan Realtime Database (Bebas Firestore)</p>
      </div>
      <div class="badge-sync">
        <span class="pulse-dot"></span>
        <span id="syncStatusText">Firebase Realtime DB Terhubung</span>
      </div>
    </header>

    <div class="actions-bar">
      <button class="btn btn-outline" onclick="refreshAllData()">🔄 Refresh Data</button>
      <button class="btn btn-outline" onclick="window.print()">🖨️ Cetak Laporan</button>
      <button class="btn btn-primary" onclick="exportCsv()">📥 Unduh CSV Transaksi</button>
    </div>

    <!-- METRICS OVERVIEW -->
    <div class="metrics-grid">
      <div class="metric-card">
        <div class="metric-label">Total Omset Penjualan</div>
        <div class="metric-value" id="valRevenue">Rp 0</div>
        <div class="metric-sub" id="valOrdersCount">0 Transaksi Sukses</div>
      </div>
      <div class="metric-card">
        <div class="metric-label">Estimasi Laba Bersih (Margin ~52%)</div>
        <div class="metric-value profit" id="valProfit">Rp 0</div>
        <div class="metric-sub">HPP Bahan Baku Otomatis</div>
      </div>
      <div class="metric-card">
        <div class="metric-label">Rata-rata Transaksi (AOV)</div>
        <div class="metric-value" id="valAvg">Rp 0</div>
        <div class="metric-sub">Per Struk Penjualan</div>
      </div>
    </div>

    <!-- METRICS CHARTS & STATUS -->
    <div class="dashboard-row">
      <div class="card">
        <div class="card-title">Metode Pembayaran</div>
        <div class="prog-bar-container">
          <div class="prog-label"><span>Tunai (Cash)</span><span id="cashLabel">Rp 0</span></div>
          <div class="prog-track"><div class="prog-fill" id="cashBar" style="width: 50%; background: #4CAF50;"></div></div>
        </div>
        <div class="prog-bar-container">
          <div class="prog-label"><span>QRIS Digital</span><span id="qrisLabel">Rp 0</span></div>
          <div class="prog-track"><div class="prog-fill" id="qrisBar" style="width: 50%; background: #FF9800;"></div></div>
        </div>
      </div>

      <div class="card">
        <div class="card-title">
          <span>Realtime Database Status</span>
          <span class="badge-status">AKTIF REALTIME</span>
        </div>
        <p style="font-size: 12.5px; color: var(--muted); line-height: 1.6; margin-bottom: 8px;">
          Laporan transaksi dan laba ditransmisikan secara instan ke node <strong>sales_reports</strong> dan <strong>transactions</strong> di Firebase Realtime Database tanpa menggunakan Firestore.
        </p>
        <div style="font-size: 11.5px; color: #FFA726; background: #131422; padding: 8px 12px; border-radius: 8px; border: 1px solid var(--border);">
          Node Aktif: /sales_reports, /laporan, /transactions, /laporan_transaksi
        </div>
      </div>
    </div>

    <!-- TRANSACTIONS TABLE -->
    <div class="card">
      <div class="card-title">
        <span>Riwayat Transaksi Penjualan Terkini</span>
        <span id="txCountBadge" style="font-size: 12px; color: var(--muted); font-weight: normal;">0 Nota</span>
      </div>
      <div style="overflow-x: auto;">
        <table>
          <thead>
            <tr>
              <th>No. Faktur</th>
              <th>Waktu</th>
              <th>Pelanggan</th>
              <th>Tipe</th>
              <th>Rincian Menu</th>
              <th>Metode</th>
              <th style="text-align: right;">Total</th>
            </tr>
          </thead>
          <tbody id="txTableBody">
            <tr><td colspan="7" style="text-align: center; color: var(--muted); padding: 24px;">Menghubungkan ke Realtime Database...</td></tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>

  <script>
    function formatRp(num) { return 'Rp ' + Math.round(num || 0).toLocaleString('id-ID'); }
    let globalTransactions = [];

    // Optional Direct Realtime Database Listener via Firebase JS SDK
    const firebaseConfig = {
      projectId: "gen-lang-client-0951595248",
      databaseURL: "https://gen-lang-client-0951595248-default-rtdb.firebaseio.com"
    };

    let rtdb = null;
    try {
      if (typeof firebase !== 'undefined') {
        const app = firebase.initializeApp(firebaseConfig);
        rtdb = firebase.database();
        // Listen to today's reports directly from Realtime Database
        const todayStr = new Date().toISOString().slice(0, 10);
        rtdb.ref('sales_reports/' + todayStr).on('value', (snap) => {
          if (snap.exists()) {
            const data = snap.val();
            updateSummaryUI(data);
          }
        });
        // Listen to transactions node directly from Realtime Database
        rtdb.ref('transactions').limitToLast(50).on('value', (snap) => {
          if (snap.exists()) {
            const val = snap.val();
            const list = Object.values(val).reverse();
            renderTransactions(list);
          }
        });
      }
    } catch(e) {
      console.log('Direct Firebase RTDB connect fallback to local API server:', e.message);
    }

    function updateSummaryUI(data) {
      document.getElementById('valRevenue').innerText = formatRp(data.totalRevenue);
      document.getElementById('valProfit').innerText = formatRp(data.estimatedProfit);
      document.getElementById('valOrdersCount').innerText = (data.totalOrders || 0) + ' Transaksi Sukses';
      document.getElementById('valAvg').innerText = formatRp(data.averageTicket || (data.totalRevenue / (data.totalOrders || 1)));
      const total = data.totalRevenue || 1;
      const cashPct = Math.min(100, Math.round(((data.totalCash || 0) / total) * 100));
      const qrisPct = Math.min(100, Math.round(((data.totalQris || 0) / total) * 100));
      document.getElementById('cashLabel').innerText = formatRp(data.totalCash) + ' (' + cashPct + '%)';
      document.getElementById('cashBar').style.width = cashPct + '%';
      document.getElementById('qrisLabel').innerText = formatRp(data.totalQris) + ' (' + qrisPct + '%)';
      document.getElementById('qrisBar').style.width = qrisPct + '%';
    }

    function renderTransactions(txList) {
      globalTransactions = txList;
      document.getElementById('txCountBadge').innerText = txList.length + ' Nota Tersimpan';
      const tbody = document.getElementById('txTableBody');
      tbody.innerHTML = '';
      if (!txList || txList.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" style="text-align: center; color: var(--muted); padding: 20px;">Belum ada transaksi di database</td></tr>';
        return;
      }
      txList.forEach(tx => {
        const d = new Date(tx.timestamp || Date.now());
        const timeStr = d.getHours().toString().padStart(2, '0') + ':' + d.getMinutes().toString().padStart(2, '0');
        const isQris = (tx.paymentMethod || '').toUpperCase() === 'QRIS';
        const tr = document.createElement('tr');
        tr.innerHTML = `
          <td style="font-weight: 700; color: #FFA726;">${'$'}{tx.invoiceNumber}</td>
          <td>${'$'}{timeStr}</td>
          <td style="font-weight: 600;">${'$'}{tx.customerName}</td>
          <td style="color: var(--muted);">${'$'}{tx.orderType || 'Dine-In'}</td>
          <td style="color: #B0B3C7;">${'$'}{tx.itemsSummary || '-'}</td>
          <td><span class="badge-method ${'$'}{isQris ? 'badge-qris' : 'badge-tunai'}">${'$'}{tx.paymentMethod}</span></td>
          <td style="text-align: right; font-weight: 800; color: #FFF;">${'$'}{formatRp(tx.finalAmount || tx.totalAmount)}</td>
        `;
        tbody.appendChild(tr);
      });
    }

    async function fetchLiveReports() {
      try {
        const resp = await fetch('/api/reports');
        const data = await resp.json();
        if (data.status === 'success') {
          updateSummaryUI(data);
          renderTransactions(data.transactions || []);
        }
      } catch (err) {
        console.error('Error fetching reports from local server:', err);
      }
    }

    function refreshAllData() {
      fetchLiveReports();
    }

    function exportCsv() {
      if (!globalTransactions || globalTransactions.length === 0) {
        alert('Tidak ada transaksi untuk diekspor');
        return;
      }
      let csv = 'Invoice,Pelanggan,Tipe,Metode,Total\n';
      globalTransactions.forEach(tx => {
        csv += `"${'$'}{tx.invoiceNumber}","${'$'}{tx.customerName}","${'$'}{tx.orderType}","${'$'}{tx.paymentMethod}",${'$'}{tx.finalAmount || tx.totalAmount}\n`;
      });
      const blob = new Blob([csv], { type: 'text/csv' });
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.setAttribute('href', url);
      a.setAttribute('download', 'laporan_transaksi_rtdb_' + Date.now() + '.csv');
      a.click();
    }

    // Polling awal dan periodik
    fetchLiveReports();
    setInterval(fetchLiveReports, 4000);
  </script>
</body>
</html>
    """.trimIndent()
  }

  private fun escapeJson(str: String): String {
    return str.replace("\\", "\\\\")
      .replace("\"", "\\\"")
      .replace("\n", "\\n")
      .replace("\r", "")
  }

  companion object {
    private const val TAG = "WebOrderServer"

    fun getLocalIpAddress(): String {
      try {
        val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
        for (intf in interfaces) {
          val addrs = Collections.list(intf.inetAddresses)
          for (addr in addrs) {
            if (!addr.isLoopbackAddress) {
              val sAddr = addr.hostAddress ?: continue
              val isIPv4 = sAddr.indexOf(':') < 0
              if (isIPv4 && (sAddr.startsWith("192.168.") || sAddr.startsWith("10.") || sAddr.startsWith("172."))) {
                return sAddr
              }
            }
          }
        }
      } catch (_: Exception) {}
      return "127.0.0.1"
    }
  }
}
