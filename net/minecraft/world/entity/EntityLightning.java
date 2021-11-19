package net.minecraft.world.entity;

import com.google.common.collect.Sets;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriterionTriggers;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.PacketPlayOutSpawnEntity;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.EnumSoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.BlockFireAbstract;
import net.minecraft.world.level.block.BlockLightningRod;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.Vec3D;

public class EntityLightning extends Entity {
    private static final int START_LIFE = 2;
    private static final double DAMAGE_RADIUS = 3.0D;
    private static final double DETECTION_RADIUS = 15.0D;
    public int life;
    public long seed;
    public int flashes;
    public boolean visualOnly;
    @Nullable
    private EntityPlayer cause;
    private final Set<Entity> hitEntities = Sets.newHashSet();
    private int blocksSetOnFire;

    public EntityLightning(EntityTypes<? extends EntityLightning> type, World world) {
        super(type, world);
        this.noCulling = true;
        this.life = 2;
        this.seed = this.random.nextLong();
        this.flashes = this.random.nextInt(3) + 1;
    }

    public void setEffect(boolean cosmetic) {
        this.visualOnly = cosmetic;
    }

    @Override
    public EnumSoundCategory getSoundCategory() {
        return EnumSoundCategory.WEATHER;
    }

    @Nullable
    public EntityPlayer getCause() {
        return this.cause;
    }

    public void setCause(@Nullable EntityPlayer channeler) {
        this.cause = channeler;
    }

    private void powerLightningRod() {
        BlockPosition blockPos = this.getStrikePosition();
        IBlockData blockState = this.level.getType(blockPos);
        if (blockState.is(Blocks.LIGHTNING_ROD)) {
            ((BlockLightningRod)blockState.getBlock()).onLightningStrike(blockState, this.level, blockPos);
        }

    }

    @Override
    public void tick() {
        super.tick();
        if (this.life == 2) {
            if (this.level.isClientSide()) {
                this.level.playLocalSound(this.locX(), this.locY(), this.locZ(), SoundEffects.LIGHTNING_BOLT_THUNDER, EnumSoundCategory.WEATHER, 10000.0F, 0.8F + this.random.nextFloat() * 0.2F, false);
                this.level.playLocalSound(this.locX(), this.locY(), this.locZ(), SoundEffects.LIGHTNING_BOLT_IMPACT, EnumSoundCategory.WEATHER, 2.0F, 0.5F + this.random.nextFloat() * 0.2F, false);
            } else {
                EnumDifficulty difficulty = this.level.getDifficulty();
                if (difficulty == EnumDifficulty.NORMAL || difficulty == EnumDifficulty.HARD) {
                    this.spawnFire(4);
                }

                this.powerLightningRod();
                clearCopperOnLightningStrike(this.level, this.getStrikePosition());
                this.gameEvent(GameEvent.LIGHTNING_STRIKE);
            }
        }

        --this.life;
        if (this.life < 0) {
            if (this.flashes == 0) {
                if (this.level instanceof WorldServer) {
                    List<Entity> list = this.level.getEntities(this, new AxisAlignedBB(this.locX() - 15.0D, this.locY() - 15.0D, this.locZ() - 15.0D, this.locX() + 15.0D, this.locY() + 6.0D + 15.0D, this.locZ() + 15.0D), (entityx) -> {
                        return entityx.isAlive() && !this.hitEntities.contains(entityx);
                    });

                    for(EntityPlayer serverPlayer : ((WorldServer)this.level).getPlayers((serverPlayer) -> {
                        return serverPlayer.distanceTo(this) < 256.0F;
                    })) {
                        CriterionTriggers.LIGHTNING_STRIKE.trigger(serverPlayer, this, list);
                    }
                }

                this.die();
            } else if (this.life < -this.random.nextInt(10)) {
                --this.flashes;
                this.life = 1;
                this.seed = this.random.nextLong();
                this.spawnFire(0);
            }
        }

        if (this.life >= 0) {
            if (!(this.level instanceof WorldServer)) {
                this.level.setSkyFlashTime(2);
            } else if (!this.visualOnly) {
                List<Entity> list2 = this.level.getEntities(this, new AxisAlignedBB(this.locX() - 3.0D, this.locY() - 3.0D, this.locZ() - 3.0D, this.locX() + 3.0D, this.locY() + 6.0D + 3.0D, this.locZ() + 3.0D), Entity::isAlive);

                for(Entity entity : list2) {
                    entity.onLightningStrike((WorldServer)this.level, this);
                }

                this.hitEntities.addAll(list2);
                if (this.cause != null) {
                    CriterionTriggers.CHANNELED_LIGHTNING.trigger(this.cause, list2);
                }
            }
        }

    }

