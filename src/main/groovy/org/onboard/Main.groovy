package org.onboard

import de.itdesign.clarity.logging.CommonLogger
import de.itdesign.clarity.rest.ClarityRestClient
import groovy.json.JsonBuilder
import groovy.sql.Sql
import groovy.json.JsonSlurper
import groovy.xml.MarkupBuilder
import groovy.xml.XmlUtil
import org.onboard.util.UtilMethods

class Main {

    static CommonLogger logger = new CommonLogger(this)

    static void main(String[] args) {

        Sql sql = DbConnection.connectDb()
        logger.info("Application started")

        if (sql != null) {

            logger.info("Db connected successfully")

            //Reading all required CSV and conversion
            List<Map> resourcesList = CsvFileParse.getCsvData("csv/resources.csv")
            List<Map> projectsList = CsvFileParse.getCsvData("csv/projects.csv")
            List<Map> tasksList = CsvFileParse.getCsvData("csv/tasks.csv")
            List<Map> assignmentsList = CsvFileParse.getCsvData("csv/assignments.csv")

            logger.info("Successfully read CSV and completed Conversion")

            logger.info("Started creating projects")

            //Method calls for projects, tasks
            def projectsMapList = createProjects(sql, projectsList)
            def tasksIdList = createTasks(sql, tasksList, projectsMapList)

            //Binding projects with it's tasks
            def projectsWithTask = getProjectsWithItsTasks(sql, projectsMapList, tasksIdList)

            logger.info("Started generating XML Xog")

            //Xml Xog creation for Resources and Assignments
            String resourceXmlString = generateResourceXmlXOG(resourcesList)

            String assignmentXmlString = generateAssignmentXmlXog(assignmentsList, projectsWithTask)

            def resourcesFormattedXml = XmlUtil.serialize(resourceXmlString)
            def assignmentsFormattedXml = XmlUtil.serialize(assignmentXmlString)

            String resourcesResultPath = "src/main/resources/xml/resources.xml"
            String assignmentResultPath = "src/main/resources/xml/assignments.xml"

            //Writing XML Xog
            writeXmlToFile(resourcesResultPath, resourcesFormattedXml)
            writeXmlToFile(assignmentResultPath, assignmentsFormattedXml)

            logger.info("Completed Xog Creation")


        } else {
            logger.error("Database connection failed. Could not connect to the database.")
            System.exit(1)
        }
    }

    //Create new projects
    static List<Map> createProjects(Sql sql, List<Map> projectList) {

        def projectsMapList = []

        projectList.each { eachProject ->

            def project = [
                    name          : eachProject.name,
                    scheduleStart : eachProject.start,
                    scheduleFinish: eachProject.finish,
                    isActive      : eachProject.is_active
            ]
            println(project)

            def projectMap = [:]

            try {
                ClarityRestClient rest
                rest = new ClarityRestClient("admin", sql.getConnection(), "http://10.0.0.248:7080")

                def projectPayload = new JsonBuilder(project).toString()
                def responseResult = rest.POST("/projects/", projectPayload)

                if (responseResult?.statusCode == 200) {
                    def jsonResponse = new JsonSlurper().parseText(responseResult?.body)
                    projectMap['name'] = project.name
                    projectMap['id'] = jsonResponse._internalId
                    projectsMapList.add(projectMap)
                } else {
                    def jsonResponse = new JsonSlurper().parseText(responseResult?.body)
                    println(jsonResponse)
                }

            } catch (Exception e) {
                println(e.getMessage())
            }
        }
        return projectsMapList
    }

    //Create tasks for related projects
    static List<Integer> createTasks(Sql sql, List<Map> taskList, List<Map> projectsMapList) {

        List<Integer> taskIds = []

        projectsMapList.each { project ->
            if (taskList.size() >= 3) {

                def projectTaskList = taskList.subList(0, 3)

                projectTaskList.each { eachTask ->

                    def task = [
                            name  : eachTask.name,
                            status: UtilMethods.getLookupValue(eachTask.status as String)
                    ]

                    try {
                        ClarityRestClient rest
                        rest = new ClarityRestClient("admin", sql.getConnection(), "http://10.0.0.248:7080")

                        def taskPayload = new JsonBuilder(task).toString()
                        def responseResult = rest.POST("/projects/$project.id/tasks", taskPayload)

                        if (responseResult?.statusCode == 200) {
                            def jsonResponse = new JsonSlurper().parseText(responseResult?.body)
                            taskIds.add(jsonResponse._internalId)
                            println(jsonResponse)
                        } else {
                            def jsonResponse = new JsonSlurper().parseText(responseResult?.body)
                            println(jsonResponse)
                        }

                    } catch (Exception e) {
                        println(e.getMessage())
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
    static List<Map> getProjectsWithItsTasks(Sql sql, List<Map> projectIdList, List<Integer> tasksIdsList) {

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

                tasksIds.each { taskId ->
                    String taskQuery = "SELECT PRNAME,PREXTERNALID FROM PRTASK p WHERE p.PRID = $taskId"
                    def taskData = sql.firstRow(taskQuery)
                    def taskMap = [name: taskData.prname, code: taskData.prexternalid != null ? taskData.prexternalid : "~rmw", internalId: taskId]
                    projectMap.tasks.add(taskMap)
                }

            } catch (Exception e) {
                println(e.getMessage())
            }

            result.add(projectMap)

        }

        return result
    }

    //Create assignments Xml Xog
    static String generateAssignmentXmlXog(List<Map> assignmentsList, List<Map> projectsWithTask) {

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
                                Task(internalTaskID: task.internalId, outlineLevel: '1', taskID: task.code, name: task.name) {
                                    Assignments {
                                        assignmentsList.each { eachAssignment ->
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

}