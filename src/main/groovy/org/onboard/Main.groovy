package org.onboard

import de.itdesign.clarity.logging.CommonLogger
import de.itdesign.clarity.rest.ClarityRestClient
import groovy.json.JsonBuilder
import groovy.sql.Sql
import groovy.json.JsonSlurper
import groovy.xml.MarkupBuilder
import groovy.xml.XmlParser
import groovy.xml.XmlUtil
import org.onboard.util.UtilMethods

class Main {

    static CommonLogger logger = new CommonLogger(this)

    static Sql sql = DbConnection.connectDb()

    static ClarityRestClient rest = getClarityRestClient()

    static void main(String[] args) {

        logger.info("Application started")

        if (sql != null) {

            logger.info("Db connected successfully")

            //Reading all required CSV and conversion
            List<Map> resourcesList = CsvFileParse.getCsvData("csv/resources.csv")
            List<Map> projectsList = CsvFileParse.getCsvData("csv/projects.csv")
            List<Map> tasksList = CsvFileParse.getCsvData("csv/tasks.csv")
            List<Map> assignmentsList = CsvFileParse.getCsvData("csv/assignments.csv")

            logger.info("Successfully read CSV and completed Conversion")

            logger.info("Started creating projects and tasks")

            //Method calls for projects, tasks
            def projectsMapList = createProjects(projectsList)
            def tasksIdList = createTasks(tasksList, projectsMapList)

            //Binding projects with it's tasks
            def projectsWithTask = getProjectsWithItsTasks(projectsMapList, tasksIdList)

            logger.info("Started generating XML Xog")

            //Xml Xog creation for Resources and Assignments
            String resourceXmlString = generateResourceXmlXOG(resourcesList)
            String assignmentXmlString = generateAssignmentXmlXog(assignmentsList, tasksList, projectsWithTask)

            def resourcesFormattedXml = XmlUtil.serialize(resourceXmlString)
            def assignmentsFormattedXml = XmlUtil.serialize(assignmentXmlString)

            String resourcesResultPath = "src/main/resources/xml/resources.xml"
            String assignmentResultPath = "src/main/resources/xml/assignments.xml"

            //Writing XML Xog
            writeXmlToFile(resourcesResultPath, resourcesFormattedXml)
            writeXmlToFile(assignmentResultPath, assignmentsFormattedXml)

            logger.info("Completed Xog Creation")

            //Post Teams to the projects
            String xmlData = new File(assignmentResultPath).text
            createTeamsForProjects(xmlData)

        } else {
            logger.error("Database connection failed. Could not connect to the database.")
            System.exit(1)
        }
    }

    //Get clarity rest client connection
    static ClarityRestClient getClarityRestClient() {
        if (sql) {
            try {
                ClarityRestClient rest
                rest = new ClarityRestClient("admin", sql.getConnection(), "http://10.0.0.248:7080")
                return rest
            } catch (Exception e) {
                logger.error("Error in Connecting to REST Client ${e.getMessage()}")
            }
        }
        return null
    }

    //Create new projects
    static List<Map> createProjects(List<Map> projectList) {

        def projectsMapList = []

        projectList.each { eachProject ->

            def project = [
                    name          : eachProject.name,
                    scheduleStart : eachProject.start,
                    scheduleFinish: eachProject.finish,
                    isActive      : eachProject.is_active
            ]

            def projectMap = [:]

            if (rest) {

                def projectPayload = new JsonBuilder(project).toString()
                def responseResult = rest.POST("/projects/", projectPayload)

                if (responseResult?.statusCode == 200) {
                    def jsonResponse = new JsonSlurper().parseText(responseResult?.body)
                    projectMap['name'] = project.name
                    projectMap['id'] = jsonResponse._internalId
                    projectsMapList.add(projectMap)
                    logger.info("Created new Project: ${jsonResponse._internalId}")
                } else {
                    def jsonResponse = new JsonSlurper().parseText(responseResult?.body)
                    logger.error("Error in project creation: ${jsonResponse}")
                }

            } else {
                logger.error("Unable to make request. Rest client not available.")
            }
        }
        return projectsMapList
    }

    //Create tasks for related projects
    static List<Integer> createTasks(List<Map> taskList, List<Map> projectsMapList) {

        List<Integer> taskIds = []

        projectsMapList.each { project ->
            if (taskList.size() >= 3) {

                def projectTaskList = taskList.subList(0, 3)

                projectTaskList.each { eachTask ->

                    def task = [
                            name  : eachTask.name,
                            status: UtilMethods.getLookupValue(eachTask.status as String)
                    ]

                    if (rest) {

                        def taskPayload = new JsonBuilder(task).toString()
                        def responseResult = rest.POST("/projects/$project.id/tasks", taskPayload)

                        if (responseResult?.statusCode == 200) {
                            def jsonResponse = new JsonSlurper().parseText(responseResult?.body)
                            taskIds.add(jsonResponse._internalId)
                            logger.info("Created new Task ${jsonResponse._internalId}")
                        } else {
                            def jsonResponse = new JsonSlurper().parseText(responseResult?.body)
                            logger.error("Error in task creation ${jsonResponse}")
                        }

                    } else {
                        logger.error("Unable to make request. Rest client not available.")
                    }

                }
                taskList = taskList - projectTaskList
            }
        }
        return taskIds
    }

