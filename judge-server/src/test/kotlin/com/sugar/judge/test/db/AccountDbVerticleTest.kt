package com.sugar.judge.test.db

import com.sugar.judge.config.EventBusNamespace
import com.sugar.judge.database.AccountDbVerticle
import com.sugar.judge.utils.PasswordTools
import io.reactiverse.pgclient.PgClient
import io.reactiverse.pgclient.PgPoolOptions
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.time.LocalDateTime

@RunWith(VertxUnitRunner::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class AccountDbVerticleTest {
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
            vertx.deployVerticle(AccountDbVerticle(), context.asyncAssertSuccess())
        }

        @AfterClass
        @JvmStatic
        fun afterClass(context: TestContext) {
            vertx.close(context.asyncAssertSuccess())
        }
    }

    //@Test
    fun testCreateUser(context: TestContext) {
        var async = context.async()
        val params = JsonObject()
        params.put("registerTime",LocalDateTime.now().toString())
                .put("username", "admin")
                .put("nickname", "超管睡着了吗")
                .put("password", PasswordTools.encrypt("123456"))
                .put("email", "trainee_c@126.com")
                .put("ipInfo", "127.0.0.7")
                .put("enabled", true)
                .put("role", "admin")
        vertx.eventBus().send<JsonObject>(EventBusNamespace.ADDR_ACCOUNT_DB.get(),
                EventBusNamespace.makeMessage(EventBusNamespace.COMMAND_CREATE_ACCOUNT, params)) {
            if (it.succeeded()) {
                println(it.result().body())
                context.assertTrue(it.result().body().containsKey("count"))
            } else {
                println(it.cause().localizedMessage)
            }
            async.complete()
        }

    }

    //@Test
    fun testGetUserByUsername(context: TestContext) {
        var async = context.async()
        val params = JsonObject().put("username", "admin")
        vertx.eventBus().send<JsonObject>(EventBusNamespace.ADDR_ACCOUNT_DB.get(),
                EventBusNamespace.makeMessage(EventBusNamespace.COMMAND_GET_ACCOUNT, params)) {
            if (it.succeeded()){
                println(it.result().body())
                context.assertEquals(PasswordTools.decrypt(it.result().body().getString("password")), "123456")
                //context.assertEquals(it.result().body().getString("nickname"), "Sugar")
                async.complete()
            }else{
                println(it.cause().localizedMessage)
            }

        }
    }

    //@Test
    fun testGetUserList(context: TestContext) {
        var async = context.async()
        val params = JsonObject().put("start", 0L).put("size", 20L)
        vertx.eventBus().send<JsonObject>(EventBusNamespace.ADDR_ACCOUNT_DB.get(),
                EventBusNamespace.makeMessage(EventBusNamespace.COMMAND_GET_ACCOUNT_LIST, params)) {
            if (it.succeeded()){
                println(it.result().body())
            }else{
                println(it.cause().localizedMessage)
            }
            async.complete()
        }
    }

    @Test
    fun testUpdateUserInfo(context: TestContext) {
        var async = context.async()
        val params = JsonObject().put("id", 4).put("nickname", "超管睡着了吗").put("enabled", true).put("role", "admin").put("language", "C++")
        vertx.eventBus().send<JsonObject>(EventBusNamespace.ADDR_ACCOUNT_DB.get(),
                EventBusNamespace.makeMessage(EventBusNamespace.COMMAND_UPDATE_ACCOUNT, params)) {
            println(it.result().body())
            context.assertEquals(it.result().body().getInteger("count"), 1)
            async.complete()
        }
    }
}