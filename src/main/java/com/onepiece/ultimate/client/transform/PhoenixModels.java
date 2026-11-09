package com.onepiece.ultimate.client.transform;

import com.onepiece.ultimate.OnePieceMod;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

/** Geometry of the phoenix: flame wings for the hybrid form and the whole bird for the full form (pixels, y up). */
final class PhoenixModels {
    static final ResourceLocation TEXTURE = OnePieceMod.id("textures/entity/phoenix.png");
    private static ModelPart wing;
    private static ModelPart bird;

    static ModelPart wing() {
        if (wing == null) {
            MeshDefinition mesh = new MeshDefinition();
            mesh.getRoot().addOrReplaceChild("wing", CubeListBuilder.create().texOffs(0, 20).addBox(-0.5f, 0.5f, 1.5f, 1, 13, 15), PartPose.ZERO);
            wing = LayerDefinition.create(mesh, 64, 64).bakeRoot().getChild("wing");
        }
        return wing;
    }

    static ModelPart bird() {
        if (bird == null) {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = mesh.getRoot();
            PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-4, -3, -7, 8, 6, 14)
                    .texOffs(0, 0).addBox(-3, -3.5f, -6, 6, 1, 11), PartPose.ZERO);
            PartDefinition neck = body.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(28, 0).addBox(-2, -2, 0, 4, 4, 5),
                    PartPose.offsetAndRotation(0, 1, 6, -0.45f, 0, 0));
            PartDefinition head = neck.addOrReplaceChild("head", CubeListBuilder.create().texOffs(28, 0).addBox(-2.5f, -2.5f, -1, 5, 5, 6),
                    PartPose.offsetAndRotation(0, 0, 5, 0.45f, 0, 0));
            head.addOrReplaceChild("beak", CubeListBuilder.create().texOffs(48, 0).addBox(-1, -1.5f, 5, 2, 2, 3)
                    .texOffs(48, 0).addBox(-0.5f, -1.8f, 7.5f, 1, 1, 1.5f), PartPose.ZERO);
            // three swept-back crest plumes
            for (int i = -1; i <= 1; i++) {
                head.addOrReplaceChild("crest" + (i + 1), CubeListBuilder.create().texOffs(0, 40).addBox(-0.5f, 0, -8, 1, 1, 9),
                        PartPose.offsetAndRotation(i * 1.2f, 2.3f, 1, 0.35f + Math.abs(i) * 0.1f, i * 0.25f, 0));
            }
            for (int side : new int[]{-1, 1}) {
                String n = side < 0 ? "wing_r" : "wing_l";
                float dir = side;
                // inner wing (arm), outer wing (hand) and a fringe of long primary feathers on each segment
                PartDefinition inner = body.addOrReplaceChild(n, CubeListBuilder.create().texOffs(0, 20)
                        .addBox(side < 0 ? -16 : 0, -0.5f, -6, 16, 1, 13), PartPose.offset(side * 4, 2, 2));
                for (int f = 0; f < 4; f++) {
                    inner.addOrReplaceChild("f" + f, CubeListBuilder.create().texOffs(0, 32).addBox(-1.5f, -0.4f, -9, 3, 0.8f, 9),
                            PartPose.offsetAndRotation(dir * (2 + f * 4), 0.1f, -5.5f, 0, -dir * 0.06f * f, 0));
                }
                PartDefinition tip = inner.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(0, 20)
                        .addBox(side < 0 ? -15 : 0, -0.5f, -5, 15, 1, 10), PartPose.offset(dir * 16, 0, -0.5f));
                for (int f = 0; f < 5; f++) {
                    tip.addOrReplaceChild("p" + f, CubeListBuilder.create().texOffs(0, 52).addBox(-1.2f, -0.4f, -12 + f, 2.4f, 0.8f, 12 - f),
                            PartPose.offsetAndRotation(dir * (3 + f * 3), 0.1f, -4.5f, 0, -dir * (0.12f + 0.12f * f), 0));
                }
            }
            // long flowing tail: a fan of five plumes, each with a hinged second segment that trails in the wind
            PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(0, 40).addBox(-2.5f, -0.5f, -6, 5, 1, 6),
                    PartPose.offset(0, 1, -7));
            for (int i = 0; i < 5; i++) {
                float yaw = (i - 2) * 0.22f;
                PartDefinition plume = tail.addOrReplaceChild("plume" + i, CubeListBuilder.create().texOffs(0, 40).addBox(-1.2f, -0.4f, -14, 2.4f, 0.8f, 14),
                        PartPose.offsetAndRotation((i - 2) * 1.0f, 0, -5, 0, yaw, 0));
                plume.addOrReplaceChild("end", CubeListBuilder.create().texOffs(0, 52).addBox(-1.6f, -0.4f, -12, 3.2f, 0.8f, 12),
                        PartPose.offset(0, 0, -14));
            }
            body.addOrReplaceChild("leg_r", CubeListBuilder.create().texOffs(48, 8).addBox(-1, -5, -1, 2, 5, 2)
                    .texOffs(48, 8).addBox(-1.5f, -6, -2.5f, 3, 1, 4), PartPose.offset(-2, -3, 1));
            body.addOrReplaceChild("leg_l", CubeListBuilder.create().texOffs(48, 8).addBox(-1, -5, -1, 2, 5, 2)
                    .texOffs(48, 8).addBox(-1.5f, -6, -2.5f, 3, 1, 4), PartPose.offset(2, -3, 1));
            bird = LayerDefinition.create(mesh, 64, 64).bakeRoot();
        }
        return bird;
    }

    private PhoenixModels() {}
}
