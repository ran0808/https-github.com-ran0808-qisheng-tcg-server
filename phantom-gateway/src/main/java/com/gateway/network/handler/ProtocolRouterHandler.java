package com.gateway.network.handler;

import com.game.common.protocol.GameProtocol;
import com.game.common.protocol.Opcode;
import com.game.common.security.JwtTokenProvider;
import com.gateway.config.ServiceMappingConfig;
import com.gateway.network.util.SendMessage;
import com.gateway.service.MatchService;
import io.jsonwebtoken.Claims;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@Slf4j
@ChannelHandler.Sharable
@RequiredArgsConstructor
public class ProtocolRouterHandler extends ChannelInboundHandlerAdapter {
    private final ServiceMappingConfig serviceMappingConfig;
    private final WebClient webClient;
    private final JwtTokenProvider jwtTokenProvider;
    @Autowired
    private MatchService matchService;
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof GameProtocol protocol){
            if (serviceMappingConfig.requiresRouting(protocol.getOpcode())){
                if(!preHandle(ctx,protocol)){
                    return;
                }
                handleRouting(ctx,protocol);
            }
            else {
                ctx.fireChannelRead(msg);
            }
        }
        else {
            ctx.fireChannelRead(msg);
        }
    }

    private boolean preHandle(ChannelHandlerContext ctx, GameProtocol protocol) {
        if (!serviceMappingConfig.requiresAuthentication(protocol.getOpcode())) {
            return true;
        }

        String token = extractTokenFromProtocol(protocol);
        if (token == null || token.isEmpty()) {
            log.warn("协议中未提供Token，操作码: {}", protocol.getOpcode());
            SendMessage.sendMessage(ctx.channel(), "请提供认证Token", Opcode.TOKEN_EXPIRED);
            return false;
        }

        log.debug("验证Token: {}", token);
        SendMessage.registerChannelToken(ctx.channel(), token);
        // 验证Token有效性
        if (!jwtTokenProvider.validateToken(token)) {
            log.warn("Token验证失败: {}", token);
            try {
                jwtTokenProvider.parseToken(token); // 尝试解析以获取更详细的错误信息
            } catch (Exception e) {
                log.error("Token解析错误详情: {}", e.getMessage());
                log.error("Token解析异常堆栈: ", e);
            }
            SendMessage.sendMessage(ctx.channel(), "Token已失效，请重新登录", Opcode.TOKEN_EXPIRED);
            return false;
        }
        try {
            Claims claims = jwtTokenProvider.parseToken(token);
            String playerId = claims.getId();
            protocol.setPlayerIdString(playerId);
            log.debug("Token验证成功，玩家ID: {}", playerId);
            return true;
        } catch (Exception e) {
            log.error("解析Token失败", e);
            SendMessage.sendMessage(ctx.channel(), "Token解析失败", Opcode.TOKEN_EXPIRED);
            return false;
        }
    }
    private String extractTokenFromProtocol(GameProtocol protocol) {
        if (protocol.getToken() != null && !protocol.getToken().isEmpty()) {
            return protocol.getToken();
        }
        return null;
    }
    private void handleRouting(ChannelHandlerContext ctx, GameProtocol protocol) {
        try{
            //使用消息队列进行匹配进行匹配
            if (protocol.getOpcode()==Opcode.MATCHMAKING_OPCODE){
                matchService.addToMatchQueue(protocol.getPlayerIdString(),protocol.getToken());
                return;
            }
            if (protocol.getOpcode()==Opcode.CANCEL_MATCHMAKING_OPCODE){
                matchService.removeFromMatchQueue(protocol.getPlayerIdString());
                return;
            }
            String servicePath = serviceMappingConfig.getServicePath(protocol.getOpcode());
            if (servicePath==null){
                SendMessage.sendMessage(ctx.channel(),"未找到对应的服务", Opcode.ALERT_OPCODE);
                return;
            }
            //2.构造HTTP请求体
            String token = extractTokenFromProtocol(protocol);
            String playerId = String.valueOf(protocol.getPlayerId());
            if (token != null && "0".equals(playerId)) {
                try {
                    Claims claims = jwtTokenProvider.parseToken(token);
                    playerId = claims.getId();
                    protocol.setPlayerIdString(playerId);
                } catch (Exception e) {
                    log.warn("无法从Token中解析玩家ID", e);
                }
            }
            //3.发送HTTP请求到后端服务
            String requestBody = new String(protocol.getBody(), StandardCharsets.UTF_8);
            log.debug("转发请求到{}，内容{}", servicePath, requestBody);
            webClient.post()
                    .uri("http://" + servicePath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Player-Id", playerId)
                    .header("Authorization", "Bearer " + token) // 添加Token到认证头
                    .header("Opcode", String.valueOf(protocol.getOpcode()))
                    .body(BodyInserters.fromValue(requestBody))
                    .retrieve()
                    .bodyToMono(String.class)
                    .subscribe(
                            responseBody -> {
                                SendMessage.sendMessage(ctx.channel(), responseBody, protocol.getOpcode());
                            },
                            error -> {
                                log.error("服务调用失败: {}", error.getMessage());
                                SendMessage.sendMessage(ctx.channel(), "服务暂时不可用", Opcode.ALERT_OPCODE);
                            }
                    );
        } catch (Exception e) {
            log.error("路由处理失败", e);
            SendMessage.sendMessage(ctx.channel(), "路由处理失败", Opcode.ALERT_OPCODE);
        }
    }
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.debug("客户端连接已断开");
        SendMessage.unregisterChannelToken(ctx.channel());
        super.channelInactive(ctx);
    }
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (cause instanceof IOException) {
            log.debug("客户端连接异常断开: {}", cause.getMessage());
        } else {
            log.error("路由处理器异常", cause);
        }
        SendMessage.unregisterChannelToken(ctx.channel());
        ctx.close();
    }

}
