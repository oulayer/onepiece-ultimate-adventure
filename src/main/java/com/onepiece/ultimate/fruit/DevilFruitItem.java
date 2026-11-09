package com.onepiece.ultimate.fruit;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class DevilFruitItem extends Item {
    private final ResourceLocation fruitId;

    public DevilFruitItem(ResourceLocation fruitId, Properties props) {
        super(props);
        this.fruitId = fruitId;
    }

    public ResourceLocation fruitId() {
        return fruitId;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.EAT;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 40;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide && entity instanceof ServerPlayer player) {
            if (FruitEating.eat(player, fruitId) && !player.getAbilities().instabuild) stack.shrink(1);
        }
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        DevilFruit fruit = DevilFruits.get(fruitId);
        if (fruit == null) return;
        tooltip.add(fruit.type().displayName().copy().append(Component.literal(" • ").withStyle(ChatFormatting.DARK_GRAY)).append(fruit.rarity().displayName()));
        tooltip.add(fruit.description().copy().withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.onepiece.curse").withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC));
    }

    @Override
    public Component getName(ItemStack stack) {
        DevilFruit fruit = DevilFruits.get(fruitId);
        return fruit == null ? super.getName(stack) : fruit.displayName().copy().withStyle(fruit.rarity().color);
    }
}
