package net.minecraft.world.level.block.entity;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.item.EnumColor;

public enum EnumBannerPatternType {
    BASE("base", "b", false),
    SQUARE_BOTTOM_LEFT("square_bottom_left", "bl"),
    SQUARE_BOTTOM_RIGHT("square_bottom_right", "br"),
    SQUARE_TOP_LEFT("square_top_left", "tl"),
    SQUARE_TOP_RIGHT("square_top_right", "tr"),
    STRIPE_BOTTOM("stripe_bottom", "bs"),
    STRIPE_TOP("stripe_top", "ts"),
    STRIPE_LEFT("stripe_left", "ls"),
    STRIPE_RIGHT("stripe_right", "rs"),
    STRIPE_CENTER("stripe_center", "cs"),
    STRIPE_MIDDLE("stripe_middle", "ms"),
    STRIPE_DOWNRIGHT("stripe_downright", "drs"),
    STRIPE_DOWNLEFT("stripe_downleft", "dls"),
    STRIPE_SMALL("small_stripes", "ss"),
    CROSS("cross", "cr"),
    STRAIGHT_CROSS("straight_cross", "sc"),
    TRIANGLE_BOTTOM("triangle_bottom", "bt"),
    TRIANGLE_TOP("triangle_top", "tt"),
    TRIANGLES_BOTTOM("triangles_bottom", "bts"),
    TRIANGLES_TOP("triangles_top", "tts"),
    DIAGONAL_LEFT("diagonal_left", "ld"),
    DIAGONAL_RIGHT("diagonal_up_right", "rd"),
    DIAGONAL_LEFT_MIRROR("diagonal_up_left", "lud"),
    DIAGONAL_RIGHT_MIRROR("diagonal_right", "rud"),
    CIRCLE_MIDDLE("circle", "mc"),
    RHOMBUS_MIDDLE("rhombus", "mr"),
    HALF_VERTICAL("half_vertical", "vh"),
    HALF_HORIZONTAL("half_horizontal", "hh"),
    HALF_VERTICAL_MIRROR("half_vertical_right", "vhr"),
    HALF_HORIZONTAL_MIRROR("half_horizontal_bottom", "hhb"),
    BORDER("border", "bo"),
    CURLY_BORDER("curly_border", "cbo"),
    GRADIENT("gradient", "gra"),
    GRADIENT_UP("gradient_up", "gru"),
    BRICKS("bricks", "bri"),
    GLOBE("globe", "glb", true),
    CREEPER("creeper", "cre", true),
    SKULL("skull", "sku", true),
    FLOWER("flower", "flo", true),
    MOJANG("mojang", "moj", true),
    PIGLIN("piglin", "pig", true);

    private static final EnumBannerPatternType[] VALUES = values();
    public static final int COUNT = VALUES.length;
    public static final int PATTERN_ITEM_COUNT = (int)Arrays.stream(VALUES).filter((bannerPattern) -> {
        return bannerPattern.hasPatternItem;
    }).count();
    public static final int AVAILABLE_PATTERNS = COUNT - PATTERN_ITEM_COUNT - 1;
    private final boolean hasPatternItem;
    private final String filename;
    final String hashname;

    private EnumBannerPatternType(String name, String id) {
        this(name, id, false);
    }

    private EnumBannerPatternType(String name, String id, boolean hasPatternItem) {
        this.filename = name;
        this.hashname = id;
        this.hasPatternItem = hasPatternItem;
    }

    public MinecraftKey location(boolean banner) {
        String string = banner ? "banner" : "shield";
        return new MinecraftKey("entity/" + string + "/" + this.getFilename());
    }

    public String getFilename() {
        return this.filename;
    }

    public String getHashname() {
        return this.hashname;
    }

    @Nullable
    public static EnumBannerPatternType byHash(String id) {
        for(EnumBannerPatternType bannerPattern : values()) {
            if (bannerPattern.hashname.equals(id)) {
                return bannerPattern;
            }
        }

        return null;
    }

    @Nullable
    public static EnumBannerPatternType byFilename(String name) {
        for(EnumBannerPatternType bannerPattern : values()) {
            if (bannerPattern.filename.equals(name)) {
                return bannerPattern;
            }
        }

        return null;
    }

    public static class Builder {
        private final List<Pair<EnumBannerPatternType, EnumColor>> patterns = Lists.newArrayList();

        public EnumBannerPatternType.Builder addPattern(EnumBannerPatternType pattern, EnumColor color) {
            return this.addPattern(Pair.of(pattern, color));
        }

        public EnumBannerPatternType.Builder addPattern(Pair<EnumBannerPatternType, EnumColor> pattern) {
            this.patterns.add(pattern);
            return this;
        }

        public NBTTagList toListTag() {
            NBTTagList listTag = new NBTTagList();

            for(Pair<EnumBannerPatternType, EnumColor> pair : this.patterns) {
                NBTTagCompound compoundTag = new NBTTagCompound();
                compoundTag.setString("Pattern", (pair.getFirst()).hashname);
                compoundTag.setInt("Color", pair.getSecond().getColorIndex());
                listTag.add(compoundTag);
            }

            return listTag;
        }
    }
}
