package net.minecraft.world.level.block.entity;

import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.nbt.GameProfileSerializer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.protocol.game.PacketDebug;
import net.minecraft.sounds.EnumSoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.tags.TagsBlock;
import net.minecraft.tags.TagsEntity;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.EntityBee;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.BlockBeehive;
import net.minecraft.world.level.block.BlockCampfire;
import net.minecraft.world.level.block.BlockFire;
import net.minecraft.world.level.block.state.IBlockData;

public class TileEntityBeehive extends TileEntity {
    public static final String TAG_FLOWER_POS = "FlowerPos";
    public static final String MIN_OCCUPATION_TICKS = "MinOccupationTicks";
    public static final String ENTITY_DATA = "EntityData";
    public static final String TICKS_IN_HIVE = "TicksInHive";
    public static final String HAS_NECTAR = "HasNectar";
    public static final String BEES = "Bees";
    private static final List<String> IGNORED_BEE_TAGS = Arrays.asList("Air", "ArmorDropChances", "ArmorItems", "Brain", "CanPickUpLoot", "DeathTime", "FallDistance", "FallFlying", "Fire", "HandDropChances", "HandItems", "HurtByTimestamp", "HurtTime", "LeftHanded", "Motion", "NoGravity", "OnGround", "PortalCooldown", "Pos", "Rotation", "CannotEnterHiveTicks", "TicksSincePollination", "CropsGrownSincePollination", "HivePos", "Passengers", "Leash", "UUID");
    public static final int MAX_OCCUPANTS = 3;
    private static final int MIN_TICKS_BEFORE_REENTERING_HIVE = 400;
    private static final int MIN_OCCUPATION_TICKS_NECTAR = 2400;
    public static final int MIN_OCCUPATION_TICKS_NECTARLESS = 600;
    private final List<TileEntityBeehive.HiveBee> stored = Lists.newArrayList();
    @Nullable
    public BlockPosition savedFlowerPos;

    public TileEntityBeehive(BlockPosition pos, IBlockData state) {
        super(TileEntityTypes.BEEHIVE, pos, state);
    }

    @Override
    public void update() {
        if (this.isFireNearby()) {
            this.emptyAllLivingFromHive((EntityHuman)null, this.level.getType(this.getPosition()), TileEntityBeehive.ReleaseStatus.EMERGENCY);
        }

        super.update();
    }

    public boolean isFireNearby() {
        if (this.level == null) {
            return false;
        } else {
            for(BlockPosition blockPos : BlockPosition.betweenClosed(this.worldPosition.offset(-1, -1, -1), this.worldPosition.offset(1, 1, 1))) {
                if (this.level.getType(blockPos).getBlock() instanceof BlockFire) {
                    return true;
                }
            }

            return false;
        }
    }

    public boolean isEmpty() {
        return this.stored.isEmpty();
    }

    public boolean isFull() {
        return this.stored.size() == 3;
    }

    public void emptyAllLivingFromHive(@Nullable EntityHuman player, IBlockData state, TileEntityBeehive.ReleaseStatus beeState) {
        List<Entity> list = this.releaseBees(state, beeState);
        if (player != null) {
            for(Entity entity : list) {
                if (entity instanceof EntityBee) {
                    EntityBee bee = (EntityBee)entity;
                    if (player.getPositionVector().distanceSquared(entity.getPositionVector()) <= 16.0D) {
                        if (!this.isSedated()) {
                            bee.setGoalTarget(player);
                        } else {
                            bee.setCannotEnterHiveTicks(400);
                        }
                    }
                }
            }
        }

    }

    private List<Entity> releaseBees(IBlockData state, TileEntityBeehive.ReleaseStatus beeState) {
        List<Entity> list = Lists.newArrayList();
        this.stored.removeIf((bee) -> {
            return releaseBee(this.level, this.worldPosition, state, bee, list, beeState, this.savedFlowerPos);
        });
        return list;
    }

    public void addBee(Entity entity, boolean hasNectar) {
        this.addOccupantWithPresetTicks(entity, hasNectar, 0);
    }

    @VisibleForDebug
    public int getBeeCount() {
        return this.stored.size();
    }

    public static int getHoneyLevel(IBlockData state) {
        return state.get(BlockBeehive.HONEY_LEVEL);
    }

    @VisibleForDebug
    public boolean isSedated() {
        return BlockCampfire.isSmokeyPos(this.level, this.getPosition());
    }

