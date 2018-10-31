package com.sugar.judge.test.rest

import com.sugar.judge.database.AccountDbVerticle
import com.sugar.judge.rest.AccountRestVerticle
import io.netty.channel.DefaultChannelId
import io.reactiverse.pgclient.PgClient
import io.reactiverse.pgclient.PgPoolOptions
import io.vertx.core.Vertx
import io.vertx.ext.auth.jwt.JWTAuth
import io.vertx.ext.auth.jwt.JWTAuthOptions
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import io.vertx.ext.web.Router
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.JWTAuthHandler
import io.vertx.kotlin.core.json.JsonObject
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

@RunWith(VertxUnitRunner::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class AccountRestVerticleTest {

    companion object {
        private val databaseConfig = io.vertx.core.json.JsonObject().put("port", 5432)
                .put("host", "127.0.0.1")
                .put("database", "sugar_judge")
                .put("user", "sugar")
                .put("password", "123456")
                .put("maxSize", 100)

        private val jwtConfig = JsonObject().put("keyStore", JsonObject().put("path", "keystore.jceks")
                .put("type", "jceks").put("password", "secret"))

        private val vertx = Vertx.vertx()

        private const val HOST = "localhost"

        private const val PORT = 8081

        @BeforeClass
        @JvmStatic
        fun beforeClass(context: TestContext) {
            DefaultChannelId.newInstance()
            // 禁用默认 DNS
            //System.getProperties().setProperty("vertx.disableDnsResolver", "true")

            // 初始化数据库连接池
            val client = PgClient.pool(vertx, PgPoolOptions(databaseConfig))

            val router = Router.router(vertx)
            router.route().handler(BodyHandler.create())
            val jwtProvider = JWTAuth.create(vertx, JWTAuthOptions(jwtConfig))
            val jwtHandler = JWTAuthHandler.create(jwtProvider)
            vertx.deployVerticle(AccountRestVerticle(router, jwtProvider, jwtHandler))
            vertx.deployVerticle(AccountDbVerticle(), context.asyncAssertSuccess())


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
    fun testLoginReq(context: TestContext) {
        val async = context.async()
        val request = webClient.post(PORT, HOST, "/rest/login")
        val header = request.headers()
        header.add("username", "sugar").add("password", "123456")
        request.send { response ->
            if (response.succeeded()) {
                println(response.result().body())
                context.assertNotNull(response.result().bodyAsJsonObject().getString("token"))
            } else {
                println(response.cause().localizedMessage)
            }
            async.complete()
        }
    }

    //@Test
    fun testRegisterReq(context: TestContext) {
        val async = context.async()
        val request = webClient.post(PORT, HOST, "/rest/reg")
        /*val user = User()
        user.username = "test1"
        user.password = "123456"
        user.language = "c++"
        user.nickname = "超管睡着了吗"
        user.email = "sugar@mail.com"
        user.enabled = true
        user.role = "admin"*/
        val params = JsonObject()
                .put("username", "sugar")
                .put("password", "123456")
                .put("language", "c++")
                .put("nickname", "这颗糖不甜")
                .put("email", "sugar@mail.com")
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

    fun testGetRankList(context: TestContext) {
        val async = context.async()
        webClient.get(PORT, HOST, "/rest/v1/rank")
                .send { response ->
                    if (response.succeeded()) {
                        context.assertEquals(response.result().statusCode(), 200)
                    } else {
                        println(response.cause().localizedMessage)
                    }
                    async.complete()
                }
    }
}