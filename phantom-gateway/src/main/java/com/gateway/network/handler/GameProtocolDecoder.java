package com.gateway.network.handler;

import com.game.common.protocol.GameProtocol;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Slf4j
public class GameProtocolDecoder extends LengthFieldBasedFrameDecoder {
    private static final int HEADER_LENGTH = 32;

    public GameProtocolDecoder(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength) {
        super(maxFrameLength, lengthFieldOffset, lengthFieldLength);
    }

    public GameProtocolDecoder() {
        super(10*1024*1024, 7, 4, 21, 0);
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        // 用父类方法获取完整帧
        ByteBuf frame = (ByteBuf) super.decode(ctx, in);
        if (frame == null) {
            return null; // 帧不完整，等待后续操作
        }
        try {
            // 解析协议头部
            GameProtocol gameProtocol = new GameProtocol();
            // 验证魔数
            int magic = frame.readInt();
            if (magic != GameProtocol.MAGIC_NUMBER) {
                return new IllegalArgumentException("协议校验失败,无效数据包" + magic);
            }
            // 获取版本信息
            gameProtocol.setVersion(frame.readByte());
            // 获取操作码
            gameProtocol.setOpcode(frame.readShort());
            // 获取字节长度
            int totalDataLength = frame.readInt();
            gameProtocol.setLength(totalDataLength);
            // 获取校验和
            short checkSum = frame.readShort();
            gameProtocol.setChecksum(checkSum);
            // 获取玩家ID
            byte[] uuidBytes = new byte[16];
            frame.readBytes(uuidBytes);
            UUID playerId = bytesToUUID(uuidBytes);
            gameProtocol.setPlayerId(playerId);
            // 获取令牌长度
            short tokenLength = frame.readShort();
            gameProtocol.setTokenLength(tokenLength);
            //读取无效字节数
            frame.readByte();
            if (totalDataLength < tokenLength) {
                throw new CorruptedFrameException("令牌长度超过数据总长度");
            }
            int bodyLength = totalDataLength - tokenLength;
            // 获取正文数据
            if (bodyLength > 0) {
                byte[] body = new byte[bodyLength];
                frame.readBytes(body);
                gameProtocol.setBody(body);
            }
            // 获取令牌数据
            if (tokenLength > 0) {
                byte[] tokenBytes = new byte[tokenLength];
                frame.readBytes(tokenBytes);
                gameProtocol.setToken(new String(tokenBytes, StandardCharsets.UTF_8));
            }
            // 校验数据完整性
            short calculatedCheckSum = gameProtocol.calculateCheckSum();
            if (calculatedCheckSum != checkSum) {
                return new IllegalArgumentException("数据校验失败");
            }

            log.debug("{}{}", gameProtocol);
            return gameProtocol;
        } finally {
            frame.release();
        }
    }
    private UUID bytesToUUID(byte[] bytes) {
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        long high = bb.getLong();
        long low = bb.getLong();
        return new UUID(high, low);
    }
}