package net.minecraft.world.level.block.entity;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.particles.Particles;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.protocol.game.PacketPlayOutTileEntityData;
import net.minecraft.sounds.EnumSoundCategory;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.util.MathHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.monster.IMonster;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.Vec3D;

public class TileEntityConduit extends TileEntity {
    private static final int BLOCK_REFRESH_RATE = 2;
    private static final int EFFECT_DURATION = 13;
    private static final float ROTATION_SPEED = -0.0375F;
    private static final int MIN_ACTIVE_SIZE = 16;
    private static final int MIN_KILL_SIZE = 42;
    private static final int KILL_RANGE = 8;
    private static final Block[] VALID_BLOCKS = new Block[]{Blocks.PRISMARINE, Blocks.PRISMARINE_BRICKS, Blocks.SEA_LANTERN, Blocks.DARK_PRISMARINE};
    public int tickCount;
    private float activeRotation;
    private boolean isActive;
    private boolean isHunting;
    private final List<BlockPosition> effectBlocks = Lists.newArrayList();
    @Nullable
    private EntityLiving destroyTarget;
    @Nullable
    private UUID destroyTargetUUID;
    private long nextAmbientSoundActivation;

    public TileEntityConduit(BlockPosition pos, IBlockData state) {
        super(TileEntityTypes.CONDUIT, pos, state);
    }

    @Override
    public void load(NBTTagCompound nbt) {
        super.load(nbt);
        if (nbt.hasUUID("Target")) {
            this.destroyTargetUUID = nbt.getUUID("Target");
        } else {
            this.destroyTargetUUID = null;
        }

    }

    @Override
    public NBTTagCompound save(NBTTagCompound nbt) {
        super.save(nbt);
        if (this.destroyTarget != null) {
            nbt.putUUID("Target", this.destroyTarget.getUniqueID());
        }

        return nbt;
    }

    @Nullable
    @Override
    public PacketPlayOutTileEntityData getUpdatePacket() {
        return new PacketPlayOutTileEntityData(this.worldPosition, 5, this.getUpdateTag());
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return this.save(new NBTTagCompound());
    }

    public static void clientTick(World world, BlockPosition pos, IBlockData state, TileEntityConduit blockEntity) {
        ++blockEntity.tickCount;
        long l = world.getTime();
        List<BlockPosition> list = blockEntity.effectBlocks;
        if (l % 40L == 0L) {
            blockEntity.isActive = updateShape(world, pos, list);
            updateHunting(blockEntity, list);
        }

        updateClientTarget(world, pos, blockEntity);
        animationTick(world, pos, list, blockEntity.destroyTarget, blockEntity.tickCount);
        if (blockEntity.isActive()) {
            ++blockEntity.activeRotation;
        }

    }

    public static void serverTick(World world, BlockPosition pos, IBlockData state, TileEntityConduit blockEntity) {
        ++blockEntity.tickCount;
        long l = world.getTime();
        List<BlockPosition> list = blockEntity.effectBlocks;
        if (l % 40L == 0L) {
            boolean bl = updateShape(world, pos, list);
            if (bl != blockEntity.isActive) {
                SoundEffect soundEvent = bl ? SoundEffects.CONDUIT_ACTIVATE : SoundEffects.CONDUIT_DEACTIVATE;
                world.playSound((EntityHuman)null, pos, soundEvent, EnumSoundCategory.BLOCKS, 1.0F, 1.0F);
            }

            blockEntity.isActive = bl;
            updateHunting(blockEntity, list);
            if (bl) {
                applyEffects(world, pos, list);
                updateDestroyTarget(world, pos, state, list, blockEntity);
            }
        }

        if (blockEntity.isActive()) {
            if (l % 80L == 0L) {
                world.playSound((EntityHuman)null, pos, SoundEffects.CONDUIT_AMBIENT, EnumSoundCategory.BLOCKS, 1.0F, 1.0F);
            }

            if (l > blockEntity.nextAmbientSoundActivation) {
                blockEntity.nextAmbientSoundActivation = l + 60L + (long)world.getRandom().nextInt(40);
                world.playSound((EntityHuman)null, pos, SoundEffects.CONDUIT_AMBIENT_SHORT, EnumSoundCategory.BLOCKS, 1.0F, 1.0F);
            }
        }

    }

