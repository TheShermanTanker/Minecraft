package net.minecraft.world.entity.ai.goal;

import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.SectionPosition;
import net.minecraft.core.particles.ParticleParamItem;
import net.minecraft.core.particles.Particles;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.EntityCreature;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.IChunkAccess;
import net.minecraft.world.phys.Vec3D;

public class PathfinderGoalRemoveBlock extends PathfinderGoalGotoTarget {
    private final Block blockToRemove;
    private final EntityInsentient removerMob;
    private int ticksSinceReachedGoal;
    private static final int WAIT_AFTER_BLOCK_FOUND = 20;

    public PathfinderGoalRemoveBlock(Block targetBlock, EntityCreature mob, double speed, int maxYDifference) {
        super(mob, speed, 24, maxYDifference);
        this.blockToRemove = targetBlock;
        this.removerMob = mob;
    }

    @Override
    public boolean canUse() {
        if (!this.removerMob.level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            return false;
        } else if (this.nextStartTick > 0) {
            --this.nextStartTick;
            return false;
        } else if (this.tryFindBlock()) {
            this.nextStartTick = reducedTickDelay(20);
            return true;
        } else {
            this.nextStartTick = this.nextStartTick(this.mob);
            return false;
        }
    }

    private boolean tryFindBlock() {
        return this.blockPos != null && this.isValidTarget(this.mob.level, this.blockPos) ? true : this.findNearestBlock();
    }

    @Override
    public void stop() {
        super.stop();
        this.removerMob.fallDistance = 1.0F;
    }

    @Override
    public void start() {
        super.start();
        this.ticksSinceReachedGoal = 0;
    }

    public void playDestroyProgressSound(GeneratorAccess world, BlockPosition pos) {
    }

    public void playBreakSound(World world, BlockPosition pos) {
    }

    @Override
    public void tick() {
        super.tick();
        World level = this.removerMob.level;
        BlockPosition blockPos = this.removerMob.getChunkCoordinates();
        BlockPosition blockPos2 = this.getPosWithBlock(blockPos, level);
        Random random = this.removerMob.getRandom();
        if (this.isReachedTarget() && blockPos2 != null) {
            if (this.ticksSinceReachedGoal > 0) {
                Vec3D vec3 = this.removerMob.getMot();
                this.removerMob.setMot(vec3.x, 0.3D, vec3.z);
                if (!level.isClientSide) {
                    double d = 0.08D;
                    ((WorldServer)level).sendParticles(new ParticleParamItem(Particles.ITEM, new ItemStack(Items.EGG)), (double)blockPos2.getX() + 0.5D, (double)blockPos2.getY() + 0.7D, (double)blockPos2.getZ() + 0.5D, 3, ((double)random.nextFloat() - 0.5D) * 0.08D, ((double)random.nextFloat() - 0.5D) * 0.08D, ((double)random.nextFloat() - 0.5D) * 0.08D, (double)0.15F);
                }
            }

            if (this.ticksSinceReachedGoal % 2 == 0) {
                Vec3D vec32 = this.removerMob.getMot();
                this.removerMob.setMot(vec32.x, -0.3D, vec32.z);
                if (this.ticksSinceReachedGoal % 6 == 0) {
                    this.playDestroyProgressSound(level, this.blockPos);
                }
            }

            if (this.ticksSinceReachedGoal > 60) {
                level.removeBlock(blockPos2, false);
                if (!level.isClientSide) {
                    for(int i = 0; i < 20; ++i) {
                        double e = random.nextGaussian() * 0.02D;
                        double f = random.nextGaussian() * 0.02D;
                        double g = random.nextGaussian() * 0.02D;
                        ((WorldServer)level).sendParticles(Particles.POOF, (double)blockPos2.getX() + 0.5D, (double)blockPos2.getY(), (double)blockPos2.getZ() + 0.5D, 1, e, f, g, (double)0.15F);
                    }

                    this.playBreakSound(level, blockPos2);
                }
            }

            ++this.ticksSinceReachedGoal;
        }

    }

    @Nullable
    private BlockPosition getPosWithBlock(BlockPosition pos, IBlockAccess world) {
        if (world.getType(pos).is(this.blockToRemove)) {
            return pos;
        } else {
            BlockPosition[] blockPoss = new BlockPosition[]{pos.below(), pos.west(), pos.east(), pos.north(), pos.south(), pos.below().below()};

            for(BlockPosition blockPos : blockPoss) {
                if (world.getType(blockPos).is(this.blockToRemove)) {
                    return blockPos;
                }
            }

            return null;
        }
    }

    @Override
    protected boolean isValidTarget(IWorldReader world, BlockPosition pos) {
        IChunkAccess chunkAccess = world.getChunkAt(SectionPosition.blockToSectionCoord(pos.getX()), SectionPosition.blockToSectionCoord(pos.getZ()), ChunkStatus.FULL, false);
        if (chunkAccess == null) {
            return false;
        } else {
            return chunkAccess.getType(pos).is(this.blockToRemove) && chunkAccess.getType(pos.above()).isAir() && chunkAccess.getType(pos.above(2)).isAir();
        }
    }
}
