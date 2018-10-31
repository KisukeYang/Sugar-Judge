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
 * Send back a response with status 200 Ok.
 *
 * @param context routing context
 */
internal fun ok(context: RoutingContext) {
    context.response().setStatusCode(200)
            .putHeader("content-type", "application/json")
            .end(JsonObject().put("status", 0).toString())
}

/**
 * Send back a response with status 200 OK.
 *
 * @param context routing context
 * @param content body content in JSON format
 */
fun ok(context: RoutingContext, content: JsonObject) {
    context.response().setStatusCode(200)
            .putHeader("content-type", "application/json")
            .end(JsonObject().put("status", 0).put("data", content).toString())
}

/**
 * Request error and send back a response with status 200 OK.
 *
 * @param context routing context
 * @param content body content in JSON format
 */
fun fail(context: RoutingContext, content: String) {
    context.response().setStatusCode(200)
            .putHeader("content-type", "application/json")
            .end(JsonObject().put("status", 1).put("data", content).toString())
}

/**
 * Send back a response with status 201 Created.
 *
 * @param context routing context
 */
fun created(context: RoutingContext) {
    context.response().setStatusCode(201).end()
}

/**
 * Send back a response with status 201 Created.
 *
 * @param context routing context
 * @param content body content in JSON format
 */
fun created(context: RoutingContext, content: String) {
    context.response().setStatusCode(201)
            .putHeader("content-type", "application/json")
            .end(content)
}

/**
 * Send back a response with status 204 No Content.
 *
 * @param context routing context
 */
fun noContent(context: RoutingContext) {
    context.response().setStatusCode(204).end()
}

/**
 * Send back a response with status 400 Bad Request.
 *
 * @param context routing context
 * @param ex      exception
 */
fun badRequest(context: RoutingContext, ex: Throwable) {
    context.response().setStatusCode(400)
            .putHeader("content-type", "application/json")
            .end(JsonObject().put("message", ex.message).encodePrettily())
}

/**
 * Send back a response with status 400 Bad Request.
 *
 * @param context routing context
 */
fun badRequest(context: RoutingContext) {
    context.response().setStatusCode(400)
            .putHeader("content-type", "application/json")
            .end(JsonObject().put("message", "错误的请求参数").encodePrettily())
}

/**
 * Send back a response with status 400 Bad Request.
 *
 * @param context routing context
 * @param message
 */
fun badRequest(context: RoutingContext, message: String) {
    context.response().setStatusCode(400)
            .putHeader("content-type", "application/json")
            .end(JsonObject().put("message", message).encodePrettily())
}

/**
 * Send back a response with status 401 Unauthorized.
 *
 * @param context routing context
 */
fun unauthorized(context: RoutingContext) {
    context.response().setStatusCode(401)
            .putHeader("content-type", "application/json")
            .end(JsonObject().put("message", "未授权").encodePrettily())
}

/**
 * Send back a response with status 403 Forbidden.
 *
 * @param context routing context
 */
fun forbidden(context: RoutingContext) {
    context.response().setStatusCode(403)
            .putHeader("content-type", "application/json")
            .end(JsonObject().put("message", "无权限操作").encodePrettily())
}

fun forbidden(context: RoutingContext, message: String) {
    context.response().setStatusCode(403)
            .putHeader("content-type", "application/json")
            .end(JsonObject().put("message", message).encodePrettily())
}

/**
 * Send back a response with status 404 Not Found.
 *
 * @param context routing context
 */
fun notFound(context: RoutingContext) {
    context.response().setStatusCode(404)
            .putHeader("content-type", "application/json")
            .end(JsonObject().put("message", "not_found").encodePrettily())
}

/**
 * Send back a response with status 500  Error.
 *
 * @param context routing context
 * @param ex      exception
 */
fun error(context: RoutingContext, ex: Throwable) {
    context.response().setStatusCode(500)
            .putHeader("content-type", "application/json")
            .end(JsonObject().put("message", ex.message).encodePrettily())
}

/**
 * Send back a response with status 500  Error.
 *
 * @param context routing context
 * @param cause   error message
 */
fun error(context: RoutingContext, cause: String) {
    context.response().setStatusCode(500)
            .putHeader("content-type", "application/json")
            .end(JsonObject().put("message", cause).encodePrettily())
}

/**
 * Send back a response with status 503 Service Unavailable.
 *
 * @param context routing context
 */
fun serviceUnavailable(context: RoutingContext) {
    context.fail(503)
}

/**
 * Send back a response with status 503 Service Unavailable.
 *
 * @param context routing context
 * @param ex      exception
 */
fun serviceUnavailable(context: RoutingContext, ex: Throwable) {
    context.response().setStatusCode(503)
            .putHeader("content-type", "application/json")
            .end(JsonObject().put("message", ex.message).encodePrettily())
}

/**
 * Send back a response with status 503 Service Unavailable.
 *
 * @param context routing context
 * @param cause   error message
 */
fun serviceUnavailable(context: RoutingContext, cause: String) {
    context.response().setStatusCode(503)
            .putHeader("content-type", "application/json")
            .end(JsonObject().put("message", cause).encodePrettily())
}


