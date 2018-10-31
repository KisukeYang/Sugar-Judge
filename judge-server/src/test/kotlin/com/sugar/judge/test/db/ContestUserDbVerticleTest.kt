package com.sugar.judge.test.db

import com.sugar.judge.config.EventBusNamespace
import com.sugar.judge.database.ContestUserDbVerticle
import io.reactiverse.pgclient.PgClient
import io.reactiverse.pgclient.PgPoolOptions
import io.vertx.core.Vertx
import io.vertx.core.eventbus.ReplyException
import io.vertx.core.json.JsonObject
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(VertxUnitRunner::class)
class ContestUserDbVerticleTest {

    companion object {

        private val config = JsonObject().put("port", 5432)
                .put("host", "127.0.0.1")
                .put("database", "sugar_judge")
                .put("user", "sugar")
                .put("password", "123456")
                .put("maxSize", 10)
        private val vertx = Vertx.vertx()

        @BeforeClass
        @JvmStatic
        fun beforeClass(context: TestContext) {
            // 初始化数据库连接池
            val client = PgClient.pool(vertx, PgPoolOptions(config))
            vertx.deployVerticle(ContestUserDbVerticle(), context.asyncAssertSuccess())
        }

        @AfterClass
        @JvmStatic
        fun afterClass(context: TestContext) {
            vertx.close(context.asyncAssertSuccess())
        }
    }


    //@Test
    fun testAddContestUser(context: TestContext) {
        val async = context.async()
        val params = JsonObject().put("contestId", 1).put("userId", 1)
        try {
            vertx.eventBus().send<JsonObject>(EventBusNamespace.ADDR_CONTEST_USER_DB.get(),
                    EventBusNamespace.makeMessage(EventBusNamespace.COMMAND_ADD_CONTEST_USER, params)) {
                println(it.result().body())
                context.assertTrue(it.result().body().containsKey("count"))
                async.complete()
            }
        }catch (e: ReplyException){
            println(e.localizedMessage)
        }
    }

    //@Test
    fun testRemoveContestUser(context: TestContext) {
        val async = context.async()
        var params = JsonObject().put("contestId", 1).put("userId", 1)
        vertx.eventBus().send<JsonObject>(EventBusNamespace.ADDR_CONTEST_USER_DB.get(),
                EventBusNamespace.makeMessage(EventBusNamespace.COMMAND_REMOVE_CONTEST_USER, params)) {
            if (it.succeeded()){
                println(it.result().body())
                context.assertTrue(it.result().body().containsKey("count"))
            }else{
                println(it.cause())
            }
            async.complete()
        }
    }

    //@Test
    fun testGetContestUserList(context: TestContext) {
        val async = context.async()
        var params = JsonObject().put("contestId", 1).put("start", 0L).put("size", 20L)
        vertx.eventBus().send<JsonObject>(EventBusNamespace.ADDR_CONTEST_USER_DB.get(),
                EventBusNamespace.makeMessage(EventBusNamespace.COMMAND_GET_CONTEST_USER_LIST, params)) {
            println(it.result().body())
            //context.assertTrue(it.result().body().containsKey("count"))
            async.complete()
        }
    }

    @Test
    fun testIncreaseUserSubmit(context: TestContext) {
        val async = context.async()
        var params = JsonObject().put("contestId", 1).put("userId", 4)
                .put("column", "submit").put("value", 1)
        vertx.eventBus().send<JsonObject>(EventBusNamespace.ADDR_CONTEST_USER_DB.get(),
                EventBusNamespace.makeMessage(EventBusNamespace.COMMAND_INCREASE_CONTEST_USER_PROP, params)) {
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