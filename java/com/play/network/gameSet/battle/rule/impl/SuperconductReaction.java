package com.play.network.gameSet.battle.rule.impl;

import com.play.network.gameSet.battle.rule.ElementReactionRule;
import com.play.network.gameSet.card.CharacterCard;
import com.play.network.player.PlayerSession;
import com.play.network.util.SendMessage;
import com.play.service.CardService;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SuperconductReaction implements ElementReactionRule {
    @Override
    public boolean canReact(String e1, String e2) {
        return ("雷".equals(e1) && "冰".equals(e2)) || ("冰".equals(e1) && "雷".equals(e2));
    }

    @Override
    public int calculateDamage(CharacterCard attacker, CharacterCard target,
                               PlayerSession currentPlayer, PlayerSession oppositePlayer,
                               CardService cardService) {
        int damage = 1;
        // 对敌方其他角色造成穿透伤害
        List<CharacterCard> enemyCharacters = oppositePlayer.getCharacterCards();
        for (CharacterCard enemyCharacter : enemyCharacters) {
            if (enemyCharacter != target && enemyCharacter.isAlive()) {
                enemyCharacter.takePiercingDamage(1);
                SendMessage.damageBroadcast(
                        currentPlayer,
                        oppositePlayer,
                        "超导穿透伤害",
                        1,
                        enemyCharacter.getCurrentHp(),
                        enemyCharacter.getName()
                );
            }
        }
        return damage;
    }

    @Override
    public String getReactionName() {
        return "超导";
    }
}