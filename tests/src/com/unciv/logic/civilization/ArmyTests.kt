package com.unciv.logic.civilization

import com.badlogic.gdx.math.Vector2
import com.unciv.logic.automation.ai.Army
import com.unciv.logic.map.mapunit.MapUnit
import com.unciv.testing.GdxTestRunner
import com.unciv.testing.TestGame
import com.unciv.utils.DebugUtils
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(GdxTestRunner::class)
class ArmyTests {
    val testGame = TestGame()
    val civ = testGame.addCiv()
    val neutralCiv = testGame.addCiv()
    val enemyCiv = testGame.addCiv()


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
    fun `Test Army Generation No units`() {
        val centerTile = testGame.getTile(Vector2(0f, 0f))
        val newArmy = Army(civ, centerTile, 3)
        assertEquals(0, newArmy.ownedMilitaryUnits.size)
        assertEquals(0, newArmy.enemyMilitaryUnits.size)

    }

    @Test
    fun `Test Army Generation units`() {
        val centerTile = testGame.getTile(Vector2(0f, 0f))
        val units = mutableListOf<MapUnit>()
        units.add(testGame.addUnit("Warrior", civ, centerTile))
        units.add(testGame.addUnit("Warrior", civ, testGame.getTile(Vector2(1f, 1f))))
        units.add(testGame.addUnit("Warrior", civ, testGame.getTile(Vector2(4f, 4f))))
        testGame.addUnit("Warrior", civ, testGame.getTile(Vector2(-4f, -4f))) // Should be out of range
        val newArmy = Army(civ, centerTile, 3)
        assertEquals(3, newArmy.ownedMilitaryUnits.size)
        assertEquals(0, newArmy.enemyMilitaryUnits.size)
        for (unit in units) {
            assertTrue(newArmy.ownedMilitaryUnits.contains(unit))
        }
    }

    @Test
    fun `Test Army Generation with enemies`() {
        DebugUtils.VISIBLE_MAP = false
        val centerTile = testGame.getTile(Vector2(0f, 0f))
        val enemyUnits = mutableListOf<MapUnit>()
        testGame.addUnit("Warrior", civ, centerTile)
        testGame.addUnit("Warrior", civ, testGame.getTile(Vector2(1f, 1f)))
        testGame.addUnit("Warrior", civ, testGame.getTile(Vector2(4f, 4f)))
        enemyUnits.add(testGame.addUnit("Warrior", enemyCiv, testGame.getTile(Vector2(2f, 2f))))
        enemyUnits.add(testGame.addUnit("Warrior", enemyCiv, testGame.getTile(Vector2(2f, 1f))))
        assertFalse(testGame.addUnit("Warrior", enemyCiv, testGame.getTile(Vector2(-4f, -4f))).isInvisible(civ))
        assertFalse(testGame.getTile(Vector2(-4f, -4f)).isVisible(civ))
        val newArmy = Army(civ, centerTile, 3)
        assertEquals(3, newArmy.ownedMilitaryUnits.size)
        assertEquals(2, newArmy.enemyMilitaryUnits.size)
        for (enemyUnit in enemyUnits) {
            assertTrue(newArmy.enemyMilitaryUnits.contains(enemyUnit))
        }
    }

    @Test
    fun `Test Army Center one unit`() {
        val centerTile = testGame.getTile(Vector2(0f, 0f))
        testGame.addUnit("Warrior", civ, centerTile)
        val newArmy = Army(civ, centerTile, 3)
        assertEquals(centerTile, newArmy.findArmyCenter())
    }
    
    @Test
    fun `Test Army Center multiple units`() {
        val centerTile = testGame.getTile(Vector2(0f, 0f))
        testGame.addUnit("Warrior", civ, centerTile)
        testGame.addUnit("Warrior", civ, testGame.getTile(Vector2(1f, 1f)))
        testGame.addUnit("Warrior", civ, testGame.getTile(Vector2(2f, 2f)))
        testGame.addUnit("Warrior", civ, testGame.getTile(Vector2(-4f, -4f)))
        val newArmy = Army(civ, centerTile, 3)
        assertEquals(testGame.getTile(Vector2(1f, 1f)), newArmy.findArmyCenter())
    }
    @Test
    fun `Test Army Scouting`() {
        DebugUtils.VISIBLE_MAP = false
        val centerTile = testGame.getTile(Vector2(0f, 0f))
        val enemyUnits = mutableListOf<MapUnit>()
        testGame.addUnit("Warrior", civ, centerTile)
        val scout = testGame.addUnit("Scout", civ, testGame.getTile(Vector2(0f, 1f)))
        enemyUnits.add(testGame.addUnit("Warrior", enemyCiv, testGame.getTile(Vector2(3f, 0f))))
        enemyUnits.add(testGame.addUnit("Warrior", enemyCiv, testGame.getTile(Vector2(2f, 0f))))
        val newArmy = Army(civ, centerTile, 3)
        assertEquals(1, newArmy.enemyMilitaryUnits.size)
        scout.movement.moveToTile(testGame.getTile(Vector2(2f, 1f)))
        assertEquals(1, newArmy.enemyMilitaryUnits.size)
        newArmy.findNewEnemyUnits(scout)
        assertEquals(2, newArmy.enemyMilitaryUnits.size)
    }
}