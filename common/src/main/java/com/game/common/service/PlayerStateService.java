package com.game.common.service;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PlayerStateService {

    // 内存中的玩家状态集合（快速访问）
    private final Set<String> matchingPlayers = ConcurrentHashMap.newKeySet();
    private final Set<String> gamingPlayers = ConcurrentHashMap.newKeySet();

    // Redis键名
    private static final String REDIS_MATCHING_PLAYERS = "players:matching";
    private static final String REDIS_GAMING_PLAYERS = "players:gaming";

    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    /**
     * 初始化时从Redis加载状态
     */
    @PostConstruct
    public void init() {
        // 从Redis加载匹配中玩家
        Set<String> redisMatching = redisTemplate.opsForSet().members(REDIS_MATCHING_PLAYERS);
        if (redisMatching != null) {
            matchingPlayers.addAll(redisMatching);
        }

        // 从Redis加载游戏中玩家
        Set<String> redisGaming = redisTemplate.opsForSet().members(REDIS_GAMING_PLAYERS);
        if (redisGaming != null) {
            gamingPlayers.addAll(redisGaming);
        }
        // 清理可能存在的状态不一致
        cleanupInconsistentStates();
    }
    /**
     * 添加玩家到匹配队列
     */
    public boolean addToMatching(String playerId) {
        if (matchingPlayers.add(playerId)) {
            redisTemplate.opsForSet().add(REDIS_MATCHING_PLAYERS, playerId);
            return true;
        }
        return false;
    }

    /**
     * 从匹配队列移除玩家
     */
    public boolean removeFromMatching(String playerId) {
        if (matchingPlayers.remove(playerId)) {
            redisTemplate.opsForSet().remove(REDIS_MATCHING_PLAYERS, playerId);
            return true;
        }
        return false;
    }

    /**
     * 添加玩家到游戏队列
     */
    public boolean addToGaming(String playerId) {
        removeFromMatching(playerId);
        if (gamingPlayers.add(playerId)) {
            redisTemplate.opsForSet().add(REDIS_GAMING_PLAYERS, playerId);
            return true;
        }
        return false;
    }

    /**
     * 从游戏队列移除玩家
     */
    public boolean removeFromGaming(String playerId) {
        if (gamingPlayers.remove(playerId)) {
            redisTemplate.opsForSet().remove(REDIS_GAMING_PLAYERS, playerId);
            return true;
        }
        return false;
    }

    /**
     * 检查玩家是否在匹配中
     */
    public boolean isMatching(String playerId) {
        return matchingPlayers.contains(playerId);
    }

    /**
     * 检查玩家是否在游戏中
     */
    public boolean isGaming(String playerId) {
        return gamingPlayers.contains(playerId);
    }

    /**
     * 获取所有匹配中玩家
     */
    public Set<String> getAllMatchingPlayers() {
        return new HashSet<>(matchingPlayers);
    }

    /**
     * 获取所有游戏中玩家
     */
    public Set<String> getAllGamingPlayers() {
        return new HashSet<>(gamingPlayers);
    }

    /**
     * 清理不一致状态（玩家不应同时处于匹配和游戏状态）
     */
    private void cleanupInconsistentStates() {
        Set<String> inconsistentPlayers = new HashSet<>(matchingPlayers);
        inconsistentPlayers.retainAll(gamingPlayers);
        for (String playerId : inconsistentPlayers) {
            matchingPlayers.remove(playerId);
            redisTemplate.opsForSet().remove(REDIS_MATCHING_PLAYERS, playerId);
        }
    }

    /**
     * 获取玩家总数统计
     */
    public PlayerStats getPlayerStats() {
        return new PlayerStats(
                matchingPlayers.size(),
                gamingPlayers.size()
        );
    }

    /**
     * 玩家统计信息
     */
    public static class PlayerStats {
        private final int matchingCount;
        private final int gamingCount;

        public PlayerStats(int matchingCount, int gamingCount) {
            this.matchingCount = matchingCount;
            this.gamingCount = gamingCount;
        }

        // Getters
        public int getMatchingCount() { return matchingCount; }
        public int getGamingCount() { return gamingCount; }
    }
}