    //Create Resources Xml Xog
    static String generateResourceXmlXOG(List<Map> resourcesList) {

        def writer = new StringWriter()
        MarkupBuilder xml = new MarkupBuilder(writer)

        xml.NikuDataBus('xmlns:xsi': 'http://www.w3.org/2001/XMLSchema-instance',
                'xsi:noNamespaceSchemaLocation': '../xsd/nikuxog_department.xsd') {

            Header(action: 'write', externalSource: 'ORACLE-FINANCIAL', objectType: 'resource', version: '6.0.12')

            Resources {
                resourcesList.forEach { eachResource ->
                    Resource(resourceId: 'RS' + eachResource.id, isActive: eachResource.is_active.toLowerCase(), employmentType: 'Employee',
                            resourceType: 'LABOR', externalId: '2323AAA') {
                        PersonalInformation(lastName: eachResource.lastname, firstName: eachResource.firstname, emailAddress: eachResource.email)
                    }
                }
            }
        }

        return writer.toString()
    }

    //Get detailed projects with it's tasks
    static List<Map> getProjectsWithItsTasks(List<Map> projectIdList, List<Integer> tasksIdsList) {

        def result = []

        projectIdList.eachWithIndex { project, index ->

            def projectMap = [:]

            try {
                String projectQuery = "SELECT NAME,CODE FROM INV_INVESTMENTS ii WHERE ii.ID = $project.id"
                def projectData = sql.firstRow(projectQuery)
                projectMap['name'] = projectData.name
                projectMap['code'] = projectData.code
                projectMap['tasks'] = []

                def tasksIds = tasksIdsList[(index * 3)..(index * 3 + 2)]

                tasksIds.eachWithIndex { taskId, i ->
                    String taskQuery = "SELECT PRNAME,PREXTERNALID FROM PRTASK p WHERE p.PRID = $taskId"
                    def taskData = sql.firstRow(taskQuery)
                    def taskMap = [name: taskData.prname, code: taskData.prexternalid != null ? taskData.prexternalid : "ts${i + 1}", internalId: taskId]
                    projectMap.tasks.add(taskMap)
                }

            } catch (Exception e) {
                logger.error("Error: ${e.getMessage()}")
            }

            result.add(projectMap)

        }

        return result
    }

    //Create assignments Xml Xog
    static String generateAssignmentXmlXog(List<Map> assignmentsList, List<Map> tasksList, List<Map> projectsWithTask) {

        def writer = new StringWriter()
        MarkupBuilder xml = new MarkupBuilder(writer)

        xml.NikuDataBus('xmlns:xsi': 'http://www.w3.org/2001/XMLSchema-instance',
                'xsi:noNamespaceSchemaLocation': '../xsd/nikuxog_department.xsd') {

            Header(action: 'write', externalSource: 'NIKU', objectType: 'project', version: '7.1.0.3023')

            Projects {
                projectsWithTask.each { project ->
                    Project(name: project.name, projectID: project.code) {
                        Tasks {
                            project.tasks.each { task ->
                                def taskMap = tasksList.find { it.name == task.name }
                                println(taskMap)
                                def taskAssignments = assignmentsList.findAll { it.task_id == taskMap.id }
                                println(taskAssignments)
                                Task(internalTaskID: task.internalId, outlineLevel: '1', taskID: task.code, name: task.name) {
                                    Assignments {
                                        taskAssignments.each { eachAssignment ->
                                            TaskLabor(actualWork: eachAssignment.actuals, remainingWork: eachAssignment.etc, resourceID: "RS" + eachAssignment.resource_id)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return writer.toString()
    }

    //Helper to write XML files
    static void writeXmlToFile(String filePath, String xmlContent) {
        def file = new File(filePath)
        file.parentFile.mkdirs()
        file.write(xmlContent)
    }

    //Create teams for projects
    static void createTeamsForProjects(String xmlData) {
        def xmlParser = new XmlParser()
        def parsedXml = xmlParser.parseText(xmlData)

        parsedXml.'Projects'.'Project'.each { eachProject ->
            def projectName = eachProject.@name
            def projectId = eachProject.@projectID

            def projectInternalId = getProjectInternalId(projectId)

            if (projectInternalId) {
                eachProject.'Tasks'.'Task'.each { eachTask ->
                    eachTask.'Assignments'.'TaskLabor'.each { eachAssignment ->
                        def resourceCode = eachAssignment.@resourceID
                        def resourceDetails = getResourceDetails(resourceCode)
                        def teamData = [
                                resource: resourceDetails.id
                        ]

                        if (rest) {

                            def teamPayload = new JsonBuilder(teamData).toString()
                            def responseResult = rest.POST("/projects/$projectInternalId/teams", teamPayload)

                            if (responseResult?.statusCode == 200) {
                                println("Added team successfully")
                            } else {
                                def jsonResponse = new JsonSlurper().parseText(responseResult?.body)
                                logger.error("Error in adding team ${jsonResponse}")
                            }

                        } else {
                            logger.error("Unable to make request. REST client is not connected")
                        }
                    }
                }
            }
        }
    }

    static String getProjectInternalId(String projectId) {
        String query = "SELECT ID FROM INV_INVESTMENTS WHERE CODE = ?"
        def result = sql.firstRow(query, projectId)
        return result.ID
    }

    static Map getResourceDetails(String resourceCode) {
        def query = "SELECT ID, UNIQUE_NAME FROM SRM_RESOURCES WHERE UNIQUE_NAME = ?"
        def resource = sql.firstRow(query, resourceCode)
        return [id: resource.ID, code: resource.UNIQUE_NAME]
    }

}