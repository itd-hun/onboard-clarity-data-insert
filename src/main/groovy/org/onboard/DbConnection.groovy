package org.onboard

import de.itdesign.clarity.logging.CommonLogger
import groovy.sql.Sql

class DbConnection {

    static CommonLogger logger = new CommonLogger(this)
    //Connecting oracle database
    static Sql connectDb() {
        Map<String, String> configData = getDbConfig();
        Sql sql = null;
        try {
            sql = Sql.newInstance(configData.url, configData.username, configData.password, configData.driver)
            def result = sql.firstRow("SELECT 1 FROM DUAL")
            if (result) {
                return sql
            } else {
                logger.info("Database connection fail.")
                return null
            }
        } catch (Exception e) {
            logger.error("Error in connecting to DB: ${e.getMessage()}")
        }
        return sql
    }

    //Getting db configure info from properties
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
