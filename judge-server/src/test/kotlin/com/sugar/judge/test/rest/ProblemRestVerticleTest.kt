package com.sugar.judge.test.rest

import com.sugar.judge.database.ProblemDbVerticle
import com.sugar.judge.rest.ProblemRestVerticle
import io.netty.channel.DefaultChannelId
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
import io.vertx.kotlin.core.DeploymentOptions
import io.vertx.kotlin.core.json.JsonObject
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(VertxUnitRunner::class)
class ProblemRestVerticleTest {

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
            val option = DeploymentOptions(config = JsonObject()
                    .put("inData", "/Users/NicolasYezi/sugar-judge/test_data/in")
                    .put("outData", "/Users/NicolasYezi/sugar-judge/test_data/out"))
            // 初始化数据库连接池
            val client = PgClient.pool(vertx, PgPoolOptions(databaseConfig))

            val router = Router.router(vertx)
            val jwtHandler = JWTAuthHandler.create(JWTAuth.create(vertx, JWTAuthOptions(jwtConfig)))
            router.route().handler(BodyHandler.create())

            vertx.deployVerticle(ProblemRestVerticle(router, jwtHandler), option)
            vertx.deployVerticle(ProblemDbVerticle(), context.asyncAssertSuccess())

            vertx.createHttpServer().requestHandler(router::accept).listen(PORT, HOST)
        }

        @AfterClass
        @JvmStatic
        fun afterClass(context: TestContext) {
            vertx.close(context.asyncAssertSuccess())
        }
    }

    private val webClient = WebClient.create(vertx)

    @Test
    fun testProblemListReq(context: TestContext) {
        val async = context.async()
        val request = webClient.get(PORT, HOST, "/rest/problems")
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
    fun testProblemInfoReq(context: TestContext) {
        val async = context.async()
        val request = webClient.get(PORT, HOST, "/rest/problems/1")
        //request.addQueryParam("start", "0").addQueryParam("limit", "20")
        request.send { response ->
            if (response.succeeded()) {
                println(response.result().body())
                context.assertEquals(response.result().statusCode(), 200)
            } else {
                println(response.cause().localizedMessage)
            }
            async.complete()
        }
    }

    //@Test
    fun createProblemReq(context: TestContext) {
        val async = context.async()
        val params = JsonObject()
                .put("timeLimit", 1000)
                .put("memoryLimit", 32767)
                .put("title", "test")
                .put("content", "test")
                .put("sampleInput", "test")
                .put("sampleOutput", "test")
                .put("theInput", "test")
                .put("theOutput", "test")
                .put("hidden", false)
        val request = webClient.post(PORT, HOST, "/rest/problems")
        request.headers().add("Authorization", "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJpZCI6NCwicm9sZSI6ImFkbWluIiwibmlja25hbWUiOiLotoXnrqHnnaHnnYDkuoblkJciLCJpYXQiOjE1MzUzNjU4NzF9.bwjk3OFDCueicT-9P6-5MSiA9T5dVRwuou_46ltRTyRGKPLFf3jpi-kk7bmz_PSWmi6Xyty2WsbXQfOdt7FBhg")
        request.sendJsonObject(params) { response ->
            if (response.succeeded()) {
                println(response.result().body())
                context.assertEquals(response.result().statusCode(), 200)

            } else {
                println(response.cause().localizedMessage)
            }
            async.complete()
        }
    }

    //@Test
    fun updateProblemReq(context: TestContext) {
        val async = context.async()
        val params = JsonObject()
                .put("timeLimit", 1000)
                .put("memoryLimit", 32767)
                .put("title", "Hello World")
                .put("content", "test")
                .put("sampleInput", "test")
                .put("sampleOutput", "test")
                .put("theInput", "test")
                .put("theOutput", "test")
                .put("hidden", true)
        val request = webClient.put(PORT, HOST, "/rest/problems/7")
        request.headers().add("Authorization", "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJpZCI6NCwicm9sZSI6ImFkbWluIiwibmlja25hbWUiOiLotoXnrqHnnaHnnYDkuoblkJciLCJpYXQiOjE1MzUzNjU4NzF9.bwjk3OFDCueicT-9P6-5MSiA9T5dVRwuou_46ltRTyRGKPLFf3jpi-kk7bmz_PSWmi6Xyty2WsbXQfOdt7FBhg")
        request.sendJsonObject(params) { response ->
            if (response.succeeded()) {

                context.assertEquals(response.result().statusCode(), 200)

                println(response.result())
            } else {
                println(response.cause().localizedMessage)
            }
            async.complete()
        }

    }

    //@Test
    fun deleteProblemReq(context: TestContext) {
        val async = context.async()
        val request = webClient.delete(PORT, HOST, "/rest/problems/7")
        request.headers().add("Authorization", "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJpZCI6NCwicm9sZSI6ImFkbWluIiwibmlja25hbWUiOiLotoXnrqHnnaHnnYDkuoblkJciLCJpYXQiOjE1MzUzNjU4NzF9.bwjk3OFDCueicT-9P6-5MSiA9T5dVRwuou_46ltRTyRGKPLFf3jpi-kk7bmz_PSWmi6Xyty2WsbXQfOdt7FBhg")
        request.send { response ->
            if (response.succeeded()) {
                context.assertEquals(response.result().statusCode(), 200)
                /*val async1 = context.async()
                webClient.get(PORT, HOST, "/rest/problems/10").send { response ->
                    if (response.succeeded()) {
                        context.assertEquals(response.result().statusCode(), 404)
                    } else {
                        println(response.cause().localizedMessage)
                    }
                    async1.complete()
                }*/
            } else {
                println(response.cause().localizedMessage)
            }
            async.complete()
        }

    }

}