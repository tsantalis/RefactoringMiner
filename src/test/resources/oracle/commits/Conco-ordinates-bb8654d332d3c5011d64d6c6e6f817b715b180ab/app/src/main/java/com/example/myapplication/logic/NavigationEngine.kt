package com.example.myapplication.logic

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import com.example.myapplication.data.Building

/**
 * Abstracts navigation calculations so [MapViewModel] never depends
 * on a concrete implementation.
 *
 * [checkArrivalWithBuilding] is part of the interface because the campus
 * navigation use-case requires building-aware arrival detection — callers
 * should not need to downcast to [CampusNavigationEngine] to access it.
 * Previously the unsafe cast `navigationEngine as CampusNavigationEngine`
 * in MapViewModel violated LSP and made the interface pointless.
 */
interface NavigationEngine {
    fun calculateNextInstruction(userPos: LatLng, route: List<LatLng>): String
    fun checkArrival(userPos: LatLng, destination: LatLng): Boolean
    fun checkArrivalWithBuilding(userPos: LatLng, targetBuilding: Building?): Boolean
    fun calculateBearing(userPos: LatLng, route: List<LatLng>, currentBearing: Float): Float
}

class CampusNavigationEngine : NavigationEngine {

    override fun checkArrivalWithBuilding(userPos: LatLng, targetBuilding: Building?): Boolean {
        if (targetBuilding == null) return false
        val distance = SphericalUtil.computeDistanceBetween(userPos, targetBuilding.getCenter())
        return distance < 50.0
    }

    override fun checkArrival(userPos: LatLng, destination: LatLng): Boolean =
        SphericalUtil.computeDistanceBetween(userPos, destination) < 15.0

    override fun calculateBearing(userPos: LatLng, route: List<LatLng>, currentBearing: Float): Float {
        val target = route.firstOrNull { SphericalUtil.computeDistanceBetween(userPos, it) > 12.0 }
        return if (target != null) {
            SphericalUtil.computeHeading(userPos, target).toFloat()
        } else {
            currentBearing
        }
    }

    override fun calculateNextInstruction(userPos: LatLng, route: List<LatLng>): String =
        "Proceed toward destination"
}
