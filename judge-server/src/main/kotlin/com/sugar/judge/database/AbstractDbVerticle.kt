/*
 * MIT License
 *
 * Copyright (c) 2018 Leibniz.Hu
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.sugar.judge.database

import com.sugar.judge.config.EventBusNamespace
import io.reactiverse.pgclient.*
import io.vertx.core.AbstractVerticle
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.experimental.launch
import org.slf4j.Logger

/**
 * 数据库访问服务Verticle的抽象类
 * 继承AbstractVerticle， 覆盖start()方法，统一了从监听EventBus并分发请求任务的逻辑
 *
 * @author Leibniz.Hu
 * Created on 2017-10-25 14:46.
 */
abstract class AbstractDbVerticle : AbstractVerticle() {
    abstract val listenAddress: String

    lateinit var client: PgClient

    override fun start() {
        this.client = PgClient.pool(vertx, PgPoolOptions(config()).setCachePreparedStatements(false))
        log().info("PgPool 连接池初始化成功!")
        // 开始监听 EventBus
        vertx.eventBus().consumer<JsonObject>(listenAddress) { message ->
            launch(context.dispatcher()) {
                val msgBody = message.body()
                val methodStr = msgBody.getString(EventBusNamespace.METHOD)
                lateinit var method: EventBusNamespace
                try {
                    method = EventBusNamespace.valueOf(methodStr)
                } catch (e: Throwable) {
                    log().error("错误的请求方法名{}", methodStr)
                    message.fail(404, e.message + "错误的请求方法名" + methodStr)
                }
                log().debug("接收到消息(${message.address()}),消息内容：{$msgBody}")

                try {
                    val result = processMethods(msgBody.getJsonObject(EventBusNamespace.PARAMS), method)
                    message.reply(result)
                } catch (e: Exception) {
                    /*if (e.localizedMessage.startsWith("duplicate key")){
                        message.fail(500, "数据已存在")
                    }else{
                        message.fail(500, e.localizedMessage)
                    }*/
                    message.fail(500, e.localizedMessage)
                }

            }
        }
    }

    /**
     * 子类需要实现的分发处理请求的方法
     * @param params   请求的参数
     * @param method   请求的方法
     */
    protected abstract suspend fun processMethods(params: JsonObject, method: EventBusNamespace): JsonObject


    /**
     * 子类需要实现的，获取子类Logger的方法
     * @return 子类的Logger
     */
    protected abstract fun log(): Logger

}