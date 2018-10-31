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

import com.sugar.judge.config.*
import com.sugar.judge.config.EventBusNamespace.*
import com.sugar.judge.config.EventBusNamespace.Companion.makeMessage
import com.sugar.judge.utils.*
import io.vertx.core.AbstractVerticle
import io.vertx.core.eventbus.Message
import io.vertx.core.eventbus.ReplyException
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.JWTAuthHandler
import io.vertx.kotlin.coroutines.awaitResult
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.experimental.launch
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

class SubmissionRestVerticle(mainRouter: Router, jwtAuthHandler: JWTAuthHandler) : AbstractVerticle() {
    private val logger = LoggerFactory.getLogger(SubmissionRestVerticle::class.java)

    private var router: Router = Router.router(vertx)

    private val jwtHandler: JWTAuthHandler = jwtAuthHandler

    init {
        mainRouter.mountSubRouter("/rest", router)
    }

    override fun start() {
        super.start()
        router.post("/submission").handler(jwtHandler).handler { handlePostSubmission(it) }
        router.get("/submission").handler { handleGetSubmissions(it) }
        router.post("/submission/:id/rejudge").handler(jwtHandler).handler { handleRejudge(it) }
        router.get("/submission/:id/code").handler(jwtHandler).handler { handleCheckoutCode(it) }
    }

    /**
     * 查询实时提交信息
     * */
    private fun handleGetSubmissions(rc: RoutingContext) {
        launch(vertx.dispatcher()) {
            try {
                val request = rc.request()
                val start = request.getParam("start") ?: "0"
                val size = request.getParam("size") ?: "20"
                val params = JsonObject().put("start", Integer.parseInt(start)).put("size", Integer.parseInt(size))
                val reply = awaitResult<Message<JsonObject>> {
                    vertx.eventBus().send(ADDR_SUBMISSION_DB.get(),
                            makeMessage(COMMAND_GET_SUBMISSION_LIST, params), it)
                }
                ok(rc, reply.body())
            } catch (e: NumberFormatException) {
                badRequest(rc, "请求参数错误")
            } catch (e: ReplyException) {
                e.printStackTrace()
                logger.error("请求失败，地址:${rc.request().path()}, 原因：${e.message}")
                error(rc, "服务器错误，请联系管理员！")
            }

        }
    }

    /**
     * 提测代码
     * */
    private fun handlePostSubmission(rc: RoutingContext) {
        val user = getCurrentUser(rc)
        if (user.getString("role") != "user") {
            forbidden(rc)
            return
        }
        val submission = rc.bodyAsJson
        if (submission.getString(KEY_SUBMISSION_PROBLEM_CODE).isEmpty()) {
            badRequest(rc, "代码[problemCode]不能为空")
            return
        }
        if (submission.getInteger(KEY_SUBMISSION_PROBLEM_ID) == null) {
            badRequest(rc, "试题[problemId]不能为空")
            return
        }
        if (submission.getString(KEY_SUBMISSION_LANGUAGE).isEmpty()) {
            badRequest(rc, "代码语言[language]不能为空")
            return
        } else {
            if (!listOf("JAVA", "C++", "C").contains(submission.getString(KEY_SUBMISSION_LANGUAGE))) {
                badRequest(rc, "不支持此种语言的代码")
                return
            }
        }

        launch(vertx.dispatcher()) {
            try {
                val problemResult = awaitResult<Message<JsonObject>> {
                    vertx.eventBus().send(ADDR_PROBLEM_DB.get(),
                            makeMessage(COMMAND_GET_ONE_PROBLEM, JsonObject().put("id", submission.getInteger("problemId"))), it)
                }
                if (problemResult.body().isEmpty) {
                    badRequest(rc, "试题不存在")
                    return@launch
                }

                submission.put(KEY_SUBMISSION_USER_ID, user.getInteger("id"))
                        .put(KEY_SUBMISSION_NICKNAME, user.getString("nickname"))
                        .put(KEY_SUBMISSION_PROBLEM_TITLE, problemResult.body().getString("title"))
                        .put(KEY_SUBMISSION_EXECUTE_TIME, 0)
                        .put(KEY_SUBMISSION_CONTEST_TIME, 0)
                        .put(KEY_SUBMISSION_STATUS, 0)
                        .put(KEY_SUBMISSION_LOCK, true)
                        .put(KEY_SUBMISSION_IP_ADDRESS, "127.0.0.1")
                        .put(KEY_SUBMISSION_SUBMIT_TIME, LocalDateTime.now().toString())
                        .put(KEY_SUBMISSION_ERROR_MESSAGE, "")
                val reply = awaitResult<Message<JsonObject>> {
                    vertx.eventBus().send(ADDR_SUBMISSION_DB.get(),
                            makeMessage(COMMAND_CREATE_SUBMISSION, submission), it)
                }
                // do judge
                ok(rc, reply.body())
            } catch (e: ReplyException) {
                e.printStackTrace()
                logger.error("请求失败，地址:${rc.request().path()}, 原因：${e.message}")
                error(rc, "服务器错误，请联系管理员！")
            }
        }
    }

    /**
     * 重测代码
     * */
    private fun handleRejudge(rc: RoutingContext) {
        launch(vertx.dispatcher()) {
            try {
                // 查询 submission
                val submissionId = rc.request().getParam("id")
                val param = JsonObject().put("id", Integer.parseInt(submissionId))
                val reply = awaitResult<Message<JsonObject>> {
                    vertx.eventBus().send(ADDR_SUBMISSION_DB.get(),
                            makeMessage(COMMAND_GET_SUBMISSION_INFO, param), it)
                }
                val submission = reply.body()
                if (submission.isEmpty) {
                    notFound(rc)
                    return@launch
                }
                val user = rc.user().principal()
                if (user.getInteger("id") != submission.getInteger("userId") && user.getString("role") != "admin") {
                    forbidden(rc)
                    return@launch
                }

                // do judge

                // 更新状态
                param.put("status", 2)
                val reply1 = awaitResult<Message<JsonObject>> {
                    vertx.eventBus().send(ADDR_SUBMISSION_DB.get(),
                            makeMessage(COMMAND_UPDATE_SUBMISSION, param), it)
                }
                ok(rc, reply.body())
            } catch (e: NumberFormatException) {
                badRequest(rc, "请求错误")
            } catch (e: ReplyException) {
                e.printStackTrace()
                logger.error("请求失败，地址:${rc.request().path()}, 原因：${e.message}")
                error(rc, "服务器错误，请联系管理员！")
            }

        }
    }

    /**
     * 查看提交代码
     * */
    private fun handleCheckoutCode(rc: RoutingContext) {

        launch(vertx.dispatcher()) {
            try {
                val submissionId = rc.request().getParam("id")
                val param = JsonObject().put("id", Integer.parseInt(submissionId))
                val reply = awaitResult<Message<JsonObject>> {
                    vertx.eventBus().send(ADDR_SUBMISSION_DB.get(),
                            makeMessage(COMMAND_GET_SUBMISSION_INFO, param), it)
                }
                val submission = reply.body()
                if (submission.isEmpty) {
                    notFound(rc)
                    return@launch
                }
                val user = rc.user().principal()
                if (user.getInteger("id") != submission.getInteger("userId") && user.getString("role") != "admin") {
                    forbidden(rc)
                    return@launch
                }

                ok(rc, JsonObject().put("data", submission.getString("code")))
            } catch (e: ReplyException) {
                e.printStackTrace()
                logger.error("请求失败，地址:${rc.request().path()}, 原因：${e.message}")
                error(rc, "服务器错误，请联系管理员！")
            }
        }
    }
}