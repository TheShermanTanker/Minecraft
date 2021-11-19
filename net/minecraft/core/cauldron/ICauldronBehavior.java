package net.minecraft.core.cauldron;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.SystemUtils;
import net.minecraft.core.BlockPosition;
import net.minecraft.sounds.EnumSoundCategory;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.stats.StatisticList;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.IDyeable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemLiquidUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtil;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BlockCauldronLayered;
import net.minecraft.world.level.block.BlockShulkerBox;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.TileEntityBanner;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.gameevent.GameEvent;

public interface ICauldronBehavior {
    Map<Item, ICauldronBehavior> EMPTY = newInteractionMap();
    Map<Item, ICauldronBehavior> WATER = newInteractionMap();
    Map<Item, ICauldronBehavior> LAVA = newInteractionMap();
    Map<Item, ICauldronBehavior> POWDER_SNOW = newInteractionMap();
    ICauldronBehavior FILL_WATER = (state, world, pos, player, hand, stack) -> {
        return emptyBucket(world, pos, player, hand, stack, Blocks.WATER_CAULDRON.getBlockData().set(BlockCauldronLayered.LEVEL, Integer.valueOf(3)), SoundEffects.BUCKET_EMPTY);
    };
    ICauldronBehavior FILL_LAVA = (state, world, pos, player, hand, stack) -> {
        return emptyBucket(world, pos, player, hand, stack, Blocks.LAVA_CAULDRON.getBlockData(), SoundEffects.BUCKET_EMPTY_LAVA);
    };
    ICauldronBehavior FILL_POWDER_SNOW = (state, world, pos, player, hand, stack) -> {
        return emptyBucket(world, pos, player, hand, stack, Blocks.POWDER_SNOW_CAULDRON.getBlockData().set(BlockCauldronLayered.LEVEL, Integer.valueOf(3)), SoundEffects.BUCKET_EMPTY_POWDER_SNOW);
    };
    ICauldronBehavior SHULKER_BOX = (state, world, pos, player, hand, stack) -> {
        Block block = Block.asBlock(stack.getItem());
        if (!(block instanceof BlockShulkerBox)) {
            return EnumInteractionResult.PASS;
        } else {
            if (!world.isClientSide) {
                ItemStack itemStack = new ItemStack(Blocks.SHULKER_BOX);
                if (stack.hasTag()) {
                    itemStack.setTag(stack.getTag().c());
                }

                player.setItemInHand(hand, itemStack);
                player.awardStat(StatisticList.CLEAN_SHULKER_BOX);
                BlockCauldronLayered.lowerFillLevel(state, world, pos);
            }

            return EnumInteractionResult.sidedSuccess(world.isClientSide);
        }
    };
    ICauldronBehavior BANNER = (state, world, pos, player, hand, stack) -> {
        if (TileEntityBanner.getPatternCount(stack) <= 0) {
            return EnumInteractionResult.PASS;
        } else {
            if (!world.isClientSide) {
                ItemStack itemStack = stack.cloneItemStack();
                itemStack.setCount(1);
                TileEntityBanner.removeLastPattern(itemStack);
                if (!player.getAbilities().instabuild) {
                    stack.subtract(1);
                }

                if (stack.isEmpty()) {
                    player.setItemInHand(hand, itemStack);
                } else if (player.getInventory().pickup(itemStack)) {
                    player.inventoryMenu.updateInventory();
                } else {
                    player.drop(itemStack, false);
                }

                player.awardStat(StatisticList.CLEAN_BANNER);
                BlockCauldronLayered.lowerFillLevel(state, world, pos);
            }

            return EnumInteractionResult.sidedSuccess(world.isClientSide);
        }
    };
    ICauldronBehavior DYED_ITEM = (state, world, pos, player, hand, stack) -> {
        Item item = stack.getItem();
        if (!(item instanceof IDyeable)) {
            return EnumInteractionResult.PASS;
        } else {
            IDyeable dyeableLeatherItem = (IDyeable)item;
            if (!dyeableLeatherItem.hasCustomColor(stack)) {
                return EnumInteractionResult.PASS;
            } else {
                if (!world.isClientSide) {
                    dyeableLeatherItem.clearColor(stack);
                    player.awardStat(StatisticList.CLEAN_ARMOR);
                    BlockCauldronLayered.lowerFillLevel(state, world, pos);
                }

                return EnumInteractionResult.sidedSuccess(world.isClientSide);
            }
        }
    };

    static Object2ObjectOpenHashMap<Item, ICauldronBehavior> newInteractionMap() {
        return SystemUtils.make(new Object2ObjectOpenHashMap<>(), (map) -> {
            map.defaultReturnValue((state, world, pos, player, hand, stack) -> {
                return EnumInteractionResult.PASS;
            });
        });
    }

