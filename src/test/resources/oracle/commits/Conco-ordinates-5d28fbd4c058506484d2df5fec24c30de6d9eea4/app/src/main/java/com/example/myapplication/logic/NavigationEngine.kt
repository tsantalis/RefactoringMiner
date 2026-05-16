package com.example.myapplication.logic

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil

/**
 * Abstracts navigation calculations so [MapViewModel] never depends
 * on a concrete implementation.
 *
 * [checkArrivalWithBuilding] now accepts a [LatLng] center and radius
 * instead of a [Building] model — the logic layer no longer imports
 * from the data layer, satisfying the Dependency Rule.
 *
 * MapViewModel is responsible for extracting the building center and
 * passing it here, which is appropriate since it already owns [uiBuildingState].
 */
interface NavigationEngine {
    fun calculateNextInstruction(userPos: LatLng, route: List<LatLng>): String
    fun checkArrival(userPos: LatLng, destination: LatLng): Boolean

    /**
     * Returns true when [userPos] is within [radiusMetres] of [buildingCenter].
     *
     * Accepts a pre-computed center rather than a Building object so the
     * logic layer has no dependency on the data layer (Dependency Rule).
     */
    fun checkArrivalWithBuilding(
        userPos:        LatLng,
        buildingCenter: LatLng?,
        radiusMetres:   Double = 50.0
    ): Boolean

    fun calculateBearing(userPos: LatLng, route: List<LatLng>, currentBearing: Float): Float
}

class CampusNavigationEngine : NavigationEngine {

    override fun checkArrivalWithBuilding(
        userPos:        LatLng,
        buildingCenter: LatLng?,
        radiusMetres:   Double
    ): Boolean {
        if (buildingCenter == null) return false
        return SphericalUtil.computeDistanceBetween(userPos, buildingCenter) < radiusMetres
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
