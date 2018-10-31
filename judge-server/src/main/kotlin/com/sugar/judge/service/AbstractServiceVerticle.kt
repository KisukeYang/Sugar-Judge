package com.sugar.judge.service

import com.sugar.judge.config.EventBusNamespace
import io.vertx.core.AbstractVerticle
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.experimental.launch
import org.slf4j.Logger

abstract class AbstractServiceVerticle: AbstractVerticle() {

    abstract val listenAddress: String

    override fun start() {
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