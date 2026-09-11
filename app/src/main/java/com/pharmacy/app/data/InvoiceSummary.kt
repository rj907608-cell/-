package com.pharmacy.app.data

/**
 * ملخص الفاتورة المجمعة لعرضها في سجل المبيعات
 * كل سطر يمثل فاتورة واحدة: رقم الفاتورة، الوقت، الإجمالي، عدد الأدوية
 */
data class InvoiceSummary(
    val invoiceId: String,
    val timestamp: Long,
    val totalAmount: Double,
    val totalProfit: Double,
    val itemsCount: Int, // عدد الأصناف أو الأدوية المختلفة في الفاتورة
    val totalUnitsSold: Int, // إجمالي عدد القطع المباعة
    val items: List<SaleRecordEntity>
)
