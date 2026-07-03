package com.techsultan.zenithpro.features.settings.data.mapper

import com.techsultan.zenithpro.features.settings.data.local.TerminalEntity
import com.techsultan.zenithpro.features.settings.data.remote.TerminalDto

fun TerminalDto.toEntity() = TerminalEntity(
    id = id,
    businessId = businessId,
    branchId = branchId,
    name = name,
    serialNumber = serialNumber,
    isActive = isActive,
    nombaSubAccountId = nombaSubAccountId,
    nombaVirtualAccountNumber = nombaVirtualAccountNumber,
    nombaVirtualAccountBank = nombaVirtualAccountBank,
    nombaVirtualAccountName = nombaVirtualAccountName,
    nombaSweepBankCode = nombaSweepBankCode,
    nombaSweepAccountNumber = nombaSweepAccountNumber,
    nombaSweepAccountName = nombaSweepAccountName,
    nombaOnboardedAt = nombaOnboardedAt,
    createdAt = createdAt,
    updatedAt = updatedAt
)