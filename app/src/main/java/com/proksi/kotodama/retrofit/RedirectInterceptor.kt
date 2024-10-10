package com.proksi.kotodama.retrofit
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException


class RedirectInterceptor {
        @Throws(IOException::class)
        fun intercept(chain: Interceptor.Chain): Response {
            var request: Request = chain.request()
            var response: Response = chain.proceed(request)

            if (response.code == 307) {
                val newUrl = response.header("Location")
                if (newUrl != null) {
                    request = request.newBuilder()
                        .url(newUrl)
                        .build()
                    response = chain.proceed(request)
                }
            }
            return response
        }
}