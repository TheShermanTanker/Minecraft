package net.minecraft.world.level.block.entity;

import java.util.List;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.particles.Particles;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.tags.TagsEntity;
import net.minecraft.util.ColorUtil;
import net.minecraft.util.MathHelper;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.phys.AxisAlignedBB;
import org.apache.commons.lang3.mutable.MutableInt;

public class TileEntityBell extends TileEntity {
    private static final int DURATION = 50;
    private static final int GLOW_DURATION = 60;
    private static final int MIN_TICKS_BETWEEN_SEARCHES = 60;
    private static final int MAX_RESONATION_TICKS = 40;
    private static final int TICKS_BEFORE_RESONATION = 5;
    private static final int SEARCH_RADIUS = 48;
    private static final int HEAR_BELL_RADIUS = 32;
    private static final int HIGHLIGHT_RAIDERS_RADIUS = 48;
    private long lastRingTimestamp;
    public int ticks;
    public boolean shaking;
    public EnumDirection clickDirection;
    private List<EntityLiving> nearbyEntities;
    private boolean resonating;
    private int resonationTicks;

    public TileEntityBell(BlockPosition pos, IBlockData state) {
        super(TileEntityTypes.BELL, pos, state);
    }

    @Override
    public boolean setProperty(int type, int data) {
        if (type == 1) {
            this.updateEntities();
            this.resonationTicks = 0;
            this.clickDirection = EnumDirection.fromType1(data);
            this.ticks = 0;
            this.shaking = true;
            return true;
        } else {
            return super.setProperty(type, data);
        }
    }

    private static void tick(World world, BlockPosition pos, IBlockData state, TileEntityBell blockEntity, TileEntityBell.ResonationEndAction bellEffect) {
        if (blockEntity.shaking) {
            ++blockEntity.ticks;
        }

        if (blockEntity.ticks >= 50) {
            blockEntity.shaking = false;
            blockEntity.ticks = 0;
        }

        if (blockEntity.ticks >= 5 && blockEntity.resonationTicks == 0 && areRaidersNearby(pos, blockEntity.nearbyEntities)) {
            blockEntity.resonating = true;
            world.playSound((EntityHuman)null, pos, SoundEffects.BELL_RESONATE, SoundCategory.BLOCKS, 1.0F, 1.0F);
        }

        if (blockEntity.resonating) {
            if (blockEntity.resonationTicks < 40) {
                ++blockEntity.resonationTicks;
            } else {
                bellEffect.run(world, pos, blockEntity.nearbyEntities);
                blockEntity.resonating = false;
            }
        }

    }

    public static void clientTick(World world, BlockPosition pos, IBlockData state, TileEntityBell blockEntity) {
        tick(world, pos, state, blockEntity, TileEntityBell::showBellParticles);
    }

    public static void serverTick(World world, BlockPosition pos, IBlockData state, TileEntityBell blockEntity) {
        tick(world, pos, state, blockEntity, TileEntityBell::makeRaidersGlow);
    }

    public void onHit(EnumDirection direction) {
        BlockPosition blockPos = this.getPosition();
        this.clickDirection = direction;
        if (this.shaking) {
            this.ticks = 0;
        } else {
            this.shaking = true;
        }

        this.level.playBlockAction(blockPos, this.getBlock().getBlock(), 1, direction.get3DDataValue());
    }

    private void updateEntities() {
        BlockPosition blockPos = this.getPosition();
        if (this.level.getTime() > this.lastRingTimestamp + 60L || this.nearbyEntities == null) {
            this.lastRingTimestamp = this.level.getTime();
            AxisAlignedBB aABB = (new AxisAlignedBB(blockPos)).inflate(48.0D);
            this.nearbyEntities = this.level.getEntitiesOfClass(EntityLiving.class, aABB);
        }

        if (!this.level.isClientSide) {
            for(EntityLiving livingEntity : this.nearbyEntities) {
                if (livingEntity.isAlive() && !livingEntity.isRemoved() && blockPos.closerThan(livingEntity.getPositionVector(), 32.0D)) {
                    livingEntity.getBehaviorController().setMemory(MemoryModuleType.HEARD_BELL_TIME, this.level.getTime());
                }
            }
        }

    }

    private static boolean areRaidersNearby(BlockPosition pos, List<EntityLiving> hearingEntities) {
        for(EntityLiving livingEntity : hearingEntities) {
            if (livingEntity.isAlive() && !livingEntity.isRemoved() && pos.closerThan(livingEntity.getPositionVector(), 32.0D) && livingEntity.getEntityType().is(TagsEntity.RAIDERS)) {
                return true;
            }
        }

        return false;
    }

    private static void makeRaidersGlow(World world, BlockPosition pos, List<EntityLiving> hearingEntities) {
        hearingEntities.stream().filter((livingEntity) -> {
            return isRaiderWithinRange(pos, livingEntity);
        }).forEach(TileEntityBell::glow);
    }

    private static void showBellParticles(World world, BlockPosition pos, List<EntityLiving> hearingEntities) {
        MutableInt mutableInt = new MutableInt(16700985);
        int i = (int)hearingEntities.stream().filter((livingEntity) -> {
            return pos.closerThan(livingEntity.getPositionVector(), 48.0D);
        }).count();
        hearingEntities.stream().filter((livingEntity) -> {
            return isRaiderWithinRange(pos, livingEntity);
        }).forEach((livingEntity) -> {
            float f = 1.0F;
            double d = Math.sqrt((livingEntity.locX() - (double)pos.getX()) * (livingEntity.locX() - (double)pos.getX()) + (livingEntity.locZ() - (double)pos.getZ()) * (livingEntity.locZ() - (double)pos.getZ()));
            double e = (double)((float)pos.getX() + 0.5F) + 1.0D / d * (livingEntity.locX() - (double)pos.getX());
            double g = (double)((float)pos.getZ() + 0.5F) + 1.0D / d * (livingEntity.locZ() - (double)pos.getZ());
            int j = MathHelper.clamp((i - 21) / -2, 3, 15);

            for(int k = 0; k < j; ++k) {
                int l = mutableInt.addAndGet(5);
                double h = (double)ColorUtil.ARGB32.red(l) / 255.0D;
                double m = (double)ColorUtil.ARGB32.green(l) / 255.0D;
                double n = (double)ColorUtil.ARGB32.blue(l) / 255.0D;
                world.addParticle(Particles.ENTITY_EFFECT, e, (double)((float)pos.getY() + 0.5F), g, h, m, n);
            }

        });
    }

    private static boolean isRaiderWithinRange(BlockPosition pos, EntityLiving entity) {
        return entity.isAlive() && !entity.isRemoved() && pos.closerThan(entity.getPositionVector(), 48.0D) && entity.getEntityType().is(TagsEntity.RAIDERS);
    }

    private static void glow(EntityLiving entity) {
        entity.addEffect(new MobEffect(MobEffects.GLOWING, 60));
    }

    @FunctionalInterface
    interface ResonationEndAction {
        void run(World world, BlockPosition pos, List<EntityLiving> hearingEntities);
    }
}