    public void addOccupantWithPresetTicks(Entity entity, boolean hasNectar, int ticksInHive) {
        if (this.stored.size() < 3) {
            entity.stopRiding();
            entity.ejectPassengers();
            NBTTagCompound compoundTag = new NBTTagCompound();
            entity.save(compoundTag);
            this.storeBee(compoundTag, ticksInHive, hasNectar);
            if (this.level != null) {
                if (entity instanceof EntityBee) {
                    EntityBee bee = (EntityBee)entity;
                    if (bee.hasFlowerPos() && (!this.hasSavedFlowerPos() || this.level.random.nextBoolean())) {
                        this.savedFlowerPos = bee.getFlowerPos();
                    }
                }

                BlockPosition blockPos = this.getPosition();
                this.level.playSound((EntityHuman)null, (double)blockPos.getX(), (double)blockPos.getY(), (double)blockPos.getZ(), SoundEffects.BEEHIVE_ENTER, EnumSoundCategory.BLOCKS, 1.0F, 1.0F);
            }

            entity.die();
        }
    }

    public void storeBee(NBTTagCompound nbtCompound, int ticksInHive, boolean hasNectar) {
        this.stored.add(new TileEntityBeehive.HiveBee(nbtCompound, ticksInHive, hasNectar ? 2400 : 600));
    }

    private static boolean releaseBee(World world, BlockPosition pos, IBlockData state, TileEntityBeehive.HiveBee bee, @Nullable List<Entity> entities, TileEntityBeehive.ReleaseStatus beeState, @Nullable BlockPosition flowerPos) {
        if ((world.isNight() || world.isRaining()) && beeState != TileEntityBeehive.ReleaseStatus.EMERGENCY) {
            return false;
        } else {
            NBTTagCompound compoundTag = bee.entityData;
            removeIgnoredBeeTags(compoundTag);
            compoundTag.set("HivePos", GameProfileSerializer.writeBlockPos(pos));
            compoundTag.setBoolean("NoGravity", true);
            EnumDirection direction = state.get(BlockBeehive.FACING);
            BlockPosition blockPos = pos.relative(direction);
            boolean bl = !world.getType(blockPos).getCollisionShape(world, blockPos).isEmpty();
            if (bl && beeState != TileEntityBeehive.ReleaseStatus.EMERGENCY) {
                return false;
            } else {
                Entity entity = EntityTypes.loadEntityRecursive(compoundTag, world, (entityx) -> {
                    return entityx;
                });
                if (entity != null) {
                    if (!entity.getEntityType().is(TagsEntity.BEEHIVE_INHABITORS)) {
                        return false;
                    } else {
                        if (entity instanceof EntityBee) {
                            EntityBee bee2 = (EntityBee)entity;
                            if (flowerPos != null && !bee2.hasFlowerPos() && world.random.nextFloat() < 0.9F) {
                                bee2.setFlowerPos(flowerPos);
                            }

                            if (beeState == TileEntityBeehive.ReleaseStatus.HONEY_DELIVERED) {
                                bee2.dropOffNectar();
                                if (state.is(TagsBlock.BEEHIVES)) {
                                    int i = getHoneyLevel(state);
                                    if (i < 5) {
                                        int j = world.random.nextInt(100) == 0 ? 2 : 1;
                                        if (i + j > 5) {
                                            --j;
                                        }

                                        world.setTypeUpdate(pos, state.set(BlockBeehive.HONEY_LEVEL, Integer.valueOf(i + j)));
                                    }
                                }
                            }

                            setBeeReleaseData(bee.ticksInHive, bee2);
                            if (entities != null) {
                                entities.add(bee2);
                            }

                            float f = entity.getWidth();
                            double d = bl ? 0.0D : 0.55D + (double)(f / 2.0F);
                            double e = (double)pos.getX() + 0.5D + d * (double)direction.getAdjacentX();
                            double g = (double)pos.getY() + 0.5D - (double)(entity.getHeight() / 2.0F);
                            double h = (double)pos.getZ() + 0.5D + d * (double)direction.getAdjacentZ();
                            entity.setPositionRotation(e, g, h, entity.getYRot(), entity.getXRot());
                        }

                        world.playSound((EntityHuman)null, pos, SoundEffects.BEEHIVE_EXIT, EnumSoundCategory.BLOCKS, 1.0F, 1.0F);
                        return world.addEntity(entity);
                    }
                } else {
                    return false;
                }
            }
        }
    }

    static void removeIgnoredBeeTags(NBTTagCompound compound) {
        for(String string : IGNORED_BEE_TAGS) {
            compound.remove(string);
        }

    }

