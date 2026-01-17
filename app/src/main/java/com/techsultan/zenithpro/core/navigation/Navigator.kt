package com.techsultan.zenithpro.core.navigation

class Navigator(val navigationState: NavigationState) {

    fun navigate(route: Route){
        if (route in navigationState.backStacks.keys){
            navigationState.topLevelRoute = route
        } else {
            navigationState.backStacks[navigationState.topLevelRoute]?.add(route)
        }
    }

    fun goBack(){
        val currentStack = navigationState.backStacks[navigationState.topLevelRoute]
            ?: error("Back stack not found for ${navigationState.topLevelRoute}")

        val currentRoute = currentStack.last()

        if (currentRoute == navigationState.topLevelRoute){
            navigationState.topLevelRoute = navigationState.startRoute
        } else {
            currentStack.removeLastOrNull()
        }
    }
}