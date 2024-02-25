package com.unciv.logic.civilization.managers

import com.badlogic.gdx.math.Vector2
import com.unciv.logic.automation.civilization.NextTurnAutomation
import com.unciv.logic.map.mapunit.MapUnit
import com.unciv.logic.map.mapunit.UnitTurnManager
import com.unciv.testing.GdxTestRunner
import com.unciv.testing.TestGame
import com.unciv.utils.DebugUtils
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import junit.framework.TestCase.fail
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.absoluteValue

@RunWith(GdxTestRunner::class)
class ThreatManangerTests {

    val testGame = TestGame()
    val civ = testGame.addCiv()
    val neutralCiv = testGame.addCiv()
    val enemyCiv = testGame.addCiv()
    val threatManager = civ.threatManager


    @Before
    fun setUp() {
        DebugUtils.VISIBLE_MAP = true // Needed to be able to see the enemy units
        testGame.makeHexagonalMap(10)
        civ.diplomacyFunctions.makeCivilizationsMeet(enemyCiv)
        civ.diplomacyFunctions.makeCivilizationsMeet(neutralCiv)
        civ.getDiplomacyManager(enemyCiv).declareWar()
    }

    @After
    fun wrapUp() {
        DebugUtils.VISIBLE_MAP = false
    }

    @Test
    fun `Distance to closest enemy with no enemies`() {
        val centerTile = testGame.getTile(Vector2(0f, 0f))
        assertEquals(5, threatManager.getDistanceToClosestEnemyUnit(centerTile, 5, false))
    }

    @Test
    fun `Find tiles with enemies with no enemies`() {
        val centerTile = testGame.getTile(Vector2(0f, 0f))
        assertEquals(0, threatManager.getTilesWithEnemyUnitsInDistance(centerTile, 5).count())
    }

    @Test
    fun `Find enemies on tiles with no enemies`() {
        val centerTile = testGame.getTile(Vector2(0f, 0f))
        assertEquals(
            0,
            threatManager.getEnemyUnitsOnTiles(
                threatManager.getTilesWithEnemyUnitsInDistance(
                    centerTile,
                    5
                )
            ).count()
        )
    }

    @Test
    fun `Find distance to enemy`() {
        val centerTile = testGame.getTile(Vector2(0f, 0f))
        testGame.addUnit("Warrior", enemyCiv, testGame.getTile(Vector2(3f, 0f)))
        testGame.addUnit("Warrior", neutralCiv, testGame.getTile(Vector2(1f, 1f)))
        assertEquals(3, threatManager.getDistanceToClosestEnemyUnit(centerTile, 5))
    }

    @Test
    fun `Find distance to closer enemy`() {
        val centerTile = testGame.getTile(Vector2(0f, 0f))
        testGame.addUnit("Warrior", enemyCiv, testGame.getTile(Vector2(3f, 0f)))
        testGame.addUnit("Warrior", enemyCiv, testGame.getTile(Vector2(4f, 0f)))
        assertEquals(3, threatManager.getDistanceToClosestEnemyUnit(centerTile, 5))
    }

    @Test
    fun `Find distance to farther enemy`() {
        val centerTile = testGame.getTile(Vector2(0f, 0f))
        assertEquals(2, threatManager.getDistanceToClosestEnemyUnit(centerTile, 2, false))
        // Cache results should say there is not a unit within a distance of 2
        // Therefore the warrior at distance 2 should not be checked
        testGame.addUnit("Warrior", enemyCiv, testGame.getTile(Vector2(2f, 0f)))
        testGame.addUnit("Warrior", enemyCiv, testGame.getTile(Vector2(4f, 0f)))
        assertEquals(4, threatManager.getDistanceToClosestEnemyUnit(centerTile, 4, false))
        assertEquals(4, threatManager.getDistanceToClosestEnemyUnit(centerTile, 5, false))
    }

