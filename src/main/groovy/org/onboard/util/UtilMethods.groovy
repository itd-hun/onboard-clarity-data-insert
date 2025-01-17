package org.onboard.util

class UtilMethods {

    //Get lookup number for status
    static int getLookupValue(String status) {
        if (status == "In Progress") {
            return 1
        } else if (status == "Completed") {
            return 2
        }
        return 0
    }

}
