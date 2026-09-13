package com.pharmacy.app.data.sync

import com.pharmacy.app.data.MedicineEntity
import com.pharmacy.app.data.SaleRecordEntity
import org.json.JSONObject

/**
 * تحويل الكيانات المحلية إلى JSON لقائمة انتظار المزامنة
 */
object SyncJsonHelper {

    fun medicineToJson(medicine: MedicineEntity): String {
        return JSONObject().apply {
            put("local_id", medicine.id)
            put("name", medicine.name)
            put("barcode", medicine.barcode)
            put("buy_price", medicine.buyPrice)
            put("sell_price", medicine.sellPrice)
            put("quantity", medicine.quantity)
            put("min_stock_alert", medicine.minStockAlert)
            put("category", medicine.category)
            put("location", medicine.location)
            put("expiry_date", medicine.expiryDate)
            put("batches_json", medicine.batchesJson)
        }.toString()
    }

    fun saleToJson(sale: SaleRecordEntity): String {
        return JSONObject().apply {
            put("local_id", sale.id)
            put("invoice_id", sale.invoiceId)
            put("medicine_id", sale.medicineId)
            put("medicine_name", sale.medicineName)
            put("barcode", sale.barcode)
            put("quantity_sold", sale.quantitySold)
            put("unit_cost_price", sale.unitCostPrice)
            put("unit_sell_price", sale.unitSellPrice)
            put("total_sell_price", sale.totalSellPrice)
            put("total_profit", sale.totalProfit)
            put("timestamp", sale.timestamp)
        }.toString()
    }

    fun invoiceDeleteToJson(invoiceId: String): String {
        return JSONObject().apply {
            put("invoiceId", invoiceId)
        }.toString()
    }
}
