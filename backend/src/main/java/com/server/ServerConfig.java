package com.server;

import java.util.Map;

public class ServerConfig {
    public static final Map<String, Integer> SERVER_NAME_TO_PORT = Map.of(
        "backup_server_1", 6001,
        "backup_server_2", 7001,
        "backup_server_3", 4001,
        "primary_server", 5001
    );

    public static final Map<String, String> SERVER_NAME_TO_WS_ADDRESS = Map.of(
        "backup_server_1", "ws://backup_server_1:8888",
        "backup_server_2", "ws://backup_server_2:8889",
        "backup_server_3", "ws://backup_server_3:8890",
        "primary_server", "ws://primary_server:8887"
    );

    public static final Map<String, String> SERVER_NAME_TO_IP = Map.of(
        "backup_server_1", "44.203.203.158",
        "backup_server_2", "18.204.230.185",
        "backup_server_3", "3.86.91.77",
        "primary_server", "3.224.220.145"
    );
}