package net.minecraft.world.entity.item;

import java.util.function.Predicate;
import net.minecraft.CrashReportSystemDetails;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.nbt.GameProfileSerializer;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.PacketPlayOutBlockChange;
import net.minecraft.network.protocol.game.PacketPlayOutSpawnEntity;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.server.level.WorldServer;
import net.minecraft.tags.TagsBlock;
import net.minecraft.tags.TagsFluid;
import net.minecraft.util.MathHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumMoveType;
import net.minecraft.world.entity.IEntitySelector;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockActionContextDirectional;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.RayTrace;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BlockAnvil;
import net.minecraft.world.level.block.BlockConcretePowder;
import net.minecraft.world.level.block.BlockFalling;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Fallable;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.material.FluidTypes;
import net.minecraft.world.phys.MovingObjectPosition;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import net.minecraft.world.phys.Vec3D;

public class EntityFallingBlock extends Entity {
    private IBlockData blockState = Blocks.SAND.getBlockData();
    public int time;
    public boolean dropItem = true;
    private boolean cancelDrop;
    public boolean hurtEntities;
    private int fallDamageMax = 40;
    private float fallDamagePerDistance;
    public NBTTagCompound blockData;
    protected static final DataWatcherObject<BlockPosition> DATA_START_POS = DataWatcher.defineId(EntityFallingBlock.class, DataWatcherRegistry.BLOCK_POS);

    public EntityFallingBlock(EntityTypes<? extends EntityFallingBlock> type, World world) {
        super(type, world);
    }

