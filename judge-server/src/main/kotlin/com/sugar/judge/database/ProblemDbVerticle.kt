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
import com.sugar.judge.config.EventBusNamespace.*
import com.sugar.judge.utils.PgResultTransformer
import io.reactiverse.pgclient.PgClient
import io.reactiverse.pgclient.PgRowSet
import io.reactiverse.pgclient.Tuple
import io.reactiverse.pgclient.impl.codec.decoder.type.ErrorOrNoticeType.HINT
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.awaitResult
import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

class ProblemDbVerticle : AbstractDbVerticle() {
    private val logger = LoggerFactory.getLogger(ProblemDbVerticle::class.java)

    override val listenAddress: String
        get() = ADDR_PROBLEM_DB.get()

    override suspend fun processMethods(params: JsonObject, method: EventBusNamespace): JsonObject {
        return when (method) {
            COMMAND_GET_PROBLEM_LIST -> getProblemPagination(params)
            COMMAND_GET_ONE_PROBLEM -> getProblemInfo(params)
            COMMAND_CREATE_PROBLEM -> insertNewProblem(params)
            COMMAND_UPDATE_PROBLEM -> updateProblemSelective(params)
            COMMAND_INCREASE_PROBLEM_PROP -> increaseProperties(params)
            COMMAND_DELETE_PROBLEM -> deleteProblemById(params)
            COMMAND_GET_PROBLEM_NEXT_ID -> getNextId()
            COMMAND_GET_PROBLEM_LIST_NOT_CONTEST -> getProblemListNotContest(params)
            else -> throw RuntimeException("错误的请求方法名，" + method.get())
        }
    }

    /**
     * 分页查询试题信息
     * */
    private suspend fun getProblemPagination(params: JsonObject): JsonObject {
        val start = params.getLong("start") ?: 0L
        val size = params.getLong("size") ?: 20L
        var result = JsonObject()
        var sql = "select id, title, accept, submit, difficulty from problems where disuse = false"
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
    }

    /**
     * 根据试题 ID 查询试题
     * */
    private suspend fun getProblemInfo(params: JsonObject): JsonObject {
        val sql = "select id, title, accept, submit, difficulty, content, background, memory_limit \"memoryLimit\", time_limit \"timeLimit\"," +
                " sample_input \"sampleInput\", sample_output \"sampleOutput\", hint, hidden, code, creator from problems where id = $1 and disuse = false"
        val queryResult = awaitResult<PgRowSet> {
            client.preparedQuery(sql, Tuple.of(params.getInteger(KEY_PROBLEM_ID)), it)
        }
        return if (queryResult.rowCount() > 0) {
            PgResultTransformer.toJsonArray(queryResult).getJsonObject(0)
        } else {
            JsonObject()
        }
    }

    /**
     * 插入新试题
     * */
    private suspend fun insertNewProblem(params: JsonObject): JsonObject {
        var insertSql = "insert into problems(id, title, time_limit, memory_limit, background, content, the_input, the_output, sample_input, sample_output, hint, code," +
                " in_file_path, out_file_path, alter_out_data, conditional_judge, difficulty, creator, create_time, update_time, hidden, disuse, type)" +
                " values($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15, $16, $17, $18, $19, $20, $21, $22, $23)"
        val tuple = Tuple.of(params.getInteger(KEY_PROBLEM_ID),
                params.getString(KEY_PROBLEM_TITLE),
                params.getInteger(KEY_PROBLEM_TIME_LIMIT),
                params.getInteger(KEY_PROBLEM_MEMORY_LIMIT),
                params.getString(KEY_PROBLEM_BACKGROUND) ?: "",
                params.getString(KEY_PROBLEM_CONTENT),
                params.getString(KEY_PROBLEM_THE_INPUT),
                params.getString(KEY_PROBLEM_THE_OUTPUT),
                params.getString(KEY_PROBLEM_SAMPLE_INPUT),
                params.getString(KEY_PROBLEM_SAMPLE_OUTPUT),
                params.getString(KEY_PROBLEM_HINT) ?: "",
                params.getString(KEY_PROBLEM_CODE) ?: "",
                params.getString(KEY_PROBLEM_IN_FILE_PATH),
                params.getString(KEY_PROBLEM_OUT_FILE_PATH),
                params.getString(KEY_PROBLEM_ALTER_OUT_DATA) ?: "",
                params.getString(KEY_PROBLEM_CONDITIONAL_JUDGE) ?: "",
                params.getInteger(KEY_PROBLEM_DIFFICULTY),
                params.getString(KEY_PROBLEM_AUTHOR),
                LocalDateTime.parse(params.getString(KEY_PROBLEM_CREATE_TIME)),
                LocalDateTime.parse(params.getString(KEY_PROBLEM_UPDATE_TIME)),
                params.getBoolean(KEY_PROBLEM_HIDDEN),
                false,
                params.getInteger(KEY_PROBLEM_TYPE))
        try {
            log().debug("准备执行 INSERT: {$insertSql}, 参数: {$params}")
            val insertResult = awaitResult<PgRowSet> {
                client.preparedQuery(insertSql, tuple, it)
            }
            return JsonObject().put("count", insertResult.rowCount())
        }catch (e: Exception){
            e.printStackTrace()
            throw RuntimeException(e)
        }
    }

