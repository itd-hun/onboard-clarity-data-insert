package org.onboard

import groovy.sql.Sql

class Main {
    static void main(String[] args) {

        Sql sql = connectDb()
        if (sql != null) {

            println("Db is connected")
            List<String> resourcesList = getCsvData("csv/resources.csv")
            List<String> projectsList = getCsvData("csv/projects.csv")
            List<String> tasksList = getCsvData("csv/tasks.csv")
            List<String> assignmentsList = getCsvData("csv/assignments.csv")
            insertResources(sql, resourcesList)
            insertProjects(sql, projectsList)
            insertTasks(sql, tasksList)
            insertAssignments(sql, assignmentsList)

        } else {
            println("Db is not connected")
        }
    }

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

    static String getResourcePath(String path) {
        URL resourceUrl = Main.class.classLoader.getResource(path)
        String filePath = resourceUrl ? resourceUrl.toURI().path : null
        return filePath
    }

    static void insertResources(Sql sql, List<String> resourcesList) {

        def columns = resourcesList.remove(0)

        try {
            resourcesList.each { data ->
                List<String> rowData = data.split(",")
                String placeholders = rowData.collect { "?" }.join(",")

                String query = "INSERT INTO z_resource ($columns) VALUES ($placeholders)"
                sql.execute(query, rowData)

            }
            println("Data inserted successfully")
        } catch (Exception e) {
            println(e.getMessage())
        }

    }

    static void insertProjects(Sql sql, List<String> projectsList) {
        String columns = projectsList.remove(0)

        try {
            projectsList.each { data ->
                List<String> rowData = data.split(",")
//                String placeholders = rowData.collect { "?" }.join(",")
                String query = "INSERT INTO z_project ($columns) VALUES (?, ?, TO_DATE(?, 'YYYY-MM-DD HH24:MI:SS'), TO_DATE(?, 'YYYY-MM-DD HH24:MI:SS'), TO_DATE(?, 'YYYY-MM-DD HH24:MI:SS'), ?)"
                sql.execute(query, rowData)

            }
        } catch (Exception e) {
            println(e.getMessage())
        }
    }

    static void insertTasks(Sql sql, List<String> tasksList) {
        String columns = tasksList.remove(0)
        try {
            tasksList.each { data ->
                List<String> rowData = data.split(",")
                String placeholders = rowData.collect { "?" }.join(",")
                String query = "INSERT INTO z_task ($columns) VALUES ($placeholders)"
                sql.execute(query, rowData)
            }
        } catch (Exception e) {
            println(e.getMessage())
        }
    }

    static void insertAssignments(Sql sql, List<String> assignmentList) {
        String columns = assignmentList.remove(0)
        try {
            assignmentList.each { data ->
                List<String> rowData = data.split(",")
                String placeholders = rowData.collect { "?" }.join(",")
                String query = "INSERT INTO z_assignment ($columns) VALUES ($placeholders)"
                sql.execute(query, rowData)
            }
        } catch (Exception e) {
            println(e.getMessage())
        }
    }

}