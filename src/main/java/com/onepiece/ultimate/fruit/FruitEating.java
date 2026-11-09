package com.onepiece.ultimate.fruit;

import com.onepiece.ultimate.ability.AbilityManager;
import com.onepiece.ultimate.config.OPConfig;
import com.onepiece.ultimate.data.OPCapabilities;
import com.onepiece.ultimate.data.OPPlayerData;
import com.onepiece.ultimate.fx.ServerFx;
import com.onepiece.ultimate.network.packet.CameraEffectPacket;
import com.onepiece.ultimate.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.joml.Vector3f;

/** Rules for eating a Devil Fruit. */
public final class FruitEating {

    /** @return true if the fruit item was consumed. */
    public static boolean eat(ServerPlayer player, ResourceLocation fruitId) {
        DevilFruit fruit = DevilFruits.get(fruitId);
        OPPlayerData data = OPCapabilities.getOrNull(player);
        if (fruit == null || data == null) return false;

        if (data.hasFruit()) {
            if (OPConfig.SECOND_FRUIT_KILLS.get()) {
                player.displayClientMessage(Component.translatable("message.onepiece.second_fruit").withStyle(ChatFormatting.DARK_RED), false);
                player.hurt(player.damageSources().magic(), Float.MAX_VALUE);
                return true;
            }
            player.displayClientMessage(Component.translatable("message.onepiece.already_have_fruit").withStyle(ChatFormatting.RED), true);
            return false;
        }

        AbilityManager.cancelAll(player, data);
        data.setFruit(fruitId);

        var level = player.serverLevel();
        int c = fruit.color();
        Vector3f rgb = new Vector3f(((c >> 16) & 255) / 255f, ((c >> 8) & 255) / 255f, (c & 255) / 255f);
        ServerFx.particles(level, new DustParticleOptions(rgb, 1.6f), player.position().add(0, 1, 0), 60, 0.6, 0.05);
        ServerFx.sound(level, player.position(), ModSounds.EAT_FRUIT.get(), 1f, 1f);
        ServerFx.animation(player, "eat_fruit");
        ServerFx.camera(player, CameraEffectPacket.Type.FLASH, 0.6f, 10);

        player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 50, 20));
        player.connection.send(new ClientboundSetTitleTextPacket(fruit.displayName().copy().withStyle(fruit.rarity().color, ChatFormatting.BOLD)));
        player.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable("message.onepiece.ate_fruit", fruit.type().displayName())));
        return true;
    }

    private FruitEating() {}
}
