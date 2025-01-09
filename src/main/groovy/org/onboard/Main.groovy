package org.onboard

class Main {
    static void main(String[] args) {

        List<String> resourcesList = getCsvData("csv/resources.csv")
        List<String> projectsList = getCsvData("csv/projects.csv")
        List<String> tasksList = getCsvData("csv/tasks.csv")
        List<String> assignmentsList = getCsvData("csv/assignments.csv")

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