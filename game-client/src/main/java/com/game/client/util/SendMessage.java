package com.game.client.util;

import com.game.common.protocol.GameProtocol;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Slf4j
public class SendMessage {
    private static final UUID DEFAULT_PLAYER_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");
    public static void sendMessage(Channel channel, String data, short opcode) {
        GameProtocol protocol = new GameProtocol();
        protocol.setOpcode(opcode);
        protocol.setPlayerId(DEFAULT_PLAYER_ID);
        protocol.setBody(data.getBytes(StandardCharsets.UTF_8));
        channel.writeAndFlush(protocol);
    }
    public static void sendMessageToken(Channel channel, String token, short opcode){
        GameProtocol protocol = new GameProtocol();
        protocol.setOpcode(opcode);
        protocol.setPlayerId(DEFAULT_PLAYER_ID);
        protocol.setToken(token);
        channel.writeAndFlush(protocol);
    }
}
