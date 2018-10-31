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

import com.sugar.judge.config.CONTEST_STATUS_STOP
import com.sugar.judge.config.EventBusNamespace.*
import com.sugar.judge.config.EventBusNamespace.Companion.makeMessage
import com.sugar.judge.utils.*
import io.vertx.core.AbstractVerticle
import io.vertx.core.eventbus.Message
import io.vertx.core.eventbus.ReplyException
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.JWTAuthHandler
import io.vertx.kotlin.coroutines.awaitResult
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.experimental.launch
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

class ContestRestVerticle(mainRouter: Router, jwtAuthHandler: JWTAuthHandler) : AbstractVerticle() {

    private val logger = LoggerFactory.getLogger(ContestRestVerticle::class.java)

    private var router: Router = Router.router(vertx)

    private val jwtHandler: JWTAuthHandler = jwtAuthHandler

    init {
        mainRouter.mountSubRouter("/rest", router)
    }

    override fun start() {
        super.start()
        router.get("/contests").handler { handleGetContestList(it) }
        router.get("/contests/:id").handler { handleGetContest(it) }
        router.post("/contests").handler(jwtHandler).handler { handleCreateContest(it) }
        router.put("/contests/:id").handler(jwtHandler).handler { handleUpdateContest(it) }
        router.delete("/contests/:id").handler(jwtHandler).handler { handleDeleteContest(it) }
        router.post("/contests/:id/problems").handler(jwtHandler).handler { handleAddContestProblem(it) }
        router.delete("/contests/:id/problems").handler(jwtHandler).handler { handleRemoveContestProblem(it) }
        router.post("/contests/:id/user").handler(jwtHandler).handler { handleJoinContest(it) }
        router.delete("/contests/:id/user").handler(jwtHandler).handler { handleQuitContest(it) }
        router.get("/contests/:id/problems").handler(jwtHandler).handler { handleGetContestProblemList(it) }
        router.get("/contests/:id/users").handler(jwtHandler).handler { handleGetContestUserList(it) }
    }

    /**
     *  获取竞赛列表
     * */
    private fun handleGetContestList(rc: RoutingContext) {
        val request = rc.request()
        val start = Integer.parseInt(request.getParam("start") ?: "0")
        val size = Integer.parseInt(request.getParam("size") ?: "20")
        val params = JsonObject().put("start", start).put("size", size)
        launch(context.dispatcher()) {
            try {
                val reply = awaitResult<Message<JsonObject>> {
                    vertx.eventBus().send(ADDR_CONTEST_DB.get(),
                            makeMessage(COMMAND_GET_CONTEST_LIST, params), it)
                }
                ok(rc, reply.body())

            } catch (e: ReplyException) {
                e.printStackTrace()
                logger.error("请求失败，地址:${rc.request().path()}, 原因：${e.message}")
                error(rc, "服务器错误，请联系管理员！")
            }
        }
    }

    /**
     *  获取竞赛信息
     * */
    private fun handleGetContest(rc: RoutingContext) {

        // 启动协程
        launch(context.dispatcher()) {
            try {
                val contestId = rc.request().getParam("id")

                val params = JsonObject().put("id", Integer.parseInt(contestId))
                        .put("hidden", false).put("disuse", false)
                val reply = awaitResult<Message<JsonObject>> {
                    vertx.eventBus().send(ADDR_CONTEST_DB.get(),
                            makeMessage(COMMAND_GET_CONTEST_INFO, params), it)
                }
                if (reply.body().isEmpty) {
                    notFound(rc)
                } else {
                    ok(rc, reply.body())
                }
            } catch (e: ReplyException) {
                e.printStackTrace()
                logger.error("请求失败，地址:${rc.request().path()}, 原因：${e.message}")
                error(rc, "服务器错误，请联系管理员！")
            }
        }
    }

