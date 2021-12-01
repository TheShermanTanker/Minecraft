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
import net.minecraft.world.item.ItemBlock;
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
        NBTTagCompound compoundTag = ItemBlock.getBlockEntityData(stack);
        if (compoundTag != null && compoundTag.hasKeyOfType("Patterns", 9)) {
            listTag = compoundTag.getList("Patterns", 10).copy();
        }

        return listTag;
    }

    public void fromItem(ItemStack stack, EnumColor baseColor) {
        this.baseColor = baseColor;
        this.fromItem(stack);
    }

    public void fromItem(ItemStack stack) {
        this.itemPatterns = getItemPatterns(stack);
        this.patterns = null;
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
    protected void saveAdditional(NBTTagCompound nbt) {
        super.saveAdditional(nbt);
        if (this.itemPatterns != null) {
            nbt.set("Patterns", this.itemPatterns);
        }

        if (this.name != null) {
            nbt.setString("CustomName", IChatBaseComponent.ChatSerializer.toJson(this.name));
        }

    }

    @Override
    public void load(NBTTagCompound nbt) {
        super.load(nbt);
        if (nbt.hasKeyOfType("CustomName", 8)) {
            this.name = IChatBaseComponent.ChatSerializer.fromJson(nbt.getString("CustomName"));
        }

        this.itemPatterns = nbt.getList("Patterns", 10);
        this.patterns = null;
    }

    @Override
    public PacketPlayOutTileEntityData getUpdatePacket() {
        return PacketPlayOutTileEntityData.create(this);
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return this.saveWithoutMetadata();
    }

    public static int getPatternCount(ItemStack stack) {
        NBTTagCompound compoundTag = ItemBlock.getBlockEntityData(stack);
        return compoundTag != null && compoundTag.hasKey("Patterns") ? compoundTag.getList("Patterns", 10).size() : 0;
    }

    public List<Pair<EnumBannerPatternType, EnumColor>> getPatterns() {
        if (this.patterns == null) {
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
        NBTTagCompound compoundTag = ItemBlock.getBlockEntityData(stack);
        if (compoundTag != null && compoundTag.hasKeyOfType("Patterns", 9)) {
            NBTTagList listTag = compoundTag.getList("Patterns", 10);
            if (!listTag.isEmpty()) {
                listTag.remove(listTag.size() - 1);
                if (listTag.isEmpty()) {
                    compoundTag.remove("Patterns");
                }

                ItemBlock.setBlockEntityData(stack, TileEntityTypes.BANNER, compoundTag);
            }
        }
    }

    public ItemStack getItem() {
        ItemStack itemStack = new ItemStack(BlockBanner.byColor(this.baseColor));
        if (this.itemPatterns != null && !this.itemPatterns.isEmpty()) {
            NBTTagCompound compoundTag = new NBTTagCompound();
            compoundTag.set("Patterns", this.itemPatterns.copy());
            ItemBlock.setBlockEntityData(itemStack, this.getTileType(), compoundTag);
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
