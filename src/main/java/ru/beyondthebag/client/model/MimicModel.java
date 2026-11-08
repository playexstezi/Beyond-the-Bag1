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
import ru.beyondthebag.entity.MimicEntity;

public final class MimicModel extends HierarchicalModel<MimicEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            new ResourceLocation(BeyondTheBag.MOD_ID, "mimic"), "main");
    private final ModelPart root, body, head, leftArm, rightArm, leftLeg, rightLeg;

    public MimicModel(ModelPart root) {
        this.root=root; body=root.getChild("body"); head=body.getChild("head");
        leftArm=body.getChild("left_arm"); rightArm=body.getChild("right_arm");
        leftLeg=root.getChild("left_leg"); rightLeg=root.getChild("right_leg");
    }
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh=new MeshDefinition(); PartDefinition root=mesh.getRoot();
        PartDefinition body=root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(16,16).addBox(-4,-6,-2,8,12,4),
                PartPose.offset(0,5,0));
        body.addOrReplaceChild("head",CubeListBuilder.create().texOffs(0,0).addBox(-4,-8,-4,8,8,8),PartPose.offset(0,-7,0));
        body.addOrReplaceChild("left_arm",CubeListBuilder.create().texOffs(40,16).addBox(-1,-2,-2,3,13,4),PartPose.offset(5,-5,0));
        body.addOrReplaceChild("right_arm",CubeListBuilder.create().texOffs(40,16).addBox(-2,-2,-2,3,13,4),PartPose.offset(-5,-5,0));
        root.addOrReplaceChild("left_leg",CubeListBuilder.create().texOffs(0,16).addBox(-2,0,-2,4,12,4),PartPose.offset(2,12,0));
        root.addOrReplaceChild("right_leg",CubeListBuilder.create().texOffs(0,16).addBox(-2,0,-2,4,12,4),PartPose.offset(-2,12,0));
        return LayerDefinition.create(mesh,64,64);
    }
    @Override public ModelPart root(){return root;}
    @Override public void setupAnim(MimicEntity entity,float limbSwing,float limbSwingAmount,float ageInTicks,float netHeadYaw,float headPitch){
        head.yRot=netHeadYaw*Mth.DEG_TO_RAD; head.xRot=headPitch*Mth.DEG_TO_RAD;
        leftLeg.xRot=Mth.cos(limbSwing*.6662F)*1.2F*limbSwingAmount;
        rightLeg.xRot=Mth.cos(limbSwing*.6662F+Mth.PI)*1.2F*limbSwingAmount;
        leftArm.xRot=Mth.cos(limbSwing*.6662F+Mth.PI)*.7F*limbSwingAmount;
        rightArm.xRot=Mth.cos(limbSwing*.6662F)*.7F*limbSwingAmount;
        body.y=5+Mth.cos(ageInTicks*.08F)*.15F;
    }
}
