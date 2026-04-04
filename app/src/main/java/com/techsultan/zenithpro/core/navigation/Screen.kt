package com.techsultan.zenithpro.core.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface Route: NavKey {

    @Serializable
    data object Auth : Route, NavKey {
        @Serializable
        data object SignIn : Route, NavKey
        @Serializable
        data object SignUp : Route, NavKey
    }

    @Serializable
    data object Home : Route, NavKey {
        @Serializable
        data object Dashboard : Route, NavKey
        @Serializable
        data object Inventory : Route, NavKey
        @Serializable
        data object Sales : Route, NavKey
        @Serializable
        data object Reports : Route, NavKey
        @Serializable
        data object Settings : Route, NavKey
        @Serializable
        data object AddProduct : Route, NavKey
        @Serializable
        data object NewSale : Route, NavKey
        @Serializable
        data object Customers : Route, NavKey
        @Serializable
        data object Expenses : Route, NavKey
        @Serializable
        data object Branches : Route, NavKey
    }

}