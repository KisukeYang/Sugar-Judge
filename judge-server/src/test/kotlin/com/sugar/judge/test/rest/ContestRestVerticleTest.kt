package com.sugar.judge.test.rest

import com.sugar.judge.config.*
import com.sugar.judge.database.ContestDbVerticle
import com.sugar.judge.database.ContestProblemDbVerticle
import com.sugar.judge.database.ContestUserDbVerticle
import com.sugar.judge.database.ProblemDbVerticle
import com.sugar.judge.rest.ContestRestVerticle
import io.reactiverse.pgclient.PgClient
import io.reactiverse.pgclient.PgPoolOptions
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
import io.vertx.kotlin.core.json.JsonArray
import io.vertx.kotlin.core.json.JsonObject
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime

@RunWith(VertxUnitRunner::class)
//@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class ContestRestVerticleTest {

    /*init {
        System.getProperties().setProperty("vertx.disableDnsResolver", "true")
    }*/

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
            //DefaultChannelId.newInstance()

            // 初始化数据库连接池
            val client = PgClient.pool(vertx, PgPoolOptions(databaseConfig))

            val router = Router.router(vertx)
            val jwtHandler = JWTAuthHandler.create(JWTAuth.create(vertx, JWTAuthOptions(jwtConfig)))
            router.route().handler(BodyHandler.create())

            vertx.deployVerticle(ContestRestVerticle(router, jwtHandler))
            vertx.deployVerticle(ContestDbVerticle(), context.asyncAssertSuccess())
            vertx.deployVerticle(ProblemDbVerticle(), context.asyncAssertSuccess())
            vertx.deployVerticle(ContestUserDbVerticle(), context.asyncAssertSuccess())
            vertx.deployVerticle(ContestProblemDbVerticle(), context.asyncAssertSuccess())
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
    fun testGetContestListReq(context: TestContext) {
        val async = context.async()
        val request = webClient.get(PORT, HOST, "/rest/contests")
        request.send { response ->
            if (response.succeeded()) {
                context.assertEquals(response.result().statusCode(), 200)
                context.assertNotNull(response.result().bodyAsJsonObject().getInteger("count"))
                println(response.result().bodyAsJsonObject())
            } else {
                println(response.cause().localizedMessage)
            }
            async.complete()
        }
    }

    //@Test
    fun testGetContestInfoReq(context: TestContext) {
        val async = context.async()
        webClient.get(PORT, HOST, "/rest/contests/13")
                .send { response ->
                    if (response.succeeded()) {
                        context.assertEquals(response.result().statusCode(), 200)
                    } else {
                        println(response.cause().localizedMessage)
                    }
                    async.complete()
                }
    }

    //@Test
    fun testCreateContestReq(context: TestContext) {
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
        val request = webClient.post(PORT, HOST, "/rest/contests")
        request.headers().add("Authorization", "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJpZCI6NCwicm9sZSI6ImFkbWluIiwibmlja25hbWUiOiLotoXnrqHnnaHnnYDkuoblkJciLCJpYXQiOjE1MzUzNjU4NzF9.bwjk3OFDCueicT-9P6-5MSiA9T5dVRwuou_46ltRTyRGKPLFf3jpi-kk7bmz_PSWmi6Xyty2WsbXQfOdt7FBhg")
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
    fun testUpdateContestReq(context: TestContext) {
        val async = context.async()
        var params = JsonObject()
                .put(KEY_CONTEST_NAME, "测试竞赛")
                .put(KEY_CONTEST_DESCRIPTION, "描述1")
                .put(KEY_CONTEST_START_TIME, LocalDateTime.now().toString())
                .put(KEY_CONTEST_TIME_LIMIT, 10)
                .put(KEY_CONTEST_COMMEND, "aaa")
                .put(KEY_CONTEST_HIDDEN, true)
                .put(KEY_CONTEST_AUTO_START, false)
                .put(KEY_CONTEST_STATUS, 2)
                .put(KEY_CONTEST_UPDATE_TIME, LocalDateTime.now().toString())
        val request = webClient.put(PORT, HOST, "/rest/contests/9")
        request.headers().add("Authorization", "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJpZCI6NCwicm9sZSI6ImFkbWluIiwibmlja25hbWUiOiLotoXnrqHnnaHnnYDkuoblkJciLCJpYXQiOjE1MzUzNjU4NzF9.bwjk3OFDCueicT-9P6-5MSiA9T5dVRwuou_46ltRTyRGKPLFf3jpi-kk7bmz_PSWmi6Xyty2WsbXQfOdt7FBhg")
        request.sendJsonObject(params) { response ->
            if (response.succeeded()) {
                context.assertEquals(response.result().statusCode(), 200)
            } else {
                println(response.cause().localizedMessage)
            }
            async.complete()
        }
    }

    //@Test
    fun testDeleteContestReq(context: TestContext) {
        val async = context.async()
        val request = webClient.delete(PORT, HOST, "/rest/contests/9")
        request.headers().add("Authorization", "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJpZCI6NCwicm9sZSI6ImFkbWluIiwibmlja25hbWUiOiLotoXnrqHnnaHnnYDkuoblkJciLCJpYXQiOjE1MzUzNjU4NzF9.bwjk3OFDCueicT-9P6-5MSiA9T5dVRwuou_46ltRTyRGKPLFf3jpi-kk7bmz_PSWmi6Xyty2WsbXQfOdt7FBhg")
        request.send { response ->
            if (response.succeeded()) {
                context.assertEquals(response.result().statusCode(), 200)
            } else {
                println(response.cause().localizedMessage)
            }
            async.complete()
        }
    }

    //@Test
    fun testJoinContestReq(context: TestContext) {
        val async = context.async()
        val request = webClient.post(PORT, HOST, "/rest/contests/9/user")
        request.headers().add("Authorization", "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJpZCI6Niwicm9sZSI6InVzZXIiLCJuaWNrbmFtZSI6Iui_memil-ezluS4jeeUnCIsImlhdCI6MTUzNTM3NDcwNH0.1CBh_0mXxGRDWWvbkiwiACJaS6A3hpWT8lw_HhIzGG7XIPdl_c2jDL1OXbPCKmoNi_0Ny1BgnPAQ8ZFpBaFwyg")
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

    //@Test
    fun testQuitContestReq(context: TestContext) {
        val async = context.async()
        val request = webClient.delete(PORT, HOST, "/rest/contests/9/user")
        request.headers().add("Authorization", "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJpZCI6Niwicm9sZSI6InVzZXIiLCJuaWNrbmFtZSI6Iui_memil-ezluS4jeeUnCIsImlhdCI6MTUzNTM3NDcwNH0.1CBh_0mXxGRDWWvbkiwiACJaS6A3hpWT8lw_HhIzGG7XIPdl_c2jDL1OXbPCKmoNi_0Ny1BgnPAQ8ZFpBaFwyg")
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

    //@Test
    fun testGetContestUserReq(context: TestContext) {
        val async = context.async()
        val request = webClient.get(PORT, HOST, "/rest/contests/9/users")
        request.headers().add("Authorization", "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJpZCI6Niwicm9sZSI6InVzZXIiLCJuaWNrbmFtZSI6Iui_memil-ezluS4jeeUnCIsImlhdCI6MTUzNTM3NDcwNH0.1CBh_0mXxGRDWWvbkiwiACJaS6A3hpWT8lw_HhIzGG7XIPdl_c2jDL1OXbPCKmoNi_0Ny1BgnPAQ8ZFpBaFwyg")
        request.addQueryParam("size", "2")
        request.addQueryParam("start", "0")
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

    @Test
    fun testGetContestProblemReq(context: TestContext) {
        val async = context.async()
        val request = webClient.get(PORT, HOST, "/rest/contests/9/problems")
        request.headers().add("Authorization", "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJpZCI6Niwicm9sZSI6InVzZXIiLCJuaWNrbmFtZSI6Iui_memil-ezluS4jeeUnCIsImlhdCI6MTUzNTM3NDcwNH0.1CBh_0mXxGRDWWvbkiwiACJaS6A3hpWT8lw_HhIzGG7XIPdl_c2jDL1OXbPCKmoNi_0Ny1BgnPAQ8ZFpBaFwyg")
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

    //@Test
    fun getAddContestProblemReq(context: TestContext) {
        val async = context.async()
        val params = JsonObject().put("problemIds", JsonArray(1, 2, 6))
        val request = webClient.post(PORT, HOST, "/rest/contests/9/problems")
        request.headers().add("Authorization", "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJpZCI6NCwicm9sZSI6ImFkbWluIiwibmlja25hbWUiOiLotoXnrqHnnaHnnYDkuoblkJciLCJpYXQiOjE1MzUzNjU4NzF9.bwjk3OFDCueicT-9P6-5MSiA9T5dVRwuou_46ltRTyRGKPLFf3jpi-kk7bmz_PSWmi6Xyty2WsbXQfOdt7FBhg")
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

    @Test
    fun getRemoveContestProblemReq(context: TestContext) {
        val async = context.async()
        val params = JsonObject().put("problemIds", JsonArray(1, 2, 6))
        val request = webClient.delete(PORT, HOST, "/rest/contests/9/problems")
        request.headers().add("Authorization", "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJpZCI6NCwicm9sZSI6ImFkbWluIiwibmlja25hbWUiOiLotoXnrqHnnaHnnYDkuoblkJciLCJpYXQiOjE1MzUzNjU4NzF9.bwjk3OFDCueicT-9P6-5MSiA9T5dVRwuou_46ltRTyRGKPLFf3jpi-kk7bmz_PSWmi6Xyty2WsbXQfOdt7FBhg")
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
}