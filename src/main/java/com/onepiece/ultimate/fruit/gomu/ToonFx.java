package com.onepiece.ultimate.fruit.gomu;

import com.onepiece.ultimate.OnePieceMod;
import com.onepiece.ultimate.ability.Ability;
import com.onepiece.ultimate.ability.AbilityContext;
import com.onepiece.ultimate.combat.CombatHits;
import com.onepiece.ultimate.combat.HitDetector;
import com.onepiece.ultimate.combat.HitInfo;
import com.onepiece.ultimate.combat.ToonPhysics;
import com.onepiece.ultimate.destruction.DestructionManager;
import com.onepiece.ultimate.entity.StretchFistEntity;
import com.onepiece.ultimate.fx.ServerFx;
import com.onepiece.ultimate.network.packet.CameraEffectPacket;
import com.onepiece.ultimate.particle.RingParticleOptions;
import com.onepiece.ultimate.registry.ModDamageTypes;
import com.onepiece.ultimate.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/** Gear Fifth helpers: hits that turn their target into bouncing rubber. */
final class ToonFx {
    static Consumer<LivingEntity> toonHit(AbilityContext ctx, Vec3 dir, float damage, float knock, float lift, float impact) {
        Consumer<LivingEntity> base = GomuFx.fistHit(ctx, dir, damage, knock, lift, 10, 1.4f, impact, 0xFFFFFF, 3, true);
        return t -> {
            base.accept(t);
            ToonPhysics.rubberize(t, 120);
        };
    }

    private ToonFx() {}
}
