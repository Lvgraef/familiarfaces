package io.github.itskillerluc.familiarfaces.server.events;

import io.github.itskillerluc.familiarfaces.FamiliarFaces;
import io.github.itskillerluc.familiarfaces.server.capability.WolfArmorCapabilityProvider;
import io.github.itskillerluc.familiarfaces.server.entities.ai.WolfCrackiness;
import io.github.itskillerluc.familiarfaces.server.init.ArmorMaterials;
import io.github.itskillerluc.familiarfaces.server.init.ItemRegistry;
import io.github.itskillerluc.familiarfaces.server.init.MobEffectRegistry;
import io.github.itskillerluc.familiarfaces.server.init.SoundEventRegistry;
import io.github.itskillerluc.familiarfaces.server.networking.FamiliarFacesNetwork;
import io.github.itskillerluc.familiarfaces.server.networking.SyncWolfArmorPacket;
import io.github.itskillerluc.familiarfaces.server.util.Util;
import io.github.itskillerluc.familiarfaces.server.util.WolfArmorUtils;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

@Mod.EventBusSubscriber(modid = FamiliarFaces.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ForgeEvents {
    @SubscribeEvent
    public static void attachCapabilities(final AttachCapabilitiesEvent<Entity> event) {
        event.addCapability(WolfArmorCapabilityProvider.IDENTIFIER, new WolfArmorCapabilityProvider());
    }

    @SubscribeEvent
    public static void damageEvent(final LivingDamageEvent event) {
        if (event.getEntity() instanceof Wolf wolf) {
            if (WolfArmorUtils.canArmorAbsorb(event.getSource(), wolf)) {
                event.setCanceled(true);
                ItemStack itemstack = WolfArmorUtils.getBodyArmorItem(wolf);
                int i = itemstack.getDamageValue();
                int j = itemstack.getMaxDamage();
                itemstack.hurtAndBreak(Mth.ceil(event.getAmount()), wolf, (p_148282_) -> {
                    p_148282_.broadcastBreakEvent(EquipmentSlot.CHEST);
                });
                WolfArmorUtils.setBodyArmorItem(wolf, itemstack);
                if (WolfCrackiness.WOLF_ARMOR.byDamage(i, j) != WolfCrackiness.WOLF_ARMOR.byDamage(WolfArmorUtils.getBodyArmorItem(wolf))) {
                    wolf.playSound(SoundEventRegistry.WOLF_ARMOR_CRACK.get());
                    if (event.getEntity().level() instanceof ServerLevel serverlevel) {
                        serverlevel.sendParticles(
                                new ItemParticleOption(ParticleTypes.ITEM, ItemRegistry.ARMADILLO_SCUTE.get().getDefaultInstance()),
                                wolf.getX(),
                                wolf.getY() + 1.0,
                                wolf.getZ(),
                                20,
                                0.2,
                                0.1,
                                0.2,
                                0.1
                        );
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void interactionEvent(final PlayerInteractEvent.EntityInteract event) {
        if (event.getTarget() instanceof Wolf wolf) {
            ItemStack itemstack = event.getItemStack();
            Player player = event.getEntity();
            if (itemstack.is(ItemRegistry.WOLF_ARMOR.get()) && wolf.isOwnedBy(player) && WolfArmorUtils.getBodyArmorItem(wolf).isEmpty() && !wolf.isBaby()) {
                WolfArmorUtils.setBodyArmorItem(wolf, itemstack.copyWithCount(1));
                Util.consume(1, player, itemstack);
                event.setCancellationResult(InteractionResult.SUCCESS);
                event.setCanceled(true);
                wolf.playSound(SoundEventRegistry.WOLF_ARMOR_EQUIP.get());
            } else if (itemstack.is(Tags.Items.SHEARS)
                    && wolf.isOwnedBy(player)
                    && WolfArmorUtils.hasArmor(wolf)
                    && (!EnchantmentHelper.hasBindingCurse(WolfArmorUtils.getBodyArmorItem(wolf)) || player.isCreative())) {
                itemstack.hurtAndBreak(1, player, (p_148282_) -> {
                    p_148282_.broadcastBreakEvent(event.getHand() == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
                });
                wolf.playSound(SoundEventRegistry.WOLF_ARMOR_UNEQUIP.get());
                ItemStack itemstack1 = WolfArmorUtils.getBodyArmorItem(wolf);
                WolfArmorUtils.setBodyArmorItem(wolf, ItemStack.EMPTY);
                wolf.spawnAtLocation(itemstack1);
                event.setCancellationResult(InteractionResult.SUCCESS);
                event.setCanceled(true);
            } else if (ArmorMaterials.ARMADILLO.getRepairIngredient().test(itemstack)
                    && wolf.isInSittingPose()
                    && WolfArmorUtils.hasArmor(wolf)
                    && wolf.isOwnedBy(player)
                    && WolfArmorUtils.getBodyArmorItem(wolf).isDamaged()) {
                itemstack.shrink(1);
                wolf.playSound(SoundEventRegistry.WOLF_ARMOR_REPAIR.get());
                ItemStack itemstack2 = WolfArmorUtils.getBodyArmorItem(wolf);

                int i = (int) ((float) itemstack2.getMaxDamage() * 0.125F);
                itemstack2.setDamageValue(Math.max(0, itemstack2.getDamageValue() - i));
                event.setCancellationResult(InteractionResult.SUCCESS);
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerJoin(final PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof Wolf wolf) {
            wolf.getCapability(WolfArmorCapabilityProvider.WOLF_ARMOR_CAPABILITY).ifPresent((capability) -> {
                FamiliarFacesNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> ((ServerPlayer) event.getEntity())), new SyncWolfArmorPacket(capability.getBodyArmorItem(), capability.getBodyArmorDropChance(), wolf));
            });
        }
    }

    @SubscribeEvent
    public static void onMobRemoved(final LivingDeathEvent event) {
        for (MobEffectInstance activeEffect : event.getEntity().getActiveEffects()) {
            if (activeEffect.getEffect() == MobEffectRegistry.WEAVING.get()) {
                MobEffectRegistry.WEAVING.get().onMobRemoved(event.getEntity(), activeEffect.getAmplifier(), Entity.RemovalReason.KILLED);
            }
            if (activeEffect.getEffect() == MobEffectRegistry.OOZING.get()) {
                MobEffectRegistry.OOZING.get().onMobRemoved(event.getEntity(), activeEffect.getAmplifier(), Entity.RemovalReason.KILLED);
            }
            if (activeEffect.getEffect() == MobEffectRegistry.WIND_CHARGED.get()) {
                MobEffectRegistry.WIND_CHARGED.get().onMobRemoved(event.getEntity(), activeEffect.getAmplifier(), Entity.RemovalReason.KILLED);
            }
        }
    }

    @SubscribeEvent
    public static void onMobHurt(final LivingHurtEvent event) {
        for (MobEffectInstance activeEffect : event.getEntity().getActiveEffects()) {
            if (activeEffect.getEffect() == MobEffectRegistry.INFESTED.get()) {
                MobEffectRegistry.INFESTED.get().onMobHurt(event.getEntity(), activeEffect.getAmplifier(), event.getSource(), event.getAmount());
            }
        }
    }

    @SubscribeEvent
    public static void entityDropLoot(final LivingDropsEvent event) {
        if (event.getEntity() instanceof Wolf wolf) {
            ItemStack itemstack = WolfArmorUtils.getBodyArmorItem(wolf);
            float f = WolfArmorUtils.getBodyArmorDropChance(wolf);
            boolean flag = f > 1.0F;
            if (!itemstack.isEmpty() && !EnchantmentHelper.hasVanishingCurse(itemstack) && (event.isRecentlyHit() | flag) && Math.max(wolf.getRandom().nextFloat() - (float) event.getLootingLevel() * 0.01F, 0.0F) < f) {
                if (!flag && itemstack.isDamageableItem()) {
                    itemstack.setDamageValue(itemstack.getMaxDamage() - wolf.getRandom().nextInt(1 + wolf.getRandom().nextInt(Math.max(itemstack.getMaxDamage() - 3, 1))));
                }

                wolf.spawnAtLocation(itemstack);
                WolfArmorUtils.setBodyArmorItem(wolf, ItemStack.EMPTY);
            }
        }
    }
}