    @Test
    fun `Find distance to enemy wrong cache`() {
        val centerTile = testGame.getTile(Vector2(0f, 0f))
        testGame.addUnit("Warrior", enemyCiv, testGame.getTile(Vector2(3f, 0f)))
        assertEquals(3, threatManager.getDistanceToClosestEnemyUnit(centerTile, 5))
        testGame.getTile(Vector2(3f, 0f)).militaryUnit!!.removeFromTile()
        testGame.addUnit("Warrior", enemyCiv, testGame.getTile(Vector2(4f, 0f)))
        testGame.addUnit("Warrior", enemyCiv, testGame.getTile(Vector2(4f, 1f)))
        assertEquals(4, threatManager.getDistanceToClosestEnemyUnit(centerTile, 5))
        testGame.getTile(Vector2(4f, 0f)).militaryUnit!!.removeFromTile()
        assertEquals(4, threatManager.getDistanceToClosestEnemyUnit(centerTile, 5, false))
        testGame.getTile(Vector2(4f, 1f)).militaryUnit!!.removeFromTile()
        assertEquals(5, threatManager.getDistanceToClosestEnemyUnit(centerTile, 5, false))
    }

    @Test
    fun `Find distance to enemy cache`() {
        val centerTile = testGame.getTile(Vector2(0f, 0f))
        testGame.addUnit("Warrior", enemyCiv, testGame.getTile(Vector2(3f, 0f)))
        assertEquals(3, threatManager.getDistanceToClosestEnemyUnit(centerTile, 5))
        // An enemy unit should never be spawned closer than we previously searched
        // Therefore our cache results should return 3 instead of the closer unit at a distance of 2
        testGame.addUnit("Warrior", enemyCiv, testGame.getTile(Vector2(2f, 0f)))
        assertEquals(3, threatManager.getDistanceToClosestEnemyUnit(centerTile, 5))
    }

    @Test
    fun `Find tiles with enemy units`() {
        val centerTile = testGame.getTile(Vector2(0f, 0f))
        testGame.addUnit("Warrior", enemyCiv, testGame.getTile(Vector2(3f, 0f)))
        testGame.addUnit("Warrior", enemyCiv, testGame.getTile(Vector2(2f, 0f)))
        testGame.addUnit("Warrior", enemyCiv, testGame.getTile(Vector2(4f, 0f)))
        assertEquals(3, threatManager.getTilesWithEnemyUnitsInDistance(centerTile, 5).count())
        assertEquals(2, threatManager.getTilesWithEnemyUnitsInDistance(centerTile, 3).count())
    }

    @Test
    fun `Find tiles with enemy units center tile`() {
        val centerTile = testGame.getTile(Vector2(0f, 0f))
        testGame.addUnit("Warrior", enemyCiv, centerTile)
        assertEquals(1, threatManager.getTilesWithEnemyUnitsInDistance(centerTile, 2).count())
    }

    @Test
    fun `Find tiles with enemy units cache`() {
        val centerTile = testGame.getTile(Vector2(0f, 0f))
        assertEquals(5, threatManager.getDistanceToClosestEnemyUnit(centerTile, 5, false))
        // We have stored in the cach that there is no enemy unit within a distance of 5
        // Therefore adding these units is illegal and should not be returned
        testGame.addUnit("Warrior", enemyCiv, testGame.getTile(Vector2(3f, 0f)))
        testGame.addUnit("Warrior", enemyCiv, testGame.getTile(Vector2(2f, 0f)))
        testGame.addUnit("Warrior", enemyCiv, testGame.getTile(Vector2(4f, 0f)))
        assertEquals(0, threatManager.getTilesWithEnemyUnitsInDistance(centerTile, 5).count())
        assertEquals(0, threatManager.getTilesWithEnemyUnitsInDistance(centerTile, 3).count())

        // Now it might be another turn, so it is allowed
        threatManager.clear()
        assertEquals(3, threatManager.getTilesWithEnemyUnitsInDistance(centerTile, 5).count())
        assertEquals(2, threatManager.getTilesWithEnemyUnitsInDistance(centerTile, 3).count())
    }

    @Test
    fun `Find distance to enemy after find tiles with enemy units`() {
        val centerTile = testGame.getTile(Vector2(0f, 0f))
        testGame.addUnit("Warrior", enemyCiv, testGame.getTile(Vector2(3f, 0f)))
        assertEquals(1, threatManager.getTilesWithEnemyUnitsInDistance(centerTile, 5).count())
        testGame.addUnit("Warrior", enemyCiv, testGame.getTile(Vector2(2f, 0f)))
        assertEquals(1, threatManager.getTilesWithEnemyUnitsInDistance(centerTile, 5).count())
    }


