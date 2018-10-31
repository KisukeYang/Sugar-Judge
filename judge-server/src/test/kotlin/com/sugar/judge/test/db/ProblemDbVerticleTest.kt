package com.sugar.judge.test.db

import com.sugar.judge.config.*
import com.sugar.judge.config.EventBusNamespace.*
import com.sugar.judge.database.ProblemDbVerticle
import io.reactiverse.pgclient.PgClient
import io.reactiverse.pgclient.PgPoolOptions
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime
import javax.swing.text.html.FormView.SUBMIT

@RunWith(VertxUnitRunner::class)
class ProblemDbVerticleTest {

    companion object {

        private val config = JsonObject().put("port", 5432)
                .put("host", "127.0.0.1")
                .put("database", "sugar_judge")
                .put("user", "sugar")
                .put("password", "123456")
                .put("maxSize", 100)
        private val vertx = Vertx.vertx()

        @BeforeClass
        @JvmStatic
        fun beforeClass(context: TestContext) {
            // 初始化数据库连接池
            val client = PgClient.pool(vertx, PgPoolOptions(config))
            vertx.deployVerticle(ProblemDbVerticle(), context.asyncAssertSuccess())
        }

        @AfterClass
        @JvmStatic
        fun afterClass(context: TestContext) {
            vertx.close(context.asyncAssertSuccess())
        }
    }

    @Test
    fun testGetProblemList(context: TestContext) {
        val async = context.async()
        var params = JsonObject().put("start", 0L).put("size", 20L)
        vertx.eventBus().send<JsonObject>(ADDR_PROBLEM_DB.get(),
                EventBusNamespace.makeMessage(COMMAND_GET_PROBLEM_LIST, params)) {
            println(it.result().body())
            //context.assertTrue(it.result().body().containsKey("count"))
            async.complete()
        }
    }

    //@Test
    fun testCreateProblem(context: TestContext) {
        val async = context.async()
        var params = JsonObject()
                .put(KEY_PROBLEM_ID, 2)
                .put(KEY_PROBLEM_TITLE, "测试创建试题")
                .put(KEY_PROBLEM_TIME_LIMIT, 1000)
                .put(KEY_PROBLEM_MEMORY_LIMIT, 32767)
                .put(KEY_PROBLEM_BACKGROUND, "测试背景知识")
                .put(KEY_PROBLEM_CONTENT, "正文")
                .put(KEY_PROBLEM_THE_INPUT, "1111")
                .put(KEY_PROBLEM_THE_OUTPUT, "2222")
                .put(KEY_PROBLEM_SAMPLE_INPUT, "1111")
                .put(KEY_PROBLEM_SAMPLE_OUTPUT, "2222")
                .put(KEY_PROBLEM_HINT, "1111")
                .put(KEY_PROBLEM_CODE, "code")
                .put(KEY_PROBLEM_IN_FILE_PATH, "/a.txt")
                .put(KEY_PROBLEM_OUT_FILE_PATH, "/b.txt")
                .put(KEY_PROBLEM_ALTER_OUT_DATA, "alter")
                .put(KEY_PROBLEM_CONDITIONAL_JUDGE, "aaa").put(KEY_PROBLEM_DIFFICULTY, 1)
                .put(KEY_PROBLEM_AUTHOR, "admin").put(KEY_PROBLEM_CREATE_TIME, LocalDateTime.now().toString())
                .put(KEY_PROBLEM_UPDATE_TIME, LocalDateTime.now().toString()).put(KEY_PROBLEM_HIDDEN, true).put(KEY_PROBLEM_TYPE, 1)

        vertx.eventBus().send<JsonObject>(ADDR_PROBLEM_DB.get(),
                EventBusNamespace.makeMessage(COMMAND_CREATE_PROBLEM, params)) {
            println(it.result())
            //context.assertEquals(it.result().body().getInteger("count"), 1)
            async.complete()
        }
    }

