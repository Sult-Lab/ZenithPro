package com.techsultan.zenithpro.core.domain

import com.techsultan.zenithpro.core.data.local.PrinterDevice

interface PrinterDriver {

    suspend fun connect(
        device: PrinterDevice
    ): Result<Unit>

    suspend fun print(
        formattedText: String
    ): Result<Unit>

    suspend fun disconnect()

    fun isConnected(): Boolean
}