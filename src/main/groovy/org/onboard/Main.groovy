package org.onboard

import de.itdesign.clarity.rest.ClarityRestClient
import de.itdesign.clarity.rest.RestResponse
import groovy.sql.Sql
import groovy.json.JsonSlurper

class Main {

    static void main(String[] args) {

        Sql sql = connectDb()

        if (sql != null) {
            println("Db connected Successfully")
            List<String> resourcesList = getCsvData("csv/resources.csv")
            List<String> projectsList = getCsvData("csv/projects.csv")
            List<String> tasksList = getCsvData("csv/tasks.csv")
            List<String> assignmentsList = getCsvData("csv/assignments.csv")

            try {
                ClarityRestClient rest
                rest = new ClarityRestClient("admin", sql.getConnection(), "http://10.0.0.248:7080")
                RestResponse response = rest.GET("/projects/")

                if (response?.statusCode == 200) {
                    def jsonResponse = new JsonSlurper().parseText(response?.body)
                    jsonResponse.each { project ->
                        println "Project ID: ${project}"
                    }
                } else {
                    println("Failed to fetch data. Status code: ${response?.statusCode}")
                }
            } catch (Exception e) {
                println(e.getMessage())
            }

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

    static List<String> getCsvData(String path) {
        String csvFilePath = getResourcePath(path)
        List<String> resourcesList = readCsvFile(csvFilePath)
        return resourcesList
    }

    //Read CSV and return list of data
    static List<String> readCsvFile(String filePath) {
        BufferedReader reader = null;
        def result = null

        try {
            reader = new BufferedReader(new FileReader(filePath))
            result = reader.readLines()
        } catch (Exception e) {
            println(e.message)
        } finally {
            reader.close()
        }
        return result
    }

    //Get CSV file path from resources
    static String getResourcePath(String path) {
        URL resourceUrl = Main.class.classLoader.getResource(path)
        String filePath = resourceUrl ? resourceUrl.toURI().path : null
        return filePath
    }

}