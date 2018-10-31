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
import io.vertx.kotlin.coroutines.awaitResult
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

class ContestDbVerticle : AbstractDbVerticle() {

    private val logger = LoggerFactory.getLogger(ContestDbVerticle::class.java)

    override val listenAddress: String
        get() = EventBusNamespace.ADDR_CONTEST_DB.get()

    override suspend fun processMethods(params: JsonObject, method: EventBusNamespace): JsonObject {
        return when (method) {
            EventBusNamespace.COMMAND_GET_CONTEST_LIST -> getContestPagination(params)
            EventBusNamespace.COMMAND_CREATE_CONTEST -> insertContest(params)
            EventBusNamespace.COMMAND_UPDATE_CONTEST -> updateContestInfo(params)
            EventBusNamespace.COMMAND_GET_CONTEST_INFO -> getContestInfo(params)
            EventBusNamespace.COMMAND_DELETE_CONTEST -> deleteContest(params)
            else -> throw RuntimeException("错误的请求方法名，" + method.get())
        }
    }

    /**
     * 分页查询
     * */
    private suspend fun getContestPagination(params: JsonObject): JsonObject {
        try {
            val start = params.getLong("start") ?: 0L
            val size = params.getLong("size") ?: 20L
            var result = JsonObject()
            var sql = "select id, contest_name \"contestName\", creator, start_time \"startTime\", time_limit \"timeLimit\", status from contests where disuse = false"
            var tuple = Tuple.of(start, size)
            val countResult = awaitResult<PgRowSet> {
                client.preparedQuery("select count(*) from ($sql) p", it)
            }
            val count = countResult.first().getInteger("count")
            result.put("total", count)
            result.put("currentPage", start/size + 1)
            result.put("pageSize", size)
            val dataSql = "select * from ($sql) p offset $1 limit $2"
            if (count > 0) {
                logger.debug("准备执行 SELECT: $dataSql, 参数: $tuple")
                val dataResult = awaitResult<PgRowSet> {
                    client.preparedQuery("$dataSql", tuple, it)
                }
                result.put("data", PgResultTransformer.toJsonArray(dataResult))
            }
            return result
        } catch (e: Exception) {
            e.printStackTrace()
            logger.error("查询失败，原因：${e.localizedMessage}")
            throw RuntimeException(e.localizedMessage)
        }

    }

    /**
     * 根据 ID 查询
     * */
    private suspend fun getContestInfo(params: JsonObject): JsonObject {
        val sql = "select id, contest_name as contestName, description, commend, creator, start_time as startTime, time_limit as timeLimit, status, create_time as createTime, update_time as updateTime from contests where id = $1 and disuse = false"
        val queryResult = awaitResult<PgRowSet> {
            client.preparedQuery(sql, Tuple.of(params.getInteger(KEY_CONTEST_ID)), it)
        }
        return if (queryResult.rowCount() > 0) {
            PgResultTransformer.toJsonArray(queryResult).getJsonObject(0)
        } else {
            JsonObject()
        }
    }

    /**
     * 插入
     * */
    private suspend fun insertContest(params: JsonObject): JsonObject {
        val insertSql = "insert into contests(contest_name, description, creator, start_time, time_limit, commend, hidden, disuse, auto_start, status, create_time, update_time)" +
                " values ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12)"
        try {
            val tuple = Tuple.of(params.getString(KEY_CONTEST_NAME),
                    params.getString(KEY_CONTEST_DESCRIPTION),
                    params.getString(KEY_CONTEST_CREATOR),
                    LocalDateTime.parse(params.getString(KEY_CONTEST_START_TIME)),
                    params.getInteger(KEY_CONTEST_TIME_LIMIT),
                    params.getString(KEY_CONTEST_COMMEND) ?: "",
                    params.getBoolean(KEY_CONTEST_HIDDEN) ?: false,
                    false,
                    params.getBoolean(KEY_CONTEST_AUTO_START),
                    params.getInteger(KEY_CONTEST_STATUS),
                    LocalDateTime.parse(params.getString(KEY_CONTEST_CREATE_TIME)),
                    LocalDateTime.parse(params.getString(KEY_CONTEST_UPDATE_TIME)))
            log().debug("准备执行 INSERT: {$insertSql}, 参数: {$params}")
            val insertResult = awaitResult<PgRowSet> {
                client.preparedQuery(insertSql, tuple, it)
            }
            return JsonObject().put("count", insertResult.rowCount())
        } catch (e: Exception) {
            e.printStackTrace()
            throw RuntimeException(e.localizedMessage)
        }

    }