    private static void updateHunting(TileEntityConduit blockEntity, List<BlockPosition> activatingBlocks) {
        blockEntity.setHunting(activatingBlocks.size() >= 42);
    }

    private static boolean updateShape(World world, BlockPosition pos, List<BlockPosition> activatingBlocks) {
        activatingBlocks.clear();

        for(int i = -1; i <= 1; ++i) {
            for(int j = -1; j <= 1; ++j) {
                for(int k = -1; k <= 1; ++k) {
                    BlockPosition blockPos = pos.offset(i, j, k);
                    if (!world.isWaterAt(blockPos)) {
                        return false;
                    }
                }
            }
        }

        for(int l = -2; l <= 2; ++l) {
            for(int m = -2; m <= 2; ++m) {
                for(int n = -2; n <= 2; ++n) {
                    int o = Math.abs(l);
                    int p = Math.abs(m);
                    int q = Math.abs(n);
                    if ((o > 1 || p > 1 || q > 1) && (l == 0 && (p == 2 || q == 2) || m == 0 && (o == 2 || q == 2) || n == 0 && (o == 2 || p == 2))) {
                        BlockPosition blockPos2 = pos.offset(l, m, n);
                        IBlockData blockState = world.getType(blockPos2);

                        for(Block block : VALID_BLOCKS) {
                            if (blockState.is(block)) {
                                activatingBlocks.add(blockPos2);
                            }
                        }
                    }
                }
            }
        }

        return activatingBlocks.size() >= 16;
    }

    private static void applyEffects(World world, BlockPosition pos, List<BlockPosition> activatingBlocks) {
        int i = activatingBlocks.size();
        int j = i / 7 * 16;
        int k = pos.getX();
        int l = pos.getY();
        int m = pos.getZ();
        AxisAlignedBB aABB = (new AxisAlignedBB((double)k, (double)l, (double)m, (double)(k + 1), (double)(l + 1), (double)(m + 1))).inflate((double)j).expandTowards(0.0D, (double)world.getHeight(), 0.0D);
        List<EntityHuman> list = world.getEntitiesOfClass(EntityHuman.class, aABB);
        if (!list.isEmpty()) {
            for(EntityHuman player : list) {
                if (pos.closerThan(player.getChunkCoordinates(), (double)j) && player.isInWaterOrRain()) {
                    player.addEffect(new MobEffect(MobEffectList.CONDUIT_POWER, 260, 0, true, true));
                }
            }

        }
    }

    private static void updateDestroyTarget(World world, BlockPosition pos, IBlockData state, List<BlockPosition> activatingBlocks, TileEntityConduit blockEntity) {
        EntityLiving livingEntity = blockEntity.destroyTarget;
        int i = activatingBlocks.size();
        if (i < 42) {
            blockEntity.destroyTarget = null;
        } else if (blockEntity.destroyTarget == null && blockEntity.destroyTargetUUID != null) {
            blockEntity.destroyTarget = findDestroyTarget(world, pos, blockEntity.destroyTargetUUID);
            blockEntity.destroyTargetUUID = null;
        } else if (blockEntity.destroyTarget == null) {
            List<EntityLiving> list = world.getEntitiesOfClass(EntityLiving.class, getDestroyRangeAABB(pos), (livingEntityx) -> {
                return livingEntityx instanceof IMonster && livingEntityx.isInWaterOrRain();
            });
            if (!list.isEmpty()) {
                blockEntity.destroyTarget = list.get(world.random.nextInt(list.size()));
            }
        } else if (!blockEntity.destroyTarget.isAlive() || !pos.closerThan(blockEntity.destroyTarget.getChunkCoordinates(), 8.0D)) {
            blockEntity.destroyTarget = null;
        }

        if (blockEntity.destroyTarget != null) {
            world.playSound((EntityHuman)null, blockEntity.destroyTarget.locX(), blockEntity.destroyTarget.locY(), blockEntity.destroyTarget.locZ(), SoundEffects.CONDUIT_ATTACK_TARGET, EnumSoundCategory.BLOCKS, 1.0F, 1.0F);
            blockEntity.destroyTarget.damageEntity(DamageSource.MAGIC, 4.0F);
        }

        if (livingEntity != blockEntity.destroyTarget) {
            world.notify(pos, state, state, 2);
        }

    }

