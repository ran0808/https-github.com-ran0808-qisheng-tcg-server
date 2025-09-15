package com.gateway.network.handler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.game.common.protocol.GameProtocol;
import com.game.common.protocol.Opcode;
import com.game.common.service.PlayerStateService;
import com.gateway.dto.*;
import com.gateway.network.util.SendMessage;
import com.gateway.network.util.UpdateStatus;
import com.gateway.service.MatchService;
import com.gateway.service.SessionManager;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

//登录协议处理器
//负责解析登录请求，打印内容并返回登录响应
@Slf4j
@Component
@ChannelHandler.Sharable
public class LoginHandler extends ChannelInboundHandlerAdapter {
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private SessionManager sessionManager;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private PlayerStateService playerStateService;
    @Autowired
    private MatchService matchService;
    @Autowired
    private UpdateStatus updateStatus;
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof GameProtocol protocol) {
            if (protocol.getOpcode() == Opcode.LOGIN_OPCODE) {
                handleLoginRequest(ctx, protocol);
            }
            else if(protocol.getOpcode()==Opcode.REGISTER_OPCODE){
                handleRegisterRequest(ctx,protocol);
            }
            else if(protocol.getOpcode()==Opcode.RECONNECT_OPCODE){
                handleReconnectRequest(ctx,protocol);
            }
            else {
                ctx.fireChannelRead(msg);
            }
        } else {
            ctx.fireChannelRead(msg);
        }
    }
    //处理断线重连请求
    private void handleReconnectRequest(ChannelHandlerContext ctx, GameProtocol protocol) {
        String tokenData = new String(protocol.getBody(),StandardCharsets.UTF_8);
        try {
            // 调用认证服务验证token
            ResponseEntity<LoginResponse> validationResponse = restTemplate.postForEntity(
                    "http://auth-service/auth/validate",
                    tokenData,
                    LoginResponse.class
            );

            if (validationResponse.getStatusCode().is2xxSuccessful() && validationResponse.getBody() != null) {
                LoginResponse response = validationResponse.getBody();
                String playerId = response.getPlayerId();
                // 重新注册会话
                sessionManager.registerSession(tokenData, ctx.channel());
                // 更新玩家状态为在线
                updateStatus.updatePlayerStatus(playerId, tokenData, PlayerStatus.ACTIVE);
                SendMessage.sendMessage(ctx.channel(), "重连成功", Opcode.RECONNECT_SUCCESS, tokenData);
            } else {
                SendMessage.sendMessage(ctx.channel(), "令牌无效或已过期", Opcode.RECONNECT_FAILED);
            }
        } catch (Exception e) {
            log.error("重连失败", e);
            SendMessage.sendMessage(ctx.channel(), "重连失败", Opcode.RECONNECT_FAILED);
        }
    }
    //处理注册请求
    private void handleRegisterRequest(ChannelHandlerContext ctx, GameProtocol protocol) {
        //1.解析协议
        String registerData = new String(protocol.getBody(),StandardCharsets.UTF_8);
        String[] parts = registerData.split(":",2);
        if (parts.length!=2){
            SendMessage.sendMessage(ctx.channel(),"注册数据格式错误",Opcode.REGISTER_FAILED);
            return;
        }
        String username = parts[0];
        String password = parts[1];
        LoginRequest request = new LoginRequest(username, password);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<LoginRequest> requestEntity = new HttpEntity<>(request, headers);
        try {
            ResponseEntity<?> response = restTemplate.postForEntity(
                    "http://auth-service/auth/register",
                    requestEntity,
                    LoginResponse.class
            );
            if(response.getStatusCode().is2xxSuccessful()&&response.getBody()!=null){
                LoginResponse loginResponse = (LoginResponse) response.getBody();
                String playerId = loginResponse.getPlayerId();
                String token = loginResponse.getToken();
                // 4. 在网关注册会话
                sessionManager.registerSession(token, ctx.channel());
                // 5. 通知游戏服务更新玩家状态
                updateStatus.updatePlayerStatus(playerId,token,PlayerStatus.ACTIVE);
                SendMessage.sendMessage(ctx.channel(), "注册成功", Opcode.REGISTER_SUCCESS, token);
            }
        } catch (HttpClientErrorException e) {
            try {
                // 尝试解析错误响应体
                String responseBody = e.getResponseBodyAsString();
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode rootNode = objectMapper.readTree(responseBody);

                String errorMessage = "注册失败";
                if (rootNode.has("message")) {
                    errorMessage = rootNode.get("message").asText();
                }

                SendMessage.sendMessage(ctx.channel(), errorMessage, Opcode.REGISTER_FAILED);
            } catch (Exception parseException) {
                SendMessage.sendMessage(ctx.channel(), "注册失败: " + e.getStatusText(), Opcode.REGISTER_FAILED);

            }
        }catch (HttpServerErrorException exception) {
            SendMessage.sendMessage(ctx.channel(), "认证服务暂时不可用", Opcode.REGISTER_FAILED);
        } catch (Exception exception0) {
            SendMessage.sendMessage(ctx.channel(), "认证服务调用失败", Opcode.REGISTER_FAILED);
        }
    }
    //处理登录请求
    private void handleLoginRequest (ChannelHandlerContext ctx, GameProtocol protocol) {
        // 1. 解析协议
        String loginData = new String(protocol.getBody(),StandardCharsets.UTF_8);
        String[] parts = loginData.split(":", 2);
        if (parts.length != 2) {
            // 发送失败消息
            SendMessage.sendMessage(ctx.channel(), "登录数据格式错误", Opcode.LOGIN_FAILED);
            return;
        }
        String username = parts[0];
        String password = parts[1];
        //2. 调用认证服务 auth-service
        LoginRequest request = new LoginRequest(username, password);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<LoginRequest> requestEntity = new HttpEntity<>(request, headers);
        try {
            ResponseEntity<?> response = restTemplate.postForEntity(
                    "http://auth-service/auth/login",
                    requestEntity,
                    LoginResponse.class
            );
            if(response.getStatusCode().is2xxSuccessful()&&response.getBody()!=null){
                LoginResponse loginResponse = (LoginResponse) response.getBody();
                String playerId = loginResponse.getPlayerId();
                String token = loginResponse.getToken();
                // 4. 在网关注册会话
                sessionManager.registerSession(token, ctx.channel());
                String registeredPlayerId = sessionManager.getPlayerIdByChannel(ctx.channel());
                log.info("会话注册后获取的玩家ID: {}", registeredPlayerId);
                // 5. 通知游戏服务更新玩家状态
                updateStatus.updatePlayerStatus(playerId,token,PlayerStatus.ACTIVE);
                SendMessage.sendMessage(ctx.channel(), "登录成功", Opcode.LOGIN_SUCCESS, token);
            }
        } catch (HttpClientErrorException e) {
            log.error("认证服务返回客户端错误: {}", e.getStatusCode());
            try {
                // 尝试解析错误响应体
                String responseBody = e.getResponseBodyAsString();
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode rootNode = objectMapper.readTree(responseBody);

                String errorMessage = "登录失败";
                if (rootNode.has("message")) {
                    errorMessage = rootNode.get("message").asText();
                }

                SendMessage.sendMessage(ctx.channel(), errorMessage, Opcode.LOGIN_FAILED);
        } catch (Exception parseException) {
                // 如果解析失败，使用默认错误消息
                log.error("解析错误响应失败: {}", parseException.getMessage());
                SendMessage.sendMessage(ctx.channel(), "登录失败: " + e.getStatusText(), Opcode.LOGIN_FAILED);

        }
        }catch (HttpServerErrorException exception) {
                // 处理5xx错误
                log.error("认证服务内部错误: {}", exception.getStatusCode());
                SendMessage.sendMessage(ctx.channel(), "认证服务暂时不可用", Opcode.LOGIN_FAILED);
        } catch (Exception exception0) {
                // 处理其他异常
                log.error("调用认证服务失败: {}", exception0.getMessage());
                SendMessage.sendMessage(ctx.channel(), "认证服务调用失败", Opcode.LOGIN_FAILED);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (cause instanceof IOException) {
            log.debug("客户端连接异常断开: {}", cause.getMessage());
        } else {
            log.error("登录处理器异常", cause);
        }
        ctx.close();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        Channel channel = ctx.channel();
        log.info("客户端连接断开，频道: {}", channel.id());
        String playerId = sessionManager.getPlayerIdByChannel(channel);
        String token = sessionManager.getTokenByChannel(channel);
        log.info("从会话管理器获取的玩家ID: {}", playerId);
        if (playerId != null) {
            try {
                // 从匹配队列中移除下线玩家
                boolean isMatching = playerStateService.isMatching(playerId);
                log.info("玩家 {} 是否在匹配中: {}", playerId, isMatching);
                if (playerStateService.isMatching(playerId)) {
                    log.info("从匹配队列中移除玩家: {}", playerId);
                    matchService.removeFromMatchQueue(playerId);
                    log.info("已从匹配队列中移除玩家: {}", playerId);
                }
                // 处理游戏中玩家下线（可能需要特殊处理）
                if (playerStateService.isGaming(playerId)) {
                    playerStateService.removeFromGaming(playerId);
                    log.warn("玩家 {} 在游戏中下线，需要特殊处理", playerId);
                }
                //改变状态
                updateStatus.updatePlayerStatus(playerId,token,PlayerStatus.OFFLINE);
            } catch (Exception e) {
                log.error("清理玩家 {} 状态时发生异常: {}", playerId, e.getMessage());
            }
        }

        try {
            sessionManager.removeSession(ctx.channel());
            log.info("客户端连接断开，已清理会话数据");
        } catch (Exception e) {
            log.error("清理会话数据时发生异常: {}", e.getMessage());
        }
    }
}