    //@Test
    fun testUpdateProblem(context: TestContext) {
        val async = context.async()
        var params = JsonObject()
                .put(KEY_PROBLEM_ID, 2)
                .put(KEY_PROBLEM_TITLE, "测试创建试题1")
                .put(KEY_PROBLEM_TIME_LIMIT, 1000)
                .put(KEY_PROBLEM_MEMORY_LIMIT, 32767)
                .put(KEY_PROBLEM_BACKGROUND, "测试背景知识1")
                .put(KEY_PROBLEM_CONTENT, "正文1111")
                .put(KEY_PROBLEM_THE_INPUT, "fffff")
                .put(KEY_PROBLEM_THE_OUTPUT, "2222")
                .put(KEY_PROBLEM_SAMPLE_INPUT, "1111")
                .put(KEY_PROBLEM_SAMPLE_OUTPUT, "2222")
                .put(KEY_PROBLEM_HINT, "1111")
                .put(KEY_PROBLEM_CODE, "code")
                .put(KEY_PROBLEM_IN_FILE_PATH, "/a.txt")
                .put(KEY_PROBLEM_OUT_FILE_PATH, "/bb.txt")
                .put(KEY_PROBLEM_ALTER_OUT_DATA, "alter")
                .put(KEY_PROBLEM_CONDITIONAL_JUDGE, "aaa").put(KEY_PROBLEM_DIFFICULTY, 1)
                .put(KEY_PROBLEM_UPDATE_TIME, LocalDateTime.now().toString()).put(KEY_PROBLEM_HIDDEN, false).put(KEY_PROBLEM_TYPE, 1)
        vertx.eventBus().send<JsonObject>(ADDR_PROBLEM_DB.get(),
                EventBusNamespace.makeMessage(COMMAND_UPDATE_PROBLEM, params)) {
            println(it.result().body())
            context.assertEquals(it.result().body().getInteger("count"), 1)
            async.complete()
        }
    }

    //@Test
    fun testGetProblemById(context: TestContext) {
        val async = context.async()
        var params = JsonObject().put(KEY_PROBLEM_ID, 2)
        vertx.eventBus().send<JsonObject>(ADDR_PROBLEM_DB.get(),
                EventBusNamespace.makeMessage(COMMAND_GET_ONE_PROBLEM, params)) {
            println(it.result().body())
            context.assertNotNull(it.result().body())
            async.complete()
        }
    }

    //@Test
    fun testRemoveProblem(context: TestContext) {
        val async = context.async()
        var params = JsonObject().put(KEY_PROBLEM_ID, 2)
        vertx.eventBus().send<JsonObject>(ADDR_PROBLEM_DB.get(),
                EventBusNamespace.makeMessage(COMMAND_DELETE_PROBLEM, params)) {
            println(it.result().body())
            context.assertEquals(it.result().body().getInteger("count"), 1)
            async.complete()

            val async1 = context.async()
            vertx.eventBus().send<JsonObject>(ADDR_PROBLEM_DB.get(),
                    EventBusNamespace.makeMessage(COMMAND_GET_ONE_PROBLEM, params)) {
                println(it.result().body())
                context.assertEquals(it.result().body().getBoolean("delete"), true)
                async1.complete()
            }
        }

    }

    @Test
    fun testIncreaseProblemProp(context: TestContext) {
        val async = context.async()
        var params = JsonObject().put(KEY_PROBLEM_ID, 2).put("column", KEY_PROBLEM_SUBMIT).put("value", 1)
        vertx.eventBus().send<JsonObject>(ADDR_PROBLEM_DB.get(),
                EventBusNamespace.makeMessage(COMMAND_INCREASE_PROBLEM_PROP, params)) {
            println(it.result().body())
            context.assertEquals(it.result().body().getInteger("count"), 1)
            async.complete()

            val async1 = context.async()
            vertx.eventBus().send<JsonObject>(ADDR_PROBLEM_DB.get(),
                    EventBusNamespace.makeMessage(COMMAND_GET_ONE_PROBLEM, params)) {
                println(it.result().body())
                context.assertNotNull(it.result().body().getInteger(SUBMIT))
                async1.complete()
            }
        }

    }
}