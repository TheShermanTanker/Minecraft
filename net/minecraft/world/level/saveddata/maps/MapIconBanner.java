package net.minecraft.world.level.saveddata.maps;

import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.GameProfileSerializer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.world.item.EnumColor;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.entity.TileEntityBanner;

public class MapIconBanner {
    private final BlockPosition pos;
    private final EnumColor color;
    @Nullable
    private final IChatBaseComponent name;

    public MapIconBanner(BlockPosition pos, EnumColor dyeColor, @Nullable IChatBaseComponent name) {
        this.pos = pos;
        this.color = dyeColor;
        this.name = name;
    }

    public static MapIconBanner load(NBTTagCompound nbt) {
        BlockPosition blockPos = GameProfileSerializer.readBlockPos(nbt.getCompound("Pos"));
        EnumColor dyeColor = EnumColor.byName(nbt.getString("Color"), EnumColor.WHITE);
        IChatBaseComponent component = nbt.hasKey("Name") ? IChatBaseComponent.ChatSerializer.fromJson(nbt.getString("Name")) : null;
        return new MapIconBanner(blockPos, dyeColor, component);
    }

    @Nullable
    public static MapIconBanner fromWorld(IBlockAccess blockView, BlockPosition blockPos) {
        TileEntity blockEntity = blockView.getTileEntity(blockPos);
        if (blockEntity instanceof TileEntityBanner) {
            TileEntityBanner bannerBlockEntity = (TileEntityBanner)blockEntity;
            EnumColor dyeColor = bannerBlockEntity.getBaseColor();
            IChatBaseComponent component = bannerBlockEntity.hasCustomName() ? bannerBlockEntity.getCustomName() : null;
            return new MapIconBanner(blockPos, dyeColor, component);
        } else {
            return null;
        }
    }

    public BlockPosition getPos() {
        return this.pos;
    }

    public EnumColor getColor() {
        return this.color;
    }

    public MapIcon.Type getDecoration() {
        switch(this.color) {
        case WHITE:
            return MapIcon.Type.BANNER_WHITE;
        case ORANGE:
            return MapIcon.Type.BANNER_ORANGE;
        case MAGENTA:
            return MapIcon.Type.BANNER_MAGENTA;
        case LIGHT_BLUE:
            return MapIcon.Type.BANNER_LIGHT_BLUE;
        case YELLOW:
            return MapIcon.Type.BANNER_YELLOW;
        case LIME:
            return MapIcon.Type.BANNER_LIME;
        case PINK:
            return MapIcon.Type.BANNER_PINK;
        case GRAY:
            return MapIcon.Type.BANNER_GRAY;
        case LIGHT_GRAY:
            return MapIcon.Type.BANNER_LIGHT_GRAY;
        case CYAN:
            return MapIcon.Type.BANNER_CYAN;
        case PURPLE:
            return MapIcon.Type.BANNER_PURPLE;
        case BLUE:
            return MapIcon.Type.BANNER_BLUE;
        case BROWN:
            return MapIcon.Type.BANNER_BROWN;
        case GREEN:
            return MapIcon.Type.BANNER_GREEN;
        case RED:
            return MapIcon.Type.BANNER_RED;
        case BLACK:
        default:
            return MapIcon.Type.BANNER_BLACK;
        }
    }

    @Nullable
    public IChatBaseComponent getName() {
        return this.name;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (object != null && this.getClass() == object.getClass()) {
            MapIconBanner mapBanner = (MapIconBanner)object;
            return Objects.equals(this.pos, mapBanner.pos) && this.color == mapBanner.color && Objects.equals(this.name, mapBanner.name);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.pos, this.color, this.name);
    }

    public NBTTagCompound save() {
        NBTTagCompound compoundTag = new NBTTagCompound();
        compoundTag.set("Pos", GameProfileSerializer.writeBlockPos(this.pos));
        compoundTag.setString("Color", this.color.getName());
        if (this.name != null) {
            compoundTag.setString("Name", IChatBaseComponent.ChatSerializer.toJson(this.name));
        }

        return compoundTag;
    }

    public String getId() {
        return "banner-" + this.pos.getX() + "," + this.pos.getY() + "," + this.pos.getZ();
    }
}