    /**
     * 更新试题信息
     * */
    private suspend fun updateProblemSelective(params: JsonObject): JsonObject {
        var updateSql = "update problems set id = id "
        var tuple = Tuple.tuple()
        var pos = 1
        // 处理可变参数
        if (!params.getString(KEY_PROBLEM_TITLE).isNullOrEmpty()){
            updateSql = updateSql.plus(", title = $$pos")
            tuple.addString(params.getString(KEY_PROBLEM_TITLE))
            pos ++
        }
        if (!params.getString(KEY_PROBLEM_CONTENT).isNullOrEmpty()){
            updateSql = updateSql.plus(", content = $$pos")
            tuple.addString(params.getString(KEY_PROBLEM_CONTENT))
            pos ++
        }
        if (!params.getString(KEY_PROBLEM_BACKGROUND).isNullOrEmpty()){
            updateSql = updateSql.plus(", background = $$pos")
            tuple.addString(params.getString(KEY_PROBLEM_BACKGROUND))
            pos ++
        }
        if (params.getInteger(KEY_PROBLEM_TIME_LIMIT) != null){
            updateSql = updateSql.plus(", time_limit = $$pos")
            tuple.addInteger(params.getInteger(KEY_PROBLEM_TIME_LIMIT))
            pos ++
        }
        if (params.getInteger(KEY_PROBLEM_MEMORY_LIMIT) != null){
            updateSql = updateSql.plus(", memory_limit = $$pos")
            tuple.addInteger(params.getInteger(KEY_PROBLEM_MEMORY_LIMIT))
            pos ++
        }
        if (!params.getString(KEY_PROBLEM_THE_INPUT).isNullOrEmpty()){
            updateSql = updateSql.plus(", the_input = $$pos")
            tuple.addString(params.getString(KEY_PROBLEM_THE_INPUT))
            pos ++
        }
        if (!params.getString(KEY_PROBLEM_THE_OUTPUT).isNullOrEmpty()){
            updateSql = updateSql.plus(", the_output = $$pos")
            tuple.addString(params.getString(KEY_PROBLEM_THE_OUTPUT))
            pos ++
        }
        if (!params.getString(KEY_PROBLEM_SAMPLE_INPUT).isNullOrEmpty()){
            updateSql = updateSql.plus(", sample_input = $$pos")
            tuple.addString(params.getString(KEY_PROBLEM_SAMPLE_INPUT))
            pos ++
        }
        if (!params.getString(KEY_PROBLEM_SAMPLE_OUTPUT).isNullOrEmpty()){
            updateSql = updateSql.plus(", sample_output = $$pos")
            tuple.addString(params.getString(KEY_PROBLEM_SAMPLE_OUTPUT))
            pos ++
        }
        if (!params.getString(KEY_PROBLEM_IN_FILE_PATH).isNullOrEmpty()){
            updateSql = updateSql.plus(", in_file_path = $$pos")
            tuple.addString(params.getString(KEY_PROBLEM_IN_FILE_PATH))
            pos ++
        }
        if (!params.getString(KEY_PROBLEM_OUT_FILE_PATH).isNullOrEmpty()){
            updateSql = updateSql.plus(", out_file_path = $$pos")
            tuple.addString(params.getString(KEY_PROBLEM_OUT_FILE_PATH))
            pos ++
        }
        if (!params.getString(KEY_PROBLEM_HINT).isNullOrEmpty()){
            updateSql = updateSql.plus(", hint = $$pos")
            tuple.addString(params.getString(KEY_PROBLEM_HINT))
            pos ++
        }
        if (!params.getString(KEY_PROBLEM_CODE).isNullOrEmpty()){
            updateSql = updateSql.plus(", code = $$pos")
            tuple.addString(params.getString(KEY_PROBLEM_CODE))
            pos ++
        }
        if (!params.getString(KEY_PROBLEM_ALTER_OUT_DATA).isNullOrEmpty()){
            updateSql = updateSql.plus(", alter_out_data = $$pos")
            tuple.addString(params.getString(KEY_PROBLEM_ALTER_OUT_DATA))
            pos ++
        }
        if (!params.getString(KEY_PROBLEM_CONDITIONAL_JUDGE).isNullOrEmpty()){
            updateSql = updateSql.plus(", conditional_judge = $$pos")
            tuple.addString(params.getString(KEY_PROBLEM_CONDITIONAL_JUDGE))
            pos ++
        }
        if (params.getInteger(KEY_PROBLEM_DIFFICULTY) != null){
            updateSql = updateSql.plus(", difficulty = $$pos")
            tuple.addInteger(params.getInteger(KEY_PROBLEM_DIFFICULTY))
            pos ++
        }
        if (!params.getString(KEY_PROBLEM_UPDATE_TIME).isNullOrEmpty()){
            updateSql = updateSql.plus(", update_time = $$pos")
            tuple.addLocalDateTime(LocalDateTime.parse(params.getString(KEY_PROBLEM_UPDATE_TIME)))
            pos ++
        }
        if (params.getBoolean(KEY_PROBLEM_HIDDEN) != null){
            updateSql = updateSql.plus(", hidden = $$pos")
            tuple.addBoolean(params.getBoolean(KEY_PROBLEM_HIDDEN))
            pos ++
        }
        if (params.getInteger(KEY_PROBLEM_TYPE) != null){
            updateSql = updateSql.plus(", type = $$pos")
            tuple.addInteger(params.getInteger(KEY_PROBLEM_TYPE))
            pos ++
        }
        tuple.addInteger(params.getInteger(KEY_PROBLEM_ID))
        updateSql = updateSql.plus(" where id = $$pos")
        log().debug("准备执行 INSERT: {$updateSql}, 参数: {$params}")
        try {
            val updateResult = awaitResult<PgRowSet> {
                client.preparedQuery(updateSql, tuple, it)
            }
            return JsonObject().put("count", updateResult.rowCount())
        }catch (e: Exception){
            e.printStackTrace()
            throw RuntimeException(e.localizedMessage)
        }

    }