    @Test
    fun `Find enemy units on tiles`() {
        val centerTile = testGame.getTile(Vector2(0f, 0f))
        testGame.addUnit("Warrior", enemyCiv, testGame.getTile(Vector2(3f, 0f)))
        testGame.addCity(enemyCiv, testGame.getTile(Vector2(3f, 0f)))
        testGame.addUnit("Bomber", enemyCiv, testGame.getTile(Vector2(3f, 0f)))
        testGame.addUnit("Warrior", enemyCiv, testGame.getTile(Vector2(2f, 0f)))
        testGame.addUnit("Warrior", enemyCiv, testGame.getTile(Vector2(4f, 0f)))
        testGame.addUnit("Warrior", neutralCiv, testGame.getTile(Vector2(-3f, -3f)))
        assertEquals(
            4,
            threatManager.getEnemyUnitsOnTiles(
                threatManager.getTilesWithEnemyUnitsInDistance(
                    centerTile,
                    5
                )
            ).count()
        )
        assertEquals(
            3,
            threatManager.getEnemyUnitsOnTiles(
                threatManager.getTilesWithEnemyUnitsInDistance(
                    centerTile,
                    3
                )
            ).count()
        )
        assertEquals(
            0,
            threatManager.getEnemyUnitsOnTiles(
                threatManager.getTilesWithEnemyUnitsInDistance(
                    centerTile,
                    1
                )
            ).count()
        )
    }

    @Test
    fun `Dangerous tiles`() {
        val centerTile = testGame.getTile(Vector2(0f, 0f))
        testGame.addUnit("Warrior", civ, centerTile)
        testGame.addUnit("Warrior", enemyCiv, testGame.getTile(Vector2(3f, 0f)))
        testGame.addUnit("Archer", enemyCiv, testGame.getTile(Vector2(-3f, 0f)))
        val dangerousTiles = threatManager.getDangerousTiles(centerTile.militaryUnit!!, 3)
        assertEquals(
            null,
            testGame.getTile(Vector2(3f, 0f)).getTilesInDistance(1)
                .firstOrNull { tile -> !dangerousTiles.contains(tile) })
        assertEquals(
            null,
            testGame.getTile(Vector2(-3f, 0f)).getTilesInDistance(2)
                .firstOrNull { tile -> !dangerousTiles.contains(tile) })
    }

    @Test
    fun `Test CombatEvaluation No Units`() {
        val centerTile = testGame.getTile(Vector2(0f, 0f))
        assertEquals(0.0, civ.threatManager.getUnitCombatEvaluationAroundTile(centerTile, 3), 0.001)
    }


    @Test
    fun `Test CombatEvaluation Owned Units`() {
        val centerTile = testGame.getTile(Vector2(0f, 0f))
        testGame.addUnit("Warrior", civ, testGame.getTile(Vector2(3f, 0f)))
        testGame.addUnit("Warrior", civ, testGame.getTile(Vector2(2f, 0f)))
        testGame.addUnit("Warrior", civ, centerTile)
        assertEquals(3.0, civ.threatManager.getUnitCombatEvaluationAroundTile(centerTile, 3), 0.001)
    }

    @Test
    fun `Test CombatEvaluation Two Units`() {
        val centerTile = testGame.getTile(Vector2(0f, 0f))
        testGame.addUnit("Warrior", enemyCiv, testGame.getTile(Vector2(1f, 0f)))
        testGame.addUnit("Warrior", civ, centerTile)
        assertEquals(0.0, civ.threatManager.getUnitCombatEvaluationAroundTile(centerTile, 2), 0.001)
    }

    @Test
    fun `Test CombatEvaluation Only Enemy Units`() {
        val centerTile = testGame.getTile(Vector2(0f, 0f))
        testGame.addUnit("Warrior", enemyCiv, testGame.getTile(Vector2(1f, 0f)))
        testGame.addUnit("Warrior", enemyCiv, centerTile)
        assertEquals(
            -2.0,
            civ.threatManager.getUnitCombatEvaluationAroundTile(centerTile, 2),
            0.001
        )
    }

    @Test
    fun `Test CombatEvaluation More Enemy Units`() {
        val centerTile = testGame.getTile(Vector2(0f, 0f))
        testGame.addUnit("Warrior", enemyCiv, testGame.getTile(Vector2(1f, 0f)))
        testGame.addUnit("Warrior", enemyCiv, testGame.getTile(Vector2(-1f, 0f)))
        testGame.addUnit("Warrior", civ, centerTile)
        assertEquals(
            -1.0,
            civ.threatManager.getUnitCombatEvaluationAroundTile(centerTile, 2),
            0.001
        )
    }

