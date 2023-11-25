package com.doc.paymentchecker.data

import com.doc.paymentchecker.data.model.Result
import com.doc.paymentchecker.data.response.PlayerApiResponse
import com.doc.paymentchecker.data.response.ServerInfo
import com.doc.paymentchecker.data.response.UserInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class Repository {

    companion object {

        @Volatile
        private var INSTANCE: Repository? = null

        fun getInstance() = INSTANCE
            ?: synchronized(this) {
                INSTANCE ?: Repository()
                    .also { INSTANCE = it }
            }
    }

    fun playerApi(userName: String, password: String) = flow {
        emit(Result.Loading)

        var connection: HttpURLConnection? = null
        try {
            val spec =
                "http://line.my-tv.cc/player_api.php?username=${userName.trim()}&password=${password.trim()}"
            val url = URL(spec)
            connection = url.openConnection() as HttpURLConnection
            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                val inputStream = connection.inputStream
                val buffersSize = 1024 * 1024
                val buffers = ByteArray(buffersSize)
                val responseStringBuilder = StringBuilder()
                while (true) {
                    val readCount = inputStream.read(buffers, 0, buffersSize)
                    if (readCount != -1) {
                        if (readCount == buffersSize)
                            responseStringBuilder.append(String(buffers))
                        else
                            responseStringBuilder.append(String(buffers.copyOf(readCount)))
                    } else {
                        inputStream.close()
                        break
                    }
                }
                val response = JSONObject("$responseStringBuilder")
                val userInfo = response.getJSONObject("user_info")
                val serverInfo = response.getJSONObject("server_info");
                val result = Result.Success(
                    value = PlayerApiResponse(
                        userInfo = UserInfo(
                            expDateTimeMillis = userInfo.getString("exp_date").toLong() * 1000L
                        ),
                        serverInfo = ServerInfo(
                            url = serverInfo.getString("url"),
                        )
                    )
                )
                emit(result)
            } else {
                emit(Result.Error(Throwable("Make sure the User & Password is correct")))
            }
        } catch (e: Throwable) {
            emit(Result.Error(e))
        } finally {
            connection?.disconnect()
        }
    }.flowOn(Dispatchers.IO)
}