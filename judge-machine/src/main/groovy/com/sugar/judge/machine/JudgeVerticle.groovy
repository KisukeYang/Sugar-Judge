package com.sugar.judge.machine

import io.vertx.kafka.client.consumer.KafkaConsumer

def consumer = KafkaConsumer.create(vertx, config())

consumer.handler({ record ->
    println("Processing key=${record.key()},value=${record.value()},partition=${record.partition()},offset=${record.offset()}")
})


