package net.minecraft.world.item;

import com.google.common.collect.ImmutableMap.Builder;
import java.util.Map;
import java.util.Optional;
import net.minecraft.advancements.CriterionTriggers;
import net.minecraft.core.BlockPosition;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.tags.TagsBlock;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.context.ItemActionContext;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BlockRotatable;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.IBlockData;

public class ItemAxe extends ItemTool {
    protected static final Map<Block, Block> STRIPPABLES = (new Builder<Block, Block>()).put(Blocks.OAK_WOOD, Blocks.STRIPPED_OAK_WOOD).put(Blocks.OAK_LOG, Blocks.STRIPPED_OAK_LOG).put(Blocks.DARK_OAK_WOOD, Blocks.STRIPPED_DARK_OAK_WOOD).put(Blocks.DARK_OAK_LOG, Blocks.STRIPPED_DARK_OAK_LOG).put(Blocks.ACACIA_WOOD, Blocks.STRIPPED_ACACIA_WOOD).put(Blocks.ACACIA_LOG, Blocks.STRIPPED_ACACIA_LOG).put(Blocks.BIRCH_WOOD, Blocks.STRIPPED_BIRCH_WOOD).put(Blocks.BIRCH_LOG, Blocks.STRIPPED_BIRCH_LOG).put(Blocks.JUNGLE_WOOD, Blocks.STRIPPED_JUNGLE_WOOD).put(Blocks.JUNGLE_LOG, Blocks.STRIPPED_JUNGLE_LOG).put(Blocks.SPRUCE_WOOD, Blocks.STRIPPED_SPRUCE_WOOD).put(Blocks.SPRUCE_LOG, Blocks.STRIPPED_SPRUCE_LOG).put(Blocks.WARPED_STEM, Blocks.STRIPPED_WARPED_STEM).put(Blocks.WARPED_HYPHAE, Blocks.STRIPPED_WARPED_HYPHAE).put(Blocks.CRIMSON_STEM, Blocks.STRIPPED_CRIMSON_STEM).put(Blocks.CRIMSON_HYPHAE, Blocks.STRIPPED_CRIMSON_HYPHAE).build();

    protected ItemAxe(ToolMaterial material, float attackDamage, float attackSpeed, Item.Info settings) {
        super(attackDamage, attackSpeed, material, TagsBlock.MINEABLE_WITH_AXE, settings);
    }

    @Override
    public EnumInteractionResult useOn(ItemActionContext context) {
        World level = context.getWorld();
        BlockPosition blockPos = context.getClickPosition();
        EntityHuman player = context.getEntity();
        IBlockData blockState = level.getType(blockPos);
        Optional<IBlockData> optional = this.getStripped(blockState);
        Optional<IBlockData> optional2 = WeatheringCopper.getPrevious(blockState);
        Optional<IBlockData> optional3 = Optional.ofNullable(HoneycombItem.WAX_OFF_BY_BLOCK.get().get(blockState.getBlock())).map((block) -> {
            return block.withPropertiesOf(blockState);
        });
        ItemStack itemStack = context.getItemStack();
        Optional<IBlockData> optional4 = Optional.empty();
        if (optional.isPresent()) {
            level.playSound(player, blockPos, SoundEffects.AXE_STRIP, SoundCategory.BLOCKS, 1.0F, 1.0F);
            optional4 = optional;
        } else if (optional2.isPresent()) {
            level.playSound(player, blockPos, SoundEffects.AXE_SCRAPE, SoundCategory.BLOCKS, 1.0F, 1.0F);
            level.levelEvent(player, 3005, blockPos, 0);
            optional4 = optional2;
        } else if (optional3.isPresent()) {
            level.playSound(player, blockPos, SoundEffects.AXE_WAX_OFF, SoundCategory.BLOCKS, 1.0F, 1.0F);
            level.levelEvent(player, 3004, blockPos, 0);
            optional4 = optional3;
        }

        if (optional4.isPresent()) {
            if (player instanceof EntityPlayer) {
                CriterionTriggers.ITEM_USED_ON_BLOCK.trigger((EntityPlayer)player, blockPos, itemStack);
            }

            level.setTypeAndData(blockPos, optional4.get(), 11);
            if (player != null) {
                itemStack.damage(1, player, (p) -> {
                    p.broadcastItemBreak(context.getHand());
                });
            }

            return EnumInteractionResult.sidedSuccess(level.isClientSide);
        } else {
            return EnumInteractionResult.PASS;
        }
    }

    private Optional<IBlockData> getStripped(IBlockData state) {
        return Optional.ofNullable(STRIPPABLES.get(state.getBlock())).map((block) -> {
            return block.getBlockData().set(BlockRotatable.AXIS, state.get(BlockRotatable.AXIS));
        });
    }
}
