/*
 * Copyright (c) 2018-2020 Karlatemp. All rights reserved.
 * @author Karlatemp <karlatemp@vip.qq.com> <https://github.com/Karlatemp>
 * @create 2020/08/07 13:56:06
 *
 * YggdrasilOfficialProxy/YggdrasilOfficialProxy.main/FakeResponse.kt
 */

package io.github.karlatemp.yop

import io.ktor.client.call.HttpClientCall
import io.ktor.client.statement.HttpResponse
import io.ktor.http.Headers
import io.ktor.http.HttpProtocolVersion
import io.ktor.http.HttpStatusCode
import io.ktor.util.date.GMTDate
import io.ktor.utils.io.ByteReadChannel
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

object NoContextResponse: HttpResponse() {
    override val call: HttpClientCall
        get() = TODO("Not yet implemented")
    override val content: ByteReadChannel
        get() = ByteReadChannel.Empty
    override val coroutineContext: CoroutineContext
        get() = EmptyCoroutineContext
    override val headers: Headers
        get() = TODO("Not yet implemented")
    override val requestTime: GMTDate
        get() = TODO("Not yet implemented")
    override val responseTime: GMTDate
        get() = TODO("Not yet implemented")
    override val status: HttpStatusCode
        get() = HttpStatusCode.NoContent
    override val version: HttpProtocolVersion
        get() = HttpProtocolVersion.HTTP_1_1

}
