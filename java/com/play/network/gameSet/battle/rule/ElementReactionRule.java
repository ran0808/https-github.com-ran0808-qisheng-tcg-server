package com.play.network.gameSet.battle.rule;

import com.play.network.gameSet.card.CharacterCard;
import com.play.network.player.PlayerSession;
import com.play.service.CardService;

public interface ElementReactionRule {
    boolean canReact(String element1,String element2);
    int calculateDamage(CharacterCard attacker, CharacterCard target,
                        PlayerSession currentPlayer, PlayerSession oppositePlayer,
                        CardService cardService);
    String getReactionName();
}
