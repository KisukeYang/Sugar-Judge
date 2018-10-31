/*
 * MIT License
 *
 * Copyright (c) 2018 Billy Yuan
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
package com.sugar.judge.utils

import io.reactiverse.pgclient.PgRowSet
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*


object PgResultTransformer {

    fun toJsonArray(pgResult: PgRowSet): JsonArray {
        val jsonArray = JsonArray()
        val columnNames = pgResult.columnsNames()
        pgResult.forEach { row ->
            val currentRow = JsonObject()
            for (columnName in columnNames) {
                var value = row.getValue(columnName) ?: continue
                value = processType(value)
                val key = coordinateKeys(columnName)
                currentRow.put(key!!, value)
            }
            jsonArray.add(currentRow)
        }
        return jsonArray
    }

    /**
     * Customize your ways of handling types in JSON.
     */
    private fun processType(value: Any): Any {
        return when (value) {
            is LocalDate -> processLocalDate(value)
            is LocalDateTime -> processLocalDateTime(value)
            is UUID -> processUUID(value)
            else -> // Make sure the type is supported by JSON if you don't handle it
                value
        }
    }

    private fun processLocalDate(localDate: LocalDate): String {
        return localDate.format(DateTimeFormatter.ISO_DATE)
    }

    private fun processLocalDateTime(localDateTime: LocalDateTime): String {
        return localDateTime.format(DateTimeFormatter.ISO_DATE_TIME)
    }

    private fun processUUID(uuid: UUID): String {
        return uuid.toString()
    }

    /**
     * Make the Json converted from row-column data unified with the Json defined in REST APIs.
     */
    private fun coordinateKeys(columnName: String): String? {
        return if ("publication_date" == columnName) {
            "publicationDate"
        } else columnName
    }
}