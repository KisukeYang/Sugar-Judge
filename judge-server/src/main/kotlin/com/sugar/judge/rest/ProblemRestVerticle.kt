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
import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.Message
import io.vertx.core.eventbus.ReplyException
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.JWTAuthHandler
import io.vertx.kotlin.core.json.get
import io.vertx.kotlin.coroutines.awaitResult
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.experimental.launch
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

class ProblemRestVerticle(mainRouter: Router, jwtAuthHandler: JWTAuthHandler) : AbstractVerticle() {
    private val logger = LoggerFactory.getLogger(ProblemRestVerticle::class.java)

    private var router: Router = Router.router(vertx)

    private val jwtHandler: JWTAuthHandler = jwtAuthHandler

    init {
        mainRouter.mountSubRouter("/rest", router)
    }

    override fun start() {
        super.start()
        router.get("/problems").handler { handleGetProblemList(it) }
        router.get("/problems/:id").handler { handleGetProblemById(it) }
        router.post("/problems").handler(jwtHandler).handler { handleAddProblem(it) }
        router.put("/problems/:id").handler(jwtHandler).handler { handleUpdateProblem(it) }
        router.delete("/problems/:id").handler(jwtHandler).handler { handleDeleteProblem(it) }
    }

    /**
     * 查询试题列表
     * */
    private fun handleGetProblemList(rc: RoutingContext) {
        launch(context.dispatcher()) {
            try {
                val request = rc.request()
                val start = Integer.parseInt(request.getParam("start") ?: "0")
                val size = Integer.parseInt(request.getParam("size") ?: "20")
                val params = JsonObject().put("start", start).put("size", size)
                val reply = awaitResult<Message<JsonObject>> {
                    vertx.eventBus().send(ADDR_PROBLEM_DB.get(),
                            makeMessage(COMMAND_GET_PROBLEM_LIST, params), it)
                }
                ok(rc, reply.body())
            } catch (e: ReplyException) {
                e.printStackTrace()
                logger.error("请求失败，地址:${rc.request().path()}, 原因：${e.message}")
                com.sugar.judge.utils.error(rc, "服务器错误，请联系管理员！")
            } catch (e: Throwable) {
                badRequest(rc)
            }
        }
    }

    /**
     * 根据 ID 获取试题详情
     * */
    private fun handleGetProblemById(rc: RoutingContext) {
        launch(context.dispatcher()) {
            try {
                val params = JsonObject().put("id", Integer.parseInt(rc.request().getParam("id")))
                val reply = awaitResult<Message<JsonObject>> {
                    vertx.eventBus().send(ADDR_PROBLEM_DB.get(),
                            makeMessage(COMMAND_GET_ONE_PROBLEM, params), it)

                }
                if (reply.body().isEmpty || reply.body().getBoolean("hidden")) {
                    notFound(rc)
                } else {
                    ok(rc, reply.body())
                }
            } catch (e: NumberFormatException) {
                badRequest(rc)
            } catch (e: ReplyException) {
                e.printStackTrace()
                logger.error("请求失败，地址:${rc.request().path()}, 原因：${e.message}")
                error(rc, "服务器错误，请联系管理员！")
            }
        }
    }

    /**
     *  创建新试题
     *
     * */
    private fun handleAddProblem(rc: RoutingContext) {
        launch(context.dispatcher()) {
            try {
                if (!checkAdmin(rc)) {
                    return@launch
                }
                if (rc.bodyAsJson.isEmpty) {
                    badRequest(rc, "请求参数错误")
                    return@launch
                }
                var params = rc.bodyAsJson
                if (!verifyProblemParam(rc, params)) {
                    return@launch
                }

                val time = LocalDateTime.now().toString()
                params.put("disuse", false)
                params.put("createTime", time)
                params.put("updateTime", time)
                params.put("author", getCurrentUser(rc).getString("nickname"))
                // 获取试题 ID
                val replyId = awaitResult<Message<JsonObject>> {
                    vertx.eventBus().send(ADDR_PROBLEM_DB.get(),
                            makeMessage(COMMAND_GET_PROBLEM_NEXT_ID, JsonObject()), it)
                }
                val newId = replyId.body().getInteger("id")
                // 写入测试数据文件
                val fileSystem = vertx.fileSystem()
                val inFilePath = config().getString("inData")
                val outFilePath = config().getString("outData")
                awaitResult<Void> {
                    fileSystem.mkdirs(inFilePath, it)
                }
                awaitResult<Void> {
                    fileSystem.mkdirs(outFilePath, it)
                }
                awaitResult<Void> {
                    fileSystem.writeFile("$inFilePath/$newId.in", Buffer.buffer(params.getString("theInput"), "UTF-8"), it)
                }
                awaitResult<Void> {
                    fileSystem.writeFile("$outFilePath/$newId.out", Buffer.buffer(params.getString("theOutput"), "UTF-8"), it)
                }
                params.put("id", newId)
                params.put("inFilePath", "$inFilePath/$newId.in")
                params.put("outFilePath", "$outFilePath/$newId.out")
                // 写入数据库
                val reply = awaitResult<Message<JsonObject>> {
                    vertx.eventBus().send(ADDR_PROBLEM_DB.get(),
                            makeMessage(COMMAND_CREATE_PROBLEM, params), it)
                }

                ok(rc, reply.body())
            } catch (e: ReplyException) {
                e.printStackTrace()
                logger.error("请求失败，地址:${rc.request().path()}, 原因：${e.message}")
                error(rc, "服务器错误，请联系管理员！")
            }
        }
    }

