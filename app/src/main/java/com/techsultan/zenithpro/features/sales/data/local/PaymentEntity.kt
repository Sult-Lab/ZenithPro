package com.techsultan.zenithpro.features.sales.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "payments",
    foreignKeys = [
        ForeignKey(
            entity = SaleEntity::class,
            parentColumns = ["id"],
            childColumns = ["saleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("saleId"),
        Index("businessId"),
        Index("checkoutReference", unique = true),
        Index("providerReference")
    ]
)
data class PaymentEntity(
    @PrimaryKey val id: String,
    val saleId: String,
    val businessId: String,
    val provider: String = "NOMBA",
    val status: String,          // PaymentStatus enum name
    val amount: Long,            // in kobo
    val currency: String = "NGN",
    val providerReference: String? = null,   // Nomba transaction ID
    val checkoutReference: String? = null,   // Nomba order reference
    val authorizationCode: String? = null,
    val paymentMethod: String,               // PaymentMethod enum name
    val transferType: String? = null,
    val syncStatus: String = "PENDING",      // SyncStatus enum name
    val createdAt: String,
    val updatedAt: String
)