package de.unisaarland.cs.se.selab.parser.restaurantparser

import java.io.File
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertFalse

/**
 * Unit test suite for RestaurantParser.
 * Validates all invalid JSON scenario files shown in the workspace screenshot.
 * Each test verifies that the parser rejects invalid data by returning false.
 */
class RestaurantParserTest {

    private val basePath = "src/test/resources/RestaurantParserTests/"

    private lateinit var parser: RestaurantJSONParser

    @BeforeEach
    fun setUp() {
        // Instantiate a fresh parser instance before each test run
        parser = RestaurantJSONParser()
    }

    /**
     * Helper method to execute parsing on a test file path and assert that validation fails.
     */
    private fun assertInvalidFile(fileName: String) {
        val file = File(basePath + fileName)
        val result = parser.parse(file.absolutePath)
        assertFalse(result, "[IMPORTANT] Initialization Info: $filename is invalid.")
    }

    // =========================================================================
    // 1. DISH & RECIPE CROSS-VALIDATION TESTS
    // =========================================================================



    @Test
    fun `restaurantsNoBasicDishOfItsType - should fail when restaurant lacks required basic dish`() {
        assertInvalidFile("restaurantsNoBasicDishOfItsType.json")
    }

    @Test
    fun `restaurantsNoDupliacteDishName - should fail when dish names within restaurant are duplicated`() {
        assertInvalidFile("restaurantsNoDupliacteDishName.json")
    }

    @Test
    fun `restaurantsRecipiesNotExist - should fail when restaurant menu refers to missing recipe ID`() {
        assertInvalidFile("restaurantsRecipiesNotExist.json")
    }

    // =========================================================================
    // 2. RESTAURANT & TABLE UNIQUENESS TESTS
    // =========================================================================

    @Test
    fun `restaurantsDifferentNameSameId - should fail when two restaurants share the same ID`() {
        assertInvalidFile("restaurantsDifferentNameSameId.json")
    }

    @Test
    fun `restaurantsSameNameDifferentId - should fail when two restaurants share the same Name`() {
        assertInvalidFile("restaurantsSameNameDifferentId.json")
    }

    @Test
    fun `restaurantsNotUniqueTables - should fail when table IDs inside a restaurant are not unique`() {
        assertInvalidFile("restaurantsNotUniqueTables.json")
    }

    // =========================================================================
    // 3. STAFF & INFRASTRUCTURE MANDATORY PRESENCE TESTS
    // =========================================================================

    @Test
    fun `restaurantsNoCookExists - should fail when restaurant has no cooks`() {
        assertInvalidFile("restaurantsNoCookExists.json")
    }

    @Test
    fun `restaurantsNoWaiterExists - should fail when restaurant has no waitstaff`() {
        assertInvalidFile("restaurantsNoWaiterExists.json")
    }

    @Test
    fun `restaurantsNoTableExists - should fail when restaurant has no tables`() {
        assertInvalidFile("restaurantsNoTableExists.json")
    }

    @Test
    fun `restaurantsNoRecipieExists - should fail when restaurant has no recipes in menu`() {
        assertInvalidFile("restaurantsNoRecipieExists.json")
    }

    @Test
    fun `restaurantsNoMultipleHeadCooksieEXEC - should fail when multiple EXEC cooks exist`() {
        assertInvalidFile("restaurantsNoMultipleHeadCooksieEXEC.json")
    }

    // =========================================================================
    // 4. BOUNDS & TIMING VALIDATION TESTS
    // =========================================================================

    @Test
    fun `restaurantsNoInvalidTableSize - should fail when table capacity is outside valid bounds`() {
        assertInvalidFile("restaurantsNoInvalidTableSize.json")
    }

    @Test
    fun `restaurantsNoInvalidTableSize1 - should fail when table capacity violates edge bounds`() {
        assertInvalidFile("restaurantsNoInvalidTableSize1.json")
    }

    @Test
    fun `restaurantsOpeningTickEndLessThanOpeningTickStart - should fail when closing tick precedes opening tick`() {
        assertInvalidFile("restaurantsOpeningTickEndLessThanOpeningTickStart.json")
    }

    @Test
    fun `restaurantsTickNotIn1to24 - should fail when operating ticks fall outside range 1 to 24`() {
        assertInvalidFile("restaurantsTickNotIn1to24.json")
    }

    @Test
    fun `restaurantsPositiveRatingsG0 - should fail when positive ratings violate bounds`() {
        assertInvalidFile("restaurantsPositiveRatingsG0.json")
    }

    @Test
    fun `restaurantsPositiveRatingsNeg0 - should fail when ratings are negative`() {
        assertInvalidFile("restaurantsPositiveRatingsNeg0.json")
    }
}
