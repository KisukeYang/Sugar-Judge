package com.sugar.judge.test.rest

import com.sugar.judge.config.*
import com.sugar.judge.database.*
import com.sugar.judge.rest.ProblemRestVerticle
import com.sugar.judge.rest.SubmissionRestVerticle
import io.netty.channel.DefaultChannelId
import io.reactiverse.pgclient.PgClient
import io.reactiverse.pgclient.PgPoolOptions
import io.vertx.core.DeploymentOptions
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.core.dns.AddressResolverOptions
import io.vertx.ext.auth.jwt.JWTAuth
import io.vertx.ext.auth.jwt.JWTAuthOptions
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import io.vertx.ext.web.Router
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.JWTAuthHandler
import io.vertx.kotlin.core.DeploymentOptions
import io.vertx.kotlin.core.json.JsonObject
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(VertxUnitRunner::class)
class SubmissionRestVerticleTest {

    companion object {
        private val databaseConfig = io.vertx.core.json.JsonObject().put("port", 5432)
                .put("host", "127.0.0.1")
                .put("database", "sugar_judge")
                .put("user", "sugar")
                .put("password", "123456")
                .put("maxSize", 100)

        private val jwtConfig = JsonObject().put("keyStore", JsonObject().put("path", "keystore.jceks")
                .put("type", "jceks").put("password", "secret"))

        private val vertx = Vertx.vertx(VertxOptions().setAddressResolverOptions(
                AddressResolverOptions().addServer("8.8.8.8")))

        private const val HOST = "127.0.0.1"

        private const val PORT = 8081

        @BeforeClass
        @JvmStatic
        fun beforeClass(context: TestContext) {
            DefaultChannelId.newInstance()
            // 初始化数据库连接池
            val client = PgClient.pool(vertx, PgPoolOptions(databaseConfig))

            val router = Router.router(vertx)
            val jwtHandler = JWTAuthHandler.create(JWTAuth.create(vertx, JWTAuthOptions(jwtConfig)))
            router.route().handler(BodyHandler.create())

            vertx.deployVerticle(SubmissionRestVerticle(router, jwtHandler), context.asyncAssertSuccess())
            vertx.deployVerticle(ProblemDbVerticle(), context.asyncAssertSuccess())
            vertx.deployVerticle(SubmissionDbVerticle(), context.asyncAssertSuccess())

            vertx.createHttpServer().requestHandler(router::accept).listen(PORT, HOST)
        }

        @AfterClass
        @JvmStatic
        fun afterClass(context: TestContext) {
            vertx.close(context.asyncAssertSuccess())
        }
    }

    private val webClient = WebClient.create(vertx)

    //@Test
    fun testGetSubmissionList(context: TestContext) {
        val async = context.async()
        val request = webClient.get(PORT, HOST, "/rest/submission")
        request.addQueryParam("start", "0").addQueryParam("size", "20")
        request.send { response ->
            if (response.succeeded()) {
                println(response.result().body())
            } else {
                println(response.cause().localizedMessage)
            }
            async.complete()
        }
    }

    //@Test
    fun testSubmitCodeSeq(context: TestContext) {
        val async = context.async()
        var params = JsonObject()
                .put(KEY_SUBMISSION_PROBLEM_ID, 1)
                .put(KEY_SUBMISSION_CONTEST_ID, 1)
                .put(KEY_SUBMISSION_LANGUAGE, "JV")
                .put(KEY_SUBMISSION_PROBLEM_CODE, "import ...")
        var request = webClient.post(PORT, HOST, "/rest/submission")
        request.headers().add("Authorization", "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJpZCI6Niwicm9sZSI6InVzZXIiLCJuaWNrbmFtZSI6Iui_memil-ezluS4jeeUnCIsImlhdCI6MTUzNTM5MjY3M30.lrrEf6el-q_KOaau_QTjEk9n9EYxh3ZtiBLd0mVUBCbeEYXFfDUP0BdKxOkNjqvbjUIT4_KGoQzqKKOr6kukzw")
        request.sendJsonObject(params) { response ->
            if (response.succeeded()) {
                println(response.result().bodyAsJsonObject())
                context.assertEquals(response.result().statusCode(), 200)
            } else {
                println(response.cause().localizedMessage)
            }
            async.complete()
        }
    }

    //@Test
    fun testReJudgeReq(context: TestContext) {
        val async = context.async()
        var request = webClient.post(PORT, HOST, "/rest/submission/4/rejudge")
        request.headers().add("Authorization", "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJpZCI6Niwicm9sZSI6InVzZXIiLCJuaWNrbmFtZSI6Iui_memil-ezluS4jeeUnCIsImlhdCI6MTUzNTM5MjY3M30.lrrEf6el-q_KOaau_QTjEk9n9EYxh3ZtiBLd0mVUBCbeEYXFfDUP0BdKxOkNjqvbjUIT4_KGoQzqKKOr6kukzw")
        request.send { response ->
            if (response.succeeded()) {
                context.assertEquals(response.result().statusCode(), 200)
            } else {
                println(response.cause().localizedMessage)
            }
            async.complete()
        }
    }

    @Test
    fun testGetCodeSeq(context: TestContext) {
        val async = context.async()
        var request = webClient.get(PORT, HOST, "/rest/submission/4/code")
        request.headers().add("Authorization", "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJpZCI6Niwicm9sZSI6InVzZXIiLCJuaWNrbmFtZSI6Iui_memil-ezluS4jeeUnCIsImlhdCI6MTUzNTM5MjY3M30.lrrEf6el-q_KOaau_QTjEk9n9EYxh3ZtiBLd0mVUBCbeEYXFfDUP0BdKxOkNjqvbjUIT4_KGoQzqKKOr6kukzw")
        request.send { response ->
            if (response.succeeded()) {
                println(response.result().bodyAsJsonObject())
                context.assertEquals(response.result().statusCode(), 200)
            } else {
                println(response.cause().localizedMessage)
            }
            async.complete()
        }
    }
}