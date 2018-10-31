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

import com.sugar.judge.config.*
import com.sugar.judge.utils.PgResultTransformer
import io.reactiverse.pgclient.PgClient
import io.reactiverse.pgclient.PgRowSet
import io.reactiverse.pgclient.Tuple
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.get
import io.vertx.kotlin.coroutines.awaitResult
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

class SubmissionDbVerticle : AbstractDbVerticle() {

    private val logger = LoggerFactory.getLogger(SubmissionDbVerticle::class.java)

    override val listenAddress: String
        get() = EventBusNamespace.ADDR_SUBMISSION_DB.get()

    override suspend fun processMethods(params: JsonObject, method: EventBusNamespace): JsonObject {
        return when (method) {
            EventBusNamespace.COMMAND_GET_SUBMISSION_LIST -> getSubmissionPagination(params)
            EventBusNamespace.COMMAND_GET_SUBMISSION_INFO -> getSubmissionInfo(params)
            EventBusNamespace.COMMAND_CREATE_SUBMISSION -> insertSubmission(params)
            EventBusNamespace.COMMAND_UPDATE_SUBMISSION -> updateSubmission(params)
            else -> throw RuntimeException("错误的请求方法名，" + method.get())
        }
    }

    private suspend fun getSubmissionPagination(params: JsonObject): JsonObject {
        val start = params.getLong("start") ?: 0L
        val size = params.getLong("size") ?: 20L
        val result = JsonObject()
        val sql = "select s.id, s.user_id  \"userId\", s.problem_id \"problemId\", s.nickname, s.problem_title \"problemTitle\", s.language,\n" +
                " s.execute_time \"executeTime\", s.status, s.submit_time \"submitTime\", s.code_lock \"codeLock\" from submission s"
        var tuple = Tuple.of(start, size)
        // 处理可变参数
        val countResult = awaitResult<PgRowSet> {
            client.preparedQuery("select count(*) from ($sql) p", it)
        }
        val count = countResult.first().getInteger("count")
        // 封装分页结果
        result.put("total", count)
        result.put("currentPage", start/size + 1)
        result.put("pageSize", size)
        val dataResult = awaitResult<PgRowSet> {
            client.preparedQuery("select * from ($sql) p offset $1 limit $2", tuple, it)
        }
        result.put("data", PgResultTransformer.toJsonArray(dataResult))
        return result
    }

    private suspend fun getSubmissionInfo(params: JsonObject): JsonObject {
        val sql = "select s.id, s.user_id  \"userId\", s.problem_id  \"problemId\", s.nickname, s.problem_title \"problemTitle\", s.language, " +
                " s.execute_time \"executeTime\", s.status, s.submit_time \"submitTime\", s.code_lock \"codeLock\", s.problem_code \"code\" from submission s where id = $1"
        try {
            val tuple = Tuple.of(params.getInteger(KEY_SUBMISSION_ID))
            val queryResult = awaitResult<PgRowSet> {
                client.preparedQuery(sql, tuple, it)
            }
            return if (queryResult.size() > 0){
                return PgResultTransformer.toJsonArray(queryResult)[0]
            }else{
                JsonObject()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw RuntimeException(e)
        }
    }

    private suspend fun insertSubmission(params: JsonObject): JsonObject {
        val insertSql = "insert into submission(user_id, problem_id, contest_id, nickname, problem_title, language, execute_time, contest_time, problem_code, status, code_lock, ip_address, submit_time, error_message)" +
                " values($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14)"
        try {
            val tuple = Tuple.of(params.getInteger(KEY_SUBMISSION_USER_ID),
                    params.getInteger(KEY_SUBMISSION_PROBLEM_ID),
                    params.getInteger(KEY_SUBMISSION_CONTEST_ID),
                    params.getString(KEY_SUBMISSION_NICKNAME),
                    params.getString(KEY_SUBMISSION_PROBLEM_TITLE),
                    params.getString(KEY_SUBMISSION_LANGUAGE),
                    params.getInteger(KEY_SUBMISSION_EXECUTE_TIME),
                    params.getInteger(KEY_SUBMISSION_CONTEST_TIME),
                    params.getString(KEY_SUBMISSION_PROBLEM_CODE),
                    params.getInteger(KEY_SUBMISSION_STATUS),
                    params.getBoolean(KEY_SUBMISSION_LOCK),
                    params.getString(KEY_SUBMISSION_IP_ADDRESS),
                    LocalDateTime.parse(params.getString(KEY_SUBMISSION_SUBMIT_TIME)),
                    params.getString(KEY_SUBMISSION_ERROR_MESSAGE))
            println(tuple.toString())
            log().debug("准备执行 INSERT: {$insertSql}, 参数: {$params}")
            val insertResult = awaitResult<PgRowSet> {
                client.preparedQuery(insertSql, tuple, it)
            }
            return JsonObject().put("count", insertResult.rowCount())
        } catch (e: Exception) {
            e.printStackTrace()
            throw RuntimeException(e)
        }
    }

    private suspend fun updateSubmission(params: JsonObject): JsonObject {
        try {
            var updateSql = "update submission set id = id"
            val tuple = Tuple.tuple()
            var pos = 1
            // 处理可变参数
            if (params.getInteger(KEY_SUBMISSION_EXECUTE_TIME) != null){
                updateSql = updateSql.plus(", execute_time=$$pos")
                tuple.addInteger(params.getInteger(KEY_SUBMISSION_EXECUTE_TIME))
                pos ++
            }
            if (params.getInteger(KEY_SUBMISSION_CONTEST_TIME) != null){
                updateSql = updateSql.plus(", contest_time=$$pos")
                tuple.addInteger(params.getInteger(KEY_SUBMISSION_CONTEST_TIME))
                pos ++
            }
            if (params.getInteger(KEY_SUBMISSION_STATUS) != null){
                updateSql = updateSql.plus(", status=$$pos")
                tuple.addInteger(params.getInteger(KEY_SUBMISSION_STATUS))
                pos ++
            }
            if (params.getBoolean(KEY_SUBMISSION_LOCK) != null){
                updateSql = updateSql.plus(", code_lock=$$pos")
                tuple.addBoolean(params.getBoolean(KEY_SUBMISSION_LOCK))
                pos ++
            }
            if (!params.getString(KEY_SUBMISSION_ERROR_MESSAGE).isNullOrEmpty()){
                updateSql = updateSql.plus(", error_message=$$pos")
                tuple.addString(params.getString(KEY_SUBMISSION_ERROR_MESSAGE))
                pos ++
            }
            tuple.addInteger(params.getInteger(KEY_SUBMISSION_ID))
            updateSql = updateSql.plus(" where id = $$pos")
            log().debug("准备执行 INSERT: {$updateSql}, 参数: {$params}")
            val updateResult = awaitResult<PgRowSet> {
                client.preparedQuery(updateSql, tuple, it)
            }
            return JsonObject().put("count", updateResult.rowCount())
        } catch (e: Exception) {
            e.printStackTrace()
            throw RuntimeException(e.localizedMessage)
        }
    }

    override fun log(): Logger {
        return logger
    }
}