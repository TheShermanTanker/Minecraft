package net.minecraft.world.item;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.EnumChatFormat;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.chat.IChatMutableComponent;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.EnumSoundCategory;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumMobSpawn;
import net.minecraft.world.entity.animal.EntityTropicalFish;
import net.minecraft.world.entity.animal.IBucketable;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidType;

public class MobBucketItem extends ItemBucket {
    private final EntityTypes<?> type;
    private final SoundEffect emptySound;

    public MobBucketItem(EntityTypes<?> type, FluidType fluid, SoundEffect emptyingSound, Item.Info settings) {
        super(fluid, settings);
        this.type = type;
        this.emptySound = emptyingSound;
    }

    @Override
    public void checkExtraContent(@Nullable EntityHuman player, World world, ItemStack stack, BlockPosition pos) {
        if (world instanceof WorldServer) {
            this.spawn((WorldServer)world, stack, pos);
            world.gameEvent(player, GameEvent.ENTITY_PLACE, pos);
        }

    }

    @Override
    protected void playEmptySound(@Nullable EntityHuman player, GeneratorAccess world, BlockPosition pos) {
        world.playSound(player, pos, this.emptySound, EnumSoundCategory.NEUTRAL, 1.0F, 1.0F);
    }

    private void spawn(WorldServer world, ItemStack stack, BlockPosition pos) {
        Entity entity = this.type.spawnCreature(world, stack, (EntityHuman)null, pos, EnumMobSpawn.BUCKET, true, false);
        if (entity instanceof IBucketable) {
            IBucketable bucketable = (IBucketable)entity;
            bucketable.loadFromBucketTag(stack.getOrCreateTag());
            bucketable.setFromBucket(true);
        }

    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable World world, List<IChatBaseComponent> tooltip, TooltipFlag context) {
        if (this.type == EntityTypes.TROPICAL_FISH) {
            NBTTagCompound compoundTag = stack.getTag();
            if (compoundTag != null && compoundTag.hasKeyOfType("BucketVariantTag", 3)) {
                int i = compoundTag.getInt("BucketVariantTag");
                EnumChatFormat[] chatFormattings = new EnumChatFormat[]{EnumChatFormat.ITALIC, EnumChatFormat.GRAY};
                String string = "color.minecraft." + EntityTropicalFish.getBaseColor(i);
                String string2 = "color.minecraft." + EntityTropicalFish.getPatternColor(i);

                for(int j = 0; j < EntityTropicalFish.COMMON_VARIANTS.length; ++j) {
                    if (i == EntityTropicalFish.COMMON_VARIANTS[j]) {
                        tooltip.add((new ChatMessage(EntityTropicalFish.getPredefinedName(j))).withStyle(chatFormattings));
                        return;
                    }
                }

                tooltip.add((new ChatMessage(EntityTropicalFish.getFishTypeName(i))).withStyle(chatFormattings));
                IChatMutableComponent mutableComponent = new ChatMessage(string);
                if (!string.equals(string2)) {
                    mutableComponent.append(", ").addSibling(new ChatMessage(string2));
                }

                mutableComponent.withStyle(chatFormattings);
                tooltip.add(mutableComponent);
            }
        }

    }
}
