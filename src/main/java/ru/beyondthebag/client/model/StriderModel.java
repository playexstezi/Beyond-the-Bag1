package ru.beyondthebag.client.model;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import ru.beyondthebag.BeyondTheBag;
import ru.beyondthebag.entity.StriderEntity;

/** Custom silhouette for the Strider anomaly. */
public class StriderModel extends HierarchicalModel<StriderEntity> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(new ResourceLocation(BeyondTheBag.MOD_ID, "strider_anomaly"), "main");

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;

    public StriderModel(ModelPart root) {
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
        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-5.0F, -8.0F, -3.0F, 10.0F, 15.0F, 6.0F,
                                new CubeDeformation(0.0F))
                        .texOffs(0, 22).addBox(-4.0F, -7.0F, -4.0F, 8.0F, 13.0F, 1.0F,
                                new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 5.0F, 0.0F));

        body.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(32, 0).addBox(-4.0F, -7.0F, -4.0F, 8.0F, 8.0F, 8.0F)
                        .texOffs(32, 16).addBox(-2.0F, -4.0F, -6.0F, 4.0F, 2.0F, 2.0F)
                        .texOffs(0, 38).addBox(-5.0F, -8.0F, -1.0F, 2.0F, 3.0F, 2.0F)
                        .texOffs(8, 38).addBox(3.0F, -8.0F, -1.0F, 2.0F, 3.0F, 2.0F),
                PartPose.offset(0.0F, -7.0F, 0.0F));

        body.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(20, 22).addBox(-2.0F, -1.0F, -2.0F, 4.0F, 13.0F, 4.0F),
                PartPose.offset(6.0F, -5.0F, 0.0F));
        body.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(20, 22).addBox(-2.0F, -1.0F, -2.0F, 4.0F, 13.0F, 4.0F),
                PartPose.offset(-6.0F, -5.0F, 0.0F));

        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(48, 26).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 10.0F, 4.0F),
                PartPose.offset(2.5F, 14.0F, 0.0F));
        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(48, 26).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 10.0F, 4.0F),
                PartPose.offset(-2.5F, 14.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(StriderEntity entity, float limbSwing, float limbSwingAmount,
            float ageInTicks, float netHeadYaw, float headPitch) {
        head.yRot = netHeadYaw * ((float)Math.PI / 180F);
        head.xRot = headPitch * ((float)Math.PI / 180F);
        leftLeg.xRot = Mth.cos(limbSwing * 0.6662F) * 1.2F * limbSwingAmount;
        rightLeg.xRot = Mth.cos(limbSwing * 0.6662F + (float)Math.PI) * 1.2F * limbSwingAmount;
        leftArm.xRot = Mth.cos(limbSwing * 0.6662F + (float)Math.PI) * 0.8F * limbSwingAmount;
        rightArm.xRot = Mth.cos(limbSwing * 0.6662F) * 0.8F * limbSwingAmount;
        body.y = 5.0F + Mth.cos(ageInTicks * 0.08F) * 0.35F;
    }
}
