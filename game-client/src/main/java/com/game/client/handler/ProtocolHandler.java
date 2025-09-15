package com.game.client.handler;

import com.game.common.protocol.Opcode;
import com.game.client.ui.LoginFrame;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.Data;
import com.game.client.util.SendMessage;

@Data
public class ProtocolHandler {
    private final LoginFrame ui;
    private Channel channel;
    private EventLoopGroup group;

    public ProtocolHandler(LoginFrame ui) {
        this.ui = ui;
    }

    public ChannelFuture connect(String host, int port) {
        group = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new GameProtocolDecoder());
                        pipeline.addLast(new GameProtocolEncoder());
                        pipeline.addLast(new ClientHandler(ui));
                    }
                });
        ChannelFuture channelFuture = bootstrap.connect(host, port);
        channelFuture.addListener((ChannelFutureListener)f->{
            if (f.isSuccess()){
                channel = f.channel();
            }
        });
        return channelFuture;
    }
    public void login(String username, String password) {
        if (channel == null || !channel.isActive()) {
            ui.onLoginFailed("未连接到服务器");
            return;
        }
        String loginData = username + ":" + password;
        SendMessage.sendMessage(channel,loginData, Opcode.LOGIN_OPCODE);
    }
    public void register(String username, String password) {
        if (channel==null||!channel.isActive()){
            ui.onRegisterFailed("未连接到服务器");
            return;
        }
        String registerData = username+":"+password;
        SendMessage.sendMessage(channel,registerData,Opcode.REGISTER_OPCODE);
    }
    public void startMatchmaking(String token){
        if (channel==null||!channel.isActive()){
            ui.onMatchmakingFailed("未连接到服务器");
        }
        SendMessage.sendMessageToken(channel,token,Opcode.MATCHMAKING_OPCODE);
    }
    public void cancelMatchmaking(String token){
        if (channel==null||!channel.isActive()){
            return;
        }
        SendMessage.sendMessageToken(channel,token,Opcode.CANCEL_MATCHMAKING_OPCODE);
    }
    public void readyForGame(String matchId, String token) {
        if (channel == null || !channel.isActive()) {
            return;
        }
        String data = matchId + ":" + token;
        SendMessage.sendMessage(channel, data, Opcode.GAME_READY_OPCODE);
    }
    public void disconnect() {
        if (channel != null) {
            channel.close();
        }
        if (group != null) {
            group.shutdownGracefully();
        }
    }


}