    public EntityFallingBlock(World world, double x, double y, double z, IBlockData block) {
        this(EntityTypes.FALLING_BLOCK, world);
        this.blockState = block;
        this.blocksBuilding = true;
        this.setPosition(x, y + (double)((1.0F - this.getHeight()) / 2.0F), z);
        this.setMot(Vec3D.ZERO);
        this.xo = x;
        this.yo = y;
        this.zo = z;
        this.setStartPos(this.getChunkCoordinates());
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    public void setStartPos(BlockPosition pos) {
        this.entityData.set(DATA_START_POS, pos);
    }

    public BlockPosition getStartPos() {
        return this.entityData.get(DATA_START_POS);
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.NONE;
    }

    @Override
    protected void initDatawatcher() {
        this.entityData.register(DATA_START_POS, BlockPosition.ZERO);
    }

    @Override
    public boolean isInteractable() {
        return !this.isRemoved();
    }

    @Override
    public void tick() {
        if (this.blockState.isAir()) {
            this.die();
        } else {
            Block block = this.blockState.getBlock();
            if (this.time++ == 0) {
                BlockPosition blockPos = this.getChunkCoordinates();
                if (this.level.getType(blockPos).is(block)) {
                    this.level.removeBlock(blockPos, false);
                } else if (!this.level.isClientSide) {
                    this.die();
                    return;
                }
            }

            if (!this.isNoGravity()) {
                this.setMot(this.getMot().add(0.0D, -0.04D, 0.0D));
            }

            this.move(EnumMoveType.SELF, this.getMot());
            if (!this.level.isClientSide) {
                BlockPosition blockPos2 = this.getChunkCoordinates();
                boolean bl = this.blockState.getBlock() instanceof BlockConcretePowder;
                boolean bl2 = bl && this.level.getFluid(blockPos2).is(TagsFluid.WATER);
                double d = this.getMot().lengthSqr();
                if (bl && d > 1.0D) {
                    MovingObjectPositionBlock blockHitResult = this.level.rayTrace(new RayTrace(new Vec3D(this.xo, this.yo, this.zo), this.getPositionVector(), RayTrace.BlockCollisionOption.COLLIDER, RayTrace.FluidCollisionOption.SOURCE_ONLY, this));
                    if (blockHitResult.getType() != MovingObjectPosition.EnumMovingObjectType.MISS && this.level.getFluid(blockHitResult.getBlockPosition()).is(TagsFluid.WATER)) {
                        blockPos2 = blockHitResult.getBlockPosition();
                        bl2 = true;
                    }
                }

                if (!this.onGround && !bl2) {
                    if (!this.level.isClientSide && (this.time > 100 && (blockPos2.getY() <= this.level.getMinBuildHeight() || blockPos2.getY() > this.level.getMaxBuildHeight()) || this.time > 600)) {
                        if (this.dropItem && this.level.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
                            this.spawnAtLocation(block);
                        }

                        this.die();
                    }
                } else {
                    IBlockData blockState = this.level.getType(blockPos2);
                    this.setMot(this.getMot().multiply(0.7D, -0.5D, 0.7D));
                    if (!blockState.is(Blocks.MOVING_PISTON)) {
                        if (!this.cancelDrop) {
                            boolean bl3 = blockState.canBeReplaced(new BlockActionContextDirectional(this.level, blockPos2, EnumDirection.DOWN, ItemStack.EMPTY, EnumDirection.UP));
                            boolean bl4 = BlockFalling.canFallThrough(this.level.getType(blockPos2.below())) && (!bl || !bl2);
                            boolean bl5 = this.blockState.canPlace(this.level, blockPos2) && !bl4;
                            if (bl3 && bl5) {
                                if (this.blockState.hasProperty(BlockProperties.WATERLOGGED) && this.level.getFluid(blockPos2).getType() == FluidTypes.WATER) {
                                    this.blockState = this.blockState.set(BlockProperties.WATERLOGGED, Boolean.valueOf(true));
                                }

                                if (this.level.setTypeAndData(blockPos2, this.blockState, 3)) {
                                    ((WorldServer)this.level).getChunkSource().chunkMap.broadcast(this, new PacketPlayOutBlockChange(blockPos2, this.level.getType(blockPos2)));
                                    this.die();
                                    if (block instanceof Fallable) {
                                        ((Fallable)block).onLand(this.level, blockPos2, this.blockState, blockState, this);
                                    }

                                    if (this.blockData != null && this.blockState.isTileEntity()) {
                                        TileEntity blockEntity = this.level.getTileEntity(blockPos2);
                                        if (blockEntity != null) {
                                            NBTTagCompound compoundTag = blockEntity.save(new NBTTagCompound());

                                            for(String string : this.blockData.getKeys()) {
                                                NBTBase tag = this.blockData.get(string);
                                                if (!"x".equals(string) && !"y".equals(string) && !"z".equals(string)) {
                                                    compoundTag.set(string, tag.clone());
                                                }
                                            }

                                            try {
                                                blockEntity.load(compoundTag);
                                            } catch (Exception var16) {
                                                LOGGER.error("Failed to load block entity from falling block", (Throwable)var16);
                                            }

                                            blockEntity.update();
                                        }
                                    }
                                } else if (this.dropItem && this.level.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
                                    this.die();
                                    this.callOnBrokenAfterFall(block, blockPos2);
                                    this.spawnAtLocation(block);
                                }
                            } else {
                                this.die();
                                if (this.dropItem && this.level.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
                                    this.callOnBrokenAfterFall(block, blockPos2);
                                    this.spawnAtLocation(block);
                                }
                            }
                        } else {
                            this.die();
                            this.callOnBrokenAfterFall(block, blockPos2);
                        }
                    }
                }
            }

            this.setMot(this.getMot().scale(0.98D));
        }
    }

    public void callOnBrokenAfterFall(Block block, BlockPosition pos) {
        if (block instanceof Fallable) {
            ((Fallable)block).onBrokenAfterFall(this.level, pos, this);
        }

    }

    @Override
    public boolean causeFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
        if (!this.hurtEntities) {
            return false;
        } else {
            int i = MathHelper.ceil(fallDistance - 1.0F);
            if (i < 0) {
                return false;
            } else {
                Predicate<Entity> predicate;
                DamageSource damageSource2;
                if (this.blockState.getBlock() instanceof Fallable) {
                    Fallable fallable = (Fallable)this.blockState.getBlock();
                    predicate = fallable.getHurtsEntitySelector();
                    damageSource2 = fallable.getFallDamageSource();
                } else {
                    predicate = IEntitySelector.NO_SPECTATORS;
                    damageSource2 = DamageSource.FALLING_BLOCK;
                }

                float f = (float)Math.min(MathHelper.floor((float)i * this.fallDamagePerDistance), this.fallDamageMax);
                this.level.getEntities(this, this.getBoundingBox(), predicate).forEach((entity) -> {
                    entity.damageEntity(damageSource2, f);
                });
                boolean bl = this.blockState.is(TagsBlock.ANVIL);
                if (bl && f > 0.0F && this.random.nextFloat() < 0.05F + (float)i * 0.05F) {
                    IBlockData blockState = BlockAnvil.damage(this.blockState);
                    if (blockState == null) {
                        this.cancelDrop = true;
                    } else {
                        this.blockState = blockState;
                    }
                }

                return false;
            }
        }
    }

