package com.example.smarthome.navigation

/**
 * All navigation routes in the app.
 */
object Routes {
    const val LOGIN = "login"
    const val SIGNUP = "signup"
    const val FLOOR_PLAN_LIST = "floor_plans"
    const val FLOOR_PLAN_GRID = "floor_plan/{floorPlanId}"
    const val REPORTING = "reporting"

    fun floorPlanGrid(floorPlanId: String) = "floor_plan/$floorPlanId"
}
