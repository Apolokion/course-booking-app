package biz.pock.coursebookingapp.data.api.interfaces

interface NetworkConnection {
    fun isNetworkAvailable(): Boolean
}