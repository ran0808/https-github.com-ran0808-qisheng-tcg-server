package com.play.network.gameSet.EffectImple;

import com.play.network.gameSet.card.CharacterCard;
import com.play.network.gameSet.status.Status;
import com.play.network.player.PlayerSession;
import com.play.network.protocol.Opcode;
import com.play.network.util.SendMessage;

import java.util.Arrays;

//实现元素附着
public class ElementAttachEffect extends SkillEffect {
    private final String element; // 附着的元素（火/水/冰等）

    public ElementAttachEffect(String element) {
        super(EffectType.ELEMENT_ATTACH, 0);
        this.element = element;
    }
    @Override
    public void execute(CharacterCard caster, CharacterCard target,
                        PlayerSession casterSession, PlayerSession targetSession) {
            target.applyElementStatus(target, new Status(Status.Type.ELEMENT_ATTACH,element));
            if (Arrays.asList("冰","雷","草","水","火").contains(caster.getElement())) {
            SendMessage.sendMessage(casterSession.getPlayerChannel(),
                    target.getName() + "被附着" + element + "元素！",
                    Opcode.BROADCAST_OPCODE, casterSession.getPlayerId());
            SendMessage.sendMessage(targetSession.getPlayerChannel(),
                    "你的" + target.getName() + "被附着" + element + "元素！",
                    Opcode.BROADCAST_OPCODE, targetSession.getPlayerId());
        }
    }
    }