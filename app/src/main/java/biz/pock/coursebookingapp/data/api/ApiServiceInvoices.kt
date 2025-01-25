package biz.pock.coursebookingapp.data.api

import biz.pock.coursebookingapp.data.model.Invoice
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Streaming

interface ApiServiceInvoices {
    @GET("/api/v1/invoices")
    suspend fun getInvoices(): Response<List<Invoice>>

    @GET("/api/v1/invoices/{invoice}/download")
    @Streaming  // Wichtig für große Dateien!
    suspend fun downloadInvoice(
        @Path("invoice") invoiceId: String
    ): Response<ResponseBody>

    @POST("/api/v1/invoices/{invoice}/extend-download-token")
    suspend fun extendDownloadToken(
        @Path("invoice") invoiceId: String,
        @Body expiryDate: Map<String, String>
    ): Response<Invoice>

    @POST("/api/v1/invoices/{invoice}/reverse")
    suspend fun cancelInvoice(
        @Path("invoice") invoiceId: String
    ): Response<Invoice>


}