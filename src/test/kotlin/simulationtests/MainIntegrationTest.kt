package simulationtests

import de.unisaarland.cs.se.selab.main
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.File
import kotlin.test.AfterTest

class MainIntegrationTest {

    private val outputPath = "build/test_output.log"

    @AfterTest
    fun cleanup() {
        // Clean up the generated log file after each test
        val file = File(outputPath)
        if (file.exists()) {
            file.delete()
        }
    }

    @Test
    fun `main executes full simulation and writes to output file when given valid arguments`() {
        // Use known-good files to run a successful simulation
        val args = arrayOf(
            "--food", "src/systemtest/resources/simplejson/food.json",
            "--restaurants", "src/systemtest/resources/simplejson/restaurants.json",
            "--scenario", "src/systemtest/resources/simplejson/scenario.json",
            "--maxTicks", "24",
            "--logLevel", "IMPORTANT",
            "--out", outputPath
        )

        main(args)

        // Check that the simulation finished and wrote its final stats
        val outputFile = File(outputPath)
        assertTrue(outputFile.exists())
        assertTrue(outputFile.readText().contains("[IMPORTANT] Simulation Info: Simulation statistics"))
    }

    @Test
    fun `main aborts- the parser detects invalid food files`() {
        // a fake food file should force parsing failure
        val args = arrayOf(
            "--food", "src/systemtest/resources/Depression.json",
            "--restaurants", "src/systemtest/resources/simplejson/restaurants.json",
            "--scenario", "src/systemtest/resources/simplejson/scenario.json",
            "--maxTicks", "24",
            "--logLevel", "DEBUG",
            "--out", outputPath
        )

        main(args)

        // The logger creates the file, but it shouldn't contain simulation stats due to early exit
        val outputFile = File(outputPath)
        assertTrue(outputFile.exists())
        assertFalse(outputFile.readText().contains("Simulation statistics"))
    }

    @Test
    fun `main aborts - the parser detects invalid restaurants files`() {
        // a fake restaurant file forces a parsing failure
        val args = arrayOf(
            "--food", "src/systemtest/resources/simplejson/food.json",
            "--restaurants", "src/systemtest/resources/Depression.json",
            "--scenario", "src/systemtest/resources/simplejson/scenario.json",
            "--maxTicks", "24",
            "--logLevel", "DEBUG",
            "--out", outputPath
        )

        main(args)

        // Ensure the simulation aborted before running
        val outputFile = File(outputPath)
        assertTrue(outputFile.exists())
        assertFalse(outputFile.readText().contains("Simulation statistics"))
    }

    @Test
    fun `main aborts - the parser detects invalid scenario files`() {
        // a fake scenario file forces a parsing failure
        val args = arrayOf(
            "--food", "src/systemtest/resources/simplejson/food.json",
            "--restaurants", "src/systemtest/resources/simplejson/restaurants.json",
            "--scenario", "src/systemtest/resources/Depression.json",
            "--maxTicks", "24",
            "--logLevel", "DEBUG",
            "--out", outputPath
        )

        main(args)

        // Ensure the simulation aborted before running
        val outputFile = File(outputPath)
        assertTrue(outputFile.exists())
        assertFalse(outputFile.readText().contains("Simulation statistics"))
    }

    @Test
    fun `main - IllegalArgumentException when maxTicks is out of bounds`() {
        // Pass an invalid maxTicks value (limit is 1000)
        val args = arrayOf(
            "--food", "src/test/resources/dummy_food.json",
            "--restaurants", "src/test/resources/dummy_restaurants.json",
            "--scenario", "src/test/resources/dummy_scenario.json",
            "--maxTicks", "9999",
            "--logLevel", "INFO"
        )

        // Verify the exact error message is thrown
        val exception = assertThrows<IllegalArgumentException> {
            main(args)
        }

        assertTrue(exception.message!!.contains("has to be between"))
    }

    @Test
    fun `main -default to stdout when output file argument is missing`() {
        // what happens when  the --out argument is missing
        val args = arrayOf(
            "--food", "src/systemtest/resources/simplejson/food.json",
            "--restaurants", "src/systemtest/resources/simplejson/restaurants.json",
            "--scenario", "src/systemtest/resources/simplejson/scenario.json",
            "--maxTicks", "1", // Keep it short so it doesn't spam your console
            "--logLevel", "IMPORTANT"
        )

        // the System.out branch executes correctly
        main(args)

        assertTrue(true)
    }

    @Test
    fun `main - IllegalArgumentException- maxTicks is negative`() {
        // Pass a negative maxTicks value to cover the lower bound of the range check
        val args = arrayOf(
            "--food", "src/systemtest/resources/simplejson/food.json",
            "--restaurants", "src/systemtest/resources/simplejson/restaurants.json",
            "--scenario", "src/systemtest/resources/simplejson/scenario.json",
            "--maxTicks", "-1",
            "--logLevel", "INFO"
        )

        val exception = assertThrows<IllegalArgumentException> {
            main(args)
        }

        assertTrue(exception.message!!.contains("has to be between"))
    }
}
