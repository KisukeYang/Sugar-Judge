/*
 * MIT License
 *
 * Copyright (c) 2018 Kisuke.Yangs
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
import io.vertx.kotlin.core.json.JsonObject
import io.vertx.kotlin.coroutines.awaitResult
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ContestProblemDbVerticle : AbstractDbVerticle(){
    private val logger = LoggerFactory.getLogger(ContestDbVerticle::class.java)

    override val listenAddress: String
        get() = EventBusNamespace.ADDR_CONTEST_PROBLEM_DB.get()

    override suspend fun processMethods(params: JsonObject, method: EventBusNamespace): JsonObject {
        return when(method){
            EventBusNamespace.COMMAND_GET_CONTEST_PROBLEM_LIST -> getContestProblemList(params)
            EventBusNamespace.COMMAND_ADD_CONTEST_PROBLEM -> addContestProblem(params)
            EventBusNamespace.COMMAND_REMOVE_CONTEST_PROBLEM -> removeContestProblem(params)
            EventBusNamespace.COMMAND_INCREASE_CONTEST_PROBLEM_PROP -> increaseContestProblemProperty(params)
            else -> throw RuntimeException("错误的请求方法名，" + method.get())
        }
    }

    /**
     * 批量添加竞赛试题
     * */
    private suspend fun addContestProblem(params: JsonObject): JsonObject{
        val insertSql = "insert into contest_problem values($1, $2, $3, $4)"
        val contestId = params.getInteger("contestId")
        var batch = mutableListOf<Tuple>()
        val problemIdArray = params.getJsonArray("problemIds")
        problemIdArray.forEach {
            if (it is JsonObject){
                batch.add(Tuple.of(contestId, it.getInteger("id"), 0, 0))
            }
        }
        try {
            val insertResult = awaitResult<PgRowSet> {
                client.preparedBatch(insertSql, batch, it)
            }
            return JsonObject().put("count", insertResult.rowCount())
        }catch (e: Exception){
            e.printStackTrace()
            throw Exception(e.localizedMessage)
        }
    }

    private suspend fun removeContestProblem(params: JsonObject): JsonObject{
        try {
            var deleteSql = "delete from contest_problem where contest_id = $1 and problem_id in("
            val contestId = params.getInteger("contestId")
            val problemIdArray = params.getJsonArray("problemIds")
            var tuple = Tuple.of(contestId)
            var pos = 2
            problemIdArray.forEach {
                if (it is Int){
                    deleteSql = deleteSql.plus("$$pos,")
                    tuple.addInteger(it)
                    pos ++
                }
            }
            deleteSql = deleteSql.removeSuffix(",").plus(")")
            val deleteResult = awaitResult<PgRowSet> {
                client.preparedQuery(deleteSql, tuple, it)
            }
            return JsonObject().put("count", deleteResult.rowCount())
        }catch (e: Exception){
            e.printStackTrace()
            throw Exception(e.localizedMessage)
        }
    }

    private suspend fun getContestProblemList(params: JsonObject): JsonObject{
        val sql = "select cp.contest_id \"contestId\", cp.problem_id \"problemId\", p.title, cp.accept, cp.submit from contest_problem cp left join problems p on cp.problem_id = p.id\n" +
                "where cp.contest_id = $1"
        val tuple = Tuple.of(params.getInteger("contestId"))
        try {
            val result = awaitResult<PgRowSet> {
                client.preparedQuery(sql, tuple, it)
            }
            return JsonObject().put("data", PgResultTransformer.toJsonArray(result))
        }catch (e: Exception){
            e.printStackTrace()
            throw RuntimeException(e.localizedMessage)
        }
    }

    private suspend fun increaseContestProblemProperty(params: JsonObject):JsonObject{
        val column = params.getString("column")
        var sql = "update contest_problem set $column = $column + ${params.getInteger("value")} where contest_id = $1 and problem_id = $2"
        try {
            val updateResult = awaitResult<PgRowSet> {
                client.preparedQuery(sql, Tuple.of(params.getInteger("contestId"), params.getInteger("problemId")), it)
            }
            return JsonObject().put("count", updateResult.rowCount())
        }catch (e: Exception){
            e.printStackTrace()
            throw RuntimeException(e.localizedMessage)
        }
    }

    override fun log(): Logger {
        return logger
    }
}