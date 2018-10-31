package com.sugar.judge.test.db

import com.sugar.judge.config.EventBusNamespace
import com.sugar.judge.config.*
import com.sugar.judge.database.SubmissionDbVerticle
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
//@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class SubmissionDbVerticleTest{

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
        fun beforeClass(context: TestContext){
            // 初始化数据库连接池
            val client = PgClient.pool(vertx, PgPoolOptions(config))
            vertx.deployVerticle(SubmissionDbVerticle(), context.asyncAssertSuccess())
        }

        @AfterClass
        @JvmStatic
        fun afterClass(context: TestContext){
            vertx.close(context.asyncAssertSuccess())
        }
    }

    @Test
    fun testGetSubmissionList(context: TestContext){
        val async = context.async()
        var params = JsonObject().put("start", 0L).put("size", 20L)
        vertx.eventBus().send<JsonObject>(EventBusNamespace.ADDR_SUBMISSION_DB.get(),
                EventBusNamespace.makeMessage(EventBusNamespace.COMMAND_GET_SUBMISSION_LIST, params)){
            if(it.succeeded()){
                println(it.result().body())
                context.assertTrue(it.result().body().containsKey("count"))
                async.complete()
            }else{
                println(it.cause())
                context.exceptionHandler()
            }
        }
    }

    //@Test
    fun testCreateSubmission(context: TestContext){
        val async = context.async()
        val params = JsonObject()
                .put(KEY_SUBMISSION_USER_ID, 4)
                .put(KEY_SUBMISSION_PROBLEM_ID, 1)
                .put(KEY_SUBMISSION_CONTEST_ID, 1)
                .put(KEY_SUBMISSION_NICKNAME, "hello")
                .put(KEY_SUBMISSION_PROBLEM_TITLE, "hello world")
                .put(KEY_SUBMISSION_LANGUAGE, "JAVA")
                .put(KEY_SUBMISSION_EXECUTE_TIME, 0)
                .put(KEY_SUBMISSION_CONTEST_TIME, 0)
                .put(KEY_SUBMISSION_PROBLEM_CODE, "import ...")
                .put(KEY_SUBMISSION_STATUS, 0)
                .put(KEY_SUBMISSION_LOCK, true)
                .put(KEY_SUBMISSION_IP_ADDRESS, "127.0.0.1")
                .put(KEY_SUBMISSION_SUBMIT_TIME, LocalDateTime.now().toString())
                .put(KEY_SUBMISSION_ERROR_MESSAGE, "abc")

        vertx.eventBus().send<JsonObject>(EventBusNamespace.ADDR_SUBMISSION_DB.get(),
                EventBusNamespace.makeMessage(EventBusNamespace.COMMAND_CREATE_SUBMISSION, params)) {
            if(it.succeeded()){
                println(it.result().body())
                //context.assertTrue(it.result().body().containsKey("count"))
                async.complete()
            }else{
                println(it.cause())
                context.exceptionHandler()
            }
        }
    }

    @Test
    fun updateSubmission(context: TestContext){
        val async = context.async()
        val params = JsonObject()
                .put(KEY_SUBMISSION_EXECUTE_TIME, 10)
                .put(KEY_SUBMISSION_CONTEST_TIME, 0)
                .put(KEY_SUBMISSION_STATUS, 0)
                .put(KEY_SUBMISSION_LOCK, false)
                .put(KEY_SUBMISSION_ERROR_MESSAGE, "bbb")
                .put(KEY_SUBMISSION_ID, 1)
        vertx.eventBus().send<JsonObject>(EventBusNamespace.ADDR_SUBMISSION_DB.get(),
                EventBusNamespace.makeMessage(EventBusNamespace.COMMAND_UPDATE_SUBMISSION, params)) {
            if(it.succeeded()){
                println(it.result().body())
                //context.assertTrue(it.result().body().containsKey("count"))
                async.complete()
            }else{
                println(it.cause())
                context.exceptionHandler()
            }
        }
    }

}