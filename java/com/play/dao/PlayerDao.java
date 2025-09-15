package com.play.dao;
import com.play.dto.PlayerDO;
import com.play.dto.PlayerStatus;
import com.play.mapper.PlayerMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;


@Service
public class PlayerDao {
    @Autowired
    PlayerMapper playerMapper;
    //新增玩家
    public void insertPlayer(PlayerDO playerDO){
            playerMapper.insert(playerDO);
    }
    //根据玩家ID查询
    public PlayerDO getPlayerById(String playerId){
           return playerMapper.selectById(playerId);
    }
    //更新玩家状态(上次活跃时间)
    public int updateStatus(String playerId, String status){
        return playerMapper.updateStatus(playerId,status);
    }

    public PlayerDO getByName(String playerName) {
        return playerMapper.getByName(playerName);
    }

    public void updateRoomId(String id, String roomId) {
        playerMapper.updateRoomId(id,roomId);
    }
    public void resetAllPlayersToOffline() {
        playerMapper.resetAllPlayersToOffline();
    }

    public PlayerStatus getPlayerStatus(String playerId) {
        return playerMapper.getPlayerStatus(playerId);
    }
}
