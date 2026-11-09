package com.onepiece.ultimate.client.input;

import com.onepiece.ultimate.client.screen.CharacterScreen;
import com.onepiece.ultimate.combat.CombatAction;
import com.onepiece.ultimate.combat.CombatManager;
import com.onepiece.ultimate.data.OPCapabilities;
import com.onepiece.ultimate.data.OPPlayerData;
import com.onepiece.ultimate.network.OPNetwork;
import com.onepiece.ultimate.network.packet.AbilityInputPacket;
import com.onepiece.ultimate.network.packet.CombatInputPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.client.event.InputEvent;

/**
 * Turns key state into edge-triggered packets. Light/heavy attacks hijack the vanilla attack/use keys only in
 * combat mode and only with an empty hand or a blade, so mining, eating and block interaction keep working.
 */
public final class InputHandler {
    private static final boolean[] abilityDown = new boolean[KeyBindings.ABILITY_SLOTS];
    private static final boolean[] hakiDown = new boolean[KeyBindings.HAKI_SLOTS];
    private static boolean blockDown;
    private static boolean heavyHeld;

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;
        OPPlayerData data = OPCapabilities.getOrNull(player);
        if (data == null) return;
        boolean canAct = mc.screen == null && player.isAlive() && !player.isSpectator();

        for (int i = 0; i < KeyBindings.ABILITY_SLOTS; i++) {
            boolean down = canAct && KeyBindings.ABILITIES[i].isDown();
            if (down != abilityDown[i]) {
                abilityDown[i] = down;
                if (data.hasFruit()) OPNetwork.sendToServer(new AbilityInputPacket(i, down));
            }
        }

        for (int i = 0; i < KeyBindings.HAKI_SLOTS; i++) {
            boolean down = canAct && KeyBindings.HAKI[i].isDown();
            if (down != hakiDown[i]) {
                hakiDown[i] = down;
                if (data.haki().anyAwakened()) OPNetwork.sendToServer(new AbilityInputPacket(com.onepiece.ultimate.ability.AbilityBars.HAKI, i, down));
            }
        }

        while (KeyBindings.COMBAT_MODE.consumeClick()) {
            if (canAct) OPNetwork.sendToServer(new CombatInputPacket(CombatAction.TOGGLE_COMBAT_MODE, 0, 0));
        }
        while (KeyBindings.DASH.consumeClick()) {
            if (canAct) OPNetwork.sendToServer(new CombatInputPacket(CombatAction.DASH, player.input.forwardImpulse, player.input.leftImpulse));
        }
        while (KeyBindings.CHARACTER.consumeClick()) {
            if (mc.screen == null) mc.setScreen(new CharacterScreen());
        }

        boolean block = canAct && KeyBindings.BLOCK.isDown();
        if (block != blockDown) {
            blockDown = block;
            OPNetwork.sendToServer(new CombatInputPacket(block ? CombatAction.BLOCK_PRESS : CombatAction.BLOCK_RELEASE, 0, 0));
        }

        if (heavyHeld && (!mc.options.keyUse.isDown() || !canAct)) {
            heavyHeld = false;
            OPNetwork.sendToServer(new CombatInputPacket(CombatAction.HEAVY_RELEASE, 0, 0));
        }
    }

    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || event.getHand() != InteractionHand.MAIN_HAND) return;
        OPPlayerData data = OPCapabilities.getOrNull(player);
        if (data == null || !data.combatMode()) return;
        ItemStack main = player.getMainHandItem();
        boolean fightingHand = main.isEmpty() || CombatManager.isBlade(main);
        if (!fightingHand) return;

        if (event.isAttack()) {
            event.setCanceled(true);
            event.setSwingHand(false);
            OPNetwork.sendToServer(new CombatInputPacket(CombatAction.LIGHT, 0, 0));
        } else if (event.isUseItem()) {
            // Let block interactions (doors, chests…) through while standing on the ground.
            boolean lookingAtBlock = mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK;
            if (lookingAtBlock && player.onGround()) return;
            if (!player.getOffhandItem().isEmpty() && !CombatManager.isBlade(player.getOffhandItem())) return;
            event.setCanceled(true);
            event.setSwingHand(false);
            if (!heavyHeld) {
                heavyHeld = true;
                OPNetwork.sendToServer(new CombatInputPacket(CombatAction.HEAVY_PRESS, 0, 0));
            }
        }
    }

    public static void reset() {
        java.util.Arrays.fill(abilityDown, false);
        java.util.Arrays.fill(hakiDown, false);
        blockDown = false;
        heavyHeld = false;
    }

    private InputHandler() {}
}
