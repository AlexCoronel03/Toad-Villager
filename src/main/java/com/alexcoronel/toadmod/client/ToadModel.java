package com.alexcoronel.toadmod.client;

import com.alexcoronel.toadmod.ToadMod;
import com.alexcoronel.toadmod.entity.ToadEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.util.Mth;

/**
 * Custom Toad model (geometry in toad.geo.json). Procedural animation: idle breathing/sway, walk, flee,
 * trade, hurt and death. The cap is a child of the head, so it stays rigidly attached.
 */
public class ToadModel extends HierarchicalModel<ToadEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ToadMod.id("toad"), "main");

    /** The reference design holds the arms out in a slight A-pose. */
    private static final float ARM_SPLAY = 0.42F;

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    private final float headY;
    private final float armY;

    public ToadModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.head = root.getChild("head");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
        this.headY = this.head.y;
        this.armY = this.rightArm.y;
    }

    public static LayerDefinition createBodyLayer() {
        return ToadGeometry.load();
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public void setupAnim(ToadEntity toad, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        boolean fleeing = toad.isFleeing();
        boolean trading = toad.isBeingTradedWith();
        boolean dying = toad.deathTime > 0;
        boolean hurt = toad.hurtTime > 0;

        // --- head (cap follows, being its child) ---
        this.head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        this.head.xRot = headPitch * Mth.DEG_TO_RAD;
        this.head.zRot = 0.0F;
        this.head.y = this.headY;
        if (toad.getUnhappyCounter() > 0) { // "no" head shake when a trade is refused
            this.head.zRot = 0.3F * Mth.sin(0.45F * ageInTicks);
            this.head.xRot = 0.4F;
        }

        // --- idle: breathing + gentle arm sway ---
        float breathe = Mth.sin(ageInTicks * 0.09F);
        this.head.y += breathe * 0.24F;
        this.body.xRot = 0.0F;
        this.rightArm.y = this.armY + breathe * 0.2F;
        this.leftArm.y = this.armY + breathe * 0.2F;
        this.rightArm.xRot = 0.0F;
        this.leftArm.xRot = 0.0F;
        this.rightArm.zRot = ARM_SPLAY + Mth.cos(ageInTicks * 0.067F) * 0.03F;
        this.leftArm.zRot = -ARM_SPLAY - Mth.cos(ageInTicks * 0.067F) * 0.03F;
        this.rightArm.yRot = 0.0F;
        this.leftArm.yRot = 0.0F;

        // --- walking: legs and arms swing in opposition ---
        float swing = limbSwing * 0.6662F;
        float amount = Math.min(limbSwingAmount, 1.0F);
        this.rightLeg.xRot = Mth.cos(swing) * 1.3F * amount;
        this.leftLeg.xRot = Mth.cos(swing + Mth.PI) * 1.3F * amount;
        this.rightLeg.yRot = 0.0F;
        this.leftLeg.yRot = 0.0F;
        this.rightArm.xRot += Mth.cos(swing + Mth.PI) * 1.0F * amount;
        this.leftArm.xRot += Mth.cos(swing) * 1.0F * amount;

        // --- fleeing: lean forward, arms thrown up and flapping ---
        if (fleeing) {
            this.body.xRot = 0.22F;
            this.head.xRot += 0.15F;
            float flap = Mth.sin(ageInTicks * 0.95F) * 0.35F;
            this.rightArm.xRot = -2.5F + flap;
            this.leftArm.xRot = -2.5F - flap;
            this.rightArm.zRot = 0.6F;
            this.leftArm.zRot = -0.6F;
        }

        // --- trading: arms offered forward, small nod ---
        if (trading) {
            this.rightArm.xRot = -0.85F;
            this.leftArm.xRot = -0.85F;
            this.rightArm.yRot = -0.25F;
            this.leftArm.yRot = 0.25F;
            this.head.xRot += Mth.sin(ageInTicks * 0.12F) * 0.06F;
        }

        // --- hurt flinch (the red overlay comes from the vanilla renderer) ---
        if (hurt) {
            float t = toad.hurtTime / 10.0F;
            this.body.xRot -= 0.3F * t;
            this.head.xRot -= 0.25F * t;
            this.rightArm.xRot = -1.2F * t;
            this.leftArm.xRot = -1.2F * t;
            this.rightArm.zRot = 0.5F * t;
            this.leftArm.zRot = -0.5F * t;
        }

        // --- dying: arms flung up (the renderer tips the whole body over) ---
        if (dying) {
            float flail = Mth.sin(ageInTicks * 0.8F) * 0.4F;
            this.rightArm.xRot = -2.8F + flail;
            this.leftArm.xRot = -2.8F - flail;
            this.rightArm.zRot = 0.4F;
            this.leftArm.zRot = -0.4F;
            this.head.xRot = -0.35F;
        }
    }
}
