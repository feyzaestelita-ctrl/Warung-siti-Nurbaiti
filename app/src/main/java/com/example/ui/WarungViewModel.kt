package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.WarungDatabase
import com.example.data.model.OrderEntity
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentMethod
import com.example.data.preferences.PaymentConfig
import com.example.data.preferences.PaymentPreferencesRepository
import com.example.data.repository.OrderRepository
import com.example.notification.OrderNotificationHelper
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OrderFormUiState(
    val customerName: String = "",
    val whatsappNumber: String = "",
    val orderedIngredients: String = "",
    val additionalNotes: String = "",
    val selectedPaymentMethod: PaymentMethod = PaymentMethod.QRIS,
    val nameError: String? = null,
    val whatsappError: String? = null,
    val ingredientsError: String? = null,
    val isSubmitting: Boolean = false,
    val confirmedOrder: OrderEntity? = null
)

class WarungViewModel(application: Application) : AndroidViewModel(application) {

    private val database = WarungDatabase.getInstance(application)
    private val orderRepository = OrderRepository(database.orderDao())
    private val paymentPreferences = PaymentPreferencesRepository(application)

    private val _formState = MutableStateFlow(OrderFormUiState())
    val formState: StateFlow<OrderFormUiState> = _formState.asStateFlow()

    val paymentConfig: StateFlow<PaymentConfig> = paymentPreferences.paymentConfig
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PaymentConfig()
        )

    val orders: StateFlow<List<OrderEntity>> = orderRepository.allOrders
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val unreadOrderCount: StateFlow<Int> = orderRepository.unreadCount
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    private val _snackbarEvents = MutableSharedFlow<String>()
    val snackbarEvents: SharedFlow<String> = _snackbarEvents.asSharedFlow()

    init {
        OrderNotificationHelper.ensureNotificationChannel(application)
        viewModelScope.launch {
            paymentConfig.collect { config ->
                val current = _formState.value.selectedPaymentMethod
                if (current == PaymentMethod.QRIS && !config.isQrisEnabled && config.isCodEnabled) {
                    _formState.update { it.copy(selectedPaymentMethod = PaymentMethod.COD) }
                } else if (current == PaymentMethod.COD && !config.isCodEnabled && config.isQrisEnabled) {
                    _formState.update { it.copy(selectedPaymentMethod = PaymentMethod.QRIS) }
                }
            }
        }
    }

    fun onCustomerNameChanged(value: String) {
        _formState.update { it.copy(customerName = value, nameError = null) }
    }

    fun onWhatsappChanged(value: String) {
        val filtered = value.filter { it.isDigit() || it == '+' || it == ' ' || it == '-' }
        _formState.update { it.copy(whatsappNumber = filtered, whatsappError = null) }
    }

    fun onIngredientsChanged(value: String) {
        _formState.update { it.copy(orderedIngredients = value, ingredientsError = null) }
    }

    fun onNotesChanged(value: String) {
        _formState.update { it.copy(additionalNotes = value) }
    }

    fun onPaymentMethodSelected(method: PaymentMethod) {
        _formState.update { it.copy(selectedPaymentMethod = method) }
    }

    fun submitOrder() {
        val state = _formState.value
        val config = paymentConfig.value

        val cleanName = state.customerName.trim()
        val cleanWhatsapp = state.whatsappNumber.trim()
        val cleanDigits = cleanWhatsapp.filter { it.isDigit() }
        val cleanIngredients = state.orderedIngredients.trim()
        val cleanNotes = state.additionalNotes.trim()

        val nameErr = if (cleanName.isEmpty()) "Nama pemesan wajib diisi" else null
        val waErr = when {
            cleanWhatsapp.isEmpty() -> "Nomor WhatsApp wajib diisi"
            cleanDigits.length < 9 -> "Masukkan nomor WhatsApp yang valid (minimal 9 digit)"
            else -> null
        }
        val ingErr = if (cleanIngredients.isEmpty()) "Tuliskan bahan makanan dapur yang dipesan" else null

        if (nameErr != null || waErr != null || ingErr != null) {
            _formState.update {
                it.copy(
                    nameError = nameErr,
                    whatsappError = waErr,
                    ingredientsError = ingErr
                )
            }
            return
        }

        val chosenMethod = state.selectedPaymentMethod
        if ((chosenMethod == PaymentMethod.QRIS && !config.isQrisEnabled) ||
            (chosenMethod == PaymentMethod.COD && !config.isCodEnabled)
        ) {
            viewModelScope.launch {
                _snackbarEvents.emit("Metode pembayaran ${chosenMethod.label} sedang dinonaktifkan owner")
            }
            return
        }

        viewModelScope.launch {
            _formState.update { it.copy(isSubmitting = true) }
            val orderCode = "WN-${(1000..9999).random()}"
            val newOrder = OrderEntity(
                orderCode = orderCode,
                customerName = cleanName,
                whatsappNumber = cleanWhatsapp,
                orderedIngredients = cleanIngredients,
                additionalNotes = cleanNotes,
                paymentMethod = chosenMethod.label,
                status = OrderStatus.BARU.name,
                createdAt = System.currentTimeMillis(),
                isReadByOwner = false
            )
            val insertedId = orderRepository.insertOrder(newOrder)
            val savedOrder = newOrder.copy(id = insertedId)

            val notified = OrderNotificationHelper.showNewOrderNotification(
                getApplication(),
                savedOrder
            )

            _formState.update {
                OrderFormUiState(
                    selectedPaymentMethod = chosenMethod,
                    isSubmitting = false,
                    confirmedOrder = savedOrder
                )
            }

            if (!notified) {
                _snackbarEvents.emit("Pesanan tersimpan. Aktifkan izin notifikasi di menu Pesanan agar notif masuk ke HP.")
            }
        }
    }

    fun dismissOrderConfirmation() {
        _formState.update { it.copy(confirmedOrder = null) }
    }

    fun uploadQrisImage(uri: Uri) {
        viewModelScope.launch {
            val saved = paymentPreferences.saveQrisImageFromUri(uri)
            _snackbarEvents.emit(
                if (saved) "Gambar QRIS Warung Nurbaiti berhasil disimpan"
                else "Gagal memuat gambar QRIS, silakan coba gambar lain"
            )
        }
    }

    fun removeQrisImage() {
        viewModelScope.launch {
            paymentPreferences.removeQrisImage()
            _snackbarEvents.emit("Gambar QRIS dihapus")
        }
    }

    fun savePaymentSettings(
        merchantName: String,
        qrisNotes: String,
        ownerWhatsapp: String,
        isQrisEnabled: Boolean,
        isCodEnabled: Boolean
    ) {
        if (!isQrisEnabled && !isCodEnabled) {
            viewModelScope.launch {
                _snackbarEvents.emit("Aktifkan minimal satu metode pembayaran (QRIS atau COD)")
            }
            return
        }
        viewModelScope.launch {
            paymentPreferences.updatePaymentSettings(
                merchantName = merchantName,
                qrisNotes = qrisNotes,
                ownerWhatsapp = ownerWhatsapp,
                isQrisEnabled = isQrisEnabled,
                isCodEnabled = isCodEnabled
            )
            _snackbarEvents.emit("Pengaturan pembayaran berhasil diperbarui")
        }
    }

    fun updateOrderStatus(orderId: Long, status: OrderStatus) {
        viewModelScope.launch {
            orderRepository.updateOrderStatus(orderId, status.name)
            _snackbarEvents.emit("Status pesanan diubah menjadi ${status.label}")
        }
    }

    fun markAllOrdersRead() {
        viewModelScope.launch {
            orderRepository.markAllAsRead()
        }
    }

    fun deleteOrder(orderId: Long) {
        viewModelScope.launch {
            orderRepository.deleteOrder(orderId)
            _snackbarEvents.emit("Pesanan dihapus")
        }
    }

    fun triggerTestNotification() {
        viewModelScope.launch {
            val sent = OrderNotificationHelper.showTestNotification(getApplication())
            _snackbarEvents.emit(
                if (sent) "Notifikasi uji berhasil dikirim ke HP Anda"
                else "Izin notifikasi belum aktif di HP Anda"
            )
        }
    }
}