    EnumInteractionResult interact(IBlockData state, World world, BlockPosition pos, EntityHuman player, EnumHand hand, ItemStack stack);

    static void bootStrap() {
        addDefaultInteractions(EMPTY);
        EMPTY.put(Items.POTION, (state, world, pos, player, hand, stack) -> {
            if (PotionUtil.getPotion(stack) != Potions.WATER) {
                return EnumInteractionResult.PASS;
            } else {
                if (!world.isClientSide) {
                    Item item = stack.getItem();
                    player.setItemInHand(hand, ItemLiquidUtil.createFilledResult(stack, player, new ItemStack(Items.GLASS_BOTTLE)));
                    player.awardStat(StatisticList.USE_CAULDRON);
                    player.awardStat(StatisticList.ITEM_USED.get(item));
                    world.setTypeUpdate(pos, Blocks.WATER_CAULDRON.getBlockData());
                    world.playSound((EntityHuman)null, pos, SoundEffects.BOTTLE_EMPTY, EnumSoundCategory.BLOCKS, 1.0F, 1.0F);
                    world.gameEvent((Entity)null, GameEvent.FLUID_PLACE, pos);
                }

                return EnumInteractionResult.sidedSuccess(world.isClientSide);
            }
        });
        addDefaultInteractions(WATER);
        WATER.put(Items.BUCKET, (state, world, pos, player, hand, stack) -> {
            return fillBucket(state, world, pos, player, hand, stack, new ItemStack(Items.WATER_BUCKET), (statex) -> {
                return statex.get(BlockCauldronLayered.LEVEL) == 3;
            }, SoundEffects.BUCKET_FILL);
        });
        WATER.put(Items.GLASS_BOTTLE, (state, world, pos, player, hand, stack) -> {
            if (!world.isClientSide) {
                Item item = stack.getItem();
                player.setItemInHand(hand, ItemLiquidUtil.createFilledResult(stack, player, PotionUtil.setPotion(new ItemStack(Items.POTION), Potions.WATER)));
                player.awardStat(StatisticList.USE_CAULDRON);
                player.awardStat(StatisticList.ITEM_USED.get(item));
                BlockCauldronLayered.lowerFillLevel(state, world, pos);
                world.playSound((EntityHuman)null, pos, SoundEffects.BOTTLE_FILL, EnumSoundCategory.BLOCKS, 1.0F, 1.0F);
                world.gameEvent((Entity)null, GameEvent.FLUID_PICKUP, pos);
            }

            return EnumInteractionResult.sidedSuccess(world.isClientSide);
        });
        WATER.put(Items.POTION, (state, world, pos, player, hand, stack) -> {
            if (state.get(BlockCauldronLayered.LEVEL) != 3 && PotionUtil.getPotion(stack) == Potions.WATER) {
                if (!world.isClientSide) {
                    player.setItemInHand(hand, ItemLiquidUtil.createFilledResult(stack, player, new ItemStack(Items.GLASS_BOTTLE)));
                    player.awardStat(StatisticList.USE_CAULDRON);
                    player.awardStat(StatisticList.ITEM_USED.get(stack.getItem()));
                    world.setTypeUpdate(pos, state.cycle(BlockCauldronLayered.LEVEL));
                    world.playSound((EntityHuman)null, pos, SoundEffects.BOTTLE_EMPTY, EnumSoundCategory.BLOCKS, 1.0F, 1.0F);
                    world.gameEvent((Entity)null, GameEvent.FLUID_PLACE, pos);
                }

                return EnumInteractionResult.sidedSuccess(world.isClientSide);
            } else {
                return EnumInteractionResult.PASS;
            }
        });
        WATER.put(Items.LEATHER_BOOTS, DYED_ITEM);
        WATER.put(Items.LEATHER_LEGGINGS, DYED_ITEM);
        WATER.put(Items.LEATHER_CHESTPLATE, DYED_ITEM);
        WATER.put(Items.LEATHER_HELMET, DYED_ITEM);
        WATER.put(Items.LEATHER_HORSE_ARMOR, DYED_ITEM);
        WATER.put(Items.WHITE_BANNER, BANNER);
        WATER.put(Items.GRAY_BANNER, BANNER);
        WATER.put(Items.BLACK_BANNER, BANNER);
        WATER.put(Items.BLUE_BANNER, BANNER);
        WATER.put(Items.BROWN_BANNER, BANNER);
        WATER.put(Items.CYAN_BANNER, BANNER);
        WATER.put(Items.GREEN_BANNER, BANNER);
        WATER.put(Items.LIGHT_BLUE_BANNER, BANNER);
        WATER.put(Items.LIGHT_GRAY_BANNER, BANNER);
        WATER.put(Items.LIME_BANNER, BANNER);
        WATER.put(Items.MAGENTA_BANNER, BANNER);
        WATER.put(Items.ORANGE_BANNER, BANNER);
        WATER.put(Items.PINK_BANNER, BANNER);
        WATER.put(Items.PURPLE_BANNER, BANNER);
        WATER.put(Items.RED_BANNER, BANNER);
        WATER.put(Items.YELLOW_BANNER, BANNER);
        WATER.put(Items.WHITE_SHULKER_BOX, SHULKER_BOX);
        WATER.put(Items.GRAY_SHULKER_BOX, SHULKER_BOX);
        WATER.put(Items.BLACK_SHULKER_BOX, SHULKER_BOX);
        WATER.put(Items.BLUE_SHULKER_BOX, SHULKER_BOX);
        WATER.put(Items.BROWN_SHULKER_BOX, SHULKER_BOX);
        WATER.put(Items.CYAN_SHULKER_BOX, SHULKER_BOX);
        WATER.put(Items.GREEN_SHULKER_BOX, SHULKER_BOX);
        WATER.put(Items.LIGHT_BLUE_SHULKER_BOX, SHULKER_BOX);
        WATER.put(Items.LIGHT_GRAY_SHULKER_BOX, SHULKER_BOX);
        WATER.put(Items.LIME_SHULKER_BOX, SHULKER_BOX);
        WATER.put(Items.MAGENTA_SHULKER_BOX, SHULKER_BOX);
        WATER.put(Items.ORANGE_SHULKER_BOX, SHULKER_BOX);
        WATER.put(Items.PINK_SHULKER_BOX, SHULKER_BOX);
        WATER.put(Items.PURPLE_SHULKER_BOX, SHULKER_BOX);
        WATER.put(Items.RED_SHULKER_BOX, SHULKER_BOX);
        WATER.put(Items.YELLOW_SHULKER_BOX, SHULKER_BOX);
        LAVA.put(Items.BUCKET, (state, world, pos, player, hand, stack) -> {
            return fillBucket(state, world, pos, player, hand, stack, new ItemStack(Items.LAVA_BUCKET), (statex) -> {
                return true;
            }, SoundEffects.BUCKET_FILL_LAVA);
        });
        addDefaultInteractions(LAVA);
        POWDER_SNOW.put(Items.BUCKET, (state, world, pos, player, hand, stack) -> {
            return fillBucket(state, world, pos, player, hand, stack, new ItemStack(Items.POWDER_SNOW_BUCKET), (statex) -> {
                return statex.get(BlockCauldronLayered.LEVEL) == 3;
            }, SoundEffects.BUCKET_FILL_POWDER_SNOW);
        });
        addDefaultInteractions(POWDER_SNOW);
    }

