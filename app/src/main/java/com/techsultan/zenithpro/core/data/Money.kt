package com.techsultan.zenithpro.core.data

import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class Money(val amount: Long)

