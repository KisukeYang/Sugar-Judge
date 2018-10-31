/*
 * MIT License
 *
 * Copyright (c) 2018 Kisuke.Yang
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.sugar.judge.database

import com.sugar.judge.config.EventBusNamespace
import com.sugar.judge.config.EventBusNamespace.*
import com.sugar.judge.utils.PgResultTransformer
import io.reactiverse.pgclient.PgRowSet
import io.reactiverse.pgclient.Tuple
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.awaitResult
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDateTime


class AccountDbVerticle : AbstractDbVerticle() {

    private val logger = LoggerFactory.getLogger(AccountDbVerticle::class.java)

    override val listenAddress: String
        get() = ADDR_ACCOUNT_DB.get()

    override suspend fun processMethods(params: JsonObject, method: EventBusNamespace): JsonObject {
        return when (method) {
            COMMAND_CREATE_ACCOUNT -> createNewUser(params)
            COMMAND_GET_ACCOUNT -> getUserByUserName(params)
            COMMAND_GET_ACCOUNT_LIST -> getUserList(params)
            COMMAND_UPDATE_ACCOUNT -> updateUser(params)
            else -> throw RuntimeException("错误的请求方法名：${method.get()}")
        }
    }


    private suspend fun createNewUser(params: JsonObject): JsonObject {
        val sql = "insert into users(username, nickname, password, enabled, role, ip_info, register_time, language, email) values ($1, $2, $3, $4, $5, $6, $7, $8, $9)"
        val tuple = Tuple.of(params.getString("username"),
                params.getString("nickname"),
                params.getString("password"),
                params.getBoolean("enabled"),
                params.getString("role"),
                params.getString("ipInfo"),
                LocalDateTime.parse(params.getString("registerTime")),
                params.getString("language"),
                params.getString("email"))
        log().debug("准备执行 INSERT: {$sql}, 参数: {$params}")
        val insertResult = awaitResult<PgRowSet> {
            client.preparedQuery(sql, tuple, it)
        }
        return JsonObject().put("count", insertResult.rowCount())
    }

    /**
     * 根据用户名查询用户信息
     * */
    private suspend fun getUserByUserName(params: JsonObject): JsonObject {
        val sql = "select * from users where username = $1"
        val queryResult = awaitResult<PgRowSet> {
            client.preparedQuery(sql, Tuple.of(params.getString("username")), it)
        }
        return if (queryResult.rowCount() > 0){
            PgResultTransformer.toJsonArray(queryResult).getJsonObject(0)
        }else{
            JsonObject()
        }
    }

    /**
     * 更新用户信息
     * */
    private suspend fun updateUser(params: JsonObject): JsonObject {
        //val sql = "update users set data = data || '$params'::jsonb where data ->> 'id' = '${params.getInteger("id")}'"
        var sql = "update users set id = id"
        var pos = 1
        val tuple = Tuple.tuple()
        // 处理可变参数
        if (!params.getString("nickname").isNullOrEmpty()){
            sql = sql.plus(", nickname=$$pos")
            tuple.addString(params.getString("nickname"))
            pos ++
        }
        if (!params.getString("password").isNullOrEmpty()){
            sql = sql.plus(", password=$$pos")
            tuple.addString(params.getString("password"))
            pos ++
        }
        if (!params.getString("role").isNullOrEmpty()){
            sql = sql.plus(", role=$$pos")
            tuple.addString(params.getString("role"))
            pos ++
        }
        if (!params.getString("email").isNullOrEmpty()){
            sql = sql.plus(", email=$$pos")
            tuple.addString(params.getString("email"))
            pos ++
        }
        if (params.getBoolean("enabled") != null){
            sql = sql.plus(", enabled=$$pos")
            tuple.addBoolean(params.getBoolean("enabled"))
            pos ++
        }
        if (!params.getString("language").isNullOrEmpty()){
            sql = sql.plus(", language=$$pos")
            tuple.addString(params.getString("language"))
            pos ++
        }
        sql = sql.plus(" where id = $$pos")
        tuple.addInteger(params.getInteger("id"))
        logger.debug("准备执行 UPDATE: {$sql}, 参数: {$params")
        val updateResult = awaitResult<PgRowSet> {
            client.preparedQuery(sql, tuple, it)
        }
        return JsonObject().put("count", updateResult.rowCount())
    }

    /**
     * 分页查询用户列表
     * */
    private suspend fun getUserList(params: JsonObject): JsonObject {
        var result = JsonObject()
        var sql = "select id, nickname, register_time \"registerTime\", ac, wa, tle, mle, re, ce, rank_point \"rankPoint\" from users "
        val tuple = Tuple.of(params.getLong("start") ?: 0L, params.getLong("size") ?: 20L)
        val countResult = awaitResult<PgRowSet> {
            client.preparedQuery("select count(*) from ($sql) u", it)
        }
        val count = countResult.first().getInteger("count")
        result.put("count", count)
        if (count > 0){
            val dataSql = "select * from ($sql) u offset $1 limit $2"
            logger.debug("准备执行 SELECT: $dataSql, 参数: $tuple")
            val dataResult = awaitResult<PgRowSet> {
                client.preparedQuery("$dataSql", tuple, it)
            }
            result.put("data", PgResultTransformer.toJsonArray(dataResult))
        }
        return result
    }

    override fun log(): Logger {
        return logger
    }
}