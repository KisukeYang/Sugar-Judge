package com.sugar.judge.test.db

import com.sugar.judge.config.EventBusNamespace
import com.sugar.judge.database.ContestProblemDbVerticle
import io.reactiverse.pgclient.PgClient
import io.reactiverse.pgclient.PgPoolOptions
import io.vertx.core.Vertx
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(VertxUnitRunner::class)
class ContestProblemDbVerticleTest{

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
        fun beforeClass(context: TestContext){
            // 初始化数据库连接池
            val client = PgClient.pool(vertx, PgPoolOptions(config))
            vertx.deployVerticle(ContestProblemDbVerticle(), context.asyncAssertSuccess())
        }

        @AfterClass
        @JvmStatic
        fun afterClass(context: TestContext){
            vertx.close(context.asyncAssertSuccess())
        }
    }

    //@Test
    fun testAddContestProblem(context: TestContext){
        val async = context.async()
        var params = JsonObject()
                .put("contestId", 1)
                .put("problemIds", JsonArray().add(1).add(2))
        vertx.eventBus().send<JsonObject>(EventBusNamespace.ADDR_CONTEST_PROBLEM_DB.get(),
                EventBusNamespace.makeMessage(EventBusNamespace.COMMAND_ADD_CONTEST_PROBLEM, params)){
            println(it.result().body())
            async.complete()
        }
    }

    //@Test
    fun testRemoveContestProblem(context: TestContext){
        val async = context.async()
        var params = JsonObject().put("contestId", 1).put("problemIds", JsonArray(listOf(1, 2)))
        vertx.eventBus().send<JsonObject>(EventBusNamespace.ADDR_CONTEST_PROBLEM_DB.get(),
                EventBusNamespace.makeMessage(EventBusNamespace.COMMAND_REMOVE_CONTEST_PROBLEM, params)){
            println(it.result().body())
            async.complete()
        }
    }

    //@Test
    fun testGetContestProblemList(context: TestContext){
        val async = context.async()
        var params = JsonObject().put("contestId", 1)
        vertx.eventBus().send<JsonObject>(EventBusNamespace.ADDR_CONTEST_PROBLEM_DB.get(),
                EventBusNamespace.makeMessage(EventBusNamespace.COMMAND_GET_CONTEST_PROBLEM_LIST, params)){
            println(it.result().body())
            context.assertNotNull(it.result().body().getJsonArray("data"))
            async.complete()
        }
    }

    @Test
    fun testIncreaseProblemSubmit(context: TestContext){
        val async = context.async()
        var params = JsonObject().put("contestId", 1).put("problemId", 2)
                .put("column", "accept").put("value", 1)
        vertx.eventBus().send<JsonObject>(EventBusNamespace.ADDR_CONTEST_PROBLEM_DB.get(),
                EventBusNamespace.makeMessage(EventBusNamespace.COMMAND_INCREASE_CONTEST_PROBLEM_PROP, params)) {
            if (it.succeeded()){
                println(it.result().body())
                context.assertTrue(it.result().body().containsKey("count"))
            }else{
                println(it.cause())
            }
            async.complete()
        }
    }

}