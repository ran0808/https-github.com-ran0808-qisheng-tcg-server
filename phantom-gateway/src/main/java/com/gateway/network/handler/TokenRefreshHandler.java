package com.gateway.network.handler;

import com.game.common.protocol.GameProtocol;
import com.game.common.protocol.Opcode;
import com.game.common.security.JwtTokenProvider;
import com.gateway.network.util.SendMessage;
import io.jsonwebtoken.Claims;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@ChannelHandler.Sharable
public class TokenRefreshHandler extends ChannelInboundHandlerAdapter {
    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof GameProtocol protocol) {
            if (protocol.getOpcode() == Opcode.TOKEN_REFRESH_REQUEST) {
                handleTokenRefreshRequest(ctx, protocol);
            } else {
                ctx.fireChannelRead(msg);
            }
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    private void handleTokenRefreshRequest(ChannelHandlerContext ctx, GameProtocol protocol) {
        try {
            String token = protocol.getToken();
            // 验证Token是否有效
            if (!jwtTokenProvider.validateToken(token)) {
                log.warn("无效的Token刷新请求");
                SendMessage.sendMessage(ctx.channel(), "Token无效", Opcode.TOKEN_REFRESH_FAILED);
                return;
            }
            // 解析Token获取用户信息
            Claims claims = jwtTokenProvider.parseToken(token);
            String userId = claims.getId();
            String username = claims.getSubject();
            // 生成新Token
            String newToken = jwtTokenProvider.createToken(userId, username);
            long newExpiryTime = System.currentTimeMillis() + jwtTokenProvider.getExpirationInMs();
            // 构建响应协议
            GameProtocol refreshResponse = new GameProtocol();
            refreshResponse.setOpcode(Opcode.TOKEN_REFRESH_RESPONSE);
            refreshResponse.setPlayerId(protocol.getPlayerId());
            refreshResponse.setToken(newToken);
            // 发送响应
            ctx.channel().writeAndFlush(refreshResponse);
            log.info("已为玩家 {} 刷新Token", userId);
        } catch (Exception e) {
            log.error("处理Token刷新请求失败", e);
            SendMessage.sendMessage(ctx.channel(), "Token刷新失败", Opcode.TOKEN_REFRESH_FAILED);
        }
    }
}
