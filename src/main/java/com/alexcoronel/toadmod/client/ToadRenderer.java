package com.alexcoronel.toadmod.client;

import com.alexcoronel.toadmod.ToadCapColor;
import com.alexcoronel.toadmod.ToadMod;
import com.alexcoronel.toadmod.entity.ToadEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class ToadRenderer extends MobRenderer<ToadEntity, ToadModel> {
    /** The model is authored at 2 texels per pixel (31 units tall); 0.625 makes toads about 1.2 blocks tall. */
    private static final float MODEL_SCALE = 0.625F;
    private static final float SHADOW = 0.4F;
    private static final Map<ToadCapColor, ResourceLocation> TEXTURES = new EnumMap<>(ToadCapColor.class);

    static {
        for (ToadCapColor color : ToadCapColor.values()) {
            TEXTURES.put(color, ToadMod.id("textures/entity/toad/toad_" + color.getSerializedName() + ".png"));
        }
    }

    public ToadRenderer(EntityRendererProvider.Context context) {
        super(context, new ToadModel(context.bakeLayer(ToadModel.LAYER)), SHADOW);
    }

    @Override
    public ResourceLocation getTextureLocation(ToadEntity toad) {
        return TEXTURES.get(toad.getCapColor());
    }

    @Override
    protected void scale(ToadEntity toad, PoseStack poseStack, float partialTick) {
        float scale = MODEL_SCALE * (toad.isBaby() ? 0.5F : 1.0F);
        poseStack.scale(scale, scale, scale);
        this.shadowRadius = SHADOW * scale / MODEL_SCALE;
    }
}
