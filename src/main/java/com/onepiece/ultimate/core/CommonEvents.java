package com.onepiece.ultimate.core;

import com.onepiece.ultimate.OnePieceMod;
import com.onepiece.ultimate.ability.AbilityManager;
import com.onepiece.ultimate.combat.CombatManager;
import com.onepiece.ultimate.combat.LaunchTracker;
import com.onepiece.ultimate.command.OnePieceCommand;
import com.onepiece.ultimate.config.OPConfig;
import com.onepiece.ultimate.data.DataSync;
import com.onepiece.ultimate.data.OPCapabilities;
import com.onepiece.ultimate.data.OPPlayerData;
import com.onepiece.ultimate.destruction.DestructionManager;
import com.onepiece.ultimate.entity.DebrisEntity;
import com.onepiece.ultimate.fruit.gomu.GearSecondAbility;
import com.onepiece.ultimate.progression.ProgressionManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Player data life cycle and server tick dispatch. */
@Mod.EventBusSubscriber(modid = OnePieceMod.MOD_ID)
public final class CommonEvents {

    @Mod.EventBusSubscriber(modid = OnePieceMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModBus {
        @SubscribeEvent
        public static void registerCaps(RegisterCapabilitiesEvent event) {
            event.register(OPPlayerData.class);
        }
    }

    @SubscribeEvent
    public static void attach(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            OPCapabilities.Provider provider = new OPCapabilities.Provider();
            event.addCapability(OPCapabilities.KEY, provider);
            event.addListener(provider::invalidate);
        }
    }

    @SubscribeEvent
    public static void clone(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        original.reviveCaps();
        try {
            OPCapabilities.get(original).ifPresent(old -> OPCapabilities.get(event.getEntity()).ifPresent(now -> {
                if (event.isWasDeath()) now.copyOnDeath(old, OPConfig.KEEP_FRUIT_ON_DEATH.get());
                else now.load(old.save());
            }));
        } finally {
            original.invalidateCaps();
        }
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) refresh(sp);
    }

    @SubscribeEvent
    public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) refresh(sp);
    }

    @SubscribeEvent
    public static void changeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) refresh(sp);
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            OPCapabilities.get(sp).ifPresent(d -> {
                AbilityManager.cancelAll(sp, d);
                CombatManager.reset(sp, d);
            });
        }
    }

    private static void refresh(ServerPlayer player) {
        OPCapabilities.get(player).ifPresent(d -> {
            d.abilities.reset();
            d.combat.reset();
            com.onepiece.ultimate.haki.HakiManager.reset(d);
            com.onepiece.ultimate.haki.HakiManager.checkAwakenings(player, d);
            GearSecondAbility.cleanup(player);
            ProgressionManager.applyAttributes(player, d);
            d.markFull();
            d.markVisual();
            d.markStats();
            DataSync.flush(player, d);
        });
    }

    @SubscribeEvent
    public static void startTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer target && event.getEntity() instanceof ServerPlayer viewer) {
            DataSync.sendVisualTo(target, viewer);
        }
    }

    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer sp)) return;
        OPCapabilities.get(sp).ifPresent(d -> PlayerTicker.tick(sp, d));
    }

    @SubscribeEvent
    public static void levelTick(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.level instanceof ServerLevel sl) DestructionManager.tick(sl);
    }

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) LaunchTracker.tick();
    }

    @SubscribeEvent
    public static void serverStopped(ServerStoppedEvent event) {
        DestructionManager.clear();
        LaunchTracker.clear();
        DebrisEntity.clearCounts();
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        OnePieceCommand.register(event.getDispatcher());
    }

    private CommonEvents() {}
}
