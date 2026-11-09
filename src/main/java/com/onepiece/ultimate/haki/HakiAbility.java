package com.onepiece.ultimate.haki;

import com.onepiece.ultimate.ability.Ability;
import com.onepiece.ultimate.ability.AbilityContext;
import com.onepiece.ultimate.ability.AbilityResource;
import com.onepiece.ultimate.data.OPPlayerData;
import net.minecraft.network.chat.Component;

/** A Haki technique: spends the haki gauge and unlocks with the level of its Haki type, not fruit mastery. */
public abstract class HakiAbility extends Ability {
    private final HakiType type;
    private final int requiredLevel;

    protected HakiAbility(String name, HakiType type, int requiredLevel, Properties props) {
        super(name, props.resource(AbilityResource.HAKI).color(type.color));
        this.type = type;
        this.requiredLevel = requiredLevel;
    }

    public HakiType type() {
        return type;
    }

    public int requiredLevel() {
        return requiredLevel;
    }

    @Override
    public boolean isUnlocked(OPPlayerData data) {
        return data.hakiLevel(type) >= requiredLevel;
    }

    @Override
    public Component lockedMessage() {
        return Component.translatable("message.onepiece.haki_locked", displayName(), type.displayName(), requiredLevel);
    }

    @Override
    public String requirementLabel() {
        return type.name().charAt(0) + String.valueOf(requiredLevel);
    }

    /** Haki techniques train their own type instead of fruit mastery. */
    protected void train(AbilityContext ctx, int amount) {
        HakiManager.awardXp(ctx.player(), ctx.data(), type, amount);
    }

    /** Haki potency: grows with the type's level and the Haki attribute. */
    protected float potency(OPPlayerData data) {
        return 1f + 0.06f * (data.hakiLevel(type) - 1) + 0.01f * data.progression().get(com.onepiece.ultimate.data.PlayerStat.HAKI);
    }
}
