package com.play.controller;

import com.play.dto.PlayerStatus;
import com.play.dto.StatusUpdateRequest;
import com.play.dto.StatusUpdateResponse;
import com.play.service.PlayerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.game.common.security.TokenService;
@RestController
@RequestMapping("/game-service")
@Slf4j
public class PlayerController {
    @Autowired
    private PlayerService playerService;
    @Autowired
    private TokenService tokenService;
    //进行状态改变开发
    @PutMapping("/{playerId}/status")
    public ResponseEntity<?> updateStatus(@RequestBody StatusUpdateRequest statusUpdateRequest,@PathVariable String playerId,@RequestHeader("Authorization") String authHeader){
        // 验证令牌
        try {
            String token = authHeader.replace("Bearer ", "");
            if (!tokenService.isTokenValid(playerId, token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(StatusUpdateResponse.failure("令牌无效"));
            }
            // 更新状态
            String currentStatus = playerService.getPlayerStatus(playerId).toString();
            boolean updated = playerService.updateStatus(playerId, currentStatus);
            if (updated){
                StatusUpdateResponse response = StatusUpdateResponse.success(
                        playerId,
                        currentStatus,
                        statusUpdateRequest.getStatus().toString()
                );
                return ResponseEntity.ok(response);
            }else {
                return ResponseEntity.badRequest()
                        .body(StatusUpdateResponse.failure("状态更新失败"));
            }
        } catch (Exception e) {
            log.error("状态更新异常: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(StatusUpdateResponse.failure("服务器内部错误"));
        }
    }

}