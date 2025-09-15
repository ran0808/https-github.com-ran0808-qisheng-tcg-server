package com.play.network.gameSet.skill.impl;

import com.play.network.gameSet.EffectImple.ElementAttachEffect;
import com.play.network.gameSet.EffectImple.SkillEffect;
import com.play.network.gameSet.battle.ElementReaction;
import com.play.network.gameSet.card.Card;
import com.play.network.gameSet.card.CharacterCard;
import com.play.network.gameSet.dice.DiceCost;
import com.play.network.gameSet.skill.Skill;
import com.play.network.gameSet.skill.SkillStrategy;
import com.play.network.player.PlayerSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.play.network.gameSet.skill.Character;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
@Component
@Character(name = "砂糖")
public class SugarElementalBurst implements SkillStrategy {
    private static final Skill.SkillType TYPE = Skill.SkillType.ELEMENTAL_BURST;
    private static final String NAME = "禁·风灵作成·柒伍同构贰型";
    private static final int BASE_DAMAGE = 1;
    private static final int ENERGY_PROVIDE = 0;
    private static final int ADDITION_CARD_ID = 12;
    @Autowired
    ElementReaction elementReaction;
    @Override
    public int execute(CharacterCard caster, CharacterCard target, PlayerSession currentPlayer, PlayerSession oppositePlayer, Map<Integer, Card> cardLibrary) {
        return 0;
    }
    @Override
    public Skill.SkillType getSkillType() {
        return TYPE;
    }

    @Override
    public String getSkillName() {
        return NAME;
    }

    @Override
    public List<DiceCost> getDiceCosts() {
        List<DiceCost> costs = new ArrayList<>();
        costs.add(new DiceCost(3, "风"));
        return costs;
    }

    @Override
    public int getBaseDamage() {
        return BASE_DAMAGE;
    }

    @Override
    public int getEnergyProvide() {
        return ENERGY_PROVIDE;
    }

    @Override
    public List<SkillEffect> getEffects() {
        List<SkillEffect> effects = new ArrayList<>();
        effects.add(new ElementAttachEffect("风"));
        return effects;
    }

    @Override
    public int getAdditionCardId() {
        return ADDITION_CARD_ID;
    }
}
