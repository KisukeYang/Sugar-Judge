package com.sugar.judge.service

import com.sugar.judge.config.EventBusNamespace
import io.vertx.core.json.JsonObject
import org.slf4j.Logger

class AccountServiceVerticle: AbstractServiceVerticle() {

    override val listenAddress: String
        get() = ""

    override suspend fun processMethods(params: JsonObject, method: EventBusNamespace): JsonObject {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun log(): Logger {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}