    private static void updateClientTarget(World world, BlockPosition pos, TileEntityConduit blockEntity) {
        if (blockEntity.destroyTargetUUID == null) {
            blockEntity.destroyTarget = null;
        } else if (blockEntity.destroyTarget == null || !blockEntity.destroyTarget.getUniqueID().equals(blockEntity.destroyTargetUUID)) {
            blockEntity.destroyTarget = findDestroyTarget(world, pos, blockEntity.destroyTargetUUID);
            if (blockEntity.destroyTarget == null) {
                blockEntity.destroyTargetUUID = null;
            }
        }

    }

    private static AxisAlignedBB getDestroyRangeAABB(BlockPosition pos) {
        int i = pos.getX();
        int j = pos.getY();
        int k = pos.getZ();
        return (new AxisAlignedBB((double)i, (double)j, (double)k, (double)(i + 1), (double)(j + 1), (double)(k + 1))).inflate(8.0D);
    }

    @Nullable
    private static EntityLiving findDestroyTarget(World world, BlockPosition pos, UUID uuid) {
        List<EntityLiving> list = world.getEntitiesOfClass(EntityLiving.class, getDestroyRangeAABB(pos), (livingEntity) -> {
            return livingEntity.getUniqueID().equals(uuid);
        });
        return list.size() == 1 ? list.get(0) : null;
    }

    private static void animationTick(World world, BlockPosition pos, List<BlockPosition> activatingBlocks, @Nullable Entity entity, int i) {
        Random random = world.random;
        double d = (double)(MathHelper.sin((float)(i + 35) * 0.1F) / 2.0F + 0.5F);
        d = (d * d + d) * (double)0.3F;
        Vec3D vec3 = new Vec3D((double)pos.getX() + 0.5D, (double)pos.getY() + 1.5D + d, (double)pos.getZ() + 0.5D);

        for(BlockPosition blockPos : activatingBlocks) {
            if (random.nextInt(50) == 0) {
                BlockPosition blockPos2 = blockPos.subtract(pos);
                float f = -0.5F + random.nextFloat() + (float)blockPos2.getX();
                float g = -2.0F + random.nextFloat() + (float)blockPos2.getY();
                float h = -0.5F + random.nextFloat() + (float)blockPos2.getZ();
                world.addParticle(Particles.NAUTILUS, vec3.x, vec3.y, vec3.z, (double)f, (double)g, (double)h);
            }
        }

        if (entity != null) {
            Vec3D vec32 = new Vec3D(entity.locX(), entity.getHeadY(), entity.locZ());
            float j = (-0.5F + random.nextFloat()) * (3.0F + entity.getWidth());
            float k = -1.0F + random.nextFloat() * entity.getHeight();
            float l = (-0.5F + random.nextFloat()) * (3.0F + entity.getWidth());
            Vec3D vec33 = new Vec3D((double)j, (double)k, (double)l);
            world.addParticle(Particles.NAUTILUS, vec32.x, vec32.y, vec32.z, vec33.x, vec33.y, vec33.z);
        }

    }

    public boolean isActive() {
        return this.isActive;
    }

    public boolean isHunting() {
        return this.isHunting;
    }

    private void setHunting(boolean eyeOpen) {
        this.isHunting = eyeOpen;
    }

    public float getActiveRotation(float tickDelta) {
        return (this.activeRotation + tickDelta) * -0.0375F;
    }
}
