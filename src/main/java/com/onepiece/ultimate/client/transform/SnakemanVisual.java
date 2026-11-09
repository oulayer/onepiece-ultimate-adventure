package com.onepiece.ultimate.client.transform;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;

/** Snakeman: leaner than Boundman – broad shoulders, long arms, the same markings, a coiled stance. */
final class SnakemanVisual extends BoundmanVisual {
    @Override
    public void applyModel(PlayerModel<?> m, AbstractClientPlayer player, float partial) {
        scale(m.body, 1.18f, 1.05f, 1.18f);
        scale(m.rightArm, 1.2f, 1.28f, 1.2f);
        scale(m.leftArm, 1.2f, 1.28f, 1.2f);
        m.rightArm.x -= 0.9f;
        m.leftArm.x += 0.9f;
    }

    @Override
    public float rootScale() {
        return 1.04f;
    }

    @Override
    public float firstPersonArmScale() {
        return 1.12f;
    }

    @Override
    public String stance(AbstractClientPlayer player, boolean moving) {
        return "snakeman_stance";
    }
}
