package com.sugar.judge.config

import io.vertx.core.json.JsonObject

enum class RestEventBusNamespace {

    ADDR_PROBLEM_REQ;

    fun get() : String {
        return this.toString()
    }

    companion object {
        //EventBus消息的方法名key
        const val PARAMS : String = "judge.params"
        //EventBus消息的参数列表key
        const val METHOD : String = "judge.method"

        fun makeMessage(method : EventBusNamespace, params : JsonObject) : JsonObject {
            return JsonObject().put(METHOD, method.get()).put(
                    PARAMS,
                    params
            )
        }

        fun makeMessage(method : EventBusNamespace): JsonObject {
            return JsonObject().put(METHOD, method.get())
        }
    }
}