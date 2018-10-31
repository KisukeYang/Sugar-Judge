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
import com.sugar.judge.utils.PgResultTransformer
import io.reactiverse.pgclient.PgClient
import io.reactiverse.pgclient.PgRowSet
import io.reactiverse.pgclient.Tuple
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.awaitResult
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ContestUserDbVerticle : AbstractDbVerticle(){
    private val logger = LoggerFactory.getLogger(ContestDbVerticle::class.java)

    override val listenAddress: String
        get() = EventBusNamespace.ADDR_CONTEST_USER_DB.get()

    override suspend fun processMethods(params: JsonObject, method: EventBusNamespace): JsonObject {
        return when(method){
            EventBusNamespace.COMMAND_GET_CONTEST_USER_LIST -> getContestUserList(params)
            EventBusNamespace.COMMAND_ADD_CONTEST_USER -> addContestUser(params)
            EventBusNamespace.COMMAND_REMOVE_CONTEST_USER -> removeContestUser(params)
            EventBusNamespace.COMMAND_INCREASE_CONTEST_USER_PROP -> increaseContestUserProp(params)
            EventBusNamespace.COMMAND_GET_CONTEST_USER -> getContestUser(params)
            else -> throw RuntimeException("错误的请求方法名，" + method.get())
        }
    }

    private suspend fun addContestUser(params: JsonObject): JsonObject{
        val sql = "insert into contest_user values ($1, $2, $3, $4, $5)"
        val tuple = Tuple.of(params.getInteger("contestId"),
                params.getInteger("userId"),
                0, 0, 0)
        try {
            val result = awaitResult<PgRowSet> {
                client.preparedQuery(sql, tuple, it)
            }
            return JsonObject().put("count", result.rowCount())
        }catch (e: Exception){
            e.printStackTrace()
            throw RuntimeException(e.localizedMessage)
        }
    }

    private suspend fun removeContestUser(params: JsonObject): JsonObject{
        val sql = "delete from contest_user where contest_id = $1 and user_id = $2"
        val tuple = Tuple.of(params.getInteger("contestId"),
                params.getInteger("userId"))
        try {
            val result = awaitResult<PgRowSet> {
                client.preparedQuery(sql, tuple, it)
            }
            return JsonObject().put("count", result.rowCount())
        }catch (e: Exception){
            e.printStackTrace()
            throw RuntimeException(e.localizedMessage)
        }
    }

    private suspend fun getContestUserList(params: JsonObject): JsonObject{
        val start = params.getLong("start") ?: 0L
        val size = params.getLong("size") ?: 20L
        val result = JsonObject()
        val sql = "select cu.contest_id, cu.user_id, u.nickname, cu.submit, cu.accept, cu.penalty from contest_user cu inner join users u on cu.user_id = u.id " +
                "where cu.contest_id = $1"
        val tuple = Tuple.of(params.getInteger("contestId"), start, size)
        val countResult = awaitResult<PgRowSet> {
            client.preparedQuery("select count(*) from ($sql) p", tuple, it)
        }
        val count = countResult.first().getInteger("count")
        // 封装分页结果
        result.put("total", count)
        result.put("currentPage", start/size + 1)
        result.put("pageSize", size)
        val dataResult = awaitResult<PgRowSet> {
            client.preparedQuery("select * from ($sql) p offset \$2 limit \$3", tuple, it)
        }
        result.put("data", PgResultTransformer.toJsonArray(dataResult))
        return result
    }

    private suspend fun increaseContestUserProp(params: JsonObject): JsonObject{
        val column = params.getString("column")
        var sql = "update contest_user set $column = $column + ${params.getInteger("value")} where contest_id = $1 and user_id = $2"
        try {
            val updateResult = awaitResult<PgRowSet> {
                client.preparedQuery(sql, Tuple.of(params.getInteger("contestId"), params.getInteger("userId")), it)
            }
            return JsonObject().put("count", updateResult.rowCount())
        }catch (e: Exception){
            e.printStackTrace()
            throw RuntimeException(e.localizedMessage)
        }
    }

    private suspend fun getContestUser(params: JsonObject):JsonObject{
        val sql = "select contest_id \"contestId\", user_id \"userId\" from contest_user where contest_id = $1 and user_id = $2"
        val tuple = Tuple.of(params.getInteger("contestId"), params.getInteger("userId"))
        try {
            val queryResult = awaitResult<PgRowSet> {
                client.preparedQuery(sql, tuple, it)
            }
            return JsonObject().put("count", queryResult.size())
        }catch (e: Exception){
            e.printStackTrace()
            throw RuntimeException(e.localizedMessage)
        }
    }

    override fun log(): Logger {
        return logger
    }
}