package net.minecraft.nbt;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.Dynamic;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistry;
import net.minecraft.core.MinecraftSerializableUUID;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.util.UtilColor;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.IBlockDataHolder;
import net.minecraft.world.level.block.state.properties.IBlockState;
import net.minecraft.world.level.material.Fluid;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class GameProfileSerializer {
    private static final Comparator<NBTTagList> YXZ_LISTTAG_INT_COMPARATOR = Comparator.comparingInt((listTag) -> {
        return listTag.getInt(1);
    }).thenComparingInt((listTag) -> {
        return listTag.getInt(0);
    }).thenComparingInt((listTag) -> {
        return listTag.getInt(2);
    });
    private static final Comparator<NBTTagList> YXZ_LISTTAG_DOUBLE_COMPARATOR = Comparator.comparingDouble((listTag) -> {
        return listTag.getDouble(1);
    }).thenComparingDouble((listTag) -> {
        return listTag.getDouble(0);
    }).thenComparingDouble((listTag) -> {
        return listTag.getDouble(2);
    });
    public static final String SNBT_DATA_TAG = "data";
    private static final char PROPERTIES_START = '{';
    private static final char PROPERTIES_END = '}';
    private static final String ELEMENT_SEPARATOR = ",";
    private static final char KEY_VALUE_SEPARATOR = ':';
    private static final Splitter COMMA_SPLITTER = Splitter.on(",");
    private static final Splitter COLON_SPLITTER = Splitter.on(':').limit(2);
    private static final Logger LOGGER = LogManager.getLogger();
    private static final int INDENT = 2;
    private static final int NOT_FOUND = -1;

    private GameProfileSerializer() {
    }

    @Nullable
    public static GameProfile deserialize(NBTTagCompound compound) {
        String string = null;
        UUID uUID = null;
        if (compound.hasKeyOfType("Name", 8)) {
            string = compound.getString("Name");
        }

        if (compound.hasUUID("Id")) {
            uUID = compound.getUUID("Id");
        }

        try {
            GameProfile gameProfile = new GameProfile(uUID, string);
            if (compound.hasKeyOfType("Properties", 10)) {
                NBTTagCompound compoundTag = compound.getCompound("Properties");

                for(String string2 : compoundTag.getKeys()) {
                    NBTTagList listTag = compoundTag.getList(string2, 10);

                    for(int i = 0; i < listTag.size(); ++i) {
                        NBTTagCompound compoundTag2 = listTag.getCompound(i);
                        String string3 = compoundTag2.getString("Value");
                        if (compoundTag2.hasKeyOfType("Signature", 8)) {
                            gameProfile.getProperties().put(string2, new Property(string2, string3, compoundTag2.getString("Signature")));
                        } else {
                            gameProfile.getProperties().put(string2, new Property(string2, string3));
                        }
                    }
                }
            }

            return gameProfile;
        } catch (Throwable var11) {
            return null;
        }
    }

    public static NBTTagCompound serialize(NBTTagCompound compound, GameProfile profile) {
        if (!UtilColor.isNullOrEmpty(profile.getName())) {
            compound.setString("Name", profile.getName());
        }

        if (profile.getId() != null) {
            compound.putUUID("Id", profile.getId());
        }

        if (!profile.getProperties().isEmpty()) {
            NBTTagCompound compoundTag = new NBTTagCompound();

            for(String string : profile.getProperties().keySet()) {
                NBTTagList listTag = new NBTTagList();

                for(Property property : profile.getProperties().get(string)) {
                    NBTTagCompound compoundTag2 = new NBTTagCompound();
                    compoundTag2.setString("Value", property.getValue());
                    if (property.hasSignature()) {
                        compoundTag2.setString("Signature", property.getSignature());
                    }

                    listTag.add(compoundTag2);
                }

                compoundTag.set(string, listTag);
            }

            compound.set("Properties", compoundTag);
        }

        return compound;
    }

    @VisibleForTesting
    public static boolean compareNbt(@Nullable NBTBase standard, @Nullable NBTBase subject, boolean equalValue) {
        if (standard == subject) {
            return true;
        } else if (standard == null) {
            return true;
        } else if (subject == null) {
            return false;
        } else if (!standard.getClass().equals(subject.getClass())) {
            return false;
        } else if (standard instanceof NBTTagCompound) {
            NBTTagCompound compoundTag = (NBTTagCompound)standard;
            NBTTagCompound compoundTag2 = (NBTTagCompound)subject;

            for(String string : compoundTag.getKeys()) {
                NBTBase tag = compoundTag.get(string);
                if (!compareNbt(tag, compoundTag2.get(string), equalValue)) {
                    return false;
                }
            }

            return true;
        } else if (standard instanceof NBTTagList && equalValue) {
            NBTTagList listTag = (NBTTagList)standard;
            NBTTagList listTag2 = (NBTTagList)subject;
            if (listTag.isEmpty()) {
                return listTag2.isEmpty();
            } else {
                for(int i = 0; i < listTag.size(); ++i) {
                    NBTBase tag2 = listTag.get(i);
                    boolean bl = false;

                    for(int j = 0; j < listTag2.size(); ++j) {
                        if (compareNbt(tag2, listTag2.get(j), equalValue)) {
                            bl = true;
                            break;
                        }
                    }

                    if (!bl) {
                        return false;
                    }
                }

                return true;
            }
        } else {
            return standard.equals(subject);
        }
    }

    public static NBTTagIntArray createUUID(UUID uuid) {
        return new NBTTagIntArray(MinecraftSerializableUUID.uuidToIntArray(uuid));
    }

    public static UUID loadUUID(NBTBase element) {
        if (element.getType() != NBTTagIntArray.TYPE) {
            throw new IllegalArgumentException("Expected UUID-Tag to be of type " + NBTTagIntArray.TYPE.getName() + ", but found " + element.getType().getName() + ".");
        } else {
            int[] is = ((NBTTagIntArray)element).getInts();
            if (is.length != 4) {
                throw new IllegalArgumentException("Expected UUID-Array to be of length 4, but found " + is.length + ".");
            } else {
                return MinecraftSerializableUUID.uuidFromIntArray(is);
            }
        }
    }

    public static BlockPosition readBlockPos(NBTTagCompound compound) {
        return new BlockPosition(compound.getInt("X"), compound.getInt("Y"), compound.getInt("Z"));
    }

    public static NBTTagCompound writeBlockPos(BlockPosition pos) {
        NBTTagCompound compoundTag = new NBTTagCompound();
        compoundTag.setInt("X", pos.getX());
        compoundTag.setInt("Y", pos.getY());
        compoundTag.setInt("Z", pos.getZ());
        return compoundTag;
    }

    public static IBlockData readBlockState(NBTTagCompound compound) {
        if (!compound.hasKeyOfType("Name", 8)) {
            return Blocks.AIR.getBlockData();
        } else {
            Block block = IRegistry.BLOCK.get(new MinecraftKey(compound.getString("Name")));
            IBlockData blockState = block.getBlockData();
            if (compound.hasKeyOfType("Properties", 10)) {
                NBTTagCompound compoundTag = compound.getCompound("Properties");
                BlockStateList<Block, IBlockData> stateDefinition = block.getStates();

                for(String string : compoundTag.getKeys()) {
                    IBlockState<?> property = stateDefinition.getProperty(string);
                    if (property != null) {
                        blockState = setValueHelper(blockState, property, string, compoundTag, compound);
                    }
                }
            }

            return blockState;
        }
    }

    private static <S extends IBlockDataHolder<?, S>, T extends Comparable<T>> S setValueHelper(S state, IBlockState<T> property, String key, NBTTagCompound properties, NBTTagCompound root) {
        Optional<T> optional = property.getValue(properties.getString(key));
        if (optional.isPresent()) {
            return state.set(property, optional.get());
        } else {
            LOGGER.warn("Unable to read property: {} with value: {} for blockstate: {}", key, properties.getString(key), root.toString());
            return state;
        }
    }

    public static NBTTagCompound writeBlockState(IBlockData state) {
        NBTTagCompound compoundTag = new NBTTagCompound();
        compoundTag.setString("Name", IRegistry.BLOCK.getKey(state.getBlock()).toString());
        ImmutableMap<IBlockState<?>, Comparable<?>> immutableMap = state.getStateMap();
        if (!immutableMap.isEmpty()) {
            NBTTagCompound compoundTag2 = new NBTTagCompound();

            for(Entry<IBlockState<?>, Comparable<?>> entry : immutableMap.entrySet()) {
                IBlockState<?> property = entry.getKey();
                compoundTag2.setString(property.getName(), getName(property, entry.getValue()));
            }

            compoundTag.set("Properties", compoundTag2);
        }

        return compoundTag;
    }

    public static NBTTagCompound writeFluidState(Fluid fluidState) {
        NBTTagCompound compoundTag = new NBTTagCompound();
        compoundTag.setString("Name", IRegistry.FLUID.getKey(fluidState.getType()).toString());
        ImmutableMap<IBlockState<?>, Comparable<?>> immutableMap = fluidState.getStateMap();
        if (!immutableMap.isEmpty()) {
            NBTTagCompound compoundTag2 = new NBTTagCompound();

            for(Entry<IBlockState<?>, Comparable<?>> entry : immutableMap.entrySet()) {
                IBlockState<?> property = entry.getKey();
                compoundTag2.setString(property.getName(), getName(property, entry.getValue()));
            }

            compoundTag.set("Properties", compoundTag2);
        }

        return compoundTag;
    }

    private static <T extends Comparable<T>> String getName(IBlockState<T> property, Comparable<?> value) {
        return property.getName((T)value);
    }

    public static String prettyPrint(NBTBase tag) {
        return prettyPrint(tag, false);
    }

    public static String prettyPrint(NBTBase tag, boolean bl) {
        return prettyPrint(new StringBuilder(), tag, 0, bl).toString();
    }

    public static StringBuilder prettyPrint(StringBuilder stringBuilder, NBTBase tag, int i, boolean bl) {
        switch(tag.getTypeId()) {
        case 0:
            break;
        case 1:
        case 2:
        case 3:
        case 4:
        case 5:
        case 6:
        case 8:
            stringBuilder.append((Object)tag);
            break;
        case 7:
            NBTTagByteArray byteArrayTag = (NBTTagByteArray)tag;
            byte[] bs = byteArrayTag.getBytes();
            int j = bs.length;
            indent(i, stringBuilder).append("byte[").append(j).append("] {\n");
            if (!bl) {
                indent(i + 1, stringBuilder).append(" // Skipped, supply withBinaryBlobs true");
            } else {
                indent(i + 1, stringBuilder);

                for(int k = 0; k < bs.length; ++k) {
                    if (k != 0) {
                        stringBuilder.append(',');
                    }

                    if (k % 16 == 0 && k / 16 > 0) {
                        stringBuilder.append('\n');
                        if (k < bs.length) {
                            indent(i + 1, stringBuilder);
                        }
                    } else if (k != 0) {
                        stringBuilder.append(' ');
                    }

                    stringBuilder.append(String.format("0x%02X", bs[k] & 255));
                }
            }

            stringBuilder.append('\n');
            indent(i, stringBuilder).append('}');
            break;
        case 9:
            NBTTagList listTag = (NBTTagList)tag;
            int l = listTag.size();
            int m = listTag.getElementType();
            String string = m == 0 ? "undefined" : NBTTagTypes.getType(m).getPrettyName();
            indent(i, stringBuilder).append("list<").append(string).append(">[").append(l).append("] [");
            if (l != 0) {
                stringBuilder.append('\n');
            }

            for(int n = 0; n < l; ++n) {
                if (n != 0) {
                    stringBuilder.append(",\n");
                }

                indent(i + 1, stringBuilder);
                prettyPrint(stringBuilder, listTag.get(n), i + 1, bl);
            }

            if (l != 0) {
                stringBuilder.append('\n');
            }

            indent(i, stringBuilder).append(']');
            break;
        case 10:
            NBTTagCompound compoundTag = (NBTTagCompound)tag;
            List<String> list = Lists.newArrayList(compoundTag.getKeys());
            Collections.sort(list);
            indent(i, stringBuilder).append('{');
            if (stringBuilder.length() - stringBuilder.lastIndexOf("\n") > 2 * (i + 1)) {
                stringBuilder.append('\n');
                indent(i + 1, stringBuilder);
            }

            int s = list.stream().mapToInt(String::length).max().orElse(0);
            String string2 = Strings.repeat(" ", s);

            for(int t = 0; t < list.size(); ++t) {
                if (t != 0) {
                    stringBuilder.append(",\n");
                }

                String string3 = list.get(t);
                indent(i + 1, stringBuilder).append('"').append(string3).append('"').append((CharSequence)string2, 0, string2.length() - string3.length()).append(": ");
                prettyPrint(stringBuilder, compoundTag.get(string3), i + 1, bl);
            }

            if (!list.isEmpty()) {
                stringBuilder.append('\n');
            }

            indent(i, stringBuilder).append('}');
            break;
        case 11:
            NBTTagIntArray intArrayTag = (NBTTagIntArray)tag;
            int[] is = intArrayTag.getInts();
            int o = 0;

            for(int p : is) {
                o = Math.max(o, String.format("%X", p).length());
            }

            int q = is.length;
            indent(i, stringBuilder).append("int[").append(q).append("] {\n");
            if (!bl) {
                indent(i + 1, stringBuilder).append(" // Skipped, supply withBinaryBlobs true");
            } else {
                indent(i + 1, stringBuilder);

                for(int r = 0; r < is.length; ++r) {
                    if (r != 0) {
                        stringBuilder.append(',');
                    }

                    if (r % 16 == 0 && r / 16 > 0) {
                        stringBuilder.append('\n');
                        if (r < is.length) {
                            indent(i + 1, stringBuilder);
                        }
                    } else if (r != 0) {
                        stringBuilder.append(' ');
                    }

                    stringBuilder.append(String.format("0x%0" + o + "X", is[r]));
                }
            }

            stringBuilder.append('\n');
            indent(i, stringBuilder).append('}');
            break;
        case 12:
            NBTTagLongArray longArrayTag = (NBTTagLongArray)tag;
            long[] ls = longArrayTag.getLongs();
            long u = 0L;

            for(long v : ls) {
                u = Math.max(u, (long)String.format("%X", v).length());
            }

            long w = (long)ls.length;
            indent(i, stringBuilder).append("long[").append(w).append("] {\n");
            if (!bl) {
                indent(i + 1, stringBuilder).append(" // Skipped, supply withBinaryBlobs true");
            } else {
                indent(i + 1, stringBuilder);

                for(int x = 0; x < ls.length; ++x) {
                    if (x != 0) {
                        stringBuilder.append(',');
                    }

                    if (x % 16 == 0 && x / 16 > 0) {
                        stringBuilder.append('\n');
                        if (x < ls.length) {
                            indent(i + 1, stringBuilder);
                        }
                    } else if (x != 0) {
                        stringBuilder.append(' ');
                    }

                    stringBuilder.append(String.format("0x%0" + u + "X", ls[x]));
                }
            }

            stringBuilder.append('\n');
            indent(i, stringBuilder).append('}');
            break;
        default:
            stringBuilder.append("<UNKNOWN :(>");
        }

        return stringBuilder;
    }

    private static StringBuilder indent(int i, StringBuilder stringBuilder) {
        int j = stringBuilder.lastIndexOf("\n") + 1;
        int k = stringBuilder.length() - j;

        for(int l = 0; l < 2 * i - k; ++l) {
            stringBuilder.append(' ');
        }

        return stringBuilder;
    }

    public static NBTTagCompound update(DataFixer fixer, DataFixTypes fixTypes, NBTTagCompound compound, int oldVersion) {
        return update(fixer, fixTypes, compound, oldVersion, SharedConstants.getGameVersion().getWorldVersion());
    }

    public static NBTTagCompound update(DataFixer fixer, DataFixTypes fixTypes, NBTTagCompound compound, int oldVersion, int targetVersion) {
        return fixer.update(fixTypes.getType(), new Dynamic<>(DynamicOpsNBT.INSTANCE, compound), oldVersion, targetVersion).getValue();
    }

    public static IChatBaseComponent toPrettyComponent(NBTBase element) {
        return (new TextComponentTagVisitor("", 0)).visit(element);
    }

    public static String structureToSnbt(NBTTagCompound compound) {
        return (new SnbtPrinterTagVisitor()).visit(packStructureTemplate(compound));
    }

    public static NBTTagCompound snbtToStructure(String string) throws CommandSyntaxException {
        return unpackStructureTemplate(MojangsonParser.parse(string));
    }

    @VisibleForTesting
    static NBTTagCompound packStructureTemplate(NBTTagCompound compound) {
        boolean bl = compound.hasKeyOfType("palettes", 9);
        NBTTagList listTag;
        if (bl) {
            listTag = compound.getList("palettes", 9).getList(0);
        } else {
            listTag = compound.getList("palette", 10);
        }

        NBTTagList listTag3 = listTag.stream().map(NBTTagCompound.class::cast).map(GameProfileSerializer::packBlockState).map(NBTTagString::valueOf).collect(Collectors.toCollection(NBTTagList::new));
        compound.set("palette", listTag3);
        if (bl) {
            NBTTagList listTag4 = new NBTTagList();
            NBTTagList listTag5 = compound.getList("palettes", 9);
            listTag5.stream().map(NBTTagList.class::cast).forEach((listTag3x) -> {
                NBTTagCompound compoundTag = new NBTTagCompound();

                for(int i = 0; i < listTag3x.size(); ++i) {
                    compoundTag.setString(listTag3.getString(i), packBlockState(listTag3x.getCompound(i)));
                }

                listTag4.add(compoundTag);
            });
            compound.set("palettes", listTag4);
        }

        if (compound.hasKeyOfType("entities", 10)) {
            NBTTagList listTag6 = compound.getList("entities", 10);
            NBTTagList listTag7 = listTag6.stream().map(NBTTagCompound.class::cast).sorted(Comparator.comparing((compoundTag) -> {
                return compoundTag.getList("pos", 6);
            }, YXZ_LISTTAG_DOUBLE_COMPARATOR)).collect(Collectors.toCollection(NBTTagList::new));
            compound.set("entities", listTag7);
        }

        NBTTagList listTag8 = compound.getList("blocks", 10).stream().map(NBTTagCompound.class::cast).sorted(Comparator.comparing((compoundTag) -> {
            return compoundTag.getList("pos", 3);
        }, YXZ_LISTTAG_INT_COMPARATOR)).peek((compoundTag) -> {
            compoundTag.setString("state", listTag3.getString(compoundTag.getInt("state")));
        }).collect(Collectors.toCollection(NBTTagList::new));
        compound.set("data", listTag8);
        compound.remove("blocks");
        return compound;
    }

    @VisibleForTesting
    static NBTTagCompound unpackStructureTemplate(NBTTagCompound compound) {
        NBTTagList listTag = compound.getList("palette", 8);
        Map<String, NBTBase> map = listTag.stream().map(NBTTagString.class::cast).map(NBTTagString::asString).collect(ImmutableMap.toImmutableMap(Function.identity(), GameProfileSerializer::unpackBlockState));
        if (compound.hasKeyOfType("palettes", 9)) {
            compound.set("palettes", compound.getList("palettes", 10).stream().map(NBTTagCompound.class::cast).map((compoundTagx) -> {
                return map.keySet().stream().map(compoundTagx::getString).map(GameProfileSerializer::unpackBlockState).collect(Collectors.toCollection(NBTTagList::new));
            }).collect(Collectors.toCollection(NBTTagList::new)));
            compound.remove("palette");
        } else {
            compound.set("palette", map.values().stream().collect(Collectors.toCollection(NBTTagList::new)));
        }

        if (compound.hasKeyOfType("data", 9)) {
            Object2IntMap<String> object2IntMap = new Object2IntOpenHashMap<>();
            object2IntMap.defaultReturnValue(-1);

            for(int i = 0; i < listTag.size(); ++i) {
                object2IntMap.put(listTag.getString(i), i);
            }

            NBTTagList listTag2 = compound.getList("data", 10);

            for(int j = 0; j < listTag2.size(); ++j) {
                NBTTagCompound compoundTag = listTag2.getCompound(j);
                String string = compoundTag.getString("state");
                int k = object2IntMap.getInt(string);
                if (k == -1) {
                    throw new IllegalStateException("Entry " + string + " missing from palette");
                }

                compoundTag.setInt("state", k);
            }

            compound.set("blocks", listTag2);
            compound.remove("data");
        }

        return compound;
    }

    @VisibleForTesting
    static String packBlockState(NBTTagCompound compound) {
        StringBuilder stringBuilder = new StringBuilder(compound.getString("Name"));
        if (compound.hasKeyOfType("Properties", 10)) {
            NBTTagCompound compoundTag = compound.getCompound("Properties");
            String string = compoundTag.getKeys().stream().sorted().map((stringx) -> {
                return stringx + ":" + compoundTag.get(stringx).asString();
            }).collect(Collectors.joining(","));
            stringBuilder.append('{').append(string).append('}');
        }

        return stringBuilder.toString();
    }

    @VisibleForTesting
    static NBTTagCompound unpackBlockState(String string) {
        NBTTagCompound compoundTag = new NBTTagCompound();
        int i = string.indexOf(123);
        String string2;
        if (i >= 0) {
            string2 = string.substring(0, i);
            NBTTagCompound compoundTag2 = new NBTTagCompound();
            if (i + 2 <= string.length()) {
                String string3 = string.substring(i + 1, string.indexOf(125, i));
                COMMA_SPLITTER.split(string3).forEach((string2) -> {
                    List<String> list = COLON_SPLITTER.splitToList(string2);
                    if (list.size() == 2) {
                        compoundTag2.setString(list.get(0), list.get(1));
                    } else {
                        LOGGER.error("Something went wrong parsing: '{}' -- incorrect gamedata!", (Object)string);
                    }

                });
                compoundTag.set("Properties", compoundTag2);
            }
        } else {
            string2 = string;
        }

        compoundTag.setString("Name", string2);
        return compoundTag;
    }
}