    /**
     *  创建竞赛
     *  参数: com.sugar.judge.entity.Contest
     * */
    private fun handleCreateContest(rc: RoutingContext) {
        // 权限验证
        if (!checkAdmin(rc)) {
            return
        }
        val user = getCurrentUser(rc)

        val param = rc.bodyAsJson
        if (param == null) {
            badRequest(rc, "参数错误")
            return
        }
        if (param.getString("contestName") == null) {
            badRequest(rc, "竞赛名称[contestName]不能为空")
            return
        }
        if (param.getString("startTime") == null) {
            badRequest(rc, "开始时间[startTime]不能为空")
            return
        }
        if (param.getInteger("timeLimit") == null) {
            badRequest(rc, "时间限制[timeLimit]不能为空")
            return
        }
        if (param.getString("description") == null) {
            badRequest(rc, "描述[description]不能为空")
            return
        }
        val current = LocalDateTime.now().toString()
        var params = rc.bodyAsJson
        params.put("disuse", false)
        params.put("createTime", current)
        params.put("updateTime", current)
        params.put("creator", user.getString("nickname"))
        params.put("autoStart", false)
        params.put("status", 1)
        params.put("hidden", false)
        launch(context.dispatcher()) {
            try {
                val reply = awaitResult<Message<JsonObject>> {
                    vertx.eventBus().send(ADDR_CONTEST_DB.get(),
                            makeMessage(COMMAND_CREATE_CONTEST, params), it)
                }
                if (reply.body().getInteger("count") > 0){
                    ok(rc)
                }else{
                    fail(rc, "创建失败")
                }
            } catch (e: ReplyException) {
                e.printStackTrace()
                logger.error("请求失败，地址:${rc.request().path()}, 原因：${e.message}")
                error(rc, "服务器错误，请联系管理员！")
            }
        }
    }

    /**
     *  更新竞赛信息
     * */
    private fun handleUpdateContest(rc: RoutingContext) {
        // 权限验证
        if (!checkAdmin(rc)) {
            return
        }
        if (rc.bodyAsJson == null) {
            badRequest(rc)
            return
        }
        val params = rc.bodyAsJson
        params.put("id", Integer.parseInt(rc.request().getParam("id")))
        params.put("updateTime", LocalDateTime.now().toString())
        launch(context.dispatcher()) {
            try {
                val reply = awaitResult<Message<JsonObject>> {
                    vertx.eventBus().send(ADDR_CONTEST_DB.get(),
                            makeMessage(COMMAND_UPDATE_CONTEST, params), it)
                }
                ok(rc, reply.body())
            } catch (e: ReplyException) {
                e.printStackTrace()
                logger.error("请求失败，地址:${rc.request().path()}, 原因：${e.message}")
                error(rc, "服务器错误，请联系管理员！")
            }

        }
    }

    /**
     *  删除竞赛
     * */
    private fun handleDeleteContest(rc: RoutingContext) {
        // 权限验证
        if (!checkAdmin(rc)) {
            return
        }
        launch(context.dispatcher()) {
            try {
                val params = JsonObject().put("id", Integer.parseInt(rc.request().getParam("id")))
                val reply = awaitResult<Message<JsonObject>> {
                    vertx.eventBus().send(ADDR_CONTEST_DB.get(),
                            makeMessage(COMMAND_DELETE_CONTEST, params), it)
                }
                ok(rc, reply.body())
            } catch (e: ReplyException) {
                e.printStackTrace()
                logger.error("请求失败，地址:${rc.request().path()}, 原因：${e.message}")
                error(rc, "服务器错误，请联系管理员！")
            }
        }
    }

