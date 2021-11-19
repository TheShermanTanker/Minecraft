package net.minecraft.util;

import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.particles.ParticleParam;
import net.minecraft.util.valueproviders.IntProviderUniform;
import net.minecraft.world.level.World;
import net.minecraft.world.phys.Vec3D;

public class ParticleUtils {
    public static void spawnParticlesOnBlockFaces(World world, BlockPosition pos, ParticleParam effect, IntProviderUniform range) {
        for(EnumDirection direction : EnumDirection.values()) {
            int i = range.sample(world.random);

            for(int j = 0; j < i; ++j) {
                spawnParticleOnFace(world, pos, direction, effect);
            }
        }

    }

    public static void spawnParticlesAlongAxis(EnumDirection.EnumAxis axis, World world, BlockPosition pos, double variance, ParticleParam effect, IntProviderUniform range) {
        Vec3D vec3 = Vec3D.atCenterOf(pos);
        boolean bl = axis == EnumDirection.EnumAxis.X;
        boolean bl2 = axis == EnumDirection.EnumAxis.Y;
        boolean bl3 = axis == EnumDirection.EnumAxis.Z;
        int i = range.sample(world.random);

        for(int j = 0; j < i; ++j) {
            double d = vec3.x + MathHelper.nextDouble(world.random, -1.0D, 1.0D) * (bl ? 0.5D : variance);
            double e = vec3.y + MathHelper.nextDouble(world.random, -1.0D, 1.0D) * (bl2 ? 0.5D : variance);
            double f = vec3.z + MathHelper.nextDouble(world.random, -1.0D, 1.0D) * (bl3 ? 0.5D : variance);
            double g = bl ? MathHelper.nextDouble(world.random, -1.0D, 1.0D) : 0.0D;
            double h = bl2 ? MathHelper.nextDouble(world.random, -1.0D, 1.0D) : 0.0D;
            double k = bl3 ? MathHelper.nextDouble(world.random, -1.0D, 1.0D) : 0.0D;
            world.addParticle(effect, d, e, f, g, h, k);
        }

    }

    public static void spawnParticleOnFace(World world, BlockPosition pos, EnumDirection direction, ParticleParam effect) {
        Vec3D vec3 = Vec3D.atCenterOf(pos);
        int i = direction.getAdjacentX();
        int j = direction.getAdjacentY();
        int k = direction.getAdjacentZ();
        double d = vec3.x + (i == 0 ? MathHelper.nextDouble(world.random, -0.5D, 0.5D) : (double)i * 0.55D);
        double e = vec3.y + (j == 0 ? MathHelper.nextDouble(world.random, -0.5D, 0.5D) : (double)j * 0.55D);
        double f = vec3.z + (k == 0 ? MathHelper.nextDouble(world.random, -0.5D, 0.5D) : (double)k * 0.55D);
        double g = i == 0 ? MathHelper.nextDouble(world.random, -1.0D, 1.0D) : 0.0D;
        double h = j == 0 ? MathHelper.nextDouble(world.random, -1.0D, 1.0D) : 0.0D;
        double l = k == 0 ? MathHelper.nextDouble(world.random, -1.0D, 1.0D) : 0.0D;
        world.addParticle(effect, d, e, f, g, h, l);
    }
}
