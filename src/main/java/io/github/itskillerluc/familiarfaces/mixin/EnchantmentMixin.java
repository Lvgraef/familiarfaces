package io.github.itskillerluc.familiarfaces.mixin;

import com.github.alexthe668.domesticationinnovation.server.enchantment.PetEnchantment;
import io.github.itskillerluc.familiarfaces.server.init.ItemRegistry;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Enchantment.class)
@Debug(export = true)
public class EnchantmentMixin {
    @Inject(method = "canEnchant" , at = @At("HEAD"), cancellable = true)
    private void familiar_faces$canEnchant(ItemStack pStack, CallbackInfoReturnable<Boolean> cir) {
        if ((Enchantment) (Object) this instanceof PetEnchantment) {
            if (pStack.getItem().equals(ItemRegistry.WOLF_ARMOR.get())) {
                cir.setReturnValue(true);
                cir.cancel();
            }
        }
    }
}