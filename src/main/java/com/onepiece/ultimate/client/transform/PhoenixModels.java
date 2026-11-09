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
            PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-4, -3, -7, 8, 6, 14), PartPose.ZERO);
            PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(28, 0).addBox(-2.5f, -2.5f, -2.5f, 5, 5, 5),
                    PartPose.offset(0, 3.5f, 9));
            head.addOrReplaceChild("beak", CubeListBuilder.create().texOffs(48, 0).addBox(-1, -1, 2.5f, 2, 2, 4), PartPose.ZERO);
            head.addOrReplaceChild("crest", CubeListBuilder.create().texOffs(0, 40).addBox(-0.5f, 2.5f, -5, 1, 5, 7), PartPose.rotation(-0.4f, 0, 0));
            PartDefinition wr = body.addOrReplaceChild("wing_r", CubeListBuilder.create().texOffs(0, 20).addBox(-20, -0.5f, -7, 20, 1, 14),
                    PartPose.offset(-4, 2, 2));
            wr.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(0, 20).addBox(-14, -0.5f, -5, 14, 1, 10), PartPose.offset(-20, 0, -1));
            PartDefinition wl = body.addOrReplaceChild("wing_l", CubeListBuilder.create().texOffs(0, 20).addBox(0, -0.5f, -7, 20, 1, 14),
                    PartPose.offset(4, 2, 2));
            wl.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(0, 20).addBox(0, -0.5f, -5, 14, 1, 10), PartPose.offset(20, 0, -1));
            PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(0, 40).addBox(-2, -0.5f, -20, 4, 1, 20),
                    PartPose.offset(0, 1, -7));
            tail.addOrReplaceChild("left", CubeListBuilder.create().texOffs(0, 40).addBox(-1.5f, -0.5f, -17, 3, 1, 17), PartPose.rotation(0, -0.4f, 0));
            tail.addOrReplaceChild("right", CubeListBuilder.create().texOffs(0, 40).addBox(-1.5f, -0.5f, -17, 3, 1, 17), PartPose.rotation(0, 0.4f, 0));
            body.addOrReplaceChild("leg_r", CubeListBuilder.create().texOffs(48, 8).addBox(-1, -5, -1, 2, 5, 2), PartPose.offset(-2, -3, 1));
            body.addOrReplaceChild("leg_l", CubeListBuilder.create().texOffs(48, 8).addBox(-1, -5, -1, 2, 5, 2), PartPose.offset(2, -3, 1));
            bird = LayerDefinition.create(mesh, 64, 64).bakeRoot();
        }
        return bird;
    }

    private PhoenixModels() {}
}
