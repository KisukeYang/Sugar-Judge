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
package com.sugar.judge.config

import io.vertx.core.json.JsonObject

enum class EventBusNamespace {

    // 地址命名
    // 用户
    ADDR_ACCOUNT_DB,
    // 试题
    ADDR_PROBLEM_DB,
    // 竞赛
    ADDR_CONTEST_DB,
    // 竞赛试题
    ADDR_CONTEST_PROBLEM_DB,
    // 竞赛用户
    ADDR_CONTEST_USER_DB,
    // 提交
    ADDR_SUBMISSION_DB,
    // 讨论版
    ADDR_DISCUSS_DB,

    // Auth 命令的命名
    COMMAND_GET_ACCOUNT,
    COMMAND_CREATE_ACCOUNT,
    COMMAND_UPDATE_ACCOUNT,
    COMMAND_GET_ACCOUNT_LIST,

    // Problem 命令的命名
    COMMAND_GET_PROBLEM_LIST,
    COMMAND_GET_PROBLEM_LIST_NOT_CONTEST,
    COMMAND_GET_ONE_PROBLEM,
    COMMAND_CREATE_PROBLEM,
    COMMAND_UPDATE_PROBLEM,
    COMMAND_GET_PROBLEM_NEXT_ID,
    COMMAND_DELETE_PROBLEM,
    COMMAND_INCREASE_PROBLEM_PROP,

    // Contest
    COMMAND_GET_CONTEST_LIST,
    COMMAND_CREATE_CONTEST,
    COMMAND_UPDATE_CONTEST,
    COMMAND_GET_CONTEST_INFO,
    COMMAND_DELETE_CONTEST,

    // Contest Problem
    COMMAND_ADD_CONTEST_PROBLEM,
    COMMAND_REMOVE_CONTEST_PROBLEM,
    COMMAND_GET_CONTEST_PROBLEM_LIST,
    COMMAND_INCREASE_CONTEST_PROBLEM_PROP,
    // Contest User
    COMMAND_ADD_CONTEST_USER,
    COMMAND_REMOVE_CONTEST_USER,
    COMMAND_GET_CONTEST_USER_LIST,
    COMMAND_GET_CONTEST_USER,
    COMMAND_INCREASE_CONTEST_USER_PROP,
    // Submission
    COMMAND_GET_SUBMISSION_LIST,
    COMMAND_CREATE_SUBMISSION,
    COMMAND_GET_SUBMISSION_INFO,
    COMMAND_UPDATE_SUBMISSION,

    // Discuss
    COMMAND_GET_DISCUSS_LIST,

    // Message
    COMMAND_GET_MESSAGE_LIST;

    fun get() : String {
        return this.toString()
    }

    companion object {
        //EventBus消息的方法名key
        const val PARAMS : String = "judge.params"
        //EventBus消息的参数列表key
        const val METHOD : String = "judge.method"

        fun makeMessage(method : EventBusNamespace, params : JsonObject) : JsonObject {
            return JsonObject().put(METHOD, method.get()).put(
                    PARAMS,
                    params
            )
        }

        fun makeMessage(method : EventBusNamespace): JsonObject{
            return JsonObject().put(METHOD, method.get())
        }
    }
}