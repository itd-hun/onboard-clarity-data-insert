package org.onboard

class CsvToDb {

    static void insertData(){
        String csvFilePath = getResourcePath("csv/resources.csv")
        List<String> resourcesList = readCsvFile(csvFilePath)
        println(resourcesList)
    }

    static List<String> readCsvFile(String filePath){
        BufferedReader reader = null;

        try{
            reader = new BufferedReader(new FileReader(filePath))
            List<String> result = reader.readLines()
            return result

        }catch (Exception e){
            println(e.message)
        }finally {
            reader.close()
        }

    }

    static String getResourcePath(String path){
        URL resourceUrl = CsvToDb.class.classLoader.getResource(path)
        String filePath = resourceUrl? resourceUrl.toURI().path : null
        return filePath
    }
}
