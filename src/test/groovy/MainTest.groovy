import de.itdesign.clarity.rest.ClarityRestClient
import de.itdesign.clarity.rest.RestResponse
import okhttp3.Response
import org.junit.jupiter.api.BeforeEach
import org.onboard.Main

import static org.mockito.Mockito.*
import org.junit.jupiter.api.Test
import static org.junit.jupiter.api.Assertions.*
import org.mockito.*
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper

class MainTest {

    @Mock
    ClarityRestClient clarityRestClientMock  // Mock ClarityRestClient

    @InjectMocks
    Main main  // Inject mocks into the Main class

    @BeforeEach
    void setup() {
        // Initialize mocks
        MockitoAnnotations.openMocks(this)

        // Mock the static method getClarityRestClient to return the mocked rest client
        try (MockedStatic<Main> mocked = mockStatic(Main.class)) {
            mocked.when(Main::getClarityRestClient).thenReturn(clarityRestClientMock)
        }
    }

    @Test
    void testCreateProjects_withMockedClarityRestClient() {
        // Mock project data
        def mockProjectData = [
                [name: "Project-Test-2", start: "2025-01-01T10:00:00", finish: "2025-01-10T10:00:00", is_active: true]
        ]

        // Call the createProjects method in mocked main class
        List<Map> result = main.createProjects(mockProjectData)

        // Assertions
        assertNotNull(result)
        assertEquals(1, result.size())  // Only one project should be created

    }

    @Test
    void testCreateProjects_withNullValues() {
        // Mock project data with some null values
        def mockProjectData = [
                [name: "Project-Test-2", start: "2025-01-01T10:00:00", finish: "2025-01-10T10:00:00", is_active: true],
                [name: null, start: "2025-02-01T10:00:00", finish: "2025-02-10T10:00:00", is_active: true]  // Null name
        ]

        // Call the createProjects method in the main class
        List<Map> result = main.createProjects(mockProjectData)

        // Assertions
        assertNotNull(result)  // Ensure the result is not null
        assertEquals(1, result.size())  // Check the number of projects processed (2 projects)

        // Check if each project has a valid ID in the result
        result.each { project ->
            assertNotNull(project['id'])  // Make sure the project ID is not null
        }
    }


}