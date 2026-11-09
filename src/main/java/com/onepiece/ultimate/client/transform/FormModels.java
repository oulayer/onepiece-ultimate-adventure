package com.onepiece.ultimate.client.transform;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Extra geometry worn by the Gomu forms, in head/body model space (pixels, y down, -z = face):
 * Snakeman's swept-back spiky hair, Gear Fifth's fluffy cloud hair and the puff used for its cloud collar.
 */
final class FormModels {
    private static ModelPart spikyHair, cloudHair, puff;

    static ModelPart spikyHair() {
        if (spikyHair == null) {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = mesh.getRoot();
            int n = 0;
            // two rows of spikes from the crown, tilted up (+xRot) and fanned out (yRot)
            float[][] spikes = {
                    {-3, -2, 0.55f, -0.35f, 7}, {-1, -2.5f, 0.7f, -0.1f, 8}, {1, -2.5f, 0.7f, 0.1f, 8}, {3, -2, 0.55f, 0.35f, 7},
                    {-2.5f, 0.5f, 0.35f, -0.5f, 7}, {0, 0.5f, 0.45f, 0, 8}, {2.5f, 0.5f, 0.35f, 0.5f, 7},
                    {-4, -3.5f, 0.9f, -0.9f, 5}, {4, -3.5f, 0.9f, 0.9f, 5}, {0, -4, 1.1f, 0, 4},
            };
            for (float[] s : spikes) {
                root.addOrReplaceChild("spike" + n++, CubeListBuilder.create().addBox(-0.9f, -0.9f, 0, 1.8f, 1.8f, s[4]),
                        PartPose.offsetAndRotation(s[0], -7.6f, s[1], s[2], s[3], 0));
            }
            spikyHair = LayerDefinition.create(mesh, 16, 16).bakeRoot();
        }
        return spikyHair;
    }

    static ModelPart cloudHair() {
        if (cloudHair == null) {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = mesh.getRoot();
            // {x, y, z, size}: a crown of overlapping puffs that billows out above and behind the head
            float[][] puffs = {
                    {0, -9.5f, 0, 6}, {-3.2f, -9, -1.5f, 5}, {3.2f, -9, -1.5f, 5}, {-3.5f, -8.5f, 2.5f, 5}, {3.5f, -8.5f, 2.5f, 5},
                    {0, -9, 3.5f, 5.5f}, {-4.6f, -6, 0.5f, 4}, {4.6f, -6, 0.5f, 4}, {0, -6.5f, 5, 4.5f}, {0, -11, 1.5f, 4},
                    {-2, -8.5f, -3.8f, 3.5f}, {2, -8.5f, -3.8f, 3.5f},
            };
            int n = 0;
            for (float[] p : puffs) {
                float h = p[3] / 2;
                root.addOrReplaceChild("puff" + n++, CubeListBuilder.create().addBox(-h, -h, -h, p[3], p[3], p[3]),
                        PartPose.offsetAndRotation(p[0], p[1], p[2], 0.3f * n, 0.5f * n, 0));
            }
            cloudHair = LayerDefinition.create(mesh, 16, 16).bakeRoot();
        }
        return cloudHair;
    }

    static ModelPart puff() {
        if (puff == null) {
            MeshDefinition mesh = new MeshDefinition();
            mesh.getRoot().addOrReplaceChild("puff", CubeListBuilder.create().addBox(-1.5f, -1.5f, -1.5f, 3, 3, 3), PartPose.ZERO);
            puff = LayerDefinition.create(mesh, 16, 16).bakeRoot().getChild("puff");
        }
        return puff;
    }

    private FormModels() {}
}
