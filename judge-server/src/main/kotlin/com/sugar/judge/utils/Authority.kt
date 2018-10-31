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
package com.sugar.judge.utils

import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext

/**
 * 获取当前登录用户
 * @param rc
 * @return JsonObject
 * */
fun getCurrentUser(rc: RoutingContext): JsonObject{
    return rc.user().principal()
}

/**
 * 权限认证：是否 Admin 权限
 * @param rc
 * @return Boolean
 * */
fun checkAdmin(rc: RoutingContext): Boolean{
    // 权限验证
    if (rc.user() == null){
        unauthorized(rc)
        return false
    }
    val user = rc.user().principal()
    if (user.getString("role") != "admin"){
        forbidden(rc)
        return false
    }
    return true
}

/**
 * 权限认证：是否 普通用户 权限
 * @param rc
 * @return Boolean
 * */
fun checkUser(rc:RoutingContext): Boolean{
    return true
}