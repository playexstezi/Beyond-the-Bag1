package ru.beyondthebag.client.model;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import ru.beyondthebag.BeyondTheBag;
import ru.beyondthebag.entity.GuardianEntity;

public final class GuardianModel extends HierarchicalModel<GuardianEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            new ResourceLocation(BeyondTheBag.MOD_ID, "guardian"), "main");
    private final ModelPart root, body, head, leftArm, rightArm, leftLeg, rightLeg;

    public GuardianModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.head = body.getChild("head");
        this.leftArm = body.getChild("left_arm");
        this.rightArm = body.getChild("right_arm");
        this.leftLeg = root.getChild("left_leg");
        this.rightLeg = root.getChild("right_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0,0).addBox(-7,-9,-4,14,17,8).texOffs(0,26).addBox(-6,-8,-5,12,15,2),
                PartPose.offset(0, 5, 0));
        body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(36,0).addBox(-5,-7,-5,10,9,10).texOffs(36,20).addBox(-3,-3,-7,6,3,2), PartPose.offset(0, -7, 0));
        body.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(20, 32).addBox(-2,-1,-2,4,13,4),
                PartPose.offset(6,-5,0));
        body.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(20, 32).addBox(-2,-1,-2,4,13,4),
                PartPose.offset(-6,-5,0));
        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(48, 32).addBox(-2,0,-2,4,10,4),
                PartPose.offset(2.5F,14,0));
        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(48, 32).addBox(-2,0,-2,4,10,4),
                PartPose.offset(-2.5F,14,0));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override public ModelPart root() { return root; }

    @Override
    public void setupAnim(GuardianEntity entity, float limbSwing, float limbSwingAmount,
            float ageInTicks, float netHeadYaw, float headPitch) {
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        head.xRot = headPitch * Mth.DEG_TO_RAD;
        leftLeg.xRot = Mth.cos(limbSwing * 0.6662F) * 1.2F * limbSwingAmount;
        rightLeg.xRot = Mth.cos(limbSwing * 0.6662F + Mth.PI) * 1.2F * limbSwingAmount;
        leftArm.xRot = Mth.cos(limbSwing * 0.6662F + Mth.PI) * 0.8F * limbSwingAmount;
        rightArm.xRot = Mth.cos(limbSwing * 0.6662F) * 0.8F * limbSwingAmount;
        body.y = 5.0F + Mth.cos(ageInTicks * 0.12F) * 0.4F;
    }
}
