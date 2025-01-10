package org.onboard

import de.itdesign.clarity.rest.ClarityRestClient
import groovy.json.JsonBuilder
import groovy.sql.Sql
import groovy.json.JsonSlurper

class Main {

    static void main(String[] args) {

        Sql sql = connectDb()

        if (sql != null) {
            println("Db connected Successfully")
            List<List<String>> resourcesList = getCsvData("csv/resources.csv")
            List<List<String>> projectsList = getCsvData("csv/projects.csv")
            List<List<String>> tasksList = getCsvData("csv/tasks.csv")
            List<List<String>> assignmentsList = getCsvData("csv/assignments.csv")

            List<Integer> projectsIdList = createProjects(sql, projectsList)
            createTasks(sql, tasksList, projectsIdList)

        } else {
            println("Db is not connected")
        }
    }

    //Connecting oracle database
    static Sql connectDb() {
        Map<String, String> configData = getDbConfig();
        Sql sql = null;
        try {
            sql = Sql.newInstance(configData.url, configData.username, configData.password, configData.driver)
            def result = sql.firstRow("SELECT 1 FROM DUAL")
        } catch (Exception e) {
            println(e.getMessage())
        }
        return sql
    }

    //Getting db configure info from properties
    static Map<String, String> getDbConfig() {
        Properties properties = new Properties()
        def propertiesFile = Main.class.classLoader.getResourceAsStream("application.properties")
        properties.load(propertiesFile)
        def dbUrl = properties.getProperty("db.url")
        def dbUsername = properties.getProperty("db.username")
        def dbPassword = properties.getProperty("db.password")
        def dbDriver = properties.getProperty("db.driver")
        def dbConfigData = [url: dbUrl, username: dbUsername, password: dbPassword, driver: dbDriver]
        return dbConfigData
    }

    //Read file resource and get CSV data
    static List<List<String>> getCsvData(String path) {
        String csvFilePath = getResourcePath(path)
        List<List<String>> resourcesList = readCsvFile(csvFilePath)
        return resourcesList
    }

    //Read CSV and return list of data
    static List<List<String>> readCsvFile(String filePath) {
        BufferedReader reader = null;
        def dataList = null

        try {
            reader = new BufferedReader(new FileReader(filePath))
            def data = reader.readLines()
            def result = []
            data.each { it -> result.add(it.split(",")) }
            dataList = result
        } catch (Exception e) {
            println(e.message)
        } finally {
            reader.close()
        }
        return dataList
    }

    //Get CSV file path from resources
    static String getResourcePath(String path) {
        URL resourceUrl = Main.class.classLoader.getResource(path)
        String filePath = resourceUrl ? resourceUrl.toURI().path : null
        return filePath
    }

    //Get lookup number for status
    static int getLookupValue(String status) {
        if (status == "In Progress") {
            return 1
        } else if (status == "Completed") {
            return 2
        }
        return 0
    }

    //Create new projects into ppm
    static List<Integer> createProjects(Sql sql, List<List<String>> projectList) {

        projectList.remove(0)

        List<Integer> projectIds = []

        projectList.each { it ->

            def project = [
                    name          : it[1],
                    scheduleStart : it[2],
                    scheduleFinish: it[3],
                    isActive      : it[5]
            ]
            println(project)

            try {
                ClarityRestClient rest
                rest = new ClarityRestClient("admin", sql.getConnection(), "http://10.0.0.248:7080")

                def projectPayload = new JsonBuilder(project).toString()
                def responseResult = rest.POST("/projects/", projectPayload)

                if (responseResult?.statusCode == 200) {
                    def jsonResponse = new JsonSlurper().parseText(responseResult?.body)
                    projectIds.add(jsonResponse._internalId)
                    println(jsonResponse)
                } else {
                    def jsonResponse = new JsonSlurper().parseText(responseResult?.body)
                    println(jsonResponse)
                }

            } catch (Exception e) {
                println(e.getMessage())
            }
        }
        return projectIds
    }

    //Create tasks for related projects
    static void createTasks(Sql sql, List<List<String>> taskList, List<Integer> projectsIdList) {

        taskList.remove(0)

        projectsIdList.each { projectId ->
            if (taskList.size() >= 3) {

                def projectTaskList = taskList.subList(0, 3)

                projectTaskList.each { it ->

                    def task = [
                            name  : it[1],
                            status: getLookupValue(it[3])
                    ]

                    try {
                        ClarityRestClient rest
                        rest = new ClarityRestClient("admin", sql.getConnection(), "http://10.0.0.248:7080")

                        def taskPayload = new JsonBuilder(task).toString()
                        def responseResult = rest.POST("/projects/$projectId/tasks", taskPayload)

                        if (responseResult?.statusCode == 200) {
                            def jsonResponse = new JsonSlurper().parseText(responseResult?.body)
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
    }
}