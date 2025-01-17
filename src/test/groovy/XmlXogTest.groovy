import org.junit.jupiter.api.Test
import org.onboard.Main

import static org.junit.jupiter.api.Assertions.*
import groovy.xml.XmlParser

class XmlXogTest {

    @Test
    void testGenerateResourceXmlXOG() {
        // Mock input data
        List<Map> resourcesList = [
                [id: '123', is_active: 'true', lastname: 'Doe', firstname: 'John', email: 'john.doe@example.com'],
                [id: '456', is_active: 'false', lastname: 'Smith', firstname: 'Jane', email: 'jane.smith@example.com']
        ]

        String xmlOutput = Main.generateResourceXmlXOG(resourcesList)

        def parsedXml = new XmlParser().parseText(xmlOutput)

        // Validate resources section
        def resources = parsedXml.Resources[0]
        assertEquals(2, resources.Resource.size()) // Should have two resources

        // Validate first resource
        def resource1 = resources.Resource[0]
        assertEquals('RS123', resource1.'@resourceId')
        assertEquals('true', resource1.'@isActive')
        assertEquals('Employee', resource1.'@employmentType')
        assertEquals('LABOR', resource1.'@resourceType')
        assertEquals('2323AAA', resource1.'@externalId')

        def personalInfo1 = resource1.PersonalInformation[0]
        assertEquals('Doe', personalInfo1.'@lastName')
        assertEquals('John', personalInfo1.'@firstName')
        assertEquals('john.doe@example.com', personalInfo1.'@emailAddress')

        // Validate second resource
        def resource2 = resources.Resource[1]
        assertEquals('RS456', resource2.'@resourceId')
        assertEquals('false', resource2.'@isActive')
        assertEquals('Employee', resource2.'@employmentType')
        assertEquals('LABOR', resource2.'@resourceType')
        assertEquals('2323AAA', resource2.'@externalId')

        def personalInfo2 = resource2.PersonalInformation[0]
        assertEquals('Smith', personalInfo2.'@lastName')
        assertEquals('Jane', personalInfo2.'@firstName')
        assertEquals('jane.smith@example.com', personalInfo2.'@emailAddress')
    }

    @Test
    void testGenerateAssignmentXmlXog() {
        // mock data for assignments, tasks, and projects
        List<Map> assignmentsList = [
                [task_id: '1', resource_id: '123', actuals: '20', etc: '10'],
                [task_id: '2', resource_id: '456', actuals: '30', etc: '15']
        ]

        List<Map> tasksList = [
                [id: '1', name: 'Task 1', internalId: 'T1', code: 'T1C'],
                [id: '2', name: 'Task 2', internalId: 'T2', code: 'T2C']
        ]

        List<Map> projectsWithTask = [
                [name: 'Project 1', code: 'P1', tasks: [
                        [name: 'Task 1', internalId: 'T1', code: 'T1C'],
                        [name: 'Task 2', internalId: 'T2', code: 'T2C']
                ]]
        ]

        String xmlOutput = Main.generateAssignmentXmlXog(assignmentsList, tasksList, projectsWithTask)

        def parsedXml = new XmlParser().parseText(xmlOutput)

        // Validate projects section
        def projects = parsedXml.Projects[0]
        assertEquals(1, projects.Project.size()) // Should have one project

        def project = projects.Project[0]
        assertEquals('Project 1', project.'@name')
        assertEquals('P1', project.'@projectID')

        // Validate tasks section for the project
        def tasks = project.Tasks[0]
        assertEquals(2, tasks.Task.size()) // Should have two tasks

        // Validate first task
        def task1 = tasks.Task[0]
        assertEquals('T1', task1.'@internalTaskID')
        assertEquals('1', task1.'@outlineLevel')
        assertEquals('T1C', task1.'@taskID')
        assertEquals('Task 1', task1.'@name')

        // Validate task assignments for Task 1
        def assignments1 = task1.Assignments[0]
        assertEquals(1, assignments1.TaskLabor.size()) // Should have one assignment for Task 1

        def taskLabor1 = assignments1.TaskLabor[0]
        assertEquals('20', taskLabor1.'@actualWork')
        assertEquals('10', taskLabor1.'@remainingWork')
        assertEquals('RS123', taskLabor1.'@resourceID')

        // Validate second task
        def task2 = tasks.Task[1]
        assertEquals('T2', task2.'@internalTaskID')
        assertEquals('1', task2.'@outlineLevel')
        assertEquals('T2C', task2.'@taskID')
        assertEquals('Task 2', task2.'@name')

        // Validate task assignments for Task 2
        def assignments2 = task2.Assignments[0]
        assertEquals(1, assignments2.TaskLabor.size()) // Should have one assignment for Task 2

        def taskLabor2 = assignments2.TaskLabor[0]
        assertEquals('30', taskLabor2.'@actualWork')
        assertEquals('15', taskLabor2.'@remainingWork')
        assertEquals('RS456', taskLabor2.'@resourceID')
    }
}
