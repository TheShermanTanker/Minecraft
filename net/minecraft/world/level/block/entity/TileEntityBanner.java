package net.minecraft.world.level.block.entity;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.game.PacketPlayOutTileEntityData;
import net.minecraft.world.INamableTileEntity;
import net.minecraft.world.item.EnumColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BlockBanner;
import net.minecraft.world.level.block.BlockBannerAbstract;
import net.minecraft.world.level.block.state.IBlockData;

public class TileEntityBanner extends TileEntity implements INamableTileEntity {
    public static final int MAX_PATTERNS = 6;
    public static final String TAG_PATTERNS = "Patterns";
    public static final String TAG_PATTERN = "Pattern";
    public static final String TAG_COLOR = "Color";
    @Nullable
    private IChatBaseComponent name;
    public EnumColor baseColor;
    @Nullable
    public NBTTagList itemPatterns;
    private boolean receivedData;
    @Nullable
    private List<Pair<EnumBannerPatternType, EnumColor>> patterns;

    public TileEntityBanner(BlockPosition pos, IBlockData state) {
        super(TileEntityTypes.BANNER, pos, state);
        this.baseColor = ((BlockBannerAbstract)state.getBlock()).getColor();
    }

    public TileEntityBanner(BlockPosition pos, IBlockData state, EnumColor baseColor) {
        this(pos, state);
        this.baseColor = baseColor;
    }

    @Nullable
    public static NBTTagList getItemPatterns(ItemStack stack) {
        NBTTagList listTag = null;
        NBTTagCompound compoundTag = stack.getTagElement("BlockEntityTag");
        if (compoundTag != null && compoundTag.hasKeyOfType("Patterns", 9)) {
            listTag = compoundTag.getList("Patterns", 10).copy();
        }

        return listTag;
    }

    public void fromItem(ItemStack stack, EnumColor baseColor) {
        this.itemPatterns = getItemPatterns(stack);
        this.baseColor = baseColor;
        this.patterns = null;
        this.receivedData = true;
        this.name = stack.hasName() ? stack.getName() : null;
    }

    @Override
    public IChatBaseComponent getDisplayName() {
        return (IChatBaseComponent)(this.name != null ? this.name : new ChatMessage("block.minecraft.banner"));
    }

    @Nullable
    @Override
    public IChatBaseComponent getCustomName() {
        return this.name;
    }

    public void setCustomName(IChatBaseComponent customName) {
        this.name = customName;
    }

    @Override
    public NBTTagCompound save(NBTTagCompound nbt) {
        super.save(nbt);
        if (this.itemPatterns != null) {
            nbt.set("Patterns", this.itemPatterns);
        }

        if (this.name != null) {
            nbt.setString("CustomName", IChatBaseComponent.ChatSerializer.toJson(this.name));
        }

        return nbt;
    }

    @Override
    public void load(NBTTagCompound nbt) {
        super.load(nbt);
        if (nbt.hasKeyOfType("CustomName", 8)) {
            this.name = IChatBaseComponent.ChatSerializer.fromJson(nbt.getString("CustomName"));
        }

        this.itemPatterns = nbt.getList("Patterns", 10);
        this.patterns = null;
        this.receivedData = true;
    }

    @Nullable
    @Override
    public PacketPlayOutTileEntityData getUpdatePacket() {
        return new PacketPlayOutTileEntityData(this.worldPosition, 6, this.getUpdateTag());
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return this.save(new NBTTagCompound());
    }

    public static int getPatternCount(ItemStack stack) {
        NBTTagCompound compoundTag = stack.getTagElement("BlockEntityTag");
        return compoundTag != null && compoundTag.hasKey("Patterns") ? compoundTag.getList("Patterns", 10).size() : 0;
    }

    public List<Pair<EnumBannerPatternType, EnumColor>> getPatterns() {
        if (this.patterns == null && this.receivedData) {
            this.patterns = createPatterns(this.baseColor, this.itemPatterns);
        }

        return this.patterns;
    }

    public static List<Pair<EnumBannerPatternType, EnumColor>> createPatterns(EnumColor baseColor, @Nullable NBTTagList patternListTag) {
        List<Pair<EnumBannerPatternType, EnumColor>> list = Lists.newArrayList();
        list.add(Pair.of(EnumBannerPatternType.BASE, baseColor));
        if (patternListTag != null) {
            for(int i = 0; i < patternListTag.size(); ++i) {
                NBTTagCompound compoundTag = patternListTag.getCompound(i);
                EnumBannerPatternType bannerPattern = EnumBannerPatternType.byHash(compoundTag.getString("Pattern"));
                if (bannerPattern != null) {
                    int j = compoundTag.getInt("Color");
                    list.add(Pair.of(bannerPattern, EnumColor.fromColorIndex(j)));
                }
            }
        }

        return list;
    }

    public static void removeLastPattern(ItemStack stack) {
        NBTTagCompound compoundTag = stack.getTagElement("BlockEntityTag");
        if (compoundTag != null && compoundTag.hasKeyOfType("Patterns", 9)) {
            NBTTagList listTag = compoundTag.getList("Patterns", 10);
            if (!listTag.isEmpty()) {
                listTag.remove(listTag.size() - 1);
                if (listTag.isEmpty()) {
                    stack.removeTag("BlockEntityTag");
                }

            }
        }
    }

    public ItemStack getItem() {
        ItemStack itemStack = new ItemStack(BlockBanner.byColor(this.baseColor));
        if (this.itemPatterns != null && !this.itemPatterns.isEmpty()) {
            itemStack.getOrCreateTagElement("BlockEntityTag").set("Patterns", this.itemPatterns.copy());
        }

        if (this.name != null) {
            itemStack.setHoverName(this.name);
        }

        return itemStack;
    }

    public EnumColor getBaseColor() {
        return this.baseColor;
    }
}
