package com.play.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatusUpdateResponse {
    private boolean success;
    private String message;
    private StatusUpdateData data;
    private long timestamp;

    // 成功响应的快捷创建方法
    public static StatusUpdateResponse success(String playerId, String previousStatus,
                                               String currentStatus) {
        StatusUpdateData data = new StatusUpdateData(
                playerId, previousStatus, currentStatus,
                Instant.now().toString()
        );

        return new StatusUpdateResponse(
                true,
                "状态更新成功",
                data,
                System.currentTimeMillis()
        );
    }

    // 失败响应的快捷创建方法
    public static StatusUpdateResponse failure(String message) {
        return new StatusUpdateResponse(
                false,
                message,
                null,
                System.currentTimeMillis()
        );
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusUpdateData {
        private String playerId;
        private String previousStatus;
        private String currentStatus;
        private String updatedAt;
    }
}