    private static void setBeeReleaseData(int ticks, EntityBee bee) {
        int i = bee.getAge();
        if (i < 0) {
            bee.setAgeRaw(Math.min(0, i + ticks));
        } else if (i > 0) {
            bee.setAgeRaw(Math.max(0, i - ticks));
        }

        bee.setLoveTicks(Math.max(0, bee.getInLoveTime() - ticks));
    }

    private boolean hasSavedFlowerPos() {
        return this.savedFlowerPos != null;
    }

    private static void tickOccupants(World world, BlockPosition pos, IBlockData state, List<TileEntityBeehive.HiveBee> bees, @Nullable BlockPosition flowerPos) {
        TileEntityBeehive.HiveBee beeData;
        for(Iterator<TileEntityBeehive.HiveBee> iterator = bees.iterator(); iterator.hasNext(); ++beeData.ticksInHive) {
            beeData = iterator.next();
            if (beeData.ticksInHive > beeData.minOccupationTicks) {
                TileEntityBeehive.ReleaseStatus beeReleaseStatus = beeData.entityData.getBoolean("HasNectar") ? TileEntityBeehive.ReleaseStatus.HONEY_DELIVERED : TileEntityBeehive.ReleaseStatus.BEE_RELEASED;
                if (releaseBee(world, pos, state, beeData, (List<Entity>)null, beeReleaseStatus, flowerPos)) {
                    iterator.remove();
                }
            }
        }

    }

    public static void serverTick(World world, BlockPosition pos, IBlockData state, TileEntityBeehive blockEntity) {
        tickOccupants(world, pos, state, blockEntity.stored, blockEntity.savedFlowerPos);
        if (!blockEntity.stored.isEmpty() && world.getRandom().nextDouble() < 0.005D) {
            double d = (double)pos.getX() + 0.5D;
            double e = (double)pos.getY();
            double f = (double)pos.getZ() + 0.5D;
            world.playSound((EntityHuman)null, d, e, f, SoundEffects.BEEHIVE_WORK, EnumSoundCategory.BLOCKS, 1.0F, 1.0F);
        }

        PacketDebug.sendHiveInfo(world, pos, state, blockEntity);
    }

    @Override
    public void load(NBTTagCompound nbt) {
        super.load(nbt);
        this.stored.clear();
        NBTTagList listTag = nbt.getList("Bees", 10);

        for(int i = 0; i < listTag.size(); ++i) {
            NBTTagCompound compoundTag = listTag.getCompound(i);
            TileEntityBeehive.HiveBee beeData = new TileEntityBeehive.HiveBee(compoundTag.getCompound("EntityData"), compoundTag.getInt("TicksInHive"), compoundTag.getInt("MinOccupationTicks"));
            this.stored.add(beeData);
        }

        this.savedFlowerPos = null;
        if (nbt.hasKey("FlowerPos")) {
            this.savedFlowerPos = GameProfileSerializer.readBlockPos(nbt.getCompound("FlowerPos"));
        }

    }

    @Override
    protected void saveAdditional(NBTTagCompound nbt) {
        super.saveAdditional(nbt);
        nbt.set("Bees", this.writeBees());
        if (this.hasSavedFlowerPos()) {
            nbt.set("FlowerPos", GameProfileSerializer.writeBlockPos(this.savedFlowerPos));
        }

    }

    public NBTTagList writeBees() {
        NBTTagList listTag = new NBTTagList();

        for(TileEntityBeehive.HiveBee beeData : this.stored) {
            beeData.entityData.remove("UUID");
            NBTTagCompound compoundTag = new NBTTagCompound();
            compoundTag.set("EntityData", beeData.entityData);
            compoundTag.setInt("TicksInHive", beeData.ticksInHive);
            compoundTag.setInt("MinOccupationTicks", beeData.minOccupationTicks);
            listTag.add(compoundTag);
        }

        return listTag;
    }

    static class HiveBee {
        final NBTTagCompound entityData;
        int ticksInHive;
        final int minOccupationTicks;

        HiveBee(NBTTagCompound entityData, int ticksInHive, int minOccupationTicks) {
            TileEntityBeehive.removeIgnoredBeeTags(entityData);
            this.entityData = entityData;
            this.ticksInHive = ticksInHive;
            this.minOccupationTicks = minOccupationTicks;
        }
    }

    public static enum ReleaseStatus {
        HONEY_DELIVERED,
        BEE_RELEASED,
        EMERGENCY;
    }
}
