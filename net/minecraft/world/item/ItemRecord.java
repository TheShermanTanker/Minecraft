package net.minecraft.world.item;

import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.EnumChatFormat;
import net.minecraft.core.BlockPosition;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.chat.IChatMutableComponent;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.stats.StatisticList;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.context.ItemActionContext;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.BlockJukeBox;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;

public class ItemRecord extends Item {
    private static final Map<SoundEffect, ItemRecord> BY_NAME = Maps.newHashMap();
    private final int analogOutput;
    private final SoundEffect sound;

    protected ItemRecord(int comparatorOutput, SoundEffect sound, Item.Info settings) {
        super(settings);
        this.analogOutput = comparatorOutput;
        this.sound = sound;
        BY_NAME.put(this.sound, this);
    }

    @Override
    public EnumInteractionResult useOn(ItemActionContext context) {
        World level = context.getWorld();
        BlockPosition blockPos = context.getClickPosition();
        IBlockData blockState = level.getType(blockPos);
        if (blockState.is(Blocks.JUKEBOX) && !blockState.get(BlockJukeBox.HAS_RECORD)) {
            ItemStack itemStack = context.getItemStack();
            if (!level.isClientSide) {
                ((BlockJukeBox)Blocks.JUKEBOX).setRecord(level, blockPos, blockState, itemStack);
                level.triggerEffect((EntityHuman)null, 1010, blockPos, Item.getId(this));
                itemStack.subtract(1);
                EntityHuman player = context.getEntity();
                if (player != null) {
                    player.awardStat(StatisticList.PLAY_RECORD);
                }
            }

            return EnumInteractionResult.sidedSuccess(level.isClientSide);
        } else {
            return EnumInteractionResult.PASS;
        }
    }

    public int getAnalogOutput() {
        return this.analogOutput;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable World world, List<IChatBaseComponent> tooltip, TooltipFlag context) {
        tooltip.add(this.getDisplayName().withStyle(EnumChatFormat.GRAY));
    }

    public IChatMutableComponent getDisplayName() {
        return new ChatMessage(this.getName() + ".desc");
    }

    @Nullable
    public static ItemRecord getBySound(SoundEffect sound) {
        return BY_NAME.get(sound);
    }

    public SoundEffect getSound() {
        return this.sound;
    }
}