    @Test
    fun `Test CombatEvaluation More Friendly Units`() {
        val centerTile = testGame.getTile(Vector2(0f, 0f))
        testGame.addUnit("Warrior", enemyCiv, testGame.getTile(Vector2(1f, 0f)))
        testGame.addUnit("Warrior", civ, testGame.getTile(Vector2(-1f, 0f)))
        testGame.addUnit("Warrior", civ, centerTile)
        assertEquals(1.0, civ.threatManager.getUnitCombatEvaluationAroundTile(centerTile, 2), 0.001)
    }

    @Test
    fun `Test CombatEvaluation Stronger Enemy`() {
        val centerTile = testGame.getTile(Vector2(0f, 0f))
        testGame.addUnit("Pikeman", enemyCiv, testGame.getTile(Vector2(1f, 0f)))
        testGame.addUnit("Warrior", civ, testGame.getTile(Vector2(-1f, 0f)))
        testGame.addUnit("Warrior", civ, centerTile)
        val combatEvaluation = civ.threatManager.getUnitCombatEvaluationAroundTile(centerTile, 2)
        assertTrue(combatEvaluation < 0)
    }

    @Test
    fun `Test CombatEvaluation Stronger Friendly Units`() {
        val centerTile = testGame.getTile(Vector2(0f, 0f))
        testGame.addUnit("Pikeman", enemyCiv, testGame.getTile(Vector2(1f, 0f)))
        testGame.addUnit("Pikeman", civ, testGame.getTile(Vector2(-1f, 0f)))
        testGame.addUnit("Warrior", civ, centerTile)
        val combatEvaluation = civ.threatManager.getUnitCombatEvaluationAroundTile(centerTile, 2)
        assertTrue(combatEvaluation > 0)
    }


    @Test
    fun `BattleSimulation`() {
        val centerTile = testGame.getTile(Vector2(0f, 0f))
        testGame.addCity(civ, testGame.getTile(Vector2(0f, 3f)), false, 1)
        val allUnits: MutableList<MapUnit> = mutableListOf()
        allUnits.add(testGame.addUnit("Warrior", civ, testGame.getTile(Vector2(2f, 2f))))
        allUnits.add(testGame.addUnit("Warrior", civ, testGame.getTile(Vector2(1f, 2f))))
        allUnits.add(testGame.addUnit("Warrior", civ, testGame.getTile(Vector2(2f, 1f))))
        allUnits.add(testGame.addUnit("Warrior", civ, testGame.getTile(Vector2(2f, 0f))))
        allUnits.add(testGame.addUnit("Warrior", civ, testGame.getTile(Vector2(0f, 2f))))

        testGame.addCity(enemyCiv, testGame.getTile(Vector2(0f, -3f)), false, 1)
        allUnits.add(testGame.addUnit("Warrior", enemyCiv, testGame.getTile(Vector2(-2f, -2f))))
        allUnits.add(testGame.addUnit("Warrior", enemyCiv, testGame.getTile(Vector2(-1f, -2f))))
        allUnits.add(testGame.addUnit("Warrior", enemyCiv, testGame.getTile(Vector2(-2f, -1f))))
        allUnits.add(testGame.addUnit("Warrior", enemyCiv, testGame.getTile(Vector2(-2f, -0f))))
        allUnits.add(testGame.addUnit("Warrior", enemyCiv, testGame.getTile(Vector2(-0f, -2f))))

        while (true) {
            TurnManager(enemyCiv).startTurn()
            NextTurnAutomation.automateUnits(enemyCiv)
            TurnManager(civ).startTurn()
            NextTurnAutomation.automateUnits(civ)
            for (unit in allUnits) {
                UnitTurnManager(unit).endTurn()
            }

            val combatEvaluation =
                civ.threatManager.getUnitCombatEvaluationAroundTile(centerTile, 5)
            if (combatEvaluation.absoluteValue > 5 || civ.units.getCivUnits()
                    .none { it.isMilitary() }
                || enemyCiv.units.getCivUnits().none { it.isMilitary() }
            ) break
        }
        val combatEvaluation = civ.threatManager.getUnitCombatEvaluationAroundTile(centerTile, 5)
        if (combatEvaluation < 0) fail("Lost with " + combatEvaluation)
//        val combatEvaluation = civ.threatManager.getUnitCombatEvaluationAroundTile(centerTile,2)
//        assertTrue(combatEvaluation > 0)
    }


}

