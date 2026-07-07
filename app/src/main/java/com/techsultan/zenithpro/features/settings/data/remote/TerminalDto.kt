package com.techsultan.zenithpro.features.settings.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TerminalDto(
    val id: String,
    @SerialName("business_id") val businessId: String,
    @SerialName("branch_id") val branchId: String,
    val name: String,
    @SerialName("serial_number") val serialNumber: String? = null,
    @SerialName("is_active") val isActive: Boolean,
    @SerialName("nomba_sub_account_id") val nombaSubAccountId: String? = null,
    @SerialName("nomba_virtual_account_number") val nombaVirtualAccountNumber: String? = null,
    @SerialName("nomba_virtual_account_bank") val nombaVirtualAccountBank: String? = null,
    @SerialName("nomba_virtual_account_name") val nombaVirtualAccountName: String? = null,
    @SerialName("nomba_sweep_bank_code") val nombaSweepBankCode: String? = null,
    @SerialName("nomba_sweep_account_number") val nombaSweepAccountNumber: String? = null,
    @SerialName("nomba_sweep_account_name") val nombaSweepAccountName: String? = null,
    @SerialName("nomba_onboarded_at")  val nombaOnboardedAt: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)