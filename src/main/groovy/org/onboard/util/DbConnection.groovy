package org.onboard.util

import groovy.sql.Sql

class DbConnection {

    static void connectDb() {
        Map<String, String> configData = getDbConfig();
        Sql sql = null;

        try {
            sql = Sql.newInstance(configData.url, configData.username, configData.password, configData.driver)
            String query = "SELECT * FROM SRM_RESOURCES sr WHERE ID = 5004002"
            def result = sql.rows(query)
            if (result) {
                println("Query result: $result")
            } else {
                println("No data found for the given ID.")
            }
            println("connected...")
        } catch (Exception e) {
            println(e.getMessage())
        }

    }

    static Map<String, String> getDbConfig() {
        Properties properties = new Properties()
        def propertiesFile = DbConnection.class.classLoader.getResourceAsStream("application.properties")
        properties.load(propertiesFile)

        def dbUrl = properties.getProperty("db.url")
        def dbUsername = properties.getProperty("db.username")
        def dbPassword = properties.getProperty("db.password")
        def dbDriver = properties.getProperty("db.driver")

        def dbConfigData = [url: dbUrl, username: dbUsername, password: dbPassword, driver: dbDriver]

        return dbConfigData

    }
}
