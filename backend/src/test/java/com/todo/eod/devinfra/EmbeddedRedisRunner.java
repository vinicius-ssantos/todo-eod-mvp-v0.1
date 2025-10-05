package com.todo.eod.devinfra;

import redis.embedded.RedisServer;

public final class EmbeddedRedisRunner {
    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getenv().getOrDefault("EMBEDDED_REDIS_PORT", "6379"));
        RedisServer server = new RedisServer(port);
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        server.start();
        System.out.printf("Embedded Redis running on port %d%n", port);
        Thread.currentThread().join();
    }

    private EmbeddedRedisRunner() {
    }
}
