package net.minecraft.core.dispenser;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import net.minecraft.SystemUtils;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.IPosition;
import net.minecraft.core.ISourceBlock;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.EnumSoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.tags.TagsBlock;
import net.minecraft.tags.TagsFluid;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumMobSpawn;
import net.minecraft.world.entity.ISaddleable;
import net.minecraft.world.entity.animal.horse.EntityHorseAbstract;
import net.minecraft.world.entity.animal.horse.EntityHorseChestedAbstract;
import net.minecraft.world.entity.decoration.EntityArmorStand;
import net.minecraft.world.entity.item.EntityTNTPrimed;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.projectile.EntityArrow;
import net.minecraft.world.entity.projectile.EntityEgg;
import net.minecraft.world.entity.projectile.EntityFireworks;
import net.minecraft.world.entity.projectile.EntityPotion;
import net.minecraft.world.entity.projectile.EntitySmallFireball;
import net.minecraft.world.entity.projectile.EntitySnowball;
import net.minecraft.world.entity.projectile.EntitySpectralArrow;
import net.minecraft.world.entity.projectile.EntityThrownExpBottle;
import net.minecraft.world.entity.projectile.EntityTippedArrow;
import net.minecraft.world.entity.projectile.IProjectile;
import net.minecraft.world.entity.vehicle.EntityBoat;
import net.minecraft.world.item.DispensibleContainerItem;
import net.minecraft.world.item.EnumColor;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemArmor;
import net.minecraft.world.item.ItemBoneMeal;
import net.minecraft.world.item.ItemMonsterEgg;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtil;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BlockBeehive;
import net.minecraft.world.level.block.BlockCampfire;
import net.minecraft.world.level.block.BlockCandle;
import net.minecraft.world.level.block.BlockCandleCake;
import net.minecraft.world.level.block.BlockDispenser;
import net.minecraft.world.level.block.BlockFireAbstract;
import net.minecraft.world.level.block.BlockPumpkinCarved;
import net.minecraft.world.level.block.BlockRespawnAnchor;
import net.minecraft.world.level.block.BlockShulkerBox;
import net.minecraft.world.level.block.BlockSkull;
import net.minecraft.world.level.block.BlockTNT;
import net.minecraft.world.level.block.BlockWitherSkull;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.IFluidSource;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.entity.TileEntityBeehive;
import net.minecraft.world.level.block.entity.TileEntityDispenser;
import net.minecraft.world.level.block.entity.TileEntitySkull;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public interface IDispenseBehavior {
    Logger LOGGER = LogManager.getLogger();
    IDispenseBehavior NOOP = (pointer, stack) -> {
        return stack;
    };

    ItemStack dispense(ISourceBlock pointer, ItemStack stack);

    static void bootStrap() {
        BlockDispenser.registerBehavior(Items.ARROW, new DispenseBehaviorProjectile() {
            @Override
            protected IProjectile a(World world, IPosition position, ItemStack stack) {
                EntityTippedArrow arrow = new EntityTippedArrow(world, position.getX(), position.getY(), position.getZ());
                arrow.pickup = EntityArrow.PickupStatus.ALLOWED;
                return arrow;
            }
        });
        BlockDispenser.registerBehavior(Items.TIPPED_ARROW, new DispenseBehaviorProjectile() {
            @Override
            protected IProjectile a(World world, IPosition position, ItemStack stack) {
                EntityTippedArrow arrow = new EntityTippedArrow(world, position.getX(), position.getY(), position.getZ());
                arrow.setEffectsFromItem(stack);
                arrow.pickup = EntityArrow.PickupStatus.ALLOWED;
                return arrow;
            }
        });
        BlockDispenser.registerBehavior(Items.SPECTRAL_ARROW, new DispenseBehaviorProjectile() {
            @Override
            protected IProjectile a(World world, IPosition position, ItemStack stack) {
                EntityArrow abstractArrow = new EntitySpectralArrow(world, position.getX(), position.getY(), position.getZ());
                abstractArrow.pickup = EntityArrow.PickupStatus.ALLOWED;
                return abstractArrow;
            }
        });
        BlockDispenser.registerBehavior(Items.EGG, new DispenseBehaviorProjectile() {
            @Override
            protected IProjectile a(World world, IPosition position, ItemStack stack) {
                return SystemUtils.make(new EntityEgg(world, position.getX(), position.getY(), position.getZ()), (entity) -> {
                    entity.setItem(stack);
                });
            }
        });
        BlockDispenser.registerBehavior(Items.SNOWBALL, new DispenseBehaviorProjectile() {
            @Override
            protected IProjectile a(World world, IPosition position, ItemStack stack) {
                return SystemUtils.make(new EntitySnowball(world, position.getX(), position.getY(), position.getZ()), (entity) -> {
                    entity.setItem(stack);
                });
            }
        });
        BlockDispenser.registerBehavior(Items.EXPERIENCE_BOTTLE, new DispenseBehaviorProjectile() {
            @Override
            protected IProjectile a(World world, IPosition position, ItemStack stack) {
                return SystemUtils.make(new EntityThrownExpBottle(world, position.getX(), position.getY(), position.getZ()), (entity) -> {
                    entity.setItem(stack);
                });
            }

            @Override
            protected float a() {
                return super.a() * 0.5F;
            }

            @Override
            protected float b() {
                return super.b() * 1.25F;
            }
        });
        BlockDispenser.registerBehavior(Items.SPLASH_POTION, new IDispenseBehavior() {
            @Override
            public ItemStack dispense(ISourceBlock pointer, ItemStack stack) {
                return (new DispenseBehaviorProjectile() {
                    @Override
                    protected IProjectile a(World world, IPosition position, ItemStack stack) {
                        return SystemUtils.make(new EntityPotion(world, position.getX(), position.getY(), position.getZ()), (entity) -> {
                            entity.setItem(stack);
                        });
                    }

                    @Override
                    protected float a() {
                        return super.a() * 0.5F;
                    }

                    @Override
                    protected float b() {
                        return super.b() * 1.25F;
                    }
                }).dispense(pointer, stack);
            }
        });
        BlockDispenser.registerBehavior(Items.LINGERING_POTION, new IDispenseBehavior() {
            @Override
            public ItemStack dispense(ISourceBlock pointer, ItemStack stack) {
                return (new DispenseBehaviorProjectile() {
                    @Override
                    protected IProjectile a(World world, IPosition position, ItemStack stack) {
                        return SystemUtils.make(new EntityPotion(world, position.getX(), position.getY(), position.getZ()), (entity) -> {
                            entity.setItem(stack);
                        });
                    }

                    @Override
                    protected float a() {
                        return super.a() * 0.5F;
                    }

                    @Override
                    protected float b() {
                        return super.b() * 1.25F;
                    }
                }).dispense(pointer, stack);
            }
        });
        DispenseBehaviorItem defaultDispenseItemBehavior = new DispenseBehaviorItem() {
            @Override
            public ItemStack a(ISourceBlock pointer, ItemStack stack) {
                EnumDirection direction = pointer.getBlockData().get(BlockDispenser.FACING);
                EntityTypes<?> entityType = ((ItemMonsterEgg)stack.getItem()).getType(stack.getTag());

                try {
                    entityType.spawnCreature(pointer.getWorld(), stack, (EntityHuman)null, pointer.getBlockPosition().relative(direction), EnumMobSpawn.DISPENSER, direction != EnumDirection.UP, false);
                } catch (Exception var6) {
                    LOGGER.error("Error while dispensing spawn egg from dispenser at {}", pointer.getBlockPosition(), var6);
                    return ItemStack.EMPTY;
                }

                stack.subtract(1);
                pointer.getWorld().gameEvent(GameEvent.ENTITY_PLACE, pointer.getBlockPosition());
                return stack;
            }
        };

        for(ItemMonsterEgg spawnEggItem : ItemMonsterEgg.eggs()) {
            BlockDispenser.registerBehavior(spawnEggItem, defaultDispenseItemBehavior);
        }

        BlockDispenser.registerBehavior(Items.ARMOR_STAND, new DispenseBehaviorItem() {
            @Override
            public ItemStack a(ISourceBlock pointer, ItemStack stack) {
                EnumDirection direction = pointer.getBlockData().get(BlockDispenser.FACING);
                BlockPosition blockPos = pointer.getBlockPosition().relative(direction);
                World level = pointer.getWorld();
                EntityArmorStand armorStand = new EntityArmorStand(level, (double)blockPos.getX() + 0.5D, (double)blockPos.getY(), (double)blockPos.getZ() + 0.5D);
                EntityTypes.updateCustomEntityTag(level, (EntityHuman)null, armorStand, stack.getTag());
                armorStand.setYRot(direction.toYRot());
                level.addEntity(armorStand);
                stack.subtract(1);
                return stack;
            }
        });
        BlockDispenser.registerBehavior(Items.SADDLE, new DispenseBehaviorMaybe() {
            @Override
            public ItemStack a(ISourceBlock pointer, ItemStack stack) {
                BlockPosition blockPos = pointer.getBlockPosition().relative(pointer.getBlockData().get(BlockDispenser.FACING));
                List<EntityLiving> list = pointer.getWorld().getEntitiesOfClass(EntityLiving.class, new AxisAlignedBB(blockPos), (entity) -> {
                    if (!(entity instanceof ISaddleable)) {
                        return false;
                    } else {
                        ISaddleable saddleable = (ISaddleable)entity;
                        return !saddleable.hasSaddle() && saddleable.canSaddle();
                    }
                });
                if (!list.isEmpty()) {
                    ((ISaddleable)list.get(0)).saddle(EnumSoundCategory.BLOCKS);
                    stack.subtract(1);
                    this.setSuccess(true);
                    return stack;
                } else {
                    return super.a(pointer, stack);
                }
            }
        });
        DispenseBehaviorItem defaultDispenseItemBehavior2 = new DispenseBehaviorMaybe() {
            @Override
            protected ItemStack a(ISourceBlock pointer, ItemStack stack) {
                BlockPosition blockPos = pointer.getBlockPosition().relative(pointer.getBlockData().get(BlockDispenser.FACING));

                for(EntityHorseAbstract abstractHorse : pointer.getWorld().getEntitiesOfClass(EntityHorseAbstract.class, new AxisAlignedBB(blockPos), (entity) -> {
                    return entity.isAlive() && entity.canWearArmor();
                })) {
                    if (abstractHorse.isArmor(stack) && !abstractHorse.isWearingArmor() && abstractHorse.isTamed()) {
                        abstractHorse.getSlot(401).set(stack.cloneAndSubtract(1));
                        this.setSuccess(true);
                        return stack;
                    }
                }

                return super.a(pointer, stack);
            }
        };
        BlockDispenser.registerBehavior(Items.LEATHER_HORSE_ARMOR, defaultDispenseItemBehavior2);
        BlockDispenser.registerBehavior(Items.IRON_HORSE_ARMOR, defaultDispenseItemBehavior2);
        BlockDispenser.registerBehavior(Items.GOLDEN_HORSE_ARMOR, defaultDispenseItemBehavior2);
        BlockDispenser.registerBehavior(Items.DIAMOND_HORSE_ARMOR, defaultDispenseItemBehavior2);
        BlockDispenser.registerBehavior(Items.WHITE_CARPET, defaultDispenseItemBehavior2);
        BlockDispenser.registerBehavior(Items.ORANGE_CARPET, defaultDispenseItemBehavior2);
        BlockDispenser.registerBehavior(Items.CYAN_CARPET, defaultDispenseItemBehavior2);
        BlockDispenser.registerBehavior(Items.BLUE_CARPET, defaultDispenseItemBehavior2);
        BlockDispenser.registerBehavior(Items.BROWN_CARPET, defaultDispenseItemBehavior2);
        BlockDispenser.registerBehavior(Items.BLACK_CARPET, defaultDispenseItemBehavior2);
        BlockDispenser.registerBehavior(Items.GRAY_CARPET, defaultDispenseItemBehavior2);
        BlockDispenser.registerBehavior(Items.GREEN_CARPET, defaultDispenseItemBehavior2);
        BlockDispenser.registerBehavior(Items.LIGHT_BLUE_CARPET, defaultDispenseItemBehavior2);
        BlockDispenser.registerBehavior(Items.LIGHT_GRAY_CARPET, defaultDispenseItemBehavior2);
        BlockDispenser.registerBehavior(Items.LIME_CARPET, defaultDispenseItemBehavior2);
        BlockDispenser.registerBehavior(Items.MAGENTA_CARPET, defaultDispenseItemBehavior2);
        BlockDispenser.registerBehavior(Items.PINK_CARPET, defaultDispenseItemBehavior2);
        BlockDispenser.registerBehavior(Items.PURPLE_CARPET, defaultDispenseItemBehavior2);
        BlockDispenser.registerBehavior(Items.RED_CARPET, defaultDispenseItemBehavior2);
        BlockDispenser.registerBehavior(Items.YELLOW_CARPET, defaultDispenseItemBehavior2);
        BlockDispenser.registerBehavior(Items.CHEST, new DispenseBehaviorMaybe() {
            @Override
            public ItemStack a(ISourceBlock pointer, ItemStack stack) {
                BlockPosition blockPos = pointer.getBlockPosition().relative(pointer.getBlockData().get(BlockDispenser.FACING));

                for(EntityHorseChestedAbstract abstractChestedHorse : pointer.getWorld().getEntitiesOfClass(EntityHorseChestedAbstract.class, new AxisAlignedBB(blockPos), (entity) -> {
                    return entity.isAlive() && !entity.isCarryingChest();
                })) {
                    if (abstractChestedHorse.isTamed() && abstractChestedHorse.getSlot(499).set(stack)) {
                        stack.subtract(1);
                        this.setSuccess(true);
                        return stack;
                    }
                }

                return super.a(pointer, stack);
            }
        });
        BlockDispenser.registerBehavior(Items.FIREWORK_ROCKET, new DispenseBehaviorItem() {
            @Override
            public ItemStack a(ISourceBlock pointer, ItemStack stack) {
                EnumDirection direction = pointer.getBlockData().get(BlockDispenser.FACING);
                EntityFireworks fireworkRocketEntity = new EntityFireworks(pointer.getWorld(), stack, pointer.getX(), pointer.getY(), pointer.getX(), true);
                IDispenseBehavior.setEntityPokingOutOfBlock(pointer, fireworkRocketEntity, direction);
                fireworkRocketEntity.shoot((double)direction.getAdjacentX(), (double)direction.getAdjacentY(), (double)direction.getAdjacentZ(), 0.5F, 1.0F);
                pointer.getWorld().addEntity(fireworkRocketEntity);
                stack.subtract(1);
                return stack;
            }

            @Override
            protected void a(ISourceBlock pointer) {
                pointer.getWorld().triggerEffect(1004, pointer.getBlockPosition(), 0);
            }
        });
        BlockDispenser.registerBehavior(Items.FIRE_CHARGE, new DispenseBehaviorItem() {
            @Override
            public ItemStack a(ISourceBlock pointer, ItemStack stack) {
                EnumDirection direction = pointer.getBlockData().get(BlockDispenser.FACING);
                IPosition position = BlockDispenser.getDispensePosition(pointer);
                double d = position.getX() + (double)((float)direction.getAdjacentX() * 0.3F);
                double e = position.getY() + (double)((float)direction.getAdjacentY() * 0.3F);
                double f = position.getZ() + (double)((float)direction.getAdjacentZ() * 0.3F);
                World level = pointer.getWorld();
                Random random = level.random;
                double g = random.nextGaussian() * 0.05D + (double)direction.getAdjacentX();
                double h = random.nextGaussian() * 0.05D + (double)direction.getAdjacentY();
                double i = random.nextGaussian() * 0.05D + (double)direction.getAdjacentZ();
                EntitySmallFireball smallFireball = new EntitySmallFireball(level, d, e, f, g, h, i);
                level.addEntity(SystemUtils.make(smallFireball, (entity) -> {
                    entity.setItem(stack);
                }));
                stack.subtract(1);
                return stack;
            }

            @Override
            protected void a(ISourceBlock pointer) {
                pointer.getWorld().triggerEffect(1018, pointer.getBlockPosition(), 0);
            }
        });
        BlockDispenser.registerBehavior(Items.OAK_BOAT, new DispenseBehaviorBoat(EntityBoat.EnumBoatType.OAK));
        BlockDispenser.registerBehavior(Items.SPRUCE_BOAT, new DispenseBehaviorBoat(EntityBoat.EnumBoatType.SPRUCE));
        BlockDispenser.registerBehavior(Items.BIRCH_BOAT, new DispenseBehaviorBoat(EntityBoat.EnumBoatType.BIRCH));
        BlockDispenser.registerBehavior(Items.JUNGLE_BOAT, new DispenseBehaviorBoat(EntityBoat.EnumBoatType.JUNGLE));
        BlockDispenser.registerBehavior(Items.DARK_OAK_BOAT, new DispenseBehaviorBoat(EntityBoat.EnumBoatType.DARK_OAK));
        BlockDispenser.registerBehavior(Items.ACACIA_BOAT, new DispenseBehaviorBoat(EntityBoat.EnumBoatType.ACACIA));
        IDispenseBehavior dispenseItemBehavior = new DispenseBehaviorItem() {
            private final DispenseBehaviorItem defaultDispenseItemBehavior = new DispenseBehaviorItem();

            @Override
            public ItemStack a(ISourceBlock pointer, ItemStack stack) {
                DispensibleContainerItem dispensibleContainerItem = (DispensibleContainerItem)stack.getItem();
                BlockPosition blockPos = pointer.getBlockPosition().relative(pointer.getBlockData().get(BlockDispenser.FACING));
                World level = pointer.getWorld();
                if (dispensibleContainerItem.emptyContents((EntityHuman)null, level, blockPos, (MovingObjectPositionBlock)null)) {
                    dispensibleContainerItem.checkExtraContent((EntityHuman)null, level, stack, blockPos);
                    return new ItemStack(Items.BUCKET);
                } else {
                    return this.defaultDispenseItemBehavior.dispense(pointer, stack);
                }
            }
        };
        BlockDispenser.registerBehavior(Items.LAVA_BUCKET, dispenseItemBehavior);
        BlockDispenser.registerBehavior(Items.WATER_BUCKET, dispenseItemBehavior);
        BlockDispenser.registerBehavior(Items.POWDER_SNOW_BUCKET, dispenseItemBehavior);
        BlockDispenser.registerBehavior(Items.SALMON_BUCKET, dispenseItemBehavior);
        BlockDispenser.registerBehavior(Items.COD_BUCKET, dispenseItemBehavior);
        BlockDispenser.registerBehavior(Items.PUFFERFISH_BUCKET, dispenseItemBehavior);
        BlockDispenser.registerBehavior(Items.TROPICAL_FISH_BUCKET, dispenseItemBehavior);
        BlockDispenser.registerBehavior(Items.AXOLOTL_BUCKET, dispenseItemBehavior);
        BlockDispenser.registerBehavior(Items.BUCKET, new DispenseBehaviorItem() {
            private final DispenseBehaviorItem defaultDispenseItemBehavior = new DispenseBehaviorItem();

            @Override
            public ItemStack a(ISourceBlock pointer, ItemStack stack) {
                GeneratorAccess levelAccessor = pointer.getWorld();
                BlockPosition blockPos = pointer.getBlockPosition().relative(pointer.getBlockData().get(BlockDispenser.FACING));
                IBlockData blockState = levelAccessor.getType(blockPos);
                Block block = blockState.getBlock();
                if (block instanceof IFluidSource) {
                    ItemStack itemStack = ((IFluidSource)block).removeFluid(levelAccessor, blockPos, blockState);
                    if (itemStack.isEmpty()) {
                        return super.a(pointer, stack);
                    } else {
                        levelAccessor.gameEvent((Entity)null, GameEvent.FLUID_PICKUP, blockPos);
                        Item item = itemStack.getItem();
                        stack.subtract(1);
                        if (stack.isEmpty()) {
                            return new ItemStack(item);
                        } else {
                            if (pointer.<TileEntityDispenser>getTileEntity().addItem(new ItemStack(item)) < 0) {
                                this.defaultDispenseItemBehavior.dispense(pointer, new ItemStack(item));
                            }

                            return stack;
                        }
                    }
                } else {
                    return super.a(pointer, stack);
                }
            }
        });
        BlockDispenser.registerBehavior(Items.FLINT_AND_STEEL, new DispenseBehaviorMaybe() {
            @Override
            protected ItemStack a(ISourceBlock pointer, ItemStack stack) {
                World level = pointer.getWorld();
                this.setSuccess(true);
                EnumDirection direction = pointer.getBlockData().get(BlockDispenser.FACING);
                BlockPosition blockPos = pointer.getBlockPosition().relative(direction);
                IBlockData blockState = level.getType(blockPos);
                if (BlockFireAbstract.canBePlacedAt(level, blockPos, direction)) {
                    level.setTypeUpdate(blockPos, BlockFireAbstract.getState(level, blockPos));
                    level.gameEvent((Entity)null, GameEvent.BLOCK_PLACE, blockPos);
                } else if (!BlockCampfire.canLight(blockState) && !BlockCandle.canLight(blockState) && !BlockCandleCake.canLight(blockState)) {
                    if (blockState.getBlock() instanceof BlockTNT) {
                        BlockTNT.explode(level, blockPos);
                        level.removeBlock(blockPos, false);
                    } else {
                        this.setSuccess(false);
                    }
                } else {
                    level.setTypeUpdate(blockPos, blockState.set(BlockProperties.LIT, Boolean.valueOf(true)));
                    level.gameEvent((Entity)null, GameEvent.BLOCK_CHANGE, blockPos);
                }

                if (this.isSuccess() && stack.isDamaged(1, level.random, (EntityPlayer)null)) {
                    stack.setCount(0);
                }

                return stack;
            }
        });
        BlockDispenser.registerBehavior(Items.BONE_MEAL, new DispenseBehaviorMaybe() {
            @Override
            protected ItemStack a(ISourceBlock pointer, ItemStack stack) {
                this.setSuccess(true);
                World level = pointer.getWorld();
                BlockPosition blockPos = pointer.getBlockPosition().relative(pointer.getBlockData().get(BlockDispenser.FACING));
                if (!ItemBoneMeal.growCrop(stack, level, blockPos) && !ItemBoneMeal.growWaterPlant(stack, level, blockPos, (EnumDirection)null)) {
                    this.setSuccess(false);
                } else if (!level.isClientSide) {
                    level.triggerEffect(1505, blockPos, 0);
                }

                return stack;
            }
        });
        BlockDispenser.registerBehavior(Blocks.TNT, new DispenseBehaviorItem() {
            @Override
            protected ItemStack a(ISourceBlock pointer, ItemStack stack) {
                World level = pointer.getWorld();
                BlockPosition blockPos = pointer.getBlockPosition().relative(pointer.getBlockData().get(BlockDispenser.FACING));
                EntityTNTPrimed primedTnt = new EntityTNTPrimed(level, (double)blockPos.getX() + 0.5D, (double)blockPos.getY(), (double)blockPos.getZ() + 0.5D, (EntityLiving)null);
                level.addEntity(primedTnt);
                level.playSound((EntityHuman)null, primedTnt.locX(), primedTnt.locY(), primedTnt.locZ(), SoundEffects.TNT_PRIMED, EnumSoundCategory.BLOCKS, 1.0F, 1.0F);
                level.gameEvent((Entity)null, GameEvent.ENTITY_PLACE, blockPos);
                stack.subtract(1);
                return stack;
            }
        });
        IDispenseBehavior dispenseItemBehavior2 = new DispenseBehaviorMaybe() {
            @Override
            protected ItemStack a(ISourceBlock pointer, ItemStack stack) {
                this.setSuccess(ItemArmor.dispenseArmor(pointer, stack));
                return stack;
            }
        };
        BlockDispenser.registerBehavior(Items.CREEPER_HEAD, dispenseItemBehavior2);
        BlockDispenser.registerBehavior(Items.ZOMBIE_HEAD, dispenseItemBehavior2);
        BlockDispenser.registerBehavior(Items.DRAGON_HEAD, dispenseItemBehavior2);
        BlockDispenser.registerBehavior(Items.SKELETON_SKULL, dispenseItemBehavior2);
        BlockDispenser.registerBehavior(Items.PLAYER_HEAD, dispenseItemBehavior2);
        BlockDispenser.registerBehavior(Items.WITHER_SKELETON_SKULL, new DispenseBehaviorMaybe() {
            @Override
            protected ItemStack a(ISourceBlock pointer, ItemStack stack) {
                World level = pointer.getWorld();
                EnumDirection direction = pointer.getBlockData().get(BlockDispenser.FACING);
                BlockPosition blockPos = pointer.getBlockPosition().relative(direction);
                if (level.isEmpty(blockPos) && BlockWitherSkull.canSpawnMob(level, blockPos, stack)) {
                    level.setTypeAndData(blockPos, Blocks.WITHER_SKELETON_SKULL.getBlockData().set(BlockSkull.ROTATION, Integer.valueOf(direction.getAxis() == EnumDirection.EnumAxis.Y ? 0 : direction.opposite().get2DRotationValue() * 4)), 3);
                    level.gameEvent((Entity)null, GameEvent.BLOCK_PLACE, blockPos);
                    TileEntity blockEntity = level.getTileEntity(blockPos);
                    if (blockEntity instanceof TileEntitySkull) {
                        BlockWitherSkull.checkSpawn(level, blockPos, (TileEntitySkull)blockEntity);
                    }

                    stack.subtract(1);
                    this.setSuccess(true);
                } else {
                    this.setSuccess(ItemArmor.dispenseArmor(pointer, stack));
                }

                return stack;
            }
        });
        BlockDispenser.registerBehavior(Blocks.CARVED_PUMPKIN, new DispenseBehaviorMaybe() {
            @Override
            protected ItemStack a(ISourceBlock pointer, ItemStack stack) {
                World level = pointer.getWorld();
                BlockPosition blockPos = pointer.getBlockPosition().relative(pointer.getBlockData().get(BlockDispenser.FACING));
                BlockPumpkinCarved carvedPumpkinBlock = (BlockPumpkinCarved)Blocks.CARVED_PUMPKIN;
                if (level.isEmpty(blockPos) && carvedPumpkinBlock.canSpawnGolem(level, blockPos)) {
                    if (!level.isClientSide) {
                        level.setTypeAndData(blockPos, carvedPumpkinBlock.getBlockData(), 3);
                        level.gameEvent((Entity)null, GameEvent.BLOCK_PLACE, blockPos);
                    }

                    stack.subtract(1);
                    this.setSuccess(true);
                } else {
                    this.setSuccess(ItemArmor.dispenseArmor(pointer, stack));
                }

                return stack;
            }
        });
        BlockDispenser.registerBehavior(Blocks.SHULKER_BOX.getItem(), new DispenseBehaviorShulkerBox());

        for(EnumColor dyeColor : EnumColor.values()) {
            BlockDispenser.registerBehavior(BlockShulkerBox.getBlockByColor(dyeColor).getItem(), new DispenseBehaviorShulkerBox());
        }

        BlockDispenser.registerBehavior(Items.GLASS_BOTTLE.getItem(), new DispenseBehaviorMaybe() {
            private final DispenseBehaviorItem defaultDispenseItemBehavior = new DispenseBehaviorItem();

            private ItemStack takeLiquid(ISourceBlock pointer, ItemStack emptyBottleStack, ItemStack filledBottleStack) {
                emptyBottleStack.subtract(1);
                if (emptyBottleStack.isEmpty()) {
                    pointer.getWorld().gameEvent((Entity)null, GameEvent.FLUID_PICKUP, pointer.getBlockPosition());
                    return filledBottleStack.cloneItemStack();
                } else {
                    if (pointer.<TileEntityDispenser>getTileEntity().addItem(filledBottleStack.cloneItemStack()) < 0) {
                        this.defaultDispenseItemBehavior.dispense(pointer, filledBottleStack.cloneItemStack());
                    }

                    return emptyBottleStack;
                }
            }

            @Override
            public ItemStack a(ISourceBlock pointer, ItemStack stack) {
                this.setSuccess(false);
                WorldServer serverLevel = pointer.getWorld();
                BlockPosition blockPos = pointer.getBlockPosition().relative(pointer.getBlockData().get(BlockDispenser.FACING));
                IBlockData blockState = serverLevel.getType(blockPos);
                if (blockState.is(TagsBlock.BEEHIVES, (state) -> {
                    return state.hasProperty(BlockBeehive.HONEY_LEVEL);
                }) && blockState.get(BlockBeehive.HONEY_LEVEL) >= 5) {
                    ((BlockBeehive)blockState.getBlock()).releaseBeesAndResetHoneyLevel(serverLevel, blockState, blockPos, (EntityHuman)null, TileEntityBeehive.ReleaseStatus.BEE_RELEASED);
                    this.setSuccess(true);
                    return this.takeLiquid(pointer, stack, new ItemStack(Items.HONEY_BOTTLE));
                } else if (serverLevel.getFluid(blockPos).is(TagsFluid.WATER)) {
                    this.setSuccess(true);
                    return this.takeLiquid(pointer, stack, PotionUtil.setPotion(new ItemStack(Items.POTION), Potions.WATER));
                } else {
                    return super.a(pointer, stack);
                }
            }
        });
        BlockDispenser.registerBehavior(Items.GLOWSTONE, new DispenseBehaviorMaybe() {
            @Override
            public ItemStack a(ISourceBlock pointer, ItemStack stack) {
                EnumDirection direction = pointer.getBlockData().get(BlockDispenser.FACING);
                BlockPosition blockPos = pointer.getBlockPosition().relative(direction);
                World level = pointer.getWorld();
                IBlockData blockState = level.getType(blockPos);
                this.setSuccess(true);
                if (blockState.is(Blocks.RESPAWN_ANCHOR)) {
                    if (blockState.get(BlockRespawnAnchor.CHARGE) != 4) {
                        BlockRespawnAnchor.charge(level, blockPos, blockState);
                        stack.subtract(1);
                    } else {
                        this.setSuccess(false);
                    }

                    return stack;
                } else {
                    return super.a(pointer, stack);
                }
            }
        });
        BlockDispenser.registerBehavior(Items.SHEARS.getItem(), new DispenseBehaviorShears());
        BlockDispenser.registerBehavior(Items.HONEYCOMB, new DispenseBehaviorMaybe() {
            @Override
            public ItemStack a(ISourceBlock pointer, ItemStack stack) {
                BlockPosition blockPos = pointer.getBlockPosition().relative(pointer.getBlockData().get(BlockDispenser.FACING));
                World level = pointer.getWorld();
                IBlockData blockState = level.getType(blockPos);
                Optional<IBlockData> optional = HoneycombItem.getWaxed(blockState);
                if (optional.isPresent()) {
                    level.setTypeUpdate(blockPos, optional.get());
                    level.triggerEffect(3003, blockPos, 0);
                    stack.subtract(1);
                    this.setSuccess(true);
                    return stack;
                } else {
                    return super.a(pointer, stack);
                }
            }
        });
    }

    static void setEntityPokingOutOfBlock(ISourceBlock pointer, Entity entity, EnumDirection direction) {
        entity.setPosition(pointer.getX() + (double)direction.getAdjacentX() * (0.5000099999997474D - (double)entity.getWidth() / 2.0D), pointer.getY() + (double)direction.getAdjacentY() * (0.5000099999997474D - (double)entity.getHeight() / 2.0D) - (double)entity.getHeight() / 2.0D, pointer.getZ() + (double)direction.getAdjacentZ() * (0.5000099999997474D - (double)entity.getWidth() / 2.0D));
    }
}
