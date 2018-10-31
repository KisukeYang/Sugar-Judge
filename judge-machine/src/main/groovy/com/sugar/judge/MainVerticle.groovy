package com.sugar.judge

import io.vertx.config.ConfigRetriever
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.DeploymentOptions
import io.vertx.core.json.JsonObject

def config = new JsonObject().put("kafka", new JsonObject()
        .put("bootstrap.servers", "localhost:9092").put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
        .put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer").put("group.id", "judge_group")
        .put("auto.offset.reset", "earliest").put("enable.auto.commit", "false"))
def fileStore = new ConfigStoreOptions().setType("file")
        .setConfig(new JsonObject().put("path", "config.json"))

def options = new ConfigRetrieverOptions().addStore(fileStore)

def retriever = ConfigRetriever.create(vertx, options)

retriever.getConfig { ar ->
    try {
        if (ar.succeeded()) {
            //jsonObject.mergeIn(ar.result())
            config.put("kafka", ar.result())
        } else {
            System.out.println("The configuration file: config.json does not exist or in wrong format, use default config.")
        }
        retriever.close()
        def option = new DeploymentOptions().setConfig(config.getJsonObject("kafka")).setInstances(2)
        vertx.deployVerticle("com/sugar/judge/machine/JudgeVerticle.groovy", option, { r ->
            if (r.succeeded()) {
                println("deploy verticle succeeded.")
            } else {
                println(r.cause().localizedMessage)
            }
        })
    } catch (Exception e) {
        e.printStackTrace()
        vertx.close()
    }

}


