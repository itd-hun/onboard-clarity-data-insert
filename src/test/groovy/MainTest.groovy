import de.itdesign.clarity.rest.ClarityRestClient
import groovy.sql.GroovyRowResult
import groovy.sql.Sql
import org.junit.jupiter.api.BeforeEach
import org.onboard.Main

import static org.mockito.Mockito.*
import org.junit.jupiter.api.Test
import static org.junit.jupiter.api.Assertions.*
import org.mockito.*

class MainTest {

    @Mock
    Sql mockSql

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

        List<Map> result = main.createProjects(mockProjectData)

        assertNotNull(result)
        assertEquals(1, result.size())

        // Check if each project has a valid ID in the result
        result.each { project ->
            assertNotNull(project['id'])
        }
    }

    @Test
    void testGetResourceDetails() {
        String resourceCode = "RS11"

        // Mock the behavior to return a GroovyRowResult (instead of a simple Map)
        GroovyRowResult mockResult = new GroovyRowResult([ID: 1, UNIQUE_NAME: resourceCode])

        when(mockSql.firstRow(anyString(), eq([resourceCode]))).thenReturn(mockResult)

        def resource = main.getResourceDetails(resourceCode)

        assertNotNull(resource)
        assertEquals(resourceCode, resource.code)
    }

    @Test
    void testGetResourceDetails_withNullResult() {
        String resourceCode = "NON_EXISTENT"

        // Mock the behavior to return null (resource not found)
        when(mockSql.firstRow(anyString(), eq([resourceCode]))).thenReturn(null)

        def resource = main.getResourceDetails(resourceCode)

        assertNull(resource)
    }

    @Test
    void testGetProjectInternalId_withValidData() {
        String projectId = "PR2266"

        def internalId = main.getProjectInternalId(projectId)

        assertNotNull(internalId)
    }

    @Test
    void testGetProjectInternalId_withNullResult() {
        String projectId = "NON_EXISTENT"

        when(mockSql.firstRow(anyString(), eq([projectId]))).thenReturn(null)

        def internalId = main.getProjectInternalId(projectId)

        assertNull(internalId)
    }
}