    static void addDefaultInteractions(Map<Item, ICauldronBehavior> behavior) {
        behavior.put(Items.LAVA_BUCKET, FILL_LAVA);
        behavior.put(Items.WATER_BUCKET, FILL_WATER);
        behavior.put(Items.POWDER_SNOW_BUCKET, FILL_POWDER_SNOW);
    }

    static EnumInteractionResult fillBucket(IBlockData state, World world, BlockPosition pos, EntityHuman player, EnumHand hand, ItemStack stack, ItemStack output, Predicate<IBlockData> predicate, SoundEffect soundEvent) {
        if (!predicate.test(state)) {
            return EnumInteractionResult.PASS;
        } else {
            if (!world.isClientSide) {
                Item item = stack.getItem();
                player.setItemInHand(hand, ItemLiquidUtil.createFilledResult(stack, player, output));
                player.awardStat(StatisticList.USE_CAULDRON);
                player.awardStat(StatisticList.ITEM_USED.get(item));
                world.setTypeUpdate(pos, Blocks.CAULDRON.getBlockData());
                world.playSound((EntityHuman)null, pos, soundEvent, EnumSoundCategory.BLOCKS, 1.0F, 1.0F);
                world.gameEvent((Entity)null, GameEvent.FLUID_PICKUP, pos);
            }

            return EnumInteractionResult.sidedSuccess(world.isClientSide);
        }
    }

    static EnumInteractionResult emptyBucket(World world, BlockPosition pos, EntityHuman player, EnumHand hand, ItemStack stack, IBlockData state, SoundEffect soundEvent) {
        if (!world.isClientSide) {
            Item item = stack.getItem();
            player.setItemInHand(hand, ItemLiquidUtil.createFilledResult(stack, player, new ItemStack(Items.BUCKET)));
            player.awardStat(StatisticList.FILL_CAULDRON);
            player.awardStat(StatisticList.ITEM_USED.get(item));
            world.setTypeUpdate(pos, state);
            world.playSound((EntityHuman)null, pos, soundEvent, EnumSoundCategory.BLOCKS, 1.0F, 1.0F);
            world.gameEvent((Entity)null, GameEvent.FLUID_PLACE, pos);
        }

        return EnumInteractionResult.sidedSuccess(world.isClientSide);
    }
}
