package io.github.itskillerluc.familiarfaces.server.events;

import io.github.itskillerluc.familiarfaces.FamiliarFaces;
import io.github.itskillerluc.familiarfaces.server.capability.IWolfArmorCapability;
import io.github.itskillerluc.familiarfaces.server.entities.Armadillo;
import io.github.itskillerluc.familiarfaces.server.entities.Bogged;
import io.github.itskillerluc.familiarfaces.server.entities.Breeze;
import io.github.itskillerluc.familiarfaces.server.init.EntityTypeRegistry;
import io.github.itskillerluc.familiarfaces.server.init.ItemRegistry;
import io.github.itskillerluc.familiarfaces.server.init.PotionRegistry;
import io.github.itskillerluc.familiarfaces.server.networking.FamiliarFacesNetwork;
import net.minecraft.core.Position;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.core.dispenser.AbstractProjectileDispenseBehavior;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod.EventBusSubscriber(modid = FamiliarFaces.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEvents {
    @SubscribeEvent
    public static void entityAttributeCreation(final EntityAttributeCreationEvent event) {
        event.put(EntityTypeRegistry.BOGGED.get(), Bogged.createAttributes().build());
        event.put(EntityTypeRegistry.BREEZE.get(), Breeze.createAttributes().build());
        event.put(EntityTypeRegistry.ARMADILLO.get(), Armadillo.createAttributes().build());
    }

    @SubscribeEvent
    public static void registerItemsToTab(final BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(ItemRegistry.BOGGED_SPAWN_EGG);
            event.accept(ItemRegistry.BREEZE_SPAWN_EGG);
            event.accept(ItemRegistry.ARMADILLO_SPAWN_EGG);
        }
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(ItemRegistry.WIND_CHARGE);
            event.accept(ItemRegistry.WOLF_ARMOR);
        }
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(ItemRegistry.BREEZE_ROD);
            event.accept(ItemRegistry.ARMADILLO_SCUTE);
        }
    }

    @SubscribeEvent
    public static void registerSpawnPlacements(final SpawnPlacementRegisterEvent event) {
        event.register(EntityTypeRegistry.ARMADILLO.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Armadillo::checkArmadilloSpawnRules, SpawnPlacementRegisterEvent.Operation.REPLACE);
        event.register(EntityTypeRegistry.BOGGED.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Bogged::checkMonsterSpawnRules, SpawnPlacementRegisterEvent.Operation.REPLACE);
        event.register(EntityTypeRegistry.BREEZE.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Breeze::checkMonsterSpawnRules, SpawnPlacementRegisterEvent.Operation.REPLACE);
    }

    @SubscribeEvent
    public static void registerDispenserBehaviour(final FMLCommonSetupEvent event) {
        FamiliarFacesNetwork.register();

        DispenserBlock.registerBehavior(ItemRegistry.WIND_CHARGE.get(), new AbstractProjectileDispenseBehavior() {
            @Override
            protected Projectile getProjectile(Level pLevel, Position pPosition, ItemStack pStack) {
                return ItemRegistry.WIND_CHARGE.get().asProjectile(pLevel, pPosition, pStack);
            }
        });

        event.enqueueWork(() -> {
            CauldronInteraction.WATER.put(ItemRegistry.WOLF_ARMOR.get(), CauldronInteraction.DYED_ITEM);

            BrewingRecipeRegistry.addRecipe(Ingredient.of(PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.WATER)), Ingredient.of(ItemRegistry.BREEZE_ROD.get()), PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.MUNDANE));
            BrewingRecipeRegistry.addRecipe(Ingredient.of(PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.AWKWARD)), Ingredient.of(ItemRegistry.BREEZE_ROD.get()), PotionUtils.setPotion(new ItemStack(Items.POTION), PotionRegistry.WIND_CHARGED.get()));
            BrewingRecipeRegistry.addRecipe(Ingredient.of(PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.WATER)), Ingredient.of(Items.SLIME_BLOCK), PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.MUNDANE));
            BrewingRecipeRegistry.addRecipe(Ingredient.of(PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.AWKWARD)), Ingredient.of(Items.SLIME_BLOCK), PotionUtils.setPotion(new ItemStack(Items.POTION), PotionRegistry.OOZING.get()));
            BrewingRecipeRegistry.addRecipe(Ingredient.of(PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.WATER)), Ingredient.of(Items.STONE), PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.MUNDANE));
            BrewingRecipeRegistry.addRecipe(Ingredient.of(PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.AWKWARD)), Ingredient.of(Items.STONE), PotionUtils.setPotion(new ItemStack(Items.POTION), PotionRegistry.INFESTED.get()));
            BrewingRecipeRegistry.addRecipe(Ingredient.of(PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.WATER)), Ingredient.of(Items.COBWEB), PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.MUNDANE));
            BrewingRecipeRegistry.addRecipe(Ingredient.of(PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.AWKWARD)), Ingredient.of(Items.COBWEB), PotionUtils.setPotion(new ItemStack(Items.POTION), PotionRegistry.WEAVING.get()));
        });
    }

    @SubscribeEvent
    public static void capabilityEvent(final RegisterCapabilitiesEvent event) {
        event.register(IWolfArmorCapability.class);
    }
}