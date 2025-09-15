package com.play.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.play.dto.PlayerDO;
import com.play.dto.PlayerStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PlayerMapper extends BaseMapper<PlayerDO> {
    int updateStatus(String playerId, String status);
    @Select("select * from player where player_name=#{playerName}")
    PlayerDO getByName(String playerName);
    @Update("update player set current_room_id =#{roomId} where player_id = #{id}")
    void updateRoomId(String id,String roomId);
    @Update("update player set status = 0 where status != 0")
    void resetAllPlayersToOffline();

    PlayerStatus getPlayerStatus(String playerId);
}
