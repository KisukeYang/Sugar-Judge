package com.sugar.judge.test.rest

import com.sugar.judge.database.DiscussDbVerticle
import com.sugar.judge.rest.DiscussRestVerticle
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
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

@RunWith(VertxUnitRunner::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class DiscussRestVerticleTest{

    companion object {
        private val databaseConfig = io.vertx.core.json.JsonObject().put("port", 5432)
                .put("host", "127.0.0.1")
                .put("database", "tinyoj")
                .put("user", "tinyoj")
                .put("password", "123456")
                .put("maxSize", 100)

        private val jwtConfig = JsonObject().put("keyStore", JsonObject().put("path", "keystore.jceks")
                .put("type", "jceks").put("password", "secret"))

        private val vertx = Vertx.vertx()

        private const val HOST = "127.0.0.1"

        private const val PORT = 8081

        @BeforeClass
        @JvmStatic
        fun beforeClass(context: TestContext) {
            DefaultChannelId.newInstance()
            //val option = DeploymentOptions(config = config)
            // 初始化数据库连接池
            val client = PgClient.pool(vertx, PgPoolOptions(databaseConfig))
            val router = Router.router(vertx)
            val jwtHandler = JWTAuthHandler.create(JWTAuth.create(vertx, JWTAuthOptions(jwtConfig)))
            router.route().handler(BodyHandler.create())

            vertx.deployVerticle(DiscussRestVerticle(router, jwtHandler), context.asyncAssertFailure())
            vertx.deployVerticle(DiscussDbVerticle(), context.asyncAssertSuccess())

            vertx.createHttpServer().requestHandler(router::accept).listen(PORT, HOST)
        }

        @AfterClass
        @JvmStatic
        fun afterClass(context: TestContext) {
            vertx.close(context.asyncAssertSuccess())
        }
    }

    private val webClient = WebClient.create(vertx)
}