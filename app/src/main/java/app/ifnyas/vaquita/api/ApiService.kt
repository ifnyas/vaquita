package app.ifnyas.vaquita.api

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

interface ApiService {

    @GET("covid/data")
    fun getData(): Call<ResponseBody>
}