package com.game.common.protocol;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * 幻影协议结构（16字节头 + 变长体）
 +------------+----------+----------+-----------+
 | 魔数(4B)   | 版本(1B) | 操作码(2B)|字节长度(4B)|
 +------------+----------+----------+-----------+
 |校验和(2B) | 玩家ID(16B)|令牌长度(2B)|无效填充(1B)|
 +---------------------------------+-----------+
 |             数据体(NB)                    |
 +-------------------------------------------+
 |             令牌(MB)                      |
 +-------------------------------------------+
 */
@Data
@AllArgsConstructor
public class GameProtocol {
    // 四个字节的魔数
    public static final int MAGIC_NUMBER = 0x00504847; // 'P','H','G'
    // 一个字节的版本号
    private byte version;
    // 两个字节的操作码 01:登录，02：退出登录，03：移动，04：技能释放
    private short opcode;
    // 四个字节的数据长度
    private int length;
    // 两个字节的校验和
    private short checksum;
    // 16个字节的玩家ID
    private UUID playerId;
    // 两个字节的令牌长度
    private short tokenLength;
    //无效填充
    private byte invalid;
    // 数据体
    private byte[] body;
    // 令牌
    private String token;

    public GameProtocol(){}

    // 构造协议
    public GameProtocol(String playerId, byte[] body){
        this.playerId = UUID.randomUUID();
        this.body = body;
    }
    public void setPlayerIdString(String playerIdString) {
        this.playerId = UUID.fromString(playerIdString);
    }

    public String getPlayerIdString() {
        return playerId != null ? playerId.toString() : null;
    }
    // 计算校验和
    public short calculateCheckSum() {
        short checksum = 0;
        // 校验版本号
        checksum ^= version;
        // 校验操作码
        checksum ^= opcode;
        // 校验数据长度
        checksum ^= (short) (length & 0xFFFF);
        checksum ^= (short) ((length >> 16) & 0xFFFF);
        // 校验玩家ID
        byte[] uuidBytes = getUUIDBytes(playerId);
        for (byte b : uuidBytes) {
            checksum ^= b;
        }
        // 校验令牌长度
        checksum ^= tokenLength;
        // 校验数据体
        if (getBody() != null) {
            for (byte b : body) {
                checksum ^= b;
            }
        }
        // 校验令牌
        if (getToken() != null) {
            byte[] tokenBytes = token.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            for (byte b : tokenBytes) {
                checksum ^= b;
            }
        }

        return checksum;
    }
    private byte[] getUUIDBytes(UUID uuid) {
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }

}