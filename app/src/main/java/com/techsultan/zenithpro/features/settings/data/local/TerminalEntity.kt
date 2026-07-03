package com.techsultan.zenithpro.features.settings.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "terminals")
data class TerminalEntity(
    @PrimaryKey val id: String,
    val businessId: String,
    val branchId: String,
    val name: String,
    val serialNumber: String?,
    val isActive: Boolean,
    val nombaSubAccountId: String?,
    val nombaVirtualAccountNumber: String?,
    val nombaVirtualAccountBank: String?,
    val nombaVirtualAccountName: String?,
    val nombaSweepBankCode: String?,
    val nombaSweepAccountNumber: String?,
    val nombaSweepAccountName: String?,
    val nombaOnboardedAt: String?,
    val createdAt: String,
    val updatedAt: String
) {
    val isNombaOnboarded: Boolean get() = nombaVirtualAccountNumber != null
    val maskedSweepAccount: String?
        get() = nombaSweepAccountNumber?.let { acc ->
            if (acc.length >= 6)
                "${acc.take(3)}****${acc.takeLast(3)}"
            else acc
        }
}