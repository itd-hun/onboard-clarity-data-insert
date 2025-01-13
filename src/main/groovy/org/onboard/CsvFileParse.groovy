package org.onboard

class CsvFileParse {
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
        URL resourceUrl = CsvFileParse.class.classLoader.getResource(path)
        String filePath = resourceUrl ? resourceUrl.toURI().path : null
        return filePath
    }
}
