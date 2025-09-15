package com.gateway.network.handler;

import com.game.common.protocol.GameProtocol;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class GameProtocolEncoder extends MessageToByteEncoder<GameProtocol> {
    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, GameProtocol gameProtocol, ByteBuf out) throws Exception {
        // 1.计算数据长度和令牌长度
        byte[] body = gameProtocol.getBody();
        int bodyLength = body != null ? body.length : 0;
        gameProtocol.setVersion((byte)0x01);
        // 计算令牌长度
        String token = gameProtocol.getToken();
        short tokenLength = 0;
        byte[] tokenBytes = null;
        if (token != null) {
            tokenBytes = token.getBytes(StandardCharsets.UTF_8);
            tokenLength = (short) tokenBytes.length;
        }
        int totalDataLength = bodyLength + (tokenBytes != null ? tokenBytes.length : 0);
        gameProtocol.setLength(totalDataLength);
        gameProtocol.setTokenLength(tokenLength);
        // 2.计算校验和
        short checkSum = gameProtocol.calculateCheckSum();
        gameProtocol.setChecksum(checkSum);
        // 3.进行数据封装打包,写入协议内容
        out.writeInt(GameProtocol.MAGIC_NUMBER);
        out.writeByte(gameProtocol.getVersion());
        out.writeShort(gameProtocol.getOpcode());
        out.writeInt(gameProtocol.getLength());
        out.writeShort(gameProtocol.getChecksum());
        out.writeBytes(getUUIDBytes(gameProtocol.getPlayerId())); // 写入 16 字节
        out.writeShort(gameProtocol.getTokenLength());
        out.writeByte(0xff);
        // 写入数据体
        if (bodyLength > 0 && body != null) {
            out.writeBytes(body);
        }
        // 写入令牌
        if (tokenLength > 0 && tokenBytes != null) {
            out.writeBytes(tokenBytes);
        }
    }
    private byte[] getUUIDBytes(UUID uuid) {
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }
}