    /**
     * 参加竞赛
     * */
    private fun handleJoinContest(rc: RoutingContext) {
        val user = getCurrentUser(rc)
        val role = user.getString("role")
        if (role == "admin") {
            forbidden(rc)
            return
        }
        launch(context.dispatcher()) {
            try {
                val contestId = Integer.parseInt(rc.request().getParam("id"))
                val userId = user.getInteger("id")
                // 查询竞赛信息
                val replyContest = awaitResult<Message<JsonObject>> {
                    vertx.eventBus().send(ADDR_CONTEST_DB.get(),
                            makeMessage(COMMAND_GET_CONTEST_INFO, JsonObject().put("id", contestId)), it)
                }
                val contest = replyContest.body()
                if (contest.isEmpty) {
                    badRequest(rc, "竞赛不存在")
                    return@launch
                }
                if (contest.getInteger("status") == CONTEST_STATUS_STOP) {
                    badRequest(rc, "竞赛已结束")
                    return@launch
                }

                // 查询用户是否已参加
                val replyUser = awaitResult<Message<JsonObject>> {
                    vertx.eventBus().send(ADDR_CONTEST_USER_DB.get(),
                            makeMessage(COMMAND_GET_CONTEST_USER, JsonObject().put("userId", userId)
                                    .put("contestId", contestId)), it)
                }
                if (replyUser.body().getInteger("count") > 0) {
                    badRequest(rc, "已参加此竞赛")
                    return@launch
                }
                // 添加竞赛用户
                val contestUser = JsonObject().put("contestId", contestId)
                        .put("userId", userId)
                awaitResult<Message<JsonObject>> {
                    vertx.eventBus().send(ADDR_CONTEST_USER_DB.get(),
                            makeMessage(COMMAND_ADD_CONTEST_USER, contestUser), it)
                }
                // 为用户生成新的 token
                ok(rc)
            } catch (e: NumberFormatException) {
                badRequest(rc)
            } catch (e: Exception) {
                e.printStackTrace()
                logger.error("请求失败，地址:${rc.request().path()}, 原因：${e.message}")
                error(rc, "服务器错误，请联系管理员！")
            }
        }
    }

    /**
     * 退出竞赛
     * */
    private fun handleQuitContest(rc: RoutingContext) {
        val user = getCurrentUser(rc)
        val role = user.getString("role")
        if (role == "admin") {
            forbidden(rc)
            return
        }
        launch(context.dispatcher()) {
            try {
                val contestId = Integer.parseInt(rc.request().getParam("id"))
                val userId = user.getInteger("id")
                // 查询竞赛信息
                val replyContest = awaitResult<Message<JsonObject>> {
                    vertx.eventBus().send(ADDR_CONTEST_DB.get(),
                            makeMessage(COMMAND_GET_CONTEST_INFO, JsonObject().put("id", contestId)), it)
                }
                val contest = replyContest.body()
                if (contest.isEmpty) {
                    badRequest(rc, "竞赛不存在")
                    return@launch
                }
                if (contest.getInteger("status") == CONTEST_STATUS_STOP) {
                    badRequest(rc, "竞赛已结束")
                    return@launch
                }
                awaitResult<Message<JsonObject>> {
                    vertx.eventBus().send(ADDR_CONTEST_USER_DB.get(),
                            makeMessage(COMMAND_REMOVE_CONTEST_USER, JsonObject()
                                    .put("contestId", contestId).put("userId", userId)), it)
                }
                ok(rc)
            } catch (e: NumberFormatException) {
                badRequest(rc)
            } catch (e: Exception) {
                e.printStackTrace()
                logger.error("请求失败，地址:${rc.request().path()}, 原因：${e.message}")
                error(rc, "服务器错误，请联系管理员！")
            }
        }
    }

    /**
     * 查询参加竞赛用户列表服务
     *
     *
     * */
    private fun handleGetContestUserList(rc: RoutingContext) {
        launch(context.dispatcher()) {
            try {
                val contestId = Integer.parseInt(rc.request().getParam("id"))
                val start = rc.request().getParam("start") ?: "0"
                val size = rc.request().getParam("size") ?: "20"
                val result = awaitResult<Message<JsonObject>> {
                    vertx.eventBus().send(ADDR_CONTEST_USER_DB.get(),
                            makeMessage(COMMAND_GET_CONTEST_USER_LIST, JsonObject()
                                    .put("contestId", contestId).put("start", start).put("size", size)), it)
                }
                ok(rc, result.body())
            } catch (e: NumberFormatException) {
                badRequest(rc)
            } catch (e: Exception) {
                e.printStackTrace()
                logger.error("请求失败，地址:${rc.request().path()}, 原因：${e.message}")
                error(rc, "服务器错误，请联系管理员！")
            }
        }
    }

