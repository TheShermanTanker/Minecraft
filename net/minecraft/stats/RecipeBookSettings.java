package net.minecraft.stats;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import java.util.Map;
import net.minecraft.SystemUtils;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.world.inventory.RecipeBookType;

public final class RecipeBookSettings {
    private static final Map<RecipeBookType, Pair<String, String>> TAG_FIELDS = ImmutableMap.of(RecipeBookType.CRAFTING, Pair.of("isGuiOpen", "isFilteringCraftable"), RecipeBookType.FURNACE, Pair.of("isFurnaceGuiOpen", "isFurnaceFilteringCraftable"), RecipeBookType.BLAST_FURNACE, Pair.of("isBlastingFurnaceGuiOpen", "isBlastingFurnaceFilteringCraftable"), RecipeBookType.SMOKER, Pair.of("isSmokerGuiOpen", "isSmokerFilteringCraftable"));
    private final Map<RecipeBookType, RecipeBookSettings.TypeSettings> states;

    private RecipeBookSettings(Map<RecipeBookType, RecipeBookSettings.TypeSettings> categoryOptions) {
        this.states = categoryOptions;
    }

    public RecipeBookSettings() {
        this(SystemUtils.make(Maps.newEnumMap(RecipeBookType.class), (enumMap) -> {
            for(RecipeBookType recipeBookType : RecipeBookType.values()) {
                enumMap.put(recipeBookType, new RecipeBookSettings.TypeSettings(false, false));
            }

        }));
    }

    public boolean isOpen(RecipeBookType category) {
        return (this.states.get(category)).open;
    }

    public void setOpen(RecipeBookType category, boolean open) {
        (this.states.get(category)).open = open;
    }

    public boolean isFiltering(RecipeBookType category) {
        return (this.states.get(category)).filtering;
    }

    public void setFiltering(RecipeBookType category, boolean filtering) {
        (this.states.get(category)).filtering = filtering;
    }

    public static RecipeBookSettings read(PacketDataSerializer buf) {
        Map<RecipeBookType, RecipeBookSettings.TypeSettings> map = Maps.newEnumMap(RecipeBookType.class);

        for(RecipeBookType recipeBookType : RecipeBookType.values()) {
            boolean bl = buf.readBoolean();
            boolean bl2 = buf.readBoolean();
            map.put(recipeBookType, new RecipeBookSettings.TypeSettings(bl, bl2));
        }

        return new RecipeBookSettings(map);
    }

    public void write(PacketDataSerializer buf) {
        for(RecipeBookType recipeBookType : RecipeBookType.values()) {
            RecipeBookSettings.TypeSettings typeSettings = this.states.get(recipeBookType);
            if (typeSettings == null) {
                buf.writeBoolean(false);
                buf.writeBoolean(false);
            } else {
                buf.writeBoolean(typeSettings.open);
                buf.writeBoolean(typeSettings.filtering);
            }
        }

    }

    public static RecipeBookSettings read(NBTTagCompound nbt) {
        Map<RecipeBookType, RecipeBookSettings.TypeSettings> map = Maps.newEnumMap(RecipeBookType.class);
        TAG_FIELDS.forEach((category, pair) -> {
            boolean bl = nbt.getBoolean(pair.getFirst());
            boolean bl2 = nbt.getBoolean(pair.getSecond());
            map.put(category, new RecipeBookSettings.TypeSettings(bl, bl2));
        });
        return new RecipeBookSettings(map);
    }

    public void write(NBTTagCompound nbt) {
        TAG_FIELDS.forEach((category, pair) -> {
            RecipeBookSettings.TypeSettings typeSettings = this.states.get(category);
            nbt.setBoolean(pair.getFirst(), typeSettings.open);
            nbt.setBoolean(pair.getSecond(), typeSettings.filtering);
        });
    }

    public RecipeBookSettings copy() {
        Map<RecipeBookType, RecipeBookSettings.TypeSettings> map = Maps.newEnumMap(RecipeBookType.class);

        for(RecipeBookType recipeBookType : RecipeBookType.values()) {
            RecipeBookSettings.TypeSettings typeSettings = this.states.get(recipeBookType);
            map.put(recipeBookType, typeSettings.copy());
        }

        return new RecipeBookSettings(map);
    }

    public void replaceFrom(RecipeBookSettings other) {
        this.states.clear();

        for(RecipeBookType recipeBookType : RecipeBookType.values()) {
            RecipeBookSettings.TypeSettings typeSettings = other.states.get(recipeBookType);
            this.states.put(recipeBookType, typeSettings.copy());
        }

    }

    @Override
    public boolean equals(Object object) {
        return this == object || object instanceof RecipeBookSettings && this.states.equals(((RecipeBookSettings)object).states);
    }

    @Override
    public int hashCode() {
        return this.states.hashCode();
    }

    static final class TypeSettings {
        boolean open;
        boolean filtering;

        public TypeSettings(boolean guiOpen, boolean filteringCraftable) {
            this.open = guiOpen;
            this.filtering = filteringCraftable;
        }

        public RecipeBookSettings.TypeSettings copy() {
            return new RecipeBookSettings.TypeSettings(this.open, this.filtering);
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            } else if (!(object instanceof RecipeBookSettings.TypeSettings)) {
                return false;
            } else {
                RecipeBookSettings.TypeSettings typeSettings = (RecipeBookSettings.TypeSettings)object;
                return this.open == typeSettings.open && this.filtering == typeSettings.filtering;
            }
        }

        @Override
        public int hashCode() {
            int i = this.open ? 1 : 0;
            return 31 * i + (this.filtering ? 1 : 0);
        }

        @Override
        public String toString() {
            return "[open=" + this.open + ", filtering=" + this.filtering + "]";
        }
    }
}