    private BlockPosition getStrikePosition() {
        Vec3D vec3 = this.getPositionVector();
        return new BlockPosition(vec3.x, vec3.y - 1.0E-6D, vec3.z);
    }

    private void spawnFire(int spreadAttempts) {
        if (!this.visualOnly && !this.level.isClientSide && this.level.getGameRules().getBoolean(GameRules.RULE_DOFIRETICK)) {
            BlockPosition blockPos = this.getChunkCoordinates();
            IBlockData blockState = BlockFireAbstract.getState(this.level, blockPos);
            if (this.level.getType(blockPos).isAir() && blockState.canPlace(this.level, blockPos)) {
                this.level.setTypeUpdate(blockPos, blockState);
                ++this.blocksSetOnFire;
            }

            for(int i = 0; i < spreadAttempts; ++i) {
                BlockPosition blockPos2 = blockPos.offset(this.random.nextInt(3) - 1, this.random.nextInt(3) - 1, this.random.nextInt(3) - 1);
                blockState = BlockFireAbstract.getState(this.level, blockPos2);
                if (this.level.getType(blockPos2).isAir() && blockState.canPlace(this.level, blockPos2)) {
                    this.level.setTypeUpdate(blockPos2, blockState);
                    ++this.blocksSetOnFire;
                }
            }

        }
    }

    private static void clearCopperOnLightningStrike(World world, BlockPosition pos) {
        IBlockData blockState = world.getType(pos);
        BlockPosition blockPos;
        IBlockData blockState2;
        if (blockState.is(Blocks.LIGHTNING_ROD)) {
            blockPos = pos.relative(blockState.get(BlockLightningRod.FACING).opposite());
            blockState2 = world.getType(blockPos);
        } else {
            blockPos = pos;
            blockState2 = blockState;
        }

        if (blockState2.getBlock() instanceof WeatheringCopper) {
            world.setTypeUpdate(blockPos, WeatheringCopper.getFirst(world.getType(blockPos)));
            BlockPosition.MutableBlockPosition mutableBlockPos = pos.mutable();
            int i = world.random.nextInt(3) + 3;

            for(int j = 0; j < i; ++j) {
                int k = world.random.nextInt(8) + 1;
                randomWalkCleaningCopper(world, blockPos, mutableBlockPos, k);
            }

        }
    }

    private static void randomWalkCleaningCopper(World world, BlockPosition pos, BlockPosition.MutableBlockPosition mutablePos, int count) {
        mutablePos.set(pos);

        for(int i = 0; i < count; ++i) {
            Optional<BlockPosition> optional = randomStepCleaningCopper(world, mutablePos);
            if (!optional.isPresent()) {
                break;
            }

            mutablePos.set(optional.get());
        }

    }

    private static Optional<BlockPosition> randomStepCleaningCopper(World world, BlockPosition pos) {
        for(BlockPosition blockPos : BlockPosition.randomInCube(world.random, 10, pos, 1)) {
            IBlockData blockState = world.getType(blockPos);
            if (blockState.getBlock() instanceof WeatheringCopper) {
                WeatheringCopper.getPrevious(blockState).ifPresent((state) -> {
                    world.setTypeUpdate(blockPos, state);
                });
                world.triggerEffect(3002, blockPos, -1);
                return Optional.of(blockPos);
            }
        }

        return Optional.empty();
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double d = 64.0D * getViewScale();
        return distance < d * d;
    }

    @Override
    protected void initDatawatcher() {
    }

    @Override
    protected void loadData(NBTTagCompound nbt) {
    }

    @Override
    protected void saveData(NBTTagCompound nbt) {
    }

    @Override
    public Packet<?> getPacket() {
        return new PacketPlayOutSpawnEntity(this);
    }

    public int getBlocksSetOnFire() {
        return this.blocksSetOnFire;
    }

    public Stream<Entity> getHitEntities() {
        return this.hitEntities.stream().filter(Entity::isAlive);
    }
}