    /**
     * 查询竞赛试题列表
     * */
    private fun handleGetContestProblemList(rc: RoutingContext) {
        launch(context.dispatcher()) {
            try {
                val contestId = Integer.parseInt(rc.request().getParam("id"))
                // 查询竞赛信息
                val replyContest = awaitResult<Message<JsonObject>> {
                    vertx.eventBus().send(ADDR_CONTEST_DB.get(),
                            makeMessage(COMMAND_GET_CONTEST_INFO, JsonObject().put("id", contestId)), it)
                }
                val contest = replyContest.body()
                if (contest.isEmpty) {
                    badRequest(rc, "竞赛不存在")
                    return@launch
                }
                val replyProblem = awaitResult<Message<JsonObject>> {
                    vertx.eventBus().send(ADDR_CONTEST_PROBLEM_DB.get(),
                            makeMessage(COMMAND_GET_CONTEST_PROBLEM_LIST, JsonObject().put("contestId", contestId)), it)
                }
                val problems = replyProblem.body()
                ok(rc, problems)
            } catch (e: NumberFormatException) {
                badRequest(rc)
            } catch (e: Exception) {
                e.printStackTrace()
                logger.error("请求失败，地址:${rc.request().path()}, 原因：${e.message}")
                error(rc, "服务器错误，请联系管理员！")
            }
        }
    }

    /**
     * 添加竞赛试题
     * */
    private fun handleAddContestProblem(rc: RoutingContext) {
        if (!checkAdmin(rc)) {
            return
        }

        launch(context.dispatcher()) {
            try {
                val body = rc.bodyAsJson
                val contestId = Integer.parseInt(rc.request().getParam("id"))
                val problemIdArray = body.getJsonArray("problemIds")
                if (body.isEmpty || body.getJsonArray("problemIds").isEmpty) {
                    badRequest(rc)
                    return@launch
                }
                // 查询竞赛信息
                val replyContest = awaitResult<Message<JsonObject>> {
                    vertx.eventBus().send(ADDR_CONTEST_DB.get(),
                            makeMessage(COMMAND_GET_CONTEST_INFO, JsonObject().put("id", contestId)), it)
                }
                val contest = replyContest.body()
                if (contest.isEmpty) {
                    badRequest(rc, "竞赛不存在")
                    return@launch
                }
                if (contest.getInteger("status") == CONTEST_STATUS_STOP) {
                    badRequest(rc, "竞赛已经结束")
                    return@launch
                }
                // 查询试题信息
                val replyProblemList = awaitResult<Message<JsonObject>> {
                    vertx.eventBus().send(ADDR_PROBLEM_DB.get(),
                            makeMessage(COMMAND_GET_PROBLEM_LIST_NOT_CONTEST, JsonObject().put("contestId", contestId).put("ids", problemIdArray)), it)
                }
                if (replyProblemList.body() == null || replyProblemList.body().getJsonArray("data").isEmpty) {
                    badRequest(rc, "没有找到可以添加的试题")
                    return@launch
                }
                val canAddArray = replyProblemList.body().getJsonArray("data")
                val reply = awaitResult<Message<JsonObject>> {
                    vertx.eventBus().send(ADDR_CONTEST_PROBLEM_DB.get(),
                            makeMessage(COMMAND_ADD_CONTEST_PROBLEM, JsonObject().put("contestId", contestId).put("problemIds", canAddArray)), it)
                }
                ok(rc, JsonObject().put("message", "成功添加道${canAddArray.size()}题目"))
            } catch (e: ReplyException) {
                e.printStackTrace()
                logger.error("请求失败，地址:${rc.request().path()}, 原因：${e.message}")
                error(rc, "服务器错误，请联系管理员！")
            }

        }
    }

    /**
     * 移除竞赛试题
     * */
    private fun handleRemoveContestProblem(rc: RoutingContext) {
        if (!checkAdmin(rc)) {
            return
        }
        launch(context.dispatcher()) {
            try {
                val body = rc.bodyAsJson
                if (body.isEmpty || body.getJsonArray("problemIds").isEmpty) {
                    badRequest(rc)
                }
                val params = JsonObject().put("contestId", Integer.parseInt(rc.request().getParam("id"))).put("problemIds", body.getJsonArray("problemIds"))
                val reply = awaitResult<Message<JsonObject>> {
                    vertx.eventBus().send(ADDR_CONTEST_PROBLEM_DB.get(),
                            makeMessage(COMMAND_REMOVE_CONTEST_PROBLEM, params), it)
                }
                ok(rc, reply.body())
            } catch (e: ReplyException) {
                e.printStackTrace()
                logger.error("请求失败，地址:${rc.request().path()}, 原因：${e.message}")
                error(rc, "服务器错误，请联系管理员！")
            }

        }
    }

}