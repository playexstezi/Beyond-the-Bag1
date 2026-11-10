package ru.beyondthebag.client.model;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import ru.beyondthebag.entity.MimicEntity;

/**
 * Vanilla player geometry with small mimic-specific gestures layered over
 * normal walking. Keeping the vanilla model preserves the complete skin UV
 * layout and all outer skin layers.
 */
public final class MimicPlayerModel extends PlayerModel<MimicEntity> {
    public MimicPlayerModel(ModelPart root) {
        super(root, false);
    }

    @Override
    public void setupAnim(MimicEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        int action = entity.getActionAnimation();
        int remaining = entity.getActionAnimationTicks();
        if (action == 0 || remaining <= 0) return;

        float progress = (10.0F - remaining) / 10.0F;
        float pulse = Mth.sin(progress * Mth.PI);
        float intensity = action == 1 ? 1.55F : action == 2 ? 1.25F : 0.95F;

        // A brief, imperfect imitation of swinging, mining or placing a block.
        this.rightArm.xRot -= intensity * pulse;
        this.leftArm.xRot -= (action == 2 ? 0.22F : 0.10F) * pulse;
        this.rightArm.zRot += 0.12F * pulse;
    }
}
