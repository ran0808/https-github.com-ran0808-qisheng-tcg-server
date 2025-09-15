package com.game.client.handler;

import com.game.common.protocol.GameProtocol;
import com.game.common.protocol.Opcode;
import com.game.client.ui.LoginFrame;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class ClientHandler extends ChannelInboundHandlerAdapter {
    private final LoginFrame ui;
    private  static String token;
    private String playerId;
    private String  opponentId;
    private String sessionId;
    public ClientHandler(LoginFrame ui) {
        this.ui = ui;
    }
    @Override
    public void channelActive(ChannelHandlerContext ctx) {}

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof GameProtocol protocol) {
            switch (protocol.getOpcode()) {
                case Opcode.LOGIN_SUCCESS:
                    handleLoginSuccess(protocol);
                    break;
                case Opcode.LOGIN_FAILED:
                    handleLoginFailed(protocol);
                    break;
                case Opcode.REGISTER_SUCCESS:
                    handleRegisterSuccess(protocol);
                    break;
                case Opcode.REGISTER_FAILED:
                    handleRegisterFailed(protocol);
                    break;
                case Opcode.MATCH_SUCCESS_OPCODE:
                    handleMatchSuccess(protocol);
                default:
            }
        }
    }
    //匹配成功处理
    private void handleMatchSuccess(GameProtocol protocol) {
        if (protocol.getBody()!=null){
            String response = new String(protocol.getBody(),StandardCharsets.UTF_8);
            Pattern pattern = Pattern.compile("匹配成功! 对手: (.*), 会话ID: (.*)");
            Matcher matcher = pattern.matcher(response);
            if (matcher.find()){
               opponentId = matcher.group(1);
               sessionId = matcher.group(2);
               ui.onMatchFound(sessionId);
            }
        }
    }


    private void handleRegisterSuccess(GameProtocol protocol) {
        if (protocol.getToken()!=null&&protocol.getPlayerId()!=null){
            token=protocol.getToken();
            playerId = String.valueOf(protocol.getPlayerId());
            ui.onRegisterSuccess(token,playerId);

        }
        else {
            ui.onRegisterFailed("格式响应错误");
        }
    }
    private void handleRegisterFailed(GameProtocol protocol) {
        String response = new String(protocol.getBody(), StandardCharsets.UTF_8);
        ui.onRegisterFailed(response);
    }
    private void handleLoginSuccess(GameProtocol protocol) {
        if (protocol.getToken()!=null&&protocol.getPlayerId()!=null){
            token=protocol.getToken();
            playerId = String.valueOf(protocol.getPlayerId());
            ui.onLoginSuccess(token,playerId);
        }
        else {
            ui.onLoginFailed("响应格式错误");
        }
    }
    private void handleLoginFailed(GameProtocol protocol) {
        String response = new String(protocol.getBody(), StandardCharsets.UTF_8);
        ui.onLoginFailed(response);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ctx.close();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
    }
}