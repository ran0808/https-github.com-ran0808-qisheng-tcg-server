package com.gateway.network.util;

import com.gateway.dto.PlayerStatus;
import com.gateway.dto.StatusUpdateRequest;
import com.gateway.dto.StatusUpdateResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;


@Slf4j
@Component
public class UpdateStatus {
    @Autowired
    private  RestTemplate restTemplate;

    public void updatePlayerStatus(String playerId, String token, PlayerStatus status) {
        try{
            //创建状态更新请求
            StatusUpdateRequest request  = new StatusUpdateRequest(status);
            //创建请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization","Bearer"+token);
            //创建请求实体
            HttpEntity<StatusUpdateRequest> entity = new HttpEntity<>(request,headers);
            //发送请求
            ResponseEntity<StatusUpdateResponse> response = restTemplate.exchange(
                    "http://game-service/{playerId}/status",
                    HttpMethod.PUT,
                    entity,
                    StatusUpdateResponse.class,
                    playerId
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("玩家 {} 状态更新成功: {}", playerId, status);
            }
            else {
                log.warn("玩家 {} 状态更新失败: {}", playerId, response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("更新玩家状态失败: {}", e.getMessage());
        }
    }
}
