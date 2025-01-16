package org.onboard

class CsvFileParse {
    //Read file resource and get CSV data
    static List<Map> getCsvData(String path) {
        String csvFilePath = getResourcePath(path)
        List<List<String>> dataList = readCsvFile(csvFilePath)
        List<Map> mappedData = mapDataWithHeaders(dataList)
        return mappedData
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

    static List<Map> mapDataWithHeaders(List<List<String>> dataList) {
        def result = []
        def headers = dataList[0]
        dataList[1..-1].each { data ->
            def rowMap = [:]
            headers.eachWithIndex { String key, int index ->
                rowMap[key] = data[index]
            }
            result.add(rowMap)
        }
        return result
    }
}
