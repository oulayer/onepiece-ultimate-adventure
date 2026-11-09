package com.onepiece.ultimate.fruit.phoenix;

import com.onepiece.ultimate.ability.Ability;
import com.onepiece.ultimate.data.OPPlayerData;
import com.onepiece.ultimate.fruit.DevilFruit;
import com.onepiece.ultimate.fruit.FruitRarity;
import com.onepiece.ultimate.fruit.FruitType;
import com.onepiece.ultimate.haki.HakiManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;

import java.util.List;

/**
 * Tori Tori no Mi, Model: Phoenix (mythical Zoan).
 * <p>
 * Passive – <b>flames of resurrection</b>: most of the damage taken is queued and burned back as blue flames over the
 * next seconds, paid with energy. Damage from attackers coated in <b>Armament Haki is not regenerated</b>.
 * <p>
 * Forms (transformation wheel): Hybrid (wings of blue flame: glide and flap) and Full Phoenix (true flight).
 */
public final class PhoenixFruit extends DevilFruit {
    private static final float REGEN_SHARE = 0.8f;

    public PhoenixFruit(ResourceLocation id) {
        super(id, FruitType.ZOAN, FruitRarity.MYTHICAL, 0x3EC7FF);
    }

    @Override
    protected List<Ability> createAbilities() {
        PhoenixAbilities.bootstrap();
        return List.of(PhoenixAbilities.TALON, PhoenixAbilities.FLAME_WAVE, PhoenixAbilities.REGENERATION, PhoenixAbilities.DIVE);
    }

    @Override
    protected List<Ability> createTransformations() {
        return List.of(PhoenixAbilities.HYBRID, PhoenixAbilities.FULL);
    }

    @Override
    public boolean isImmuneTo(ServerPlayer player, OPPlayerData data, DamageSource source) {
        return source.is(DamageTypes.IN_FIRE) || source.is(DamageTypes.ON_FIRE);
    }

    @Override
    public float modifyIncomingDamage(ServerPlayer player, OPPlayerData data, DamageSource source, float amount) {
        if (HakiManager.isImbued(source.getEntity())) {
            if (player.getRandom().nextInt(3) == 0) {
                player.displayClientMessage(Component.translatable("message.onepiece.phoenix_haki").withStyle(ChatFormatting.DARK_PURPLE), true);
            }
            return amount;
        }
        if (data.energy() > 5f) data.abilities.regenPool = Math.min(60f, data.abilities.regenPool + amount * REGEN_SHARE);
        return amount;
    }

    @Override
    public void passiveTick(ServerPlayer player, OPPlayerData data) {
        var rt = data.abilities;
        if (rt.regenPool <= 0 || player.getHealth() >= player.getMaxHealth()) {
            if (player.getHealth() >= player.getMaxHealth()) rt.regenPool = 0;
            return;
        }
        float heal = Math.min(rt.regenPool, 0.25f);
        if (!data.consumeEnergy(heal * 2.4f)) {
            rt.regenPool = 0;
            return;
        }
        player.heal(heal);
        rt.regenPool -= heal;
        if (player.tickCount % 3 == 0) PhoenixFx.body(player.serverLevel(), player, 2);
    }
}
