package net.minecraft.world.item;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.sounds.EnumSoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.tags.TagsBlock;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.context.ItemActionContext;
import net.minecraft.world.level.IMaterial;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;

public class ItemHoe extends ItemTool {
    protected static final Map<Block, Pair<Predicate<ItemActionContext>, Consumer<ItemActionContext>>> TILLABLES = Maps.newHashMap(ImmutableMap.of(Blocks.GRASS_BLOCK, Pair.of(ItemHoe::onlyIfAirAbove, changeIntoState(Blocks.FARMLAND.getBlockData())), Blocks.DIRT_PATH, Pair.of(ItemHoe::onlyIfAirAbove, changeIntoState(Blocks.FARMLAND.getBlockData())), Blocks.DIRT, Pair.of(ItemHoe::onlyIfAirAbove, changeIntoState(Blocks.FARMLAND.getBlockData())), Blocks.COARSE_DIRT, Pair.of(ItemHoe::onlyIfAirAbove, changeIntoState(Blocks.DIRT.getBlockData())), Blocks.ROOTED_DIRT, Pair.of((useOnContext) -> {
        return true;
    }, changeIntoStateAndDropItem(Blocks.DIRT.getBlockData(), Items.HANGING_ROOTS))));

    protected ItemHoe(ToolMaterial material, int attackDamage, float attackSpeed, Item.Info settings) {
        super((float)attackDamage, attackSpeed, material, TagsBlock.MINEABLE_WITH_HOE, settings);
    }

    @Override
    public EnumInteractionResult useOn(ItemActionContext context) {
        World level = context.getWorld();
        BlockPosition blockPos = context.getClickPosition();
        Pair<Predicate<ItemActionContext>, Consumer<ItemActionContext>> pair = TILLABLES.get(level.getType(blockPos).getBlock());
        if (pair == null) {
            return EnumInteractionResult.PASS;
        } else {
            Predicate<ItemActionContext> predicate = pair.getFirst();
            Consumer<ItemActionContext> consumer = pair.getSecond();
            if (predicate.test(context)) {
                EntityHuman player = context.getEntity();
                level.playSound(player, blockPos, SoundEffects.HOE_TILL, EnumSoundCategory.BLOCKS, 1.0F, 1.0F);
                if (!level.isClientSide) {
                    consumer.accept(context);
                    if (player != null) {
                        context.getItemStack().damage(1, player, (p) -> {
                            p.broadcastItemBreak(context.getHand());
                        });
                    }
                }

                return EnumInteractionResult.sidedSuccess(level.isClientSide);
            } else {
                return EnumInteractionResult.PASS;
            }
        }
    }

    public static Consumer<ItemActionContext> changeIntoState(IBlockData result) {
        return (context) -> {
            context.getWorld().setTypeAndData(context.getClickPosition(), result, 11);
        };
    }

    public static Consumer<ItemActionContext> changeIntoStateAndDropItem(IBlockData result, IMaterial droppedItem) {
        return (context) -> {
            context.getWorld().setTypeAndData(context.getClickPosition(), result, 11);
            Block.popResourceFromFace(context.getWorld(), context.getClickPosition(), context.getClickedFace(), new ItemStack(droppedItem));
        };
    }

    public static boolean onlyIfAirAbove(ItemActionContext context) {
        return context.getClickedFace() != EnumDirection.DOWN && context.getWorld().getType(context.getClickPosition().above()).isAir();
    }
}