    @Override
    protected void saveData(NBTTagCompound nbt) {
        nbt.set("BlockState", GameProfileSerializer.writeBlockState(this.blockState));
        nbt.setInt("Time", this.time);
        nbt.setBoolean("DropItem", this.dropItem);
        nbt.setBoolean("HurtEntities", this.hurtEntities);
        nbt.setFloat("FallHurtAmount", this.fallDamagePerDistance);
        nbt.setInt("FallHurtMax", this.fallDamageMax);
        if (this.blockData != null) {
            nbt.set("TileEntityData", this.blockData);
        }

    }

    @Override
    protected void loadData(NBTTagCompound nbt) {
        this.blockState = GameProfileSerializer.readBlockState(nbt.getCompound("BlockState"));
        this.time = nbt.getInt("Time");
        if (nbt.hasKeyOfType("HurtEntities", 99)) {
            this.hurtEntities = nbt.getBoolean("HurtEntities");
            this.fallDamagePerDistance = nbt.getFloat("FallHurtAmount");
            this.fallDamageMax = nbt.getInt("FallHurtMax");
        } else if (this.blockState.is(TagsBlock.ANVIL)) {
            this.hurtEntities = true;
        }

        if (nbt.hasKeyOfType("DropItem", 99)) {
            this.dropItem = nbt.getBoolean("DropItem");
        }

        if (nbt.hasKeyOfType("TileEntityData", 10)) {
            this.blockData = nbt.getCompound("TileEntityData");
        }

        if (this.blockState.isAir()) {
            this.blockState = Blocks.SAND.getBlockData();
        }

    }

    public World getLevel() {
        return this.level;
    }

    public void setHurtsEntities(float fallHurtAmount, int fallHurtMax) {
        this.hurtEntities = true;
        this.fallDamagePerDistance = fallHurtAmount;
        this.fallDamageMax = fallHurtMax;
    }

    @Override
    public boolean displayFireAnimation() {
        return false;
    }

    @Override
    public void appendEntityCrashDetails(CrashReportSystemDetails section) {
        super.appendEntityCrashDetails(section);
        section.setDetail("Immitating BlockState", this.blockState.toString());
    }

    public IBlockData getBlock() {
        return this.blockState;
    }

    @Override
    public boolean onlyOpCanSetNbt() {
        return true;
    }

    @Override
    public Packet<?> getPacket() {
        return new PacketPlayOutSpawnEntity(this, Block.getCombinedId(this.getBlock()));
    }

    @Override
    public void recreateFromPacket(PacketPlayOutSpawnEntity packet) {
        super.recreateFromPacket(packet);
        this.blockState = Block.getByCombinedId(packet.getData());
        this.blocksBuilding = true;
        double d = packet.getX();
        double e = packet.getY();
        double f = packet.getZ();
        this.setPosition(d, e + (double)((1.0F - this.getHeight()) / 2.0F), f);
        this.setStartPos(this.getChunkCoordinates());
    }
}
