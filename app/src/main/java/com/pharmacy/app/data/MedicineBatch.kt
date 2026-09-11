package com.pharmacy.app.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * دفعة توريد دواء في المخزون (Medicine Batch)
 * تتيح تتبع الكميات والأسعار لكل دفعة بشكل منفصل عند اختلاف السعر
 */
data class MedicineBatch(
    val batchNumber: Int,
    val quantity: Int,
    val buyPrice: Double = 0.0,
    val sellPrice: Double = 0.0,
    val dateAdded: Long = System.currentTimeMillis()
)

/**
 * محول دفعات الأدوية من وإلى JSON باستخدام مكتبة أندرويد القياسية
 */
object BatchConverter {

    fun fromJson(json: String?): List<MedicineBatch> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(json)
            val list = mutableListOf<MedicineBatch>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    MedicineBatch(
                        batchNumber = obj.optInt("batchNumber", i + 1),
                        quantity = obj.optInt("quantity", 0),
                        buyPrice = obj.optDouble("buyPrice", 0.0),
                        sellPrice = obj.optDouble("sellPrice", 0.0),
                        dateAdded = obj.optLong("dateAdded", System.currentTimeMillis())
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun toJson(batches: List<MedicineBatch>): String {
        val array = JSONArray()
        for (b in batches) {
            val obj = JSONObject()
            obj.put("batchNumber", b.batchNumber)
            obj.put("quantity", b.quantity)
            obj.put("buyPrice", b.buyPrice)
            obj.put("sellPrice", b.sellPrice)
            obj.put("dateAdded", b.dateAdded)
            array.put(obj)
        }
        return array.toString()
    }
}
