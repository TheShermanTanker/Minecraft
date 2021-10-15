package net.minecraft.world.level.dimension.end;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EntityEnderCrystal;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.levelgen.feature.WorldGenEnder;
import net.minecraft.world.level.levelgen.feature.WorldGenerator;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureEndSpikeConfiguration;

public enum EnumDragonRespawn {
    START {
        @Override
        public void tick(WorldServer world, EnderDragonBattle fight, List<EntityEnderCrystal> crystals, int i, BlockPosition pos) {
            BlockPosition blockPos = new BlockPosition(0, 128, 0);

            for(EntityEnderCrystal endCrystal : crystals) {
                endCrystal.setBeamTarget(blockPos);
            }

            fight.setRespawnPhase(PREPARING_TO_SUMMON_PILLARS);
        }
    },
    PREPARING_TO_SUMMON_PILLARS {
        @Override
        public void tick(WorldServer world, EnderDragonBattle fight, List<EntityEnderCrystal> crystals, int i, BlockPosition pos) {
            if (i < 100) {
                if (i == 0 || i == 50 || i == 51 || i == 52 || i >= 95) {
                    world.triggerEffect(3001, new BlockPosition(0, 128, 0), 0);
                }
            } else {
                fight.setRespawnPhase(SUMMONING_PILLARS);
            }

        }
    },
    SUMMONING_PILLARS {
        @Override
        public void tick(WorldServer world, EnderDragonBattle fight, List<EntityEnderCrystal> crystals, int i, BlockPosition pos) {
            int j = 40;
            boolean bl = i % 40 == 0;
            boolean bl2 = i % 40 == 39;
            if (bl || bl2) {
                List<WorldGenEnder.Spike> list = WorldGenEnder.getSpikesForLevel(world);
                int k = i / 40;
                if (k < list.size()) {
                    WorldGenEnder.Spike endSpike = list.get(k);
                    if (bl) {
                        for(EntityEnderCrystal endCrystal : crystals) {
                            endCrystal.setBeamTarget(new BlockPosition(endSpike.getCenterX(), endSpike.getHeight() + 1, endSpike.getCenterZ()));
                        }
                    } else {
                        int l = 10;

                        for(BlockPosition blockPos : BlockPosition.betweenClosed(new BlockPosition(endSpike.getCenterX() - 10, endSpike.getHeight() - 10, endSpike.getCenterZ() - 10), new BlockPosition(endSpike.getCenterX() + 10, endSpike.getHeight() + 10, endSpike.getCenterZ() + 10))) {
                            world.removeBlock(blockPos, false);
                        }

                        world.explode((Entity)null, (double)((float)endSpike.getCenterX() + 0.5F), (double)endSpike.getHeight(), (double)((float)endSpike.getCenterZ() + 0.5F), 5.0F, Explosion.Effect.DESTROY);
                        WorldGenFeatureEndSpikeConfiguration spikeConfiguration = new WorldGenFeatureEndSpikeConfiguration(true, ImmutableList.of(endSpike), new BlockPosition(0, 128, 0));
                        WorldGenerator.END_SPIKE.configured(spikeConfiguration).place(world, world.getChunkSource().getChunkGenerator(), new Random(), new BlockPosition(endSpike.getCenterX(), 45, endSpike.getCenterZ()));
                    }
                } else if (bl) {
                    fight.setRespawnPhase(SUMMONING_DRAGON);
                }
            }

        }
    },
    SUMMONING_DRAGON {
        @Override
        public void tick(WorldServer world, EnderDragonBattle fight, List<EntityEnderCrystal> crystals, int i, BlockPosition pos) {
            if (i >= 100) {
                fight.setRespawnPhase(END);
                fight.resetCrystals();

                for(EntityEnderCrystal endCrystal : crystals) {
                    endCrystal.setBeamTarget((BlockPosition)null);
                    world.explode(endCrystal, endCrystal.locX(), endCrystal.locY(), endCrystal.locZ(), 6.0F, Explosion.Effect.NONE);
                    endCrystal.die();
                }
            } else if (i >= 80) {
                world.triggerEffect(3001, new BlockPosition(0, 128, 0), 0);
            } else if (i == 0) {
                for(EntityEnderCrystal endCrystal2 : crystals) {
                    endCrystal2.setBeamTarget(new BlockPosition(0, 128, 0));
                }
            } else if (i < 5) {
                world.triggerEffect(3001, new BlockPosition(0, 128, 0), 0);
            }

        }
    },
    END {
        @Override
        public void tick(WorldServer world, EnderDragonBattle fight, List<EntityEnderCrystal> crystals, int i, BlockPosition pos) {
        }
    };

    public abstract void tick(WorldServer world, EnderDragonBattle fight, List<EntityEnderCrystal> crystals, int i, BlockPosition pos);
}
