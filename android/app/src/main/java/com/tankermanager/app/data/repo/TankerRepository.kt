package com.tankermanager.app.data.repo

import com.google.gson.Gson
import com.tankermanager.app.data.api.ApiClient
import com.tankermanager.app.data.api.TankerApi
import com.tankermanager.app.data.model.ApiError
import retrofit2.HttpException
import java.io.IOException

class TankerRepository(private val sessionStore: SessionStore) {
    private val api: TankerApi get() = ApiClient.get(sessionStore)

    suspend fun <T> safe(block: suspend TankerApi.() -> T): Result<T> = try {
        Result.success(api.block())
    } catch (e: HttpException) {
        val body = e.response()?.errorBody()?.string()
        val msg = try {
            Gson().fromJson(body, ApiError::class.java)?.error
                ?: Gson().fromJson(body, ApiError::class.java)?.message
        } catch (_: Exception) {
            null
        }
        Result.failure(Exception(msg ?: "Request failed (${e.code()})"))
    } catch (e: IOException) {
        Result.failure(Exception("No connection. Is the server running?"))
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun api() = api
    fun session() = sessionStore
}
