import org.junit.jupiter.api.Test
import org.onboard.util.UtilMethods
import static org.junit.jupiter.api.Assertions.*

class GeneralTest {

    @Test
    void testGetLookupValue() {
        // Test when the status is "In Progress"
        int resultInProgress = UtilMethods.getLookupValue("In Progress")
        assertEquals(1, resultInProgress)

        // Test when the status is "Completed"
        int resultCompleted = UtilMethods.getLookupValue("Completed")
        assertEquals(2, resultCompleted)

        // Test when the status is an unknown status like "Pending"
        int resultUnknownStatus = UtilMethods.getLookupValue("Pending")
        assertEquals(0, resultUnknownStatus)

        // Test when the status is an empty string
        int resultEmptyString = UtilMethods.getLookupValue("")
        assertEquals(0, resultEmptyString)

        // Test when the status is null
        int resultNullStatus = UtilMethods.getLookupValue(null)
        assertEquals(0, resultNullStatus)
    }
}
