package io.github.itskillerluc.familiarfaces.client.renderers.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.itskillerluc.familiarfaces.FamiliarFaces;
import io.github.itskillerluc.familiarfaces.client.models.entity.BreezeModel;
import io.github.itskillerluc.familiarfaces.server.entities.Breeze;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public class BreezeWindLayer extends RenderLayer<Breeze, BreezeModel<Breeze>> {
    public static final ModelLayerLocation BREEZE_WIND = new ModelLayerLocation(new ResourceLocation(FamiliarFaces.MODID, "breeze_wind"), "main");

    private static final ResourceLocation TEXTURE_LOCATION = new ResourceLocation(FamiliarFaces.MODID, "textures/entity/breeze/breeze_wind.png");
    private final BreezeModel<Breeze> model;

    public BreezeWindLayer(EntityRendererProvider.Context context, RenderLayerParent<Breeze, BreezeModel<Breeze>> renderer) {
        super(renderer);
        this.model = new BreezeModel<>(context.bakeLayer(BREEZE_WIND));
    }

    public void render(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            Breeze livingEntity,
            float limbSwing,
            float limbSwingAmount,
            float partialTick,
            float ageInTicks,
            float netHeadYaw,
            float headPitch
    ) {
        float f = (float) livingEntity.tickCount + partialTick;
        VertexConsumer vertexconsumer = bufferSource.getBuffer(WindChargeRenderer.breezeWind(TEXTURE_LOCATION, this.xOffset(f) % 1.0F, 0.0F));
        this.model.setupAnim(livingEntity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        BreezeRenderer.enable(this.model, this.model.wind()).renderToBuffer(poseStack, vertexconsumer, packedLight, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);
    }

    private float xOffset(float tickCount) {
        return tickCount * 0.02F;
    }
}