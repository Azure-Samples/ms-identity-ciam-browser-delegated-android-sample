package com.azuresamples.msaldelegatedandroidkotlinsampleapp

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response


object ApiClient {
    private val client = OkHttpClient()

    fun performGetApiRequest(WEB_API_BASE_URL: String, accessToken: String?): Response {
        val fullUrl = "$WEB_API_BASE_URL/api/todolist"

        val requestBuilder = Request.Builder()
                .url(fullUrl)
                .addHeader("Authorization", "Bearer $accessToken")
                .get()

        val request = requestBuilder.build()

        client.newCall(request).execute().use { response -> return response }
    }
}
