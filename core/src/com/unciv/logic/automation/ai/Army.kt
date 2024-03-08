package com.unciv.logic.automation.ai

import com.unciv.logic.automation.unit.UnitAutomation
import com.unciv.logic.city.City
import com.unciv.logic.civilization.Civilization
import com.unciv.logic.map.mapunit.MapUnit
import com.unciv.logic.map.tile.Tile
import kotlin.math.log10
import kotlin.math.roundToInt

class Army(civInfo: Civilization, armyOrigin: Tile, unitDistance: Int) {

    val civInfo: Civilization
    val unitDistance: Int

    val centerTile: Tile
    var ownedMilitaryUnits: HashSet<MapUnit> = HashSet()
    var closestCity: City?

    var enemyMilitaryUnits: HashSet<MapUnit> = HashSet()
    var enemyCities: HashSet<City> = HashSet()
    var combatEvaluation: Double = 0.0


    init {
        this.civInfo = civInfo
        this.unitDistance = unitDistance
        generateArmyFromPosition(armyOrigin)
        centerTile = if (ownedMilitaryUnits.isNotEmpty()) findArmyCenter()
        else armyOrigin
        closestCity = civInfo.cities.minByOrNull { centerTile.aerialDistanceTo(it.getCenterTile()) }
        findEnemyThreats()
        evaluateCombat()
    }

    private fun generateArmyFromPosition(origin: Tile) {
        fun getNearbyAlliedUnits(tile: Tile): Sequence<MapUnit> =
            tile.getTilesInDistance(unitDistance).filter {
                it.militaryUnit != null
                    && it.militaryUnit!!.civ == civInfo
                    && !ownedMilitaryUnits.contains(it.militaryUnit!!)
            }.map { it.militaryUnit!! }

        val foundUnits: MutableList<MapUnit> = mutableListOf()
        foundUnits.addAll(getNearbyAlliedUnits(origin))
        ownedMilitaryUnits.addAll(foundUnits)

        while (foundUnits.isNotEmpty()) {
            val unit = foundUnits[0]
            foundUnits.removeAt(0)
            val newUnits = getNearbyAlliedUnits(unit.getTile())
            foundUnits.addAll(newUnits)
            ownedMilitaryUnits.addAll(newUnits)
        }
    }

    fun findArmyCenter(): Tile {
        val totalX = ownedMilitaryUnits.sumOf { it.getTile().position.x.toDouble() }
        val averageX = totalX / ownedMilitaryUnits.count()
        val totalY = ownedMilitaryUnits.sumOf { it.getTile().position.y.toDouble() }
        val averageY = totalY / ownedMilitaryUnits.count()
        return civInfo.gameInfo.tileMap[averageX.roundToInt(), averageY.roundToInt()]
    }

    fun findEnemyThreats() {
        for (ownedUnit in ownedMilitaryUnits) {
            val tilesWithEnemies = civInfo.threatManager.getTilesWithEnemyThreatInDistance(
                ownedUnit.getTile(),
                unitDistance
            )
            val newEnemies = civInfo.threatManager.getEnemyUnitsOnTiles(tilesWithEnemies)
            enemyMilitaryUnits.addAll(newEnemies.filter { !enemyMilitaryUnits.contains(it) })
            val newCities = civInfo.threatManager.getEnemyCitiesOnTiles(tilesWithEnemies)
            enemyCities.addAll(newCities.filter { !enemyCities.contains(it) })
        }
    }

    /**
     * Finds the new military units around a unit that might have scouted more enemies.
     * Also re-evaluates the combat situation
     */
    fun findNewEnemyUnits(ownedUnit: MapUnit) {
        enemyMilitaryUnits.addAll(civInfo.threatManager.getEnemyMilitaryUnitsInDistance(ownedUnit.getTile(), unitDistance))
        evaluateCombat()
    }

    fun evaluateCombat() {
        combatEvaluation = civInfo.threatManager.getCombatEvaluation(
            ownedMilitaryUnits.toList(),
            enemyMilitaryUnits.toList()
        )
    }

    fun automateArmyUnits() {
        // Values for stance between -1 and 1, -1 is defensive, 1 is offensive.
        // reaches -1 when there are 9 or more enemy units nearby and reacher 1 when we have more than 9 units nearby.
        // reaches .5 when we have 2 more units 
        val stance = if (combatEvaluation >= 0) log10(combatEvaluation + 1) else -log10(-combatEvaluation + 1)

        // Scout
        // Retreat and heal units
        val unitsByHealth = ownedMilitaryUnits.sortedBy { it.health }
        for (unit in unitsByHealth) {
            if (unit.health < 50 * (20 * -stance)) {
                UnitAutomation.trySwapRetreat(unit)
            }
        }
        // Fortify
        // Attack
    }
}

