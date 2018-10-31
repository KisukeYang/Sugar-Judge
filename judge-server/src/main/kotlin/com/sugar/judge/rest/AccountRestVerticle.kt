/*
 * MIT License
 *
 * Copyright (c) 2018 Kisuke.Yang
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
package com.sugar.judge.rest

import com.sugar.judge.config.EventBusNamespace.*
import com.sugar.judge.config.EventBusNamespace.Companion.makeMessage
import com.sugar.judge.utils.*
import io.vertx.core.AbstractVerticle
import io.vertx.core.eventbus.Message
import io.vertx.core.eventbus.ReplyException
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.jwt.JWTAuth
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.JWTAuthHandler
import io.vertx.kotlin.core.json.get
import io.vertx.kotlin.coroutines.awaitResult
import io.vertx.kotlin.coroutines.dispatcher
import io.vertx.kotlin.ext.jwt.JWTOptions
import kotlinx.coroutines.experimental.launch
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

class AccountRestVerticle(mainRouter: Router, jwtProvider: JWTAuth, jwtAuthHandler: JWTAuthHandler) : AbstractVerticle() {

    private val logger = LoggerFactory.getLogger(AccountRestVerticle::class.java)

    private var router: Router = Router.router(vertx)

    private val provider: JWTAuth = jwtProvider

    private val jwtHandler: JWTAuthHandler = jwtAuthHandler

    private val jwtOptions = JWTOptions().setAlgorithm("HS512")//.setExpiresInMinutes(60 * 4)

    init {
        mainRouter.mountSubRouter("/rest", router)
    }

    override fun start() {
        super.start()
        router.post("/login").handler { handlerLogin(it) }
        router.post("/reg").handler { handleRegister(it) }
        router.put("/user").handler { handleUpdateUserInfo(it) }
    }

    private fun handlerLogin(rc: RoutingContext) {
        /*val request = rc.request()
        val requestHeader = request.headers()*/
        val requestData = rc.bodyAsJson
        val username = requestData.getString("username")
        val password = requestData.getString("password")
        if (!StringUtils.isEmpty(username) && !StringUtils.isEmpty(password)) {
            launch(context.dispatcher()) {
                try {
                    val reply = awaitResult<Message<JsonObject>> {
                        vertx.eventBus().send(ADDR_ACCOUNT_DB.get(),
                                makeMessage(COMMAND_GET_ACCOUNT, JsonObject().put("username", username)), it)
                    }
                    val result = reply.body()
                    if (result.isEmpty) {
                        fail(rc, "用户名或密码错误")
                    } else {
                        val userPassword = result.getString("password")
                        if (password == PasswordTools.decrypt(userPassword)) {
                            val role = result.getString("role")
                            val name = result.getString("nickname")
                            val id = result.getInteger("id")
                            val token = provider.generateToken(JsonObject().put("id", id).put("role", role).put("nickname", name), jwtOptions)
                            val response = JsonObject().put("token", token).put("role", role).put("nickname", name).put("id", id)
                            ok(rc, response)
                        } else {
                            fail(rc, "用户名或密码错误")
                        }
                    }
                } catch (e: ReplyException) {
                    e.printStackTrace()
                    logger.error("请求失败，地址:${rc.request().path()}, 原因：${e.message}")
                    error(rc, "服务器错误，请联系管理员！")
                }
            }
        } else {
            fail(rc, "用户名或密码错误")
        }
    }

    private fun handleRegister(rc: RoutingContext) {
        if (rc.bodyAsJson == null) {
            badRequest(rc)
            return
        }
        val params = rc.bodyAsJson
        if (params.getString("username").isEmpty()) {
            badRequest(rc, "用户名[username]不能为空")
            return
        }
        if (params.getString("password").isEmpty()) {
            badRequest(rc, "密码[password]不能为空")
            return
        }
        if (params.getString("email").isEmpty()) {
            badRequest(rc, "邮箱[email]不能为空")
            return
        }
        if (params.getString("nickname").isEmpty()) {
            badRequest(rc, "昵称[nickname]不能为空")
            return
        }
        params.put("password", PasswordTools.encrypt(params.getString("password")))
        params.put("registerTime", LocalDateTime.now().toString())
        params.put("role", "user")
        params.put("enabled", true)

        launch(context.dispatcher()) {
            try {
                val reply = awaitResult<Message<JsonObject>> {
                    vertx.eventBus().send(ADDR_ACCOUNT_DB.get(),
                            makeMessage(COMMAND_CREATE_ACCOUNT, params), it)
                }
                ok(rc, JsonObject(reply.body().toString()).put("message", "注册成功"))
            } catch (e: ReplyException) {
                if (e.localizedMessage.contains("duplicate key")) {
                    fail(rc, "用户名已经注册！")
                } else {
                    logger.error("请求失败，地址:${rc.request().path()}, 原因：${e.message}")
                    error(rc, "服务器错误，请联系管理员！")
                }
            }
        }
    }

    private fun handleUpdateUserInfo(rc: RoutingContext) {

    }
}