    private fun handleUpdateProblem(rc: RoutingContext) {
        launch(context.dispatcher()) {

            try {
                if (!checkAdmin(rc)) {
                    return@launch
                }
                if (rc.body == null) {
                    badRequest(rc)
                }
                // json 格式化 校验
                val params = rc.bodyAsJson
                val problemId = Integer.parseInt(rc.request().getParam("id"))
                params.put("id", problemId)
                val queryReply = awaitResult<Message<JsonObject>> {
                    vertx.eventBus().send(ADDR_PROBLEM_DB.get(),
                            makeMessage(COMMAND_GET_ONE_PROBLEM,
                                    params)
                            , it)
                }
                if (queryReply.body() == null) { // 试题不存在
                    notFound(rc)
                    return@launch
                } else {
                    // 开始更新试题信息
                    val fileSystem = vertx.fileSystem()
                    val inFilePath = config().getString("inData")
                    val outFilePath = config().getString("outData")
                    if (params.getString("theInput").isNotBlank()) {
                        // 写入测试数据文件
                        awaitResult<Void> {
                            fileSystem.mkdirs(inFilePath, it)
                        }
                        awaitResult<Void> {
                            fileSystem.writeFile("$inFilePath/$problemId.in", Buffer.buffer(params.getString("theInput"), "UTF-8"), it)
                        }
                    }
                    if (params.getString("theInput").isNotBlank()) {
                        awaitResult<Void> {
                            fileSystem.mkdirs(outFilePath, it)
                        }
                        awaitResult<Void> {
                            fileSystem.writeFile("$outFilePath/$problemId.out", Buffer.buffer(params.getString("theOutput"), "UTF-8"), it)
                        }
                    }
                    // 更新数据库
                    val reply = awaitResult<Message<JsonObject>> {
                        vertx.eventBus().send(ADDR_PROBLEM_DB.get(),
                                makeMessage(COMMAND_UPDATE_PROBLEM, params), it)
                    }
                    ok(rc, reply.body())
                }
            } catch (e: NumberFormatException) {
                badRequest(rc)
            } catch (e: ReplyException) {
                e.printStackTrace()
                logger.error("请求失败，地址:${rc.request().path()}, 原因：${e.message}")
                error(rc, "服务器错误，请联系管理员！")
            }
        }
    }

    /**
     * 删除试题
     * */
    private fun handleDeleteProblem(rc: RoutingContext) {
        if (!checkAdmin(rc)) {
            return
        }
        launch(context.dispatcher()) {
            try {
                val params = JsonObject().put("id", Integer.parseInt(rc.request().getParam("id")))
                val reply = awaitResult<Message<JsonObject>> {
                    vertx.eventBus().send(ADDR_PROBLEM_DB.get(),
                            makeMessage(COMMAND_DELETE_PROBLEM, params), it)
                }
                ok(rc)
            } catch (e: NumberFormatException) {
                badRequest(rc)
            } catch (e: ReplyException) {
                e.printStackTrace()
                logger.error("请求失败，地址:${rc.request().path()}, 原因：${e.message}")
                error(rc, "服务器错误，请联系管理员！")
            }
        }

    }

    private fun verifyProblemParam(rc: RoutingContext, params: JsonObject): Boolean {
        if (!params.containsKey("title")) {
            badRequest(rc, "[title]标题不能为空")
            return false
        }
        if (!params.containsKey("content")) {
            badRequest(rc, "[content]正文不能为空")
            return false
        }
        if (!params.containsKey("memoryLimit")) {
            badRequest(rc, "[memoryLimit]内存限制不能为空")
            return false
        }
        if (!params.containsKey("timeLimit")) {
            badRequest(rc, "[timeLimit]时间限制不能为空")
            return false
        }
        if (!params.containsKey("sampleInput")) {
            badRequest(rc, "[sampleInput]示例输入不能为空")
            return false
        }
        if (!params.containsKey("sampleOutput")) {
            badRequest(rc, "[sampleOutput]示例输出不能为空")
            return false
        }
        if (!params.containsKey("theInput")) {
            badRequest(rc, "[theInput]测试输入数据不能为空")
            return false
        }
        if (!params.containsKey("theOutput")) {
            badRequest(rc, "[theOutput]测试输出数据不能为空")
            return false
        }
        if (!params.containsKey("hidden")) {
            badRequest(rc, "[hidden]是否隐藏不能为空")
            return false
        }
        return true
    }
}