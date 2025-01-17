import org.junit.jupiter.api.Test
import static org.junit.jupiter.api.Assertions.*
import org.onboard.CsvFileParse

class CsvFileParseTest {

    @Test
    void testGetCsvData() {
        // Mock the dependencies
        CsvFileParse.metaClass.static.getResourcePath = { path -> "mocked/path/to/file.csv" }
        CsvFileParse.metaClass.static.readCsvFile = { filePath ->
            // Mocked CSV data: first row is headers, the rest are data
            return [
                    ["name", "age"],
                    ["John", "30"],
                    ["Jane", "25"]
            ]
        }
        CsvFileParse.metaClass.static.mapDataWithHeaders = { dataList ->
            // Mocked mapped data
            return [
                    [name: "John", age: "30"],
                    [name: "Jane", age: "25"]
            ]
        }

        // Test the getCsvData method
        List<Map> result = CsvFileParse.getCsvData("mocked/path")

        // Assertions
        assertNotNull(result)
        assertEquals(2, result.size())  // Should have two rows of data
        assertEquals("John", result[0].name)
        assertEquals("30", result[0].age)
        assertEquals("Jane", result[1].name)
        assertEquals("25", result[1].age)
    }
}
