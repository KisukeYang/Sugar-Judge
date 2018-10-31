package com.sugar.judge.test.db

import com.sugar.judge.config.*
import com.sugar.judge.database.ContestDbVerticle
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

@RunWith(VertxUnitRunner::class)
class ContestDbVerticleTest{

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
            vertx.deployVerticle(ContestDbVerticle(), context.asyncAssertSuccess())
        }

        @AfterClass
        @JvmStatic
        fun afterClass(context: TestContext){
            vertx.close(context.asyncAssertSuccess())
        }
    }

    //@Test
    fun testGetContestList(context: TestContext){
        val async = context.async()
        var params = JsonObject().put("start", 0L).put("size", 20L)
        vertx.eventBus().send<JsonObject>(EventBusNamespace.ADDR_CONTEST_DB.get(),
                EventBusNamespace.makeMessage(EventBusNamespace.COMMAND_GET_CONTEST_LIST, params)){
            println(it.result().body())
            context.assertTrue(it.result().body().containsKey("count"))
            async.complete()
        }
    }

    //@Test
    fun testCreateContest(context: TestContext){
        val async = context.async()
        var params = JsonObject()
                .put(KEY_CONTEST_NAME, "测试竞赛")
                .put(KEY_CONTEST_DESCRIPTION, "描述")
                .put(KEY_CONTEST_CREATOR, "admin")
                .put(KEY_CONTEST_START_TIME, LocalDateTime.now().toString())
                .put(KEY_CONTEST_TIME_LIMIT, 10)
                .put(KEY_CONTEST_COMMEND, "aaa")
                .put(KEY_CONTEST_HIDDEN, false)
                .put(KEY_CONTEST_DISUSE, false)
                .put(KEY_CONTEST_AUTO_START, true)
                .put(KEY_CONTEST_STATUS, 0)
                .put(KEY_CONTEST_CREATE_TIME, LocalDateTime.now().toString())
                .put(KEY_CONTEST_UPDATE_TIME, LocalDateTime.now().toString())
        vertx.eventBus().send<JsonObject>(EventBusNamespace.ADDR_CONTEST_DB.get(),
                EventBusNamespace.makeMessage(EventBusNamespace.COMMAND_CREATE_CONTEST, params)){
            println(it.result().body())
            //context.assertTrue(it.result().body().containsKey("problemIds"))
            async.complete()
        }
    }

    @Test
    fun testUpdateContest(context: TestContext){
        val async = context.async()
        var params = JsonObject()
                .put(KEY_CONTEST_NAME, "测试竞赛")
                .put(KEY_CONTEST_DESCRIPTION, "描述1")
                .put(KEY_CONTEST_START_TIME, LocalDateTime.now().toString())
                .put(KEY_CONTEST_TIME_LIMIT, 10)
                .put(KEY_CONTEST_COMMEND, "bbb")
                .put(KEY_CONTEST_HIDDEN, false)
                .put(KEY_CONTEST_DISUSE, false)
                .put(KEY_CONTEST_AUTO_START, true)
                .put(KEY_CONTEST_STATUS, 1)
                .put(KEY_CONTEST_UPDATE_TIME, LocalDateTime.now().toString())
                .put(KEY_CONTEST_ID, 1)
        vertx.eventBus().send<JsonObject>(EventBusNamespace.ADDR_CONTEST_DB.get(),
                EventBusNamespace.makeMessage(EventBusNamespace.COMMAND_UPDATE_CONTEST, params)){
            println(it.result().body())
            async.complete()
        }
        /*val async1 = context.async()
        vertx.eventBus().send<JsonObject>(EventBusNamespace.ADDR_CONTEST_DB.get(),
                EventBusNamespace.makeMessage(EventBusNamespace.COMMAND_GET_CONTEST_INFO, params)){
            println(it.result().body())
            context.assertEquals(it.result().body().getString("description"), "测试竞赛更新")
            context.assertEquals(it.result().body().getString("contestStatus"), CONTEST_STATUS_DISUSE)
            async1.complete()
        }*/
    }

    @Test
    fun testRemoveContest(context: TestContext){
        val async = context.async()
        var params = JsonObject().put(KEY_CONTEST_ID, 1)
        vertx.eventBus().send<JsonObject>(EventBusNamespace.ADDR_CONTEST_DB.get(),
                EventBusNamespace.makeMessage(EventBusNamespace.COMMAND_DELETE_CONTEST, params)){
            println(it.result().body())
            async.complete()
        }
        /*val async1 = context.async()
        vertx.eventBus().send<JsonObject>(EventBusNamespace.ADDR_CONTEST_DB.get(),
                EventBusNamespace.makeMessage(EventBusNamespace.COMMAND_GET_CONTEST_INFO, params)){
            println(it.result().body())
            context.assertEquals(it.result().body().getBoolean("disuse"), false)
            async1.complete()
        }*/
    }

    //@Test
    fun testGetContestInfo(context: TestContext){
        val async = context.async()
        var params = JsonObject().put("id", 1)
        vertx.eventBus().send<JsonObject>(EventBusNamespace.ADDR_CONTEST_DB.get(),
                EventBusNamespace.makeMessage(EventBusNamespace.COMMAND_GET_CONTEST_INFO, params)){
            println(it.result().body())
            context.assertTrue(it.result().body().containsKey("contestName"))
            async.complete()
        }
    }
}