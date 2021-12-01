package net.minecraft.world.item;

import java.util.Optional;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.DynamicOpsNBT;
import net.minecraft.nbt.GameProfileSerializer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.EnumSoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.village.poi.VillagePlaceType;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.context.ItemActionContext;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Blocks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ItemCompass extends Item implements ItemVanishable {
    private static final Logger LOGGER = LogManager.getLogger();
    public static final String TAG_LODESTONE_POS = "LodestonePos";
    public static final String TAG_LODESTONE_DIMENSION = "LodestoneDimension";
    public static final String TAG_LODESTONE_TRACKED = "LodestoneTracked";

    public ItemCompass(Item.Info settings) {
        super(settings);
    }

    public static boolean isLodestoneCompass(ItemStack stack) {
        NBTTagCompound compoundTag = stack.getTag();
        return compoundTag != null && (compoundTag.hasKey("LodestoneDimension") || compoundTag.hasKey("LodestonePos"));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return isLodestoneCompass(stack) || super.isFoil(stack);
    }

    public static Optional<ResourceKey<World>> getLodestoneDimension(NBTTagCompound nbt) {
        return World.RESOURCE_KEY_CODEC.parse(DynamicOpsNBT.INSTANCE, nbt.get("LodestoneDimension")).result();
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (!world.isClientSide) {
            if (isLodestoneCompass(stack)) {
                NBTTagCompound compoundTag = stack.getOrCreateTag();
                if (compoundTag.hasKey("LodestoneTracked") && !compoundTag.getBoolean("LodestoneTracked")) {
                    return;
                }

                Optional<ResourceKey<World>> optional = getLodestoneDimension(compoundTag);
                if (optional.isPresent() && optional.get() == world.getDimensionKey() && compoundTag.hasKey("LodestonePos")) {
                    BlockPosition blockPos = GameProfileSerializer.readBlockPos(compoundTag.getCompound("LodestonePos"));
                    if (!world.isValidLocation(blockPos) || !((WorldServer)world).getPoiManager().existsAtPosition(VillagePlaceType.LODESTONE, blockPos)) {
                        compoundTag.remove("LodestonePos");
                    }
                }
            }

        }
    }

    @Override
    public EnumInteractionResult useOn(ItemActionContext context) {
        BlockPosition blockPos = context.getClickPosition();
        World level = context.getWorld();
        if (!level.getType(blockPos).is(Blocks.LODESTONE)) {
            return super.useOn(context);
        } else {
            level.playSound((EntityHuman)null, blockPos, SoundEffects.LODESTONE_COMPASS_LOCK, EnumSoundCategory.PLAYERS, 1.0F, 1.0F);
            EntityHuman player = context.getEntity();
            ItemStack itemStack = context.getItemStack();
            boolean bl = !player.getAbilities().instabuild && itemStack.getCount() == 1;
            if (bl) {
                this.addLodestoneTags(level.getDimensionKey(), blockPos, itemStack.getOrCreateTag());
            } else {
                ItemStack itemStack2 = new ItemStack(Items.COMPASS, 1);
                NBTTagCompound compoundTag = itemStack.hasTag() ? itemStack.getTag().copy() : new NBTTagCompound();
                itemStack2.setTag(compoundTag);
                if (!player.getAbilities().instabuild) {
                    itemStack.subtract(1);
                }

                this.addLodestoneTags(level.getDimensionKey(), blockPos, compoundTag);
                if (!player.getInventory().pickup(itemStack2)) {
                    player.drop(itemStack2, false);
                }
            }

            return EnumInteractionResult.sidedSuccess(level.isClientSide);
        }
    }

    private void addLodestoneTags(ResourceKey<World> worldKey, BlockPosition pos, NBTTagCompound nbt) {
        nbt.set("LodestonePos", GameProfileSerializer.writeBlockPos(pos));
        World.RESOURCE_KEY_CODEC.encodeStart(DynamicOpsNBT.INSTANCE, worldKey).resultOrPartial(LOGGER::error).ifPresent((tag) -> {
            nbt.set("LodestoneDimension", tag);
        });
        nbt.setBoolean("LodestoneTracked", true);
    }

    @Override
    public String getDescriptionId(ItemStack stack) {
        return isLodestoneCompass(stack) ? "item.minecraft.lodestone_compass" : super.getDescriptionId(stack);
    }
}
