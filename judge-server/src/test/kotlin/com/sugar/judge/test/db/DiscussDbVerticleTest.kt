package com.sugar.judge.test.db

import com.sugar.judge.database.DiscussDbVerticle
import io.reactiverse.pgclient.PgClient
import io.reactiverse.pgclient.PgPoolOptions
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.FixMethodOrder
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

@RunWith(VertxUnitRunner::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class DiscussDbVerticleTest{


    companion object {

        private val config = JsonObject().put("port", 5432)
                .put("host", "127.0.0.1")
                .put("database", "suage_judge")
                .put("user", "sugar")
                .put("password", "123456")
                .put("maxSize", 100)
        private val vertx = Vertx.vertx()

        @BeforeClass
        @JvmStatic
        fun beforeClass(context: TestContext){
            // 初始化数据库连接池
            val client = PgClient.pool(vertx, PgPoolOptions(config))
            vertx.deployVerticle(DiscussDbVerticle(), context.asyncAssertSuccess())
        }

        @AfterClass
        @JvmStatic
        fun afterClass(context: TestContext){
            vertx.close(context.asyncAssertSuccess())
        }
    }
}