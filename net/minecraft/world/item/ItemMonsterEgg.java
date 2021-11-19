package net.minecraft.world.item;

import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.level.WorldServer;
import net.minecraft.stats.StatisticList;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.InteractionResultWrapper;
import net.minecraft.world.entity.EntityAgeable;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumMobSpawn;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.context.ItemActionContext;
import net.minecraft.world.level.MobSpawnerAbstract;
import net.minecraft.world.level.RayTrace;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.BlockFluids;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.entity.TileEntityMobSpawner;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.MovingObjectPosition;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import net.minecraft.world.phys.Vec3D;

public class ItemMonsterEgg extends Item {
    public static final Map<EntityTypes<? extends EntityInsentient>, ItemMonsterEgg> BY_ID = Maps.newIdentityHashMap();
    private final int backgroundColor;
    private final int highlightColor;
    private final EntityTypes<?> defaultType;

    public ItemMonsterEgg(EntityTypes<? extends EntityInsentient> type, int primaryColor, int secondaryColor, Item.Info settings) {
        super(settings);
        this.defaultType = type;
        this.backgroundColor = primaryColor;
        this.highlightColor = secondaryColor;
        BY_ID.put(type, this);
    }

    @Override
    public EnumInteractionResult useOn(ItemActionContext context) {
        World level = context.getWorld();
        if (!(level instanceof WorldServer)) {
            return EnumInteractionResult.SUCCESS;
        } else {
            ItemStack itemStack = context.getItemStack();
            BlockPosition blockPos = context.getClickPosition();
            EnumDirection direction = context.getClickedFace();
            IBlockData blockState = level.getType(blockPos);
            if (blockState.is(Blocks.SPAWNER)) {
                TileEntity blockEntity = level.getTileEntity(blockPos);
                if (blockEntity instanceof TileEntityMobSpawner) {
                    MobSpawnerAbstract baseSpawner = ((TileEntityMobSpawner)blockEntity).getSpawner();
                    EntityTypes<?> entityType = this.getType(itemStack.getTag());
                    baseSpawner.setMobName(entityType);
                    blockEntity.update();
                    level.notify(blockPos, blockState, blockState, 3);
                    itemStack.subtract(1);
                    return EnumInteractionResult.CONSUME;
                }
            }

            BlockPosition blockPos2;
            if (blockState.getCollisionShape(level, blockPos).isEmpty()) {
                blockPos2 = blockPos;
            } else {
                blockPos2 = blockPos.relative(direction);
            }

            EntityTypes<?> entityType2 = this.getType(itemStack.getTag());
            if (entityType2.spawnCreature((WorldServer)level, itemStack, context.getEntity(), blockPos2, EnumMobSpawn.SPAWN_EGG, true, !Objects.equals(blockPos, blockPos2) && direction == EnumDirection.UP) != null) {
                itemStack.subtract(1);
                level.gameEvent(context.getEntity(), GameEvent.ENTITY_PLACE, blockPos);
            }

            return EnumInteractionResult.CONSUME;
        }
    }

    @Override
    public InteractionResultWrapper<ItemStack> use(World world, EntityHuman user, EnumHand hand) {
        ItemStack itemStack = user.getItemInHand(hand);
        MovingObjectPosition hitResult = getPlayerPOVHitResult(world, user, RayTrace.FluidCollisionOption.SOURCE_ONLY);
        if (hitResult.getType() != MovingObjectPosition.EnumMovingObjectType.BLOCK) {
            return InteractionResultWrapper.pass(itemStack);
        } else if (!(world instanceof WorldServer)) {
            return InteractionResultWrapper.success(itemStack);
        } else {
            MovingObjectPositionBlock blockHitResult = (MovingObjectPositionBlock)hitResult;
            BlockPosition blockPos = blockHitResult.getBlockPosition();
            if (!(world.getType(blockPos).getBlock() instanceof BlockFluids)) {
                return InteractionResultWrapper.pass(itemStack);
            } else if (world.mayInteract(user, blockPos) && user.mayUseItemAt(blockPos, blockHitResult.getDirection(), itemStack)) {
                EntityTypes<?> entityType = this.getType(itemStack.getTag());
                if (entityType.spawnCreature((WorldServer)world, itemStack, user, blockPos, EnumMobSpawn.SPAWN_EGG, false, false) == null) {
                    return InteractionResultWrapper.pass(itemStack);
                } else {
                    if (!user.getAbilities().instabuild) {
                        itemStack.subtract(1);
                    }

                    user.awardStat(StatisticList.ITEM_USED.get(this));
                    world.gameEvent(GameEvent.ENTITY_PLACE, user);
                    return InteractionResultWrapper.consume(itemStack);
                }
            } else {
                return InteractionResultWrapper.fail(itemStack);
            }
        }
    }

    public boolean spawnsEntity(@Nullable NBTTagCompound nbt, EntityTypes<?> type) {
        return Objects.equals(this.getType(nbt), type);
    }

    public int getColor(int tintIndex) {
        return tintIndex == 0 ? this.backgroundColor : this.highlightColor;
    }

    @Nullable
    public static ItemMonsterEgg byId(@Nullable EntityTypes<?> type) {
        return BY_ID.get(type);
    }

    public static Iterable<ItemMonsterEgg> eggs() {
        return Iterables.unmodifiableIterable(BY_ID.values());
    }

    public EntityTypes<?> getType(@Nullable NBTTagCompound nbt) {
        if (nbt != null && nbt.hasKeyOfType("EntityTag", 10)) {
            NBTTagCompound compoundTag = nbt.getCompound("EntityTag");
            if (compoundTag.hasKeyOfType("id", 8)) {
                return EntityTypes.byString(compoundTag.getString("id")).orElse(this.defaultType);
            }
        }

        return this.defaultType;
    }

    public Optional<EntityInsentient> spawnOffspringFromSpawnEgg(EntityHuman user, EntityInsentient entity, EntityTypes<? extends EntityInsentient> entityType, WorldServer world, Vec3D pos, ItemStack stack) {
        if (!this.spawnsEntity(stack.getTag(), entityType)) {
            return Optional.empty();
        } else {
            EntityInsentient mob;
            if (entity instanceof EntityAgeable) {
                mob = ((EntityAgeable)entity).createChild(world, (EntityAgeable)entity);
            } else {
                mob = entityType.create(world);
            }

            if (mob == null) {
                return Optional.empty();
            } else {
                mob.setBaby(true);
                if (!mob.isBaby()) {
                    return Optional.empty();
                } else {
                    mob.setPositionRotation(pos.getX(), pos.getY(), pos.getZ(), 0.0F, 0.0F);
                    world.addAllEntities(mob);
                    if (stack.hasName()) {
                        mob.setCustomName(stack.getName());
                    }

                    if (!user.getAbilities().instabuild) {
                        stack.subtract(1);
                    }

                    return Optional.of(mob);
                }
            }
        }
    }
}
