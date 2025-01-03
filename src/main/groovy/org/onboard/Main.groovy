package org.onboard

import org.onboard.util.DbConnection

class Main {
    static void main(String[] args) {
        CsvToDb.insertData()
//       println(DbConnection.getDbConfig())
        DbConnection.connectDb()
    }
}