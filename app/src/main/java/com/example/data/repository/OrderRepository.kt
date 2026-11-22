package com.example.data.repository

import com.example.data.local.OrderDao
import com.example.data.model.OrderEntity
import kotlinx.coroutines.flow.Flow

class OrderRepository(private val orderDao: OrderDao) {
    val allOrders: Flow<List<OrderEntity>> = orderDao.getAllOrders()
    val unreadCount: Flow<Int> = orderDao.getUnreadCount()

    suspend fun insertOrder(order: OrderEntity): Long {
        return orderDao.insertOrder(order)
    }

    suspend fun updateOrderStatus(orderId: Long, status: String) {
        orderDao.updateOrderStatus(orderId, status)
    }

    suspend fun markAllAsRead() {
        orderDao.markAllAsRead()
    }

    suspend fun deleteOrder(orderId: Long) {
        orderDao.deleteOrderById(orderId)
    }
}