    /**
     * 更新
     * */
    private suspend fun updateContestInfo(params: JsonObject): JsonObject {
        try {
            var updateSql = "update contests set id = id"
            var tuple = Tuple.tuple()
            var pos = 1
            if (!params.getString(KEY_CONTEST_NAME).isNullOrEmpty()) {
                updateSql = updateSql.plus(", contest_name = $$pos")
                tuple.addString(params.getString(KEY_CONTEST_NAME))
                pos++
            }
            if (!params.getString(KEY_CONTEST_DESCRIPTION).isNullOrEmpty()) {
                updateSql = updateSql.plus(", description = $$pos")
                tuple.addString(params.getString(KEY_CONTEST_DESCRIPTION))
                pos++
            }
            if (!params.getString(KEY_CONTEST_COMMEND).isNullOrEmpty()) {
                updateSql = updateSql.plus(", commend = $$pos")
                tuple.addString(params.getString(KEY_CONTEST_COMMEND))
                pos++
            }
            if (!params.getString(KEY_CONTEST_START_TIME).isNullOrEmpty()) {
                updateSql = updateSql.plus(", start_time = $$pos")
                tuple.addLocalDateTime(LocalDateTime.parse(params.getString(KEY_CONTEST_START_TIME)))
                pos++
            }
            if (params.getBoolean(KEY_CONTEST_HIDDEN) != null) {
                updateSql = updateSql.plus(", hidden = $$pos")
                tuple.addBoolean(params.getBoolean(KEY_CONTEST_HIDDEN))
                pos++
            }
            if (params.getBoolean(KEY_CONTEST_AUTO_START) != null) {
                updateSql = updateSql.plus(", auto_start = $$pos")
                tuple.addBoolean(params.getBoolean(KEY_CONTEST_AUTO_START))
                pos++
            }
            if (params.getInteger(KEY_CONTEST_STATUS) != null) {
                updateSql = updateSql.plus(", status = $$pos")
                tuple.addInteger(params.getInteger(KEY_CONTEST_STATUS))
                pos++
            }
            if (!params.getString(KEY_CONTEST_UPDATE_TIME).isNullOrEmpty()) {
                updateSql = updateSql.plus(", update_time = $$pos")
                tuple.addLocalDateTime(LocalDateTime.parse(params.getString(KEY_CONTEST_UPDATE_TIME)))
                pos++
            }
            updateSql = updateSql.plus(" where id = $$pos")
            tuple.addInteger(params.getInteger(KEY_CONTEST_ID))
            log().debug("准备执行 UPDATE: {$updateSql}, 参数: {$params}")
            val insertResult = awaitResult<PgRowSet> {
                client.preparedQuery(updateSql, tuple, it)
            }
            return JsonObject().put("count", insertResult.rowCount())
        } catch (e: Exception) {
            e.printStackTrace()
            throw RuntimeException(e.localizedMessage)
        }
    }

    private suspend fun deleteContest(params: JsonObject): JsonObject {
        val sql = "update contests set disuse = true where id = $1"
        log().debug("准备执行 DELETE: {$sql}, 参数: {$params}")
        try {
            val updateResult = awaitResult<PgRowSet> {
                client.preparedQuery(sql, Tuple.of(params.getInteger(KEY_CONTEST_ID)), it)
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