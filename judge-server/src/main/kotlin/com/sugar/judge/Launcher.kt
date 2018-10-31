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
package com.sugar.judge

import io.vertx.core.DeploymentOptions
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.core.impl.launcher.VertxCommandLauncher
import io.vertx.core.impl.launcher.VertxLifecycleHooks
import io.vertx.core.json.JsonObject



/**
 * 类描述：
 * 启动类
 *
 * @author Yezi
 * @Date 2018/4/21 下午5:06
 */
class Launcher : VertxCommandLauncher(), VertxLifecycleHooks {


    init {
        // -Dsun.net.inetaddr.ttl=0
        java.security.Security.setProperty("networkaddress.cache.ttl" , "0")
        //日志处理方式
        System.setProperty(
            "vertx.logger-delegate-factory-class-name",
            "io.vertx.core.logging.SLF4JLogDelegateFactory"
        )
        // log4j2.xml
        System.setProperty("log4j.configuration", "classpath*:log4j2.xml")
        // 禁用默认 DNS
        System.getProperties().setProperty("vertx.disableDnsResolver", "true")

    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            // 如果启动报 DNS 异常，可放开下面的注释
            //DefaultChannelId.newInstance()
            val launcher = Launcher()
            // 设置线程池核心线程数量
            launcher.beforeStartingVertx(VertxOptions().setEventLoopPoolSize(1))
            launcher.execute("run", "com.sugar.judge.MainVerticle")
        }
    }
    /**
     * Main entry point.
     *
     * @param args the user command line arguments.
     */


    /**
     * Utility method to execute a specific command.
     *
     * @param cmd  the command
     * @param args the arguments
     */
    fun executeCommand(cmd: String, vararg args: String) {
        Launcher().execute(cmd, *args)
    }

    /**
     * Hook for sub-classes of [Launcher] after the config has been parsed.
     *
     * @param config the read config, empty if none are provided.
     */
    override fun afterConfigParsed(config: JsonObject) {}

    /**
     * Hook for sub-classes of [Launcher] before the vertx instance is started.
     *
     * @param options the configured Vert.x options. Modify them to customize the Vert.x instance.
     */
    override fun beforeStartingVertx(options: VertxOptions) {

    }

    /**
     * Hook for sub-classes of [Launcher] after the vertx instance is started.
     *
     * @param vertx the created Vert.x instance
     */
    override fun afterStartingVertx(vertx: Vertx) {

    }

    /**
     * Hook for sub-classes of [Launcher] before the verticle is deployed.
     *
     * @param deploymentOptions the current deployment options. Modify them to customize the deployment.
     */
    override fun beforeDeployingVerticle(deploymentOptions: DeploymentOptions) {

    }

    override fun beforeStoppingVertx(vertx: Vertx) {

    }

    override fun afterStoppingVertx() {

    }

    /**
     * A deployment failure has been encountered. You can override this method to customize the behavior.
     * By default it closes the `vertx` instance.
     *
     * @param vertx             the vert.x instance
     * @param mainVerticle      the verticle
     * @param deploymentOptions the verticle deployment options
     * @param cause             the cause of the failure
     */
    override fun handleDeployFailed(
        vertx: Vertx,
        mainVerticle: String,
        deploymentOptions: DeploymentOptions,
        cause: Throwable
    ) {
        // Default behaviour is to close Vert.x if the deploy failed
        vertx.close()
    }
}

