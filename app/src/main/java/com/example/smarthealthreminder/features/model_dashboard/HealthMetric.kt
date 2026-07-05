package com.example.smarthealthreminder.features.model_dashboard
enum class MetricType {
    STEPS, SLEEP, HEART_RATE
}

data class HealthMetric(
    val id: Int = 0,
    val userId: Int,
    val type: MetricType,
    val value: String,
    val unit: String? = null,
    val date: String,
    val source: String? = null,
    val createdAt: String? = null
)