    private suspend fun deleteProblemById(params: JsonObject): JsonObject{
        val sql = "update problems set disuse = true where id = $1"
        log().debug("准备执行 DELETE: {$sql}, 参数: {$params}")
        try {
            val updateResult = awaitResult<PgRowSet> {
                client.preparedQuery(sql, Tuple.of(params.getInteger(KEY_PROBLEM_ID)), it)
            }
            return JsonObject().put("count", updateResult.rowCount())
        }catch (e: Exception){
            e.printStackTrace()
            throw RuntimeException(e.localizedMessage)
        }
    }

    private suspend fun increaseProperties(params: JsonObject): JsonObject {
        val column = params.getString("column")
        var sql = "update problems set $column = $column + ${params.getInteger("value")} where id = $1"
        try {
            val updateResult = awaitResult<PgRowSet> {
                client.preparedQuery(sql, Tuple.of(params.getInteger(KEY_PROBLEM_ID)), it)
            }
            return JsonObject().put("count", updateResult.rowCount())
        }catch (e: Exception){
            e.printStackTrace()
            throw RuntimeException(e.localizedMessage)
        }
    }

    private suspend fun getNextId(): JsonObject {
        val seqResult = awaitResult<PgRowSet> {
            client.preparedQuery("select nextval('problem_id_seq')", it)
        }
        return JsonObject().put("id", seqResult.first().getInteger("nextval"))
    }

    private suspend fun getProblemListNotContest(params: JsonObject):JsonObject{
        var querySql = "select p.id from problems p " +
                "where not exists(select 1 from contest_problem cp where cp.contest_id = $1 and cp.problem_id = p.id) "
        try {
            var tuple = Tuple.of(params.getInteger("contestId"))
            val ids = params.getJsonArray("ids")
            if (ids != null && ids.size() > 0){
                querySql = querySql.plus(" and p.id in (")
                var pos = 2
                ids.forEach {
                    if (it is Int){
                        tuple.addInteger(it)
                        querySql = querySql.plus("$$pos,")
                        pos ++
                    }
                }
                querySql = querySql.removeSuffix(",").plus(")")
            }
            val queryResult = awaitResult<PgRowSet> {
                client.preparedQuery(querySql, tuple, it)
            }
            return JsonObject().put("data", PgResultTransformer.toJsonArray(queryResult))
        }catch (e: Exception){
            e.printStackTrace()
            throw RuntimeException(e.localizedMessage)
        }


    }
    override fun log(): Logger {
        return logger
    }
}