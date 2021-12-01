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
    private static final Comparator<NBTTagList> YXZ_LISTTAG_INT_COMPARATOR = Comparator.comparingInt((nbt) -> {
        return nbt.getInt(1);
    }).thenComparingInt((nbt) -> {
        return nbt.getInt(0);
    }).thenComparingInt((nbt) -> {
        return nbt.getInt(2);
    });
    private static final Comparator<NBTTagList> YXZ_LISTTAG_DOUBLE_COMPARATOR = Comparator.comparingDouble((nbt) -> {
        return nbt.getDouble(1);
    }).thenComparingDouble((nbt) -> {
        return nbt.getDouble(0);
    }).thenComparingDouble((nbt) -> {
        return nbt.getDouble(2);
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

    public static NBTTagCompound writeFluidState(Fluid state) {
        NBTTagCompound compoundTag = new NBTTagCompound();
        compoundTag.setString("Name", IRegistry.FLUID.getKey(state.getType()).toString());
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

    private static <T extends Comparable<T>> String getName(IBlockState<T> property, Comparable<?> value) {
        return property.getName((T)value);
    }

    public static String prettyPrint(NBTBase nbt) {
        return prettyPrint(nbt, false);
    }

    public static String prettyPrint(NBTBase nbt, boolean withArrayContents) {
        return prettyPrint(new StringBuilder(), nbt, 0, withArrayContents).toString();
    }

    public static StringBuilder prettyPrint(StringBuilder stringBuilder, NBTBase nbt, int depth, boolean withArrayContents) {
        switch(nbt.getTypeId()) {
        case 0:
            break;
        case 1:
        case 2:
        case 3:
        case 4:
        case 5:
        case 6:
        case 8:
            stringBuilder.append((Object)nbt);
            break;
        case 7:
            NBTTagByteArray byteArrayTag = (NBTTagByteArray)nbt;
            byte[] bs = byteArrayTag.getBytes();
            int i = bs.length;
            indent(depth, stringBuilder).append("byte[").append(i).append("] {\n");
            if (!withArrayContents) {
                indent(depth + 1, stringBuilder).append(" // Skipped, supply withBinaryBlobs true");
            } else {
                indent(depth + 1, stringBuilder);

                for(int j = 0; j < bs.length; ++j) {
                    if (j != 0) {
                        stringBuilder.append(',');
                    }

                    if (j % 16 == 0 && j / 16 > 0) {
                        stringBuilder.append('\n');
                        if (j < bs.length) {
                            indent(depth + 1, stringBuilder);
                        }
                    } else if (j != 0) {
                        stringBuilder.append(' ');
                    }

                    stringBuilder.append(String.format("0x%02X", bs[j] & 255));
                }
            }

            stringBuilder.append('\n');
            indent(depth, stringBuilder).append('}');
            break;
        case 9:
            NBTTagList listTag = (NBTTagList)nbt;
            int k = listTag.size();
            int l = listTag.getElementType();
            String string = l == 0 ? "undefined" : NBTTagTypes.getType(l).getPrettyName();
            indent(depth, stringBuilder).append("list<").append(string).append(">[").append(k).append("] [");
            if (k != 0) {
                stringBuilder.append('\n');
            }

            for(int m = 0; m < k; ++m) {
                if (m != 0) {
                    stringBuilder.append(",\n");
                }

                indent(depth + 1, stringBuilder);
                prettyPrint(stringBuilder, listTag.get(m), depth + 1, withArrayContents);
            }

            if (k != 0) {
                stringBuilder.append('\n');
            }

            indent(depth, stringBuilder).append(']');
            break;
        case 10:
            NBTTagCompound compoundTag = (NBTTagCompound)nbt;
            List<String> list = Lists.newArrayList(compoundTag.getKeys());
            Collections.sort(list);
            indent(depth, stringBuilder).append('{');
            if (stringBuilder.length() - stringBuilder.lastIndexOf("\n") > 2 * (depth + 1)) {
                stringBuilder.append('\n');
                indent(depth + 1, stringBuilder);
            }

            int r = list.stream().mapToInt(String::length).max().orElse(0);
            String string2 = Strings.repeat(" ", r);

            for(int s = 0; s < list.size(); ++s) {
                if (s != 0) {
                    stringBuilder.append(",\n");
                }

                String string3 = list.get(s);
                indent(depth + 1, stringBuilder).append('"').append(string3).append('"').append((CharSequence)string2, 0, string2.length() - string3.length()).append(": ");
                prettyPrint(stringBuilder, compoundTag.get(string3), depth + 1, withArrayContents);
            }

            if (!list.isEmpty()) {
                stringBuilder.append('\n');
            }

            indent(depth, stringBuilder).append('}');
            break;
        case 11:
            NBTTagIntArray intArrayTag = (NBTTagIntArray)nbt;
            int[] is = intArrayTag.getInts();
            int n = 0;

            for(int o : is) {
                n = Math.max(n, String.format("%X", o).length());
            }

            int p = is.length;
            indent(depth, stringBuilder).append("int[").append(p).append("] {\n");
            if (!withArrayContents) {
                indent(depth + 1, stringBuilder).append(" // Skipped, supply withBinaryBlobs true");
            } else {
                indent(depth + 1, stringBuilder);

                for(int q = 0; q < is.length; ++q) {
                    if (q != 0) {
                        stringBuilder.append(',');
                    }

                    if (q % 16 == 0 && q / 16 > 0) {
                        stringBuilder.append('\n');
                        if (q < is.length) {
                            indent(depth + 1, stringBuilder);
                        }
                    } else if (q != 0) {
                        stringBuilder.append(' ');
                    }

                    stringBuilder.append(String.format("0x%0" + n + "X", is[q]));
                }
            }

            stringBuilder.append('\n');
            indent(depth, stringBuilder).append('}');
            break;
        case 12:
            NBTTagLongArray longArrayTag = (NBTTagLongArray)nbt;
            long[] ls = longArrayTag.getLongs();
            long t = 0L;

            for(long u : ls) {
                t = Math.max(t, (long)String.format("%X", u).length());
            }

            long v = (long)ls.length;
            indent(depth, stringBuilder).append("long[").append(v).append("] {\n");
            if (!withArrayContents) {
                indent(depth + 1, stringBuilder).append(" // Skipped, supply withBinaryBlobs true");
            } else {
                indent(depth + 1, stringBuilder);

                for(int w = 0; w < ls.length; ++w) {
                    if (w != 0) {
                        stringBuilder.append(',');
                    }

                    if (w % 16 == 0 && w / 16 > 0) {
                        stringBuilder.append('\n');
                        if (w < ls.length) {
                            indent(depth + 1, stringBuilder);
                        }
                    } else if (w != 0) {
                        stringBuilder.append(' ');
                    }

                    stringBuilder.append(String.format("0x%0" + t + "X", ls[w]));
                }
            }

            stringBuilder.append('\n');
            indent(depth, stringBuilder).append('}');
            break;
        default:
            stringBuilder.append("<UNKNOWN :(>");
        }

        return stringBuilder;
    }

    private static StringBuilder indent(int depth, StringBuilder stringBuilder) {
        int i = stringBuilder.lastIndexOf("\n") + 1;
        int j = stringBuilder.length() - i;

        for(int k = 0; k < 2 * depth - j; ++k) {
            stringBuilder.append(' ');
        }

        return stringBuilder;
    }

    public static NBTTagCompound update(DataFixer fixer, DataFixTypes fixTypes, NBTTagCompound compound, int oldVersion) {
        return update(fixer, fixTypes, compound, oldVersion, SharedConstants.getCurrentVersion().getWorldVersion());
    }

    public static NBTTagCompound update(DataFixer fixer, DataFixTypes fixTypes, NBTTagCompound compound, int oldVersion, int targetVersion) {
        return fixer.update(fixTypes.getType(), new Dynamic<>(DynamicOpsNBT.INSTANCE, compound), oldVersion, targetVersion).getValue();
    }

    public static IChatBaseComponent toPrettyComponent(NBTBase element) {
        return (new TagVisitorTextComponent("", 0)).visit(element);
    }

    public static String structureToSnbt(NBTTagCompound compound) {
        return (new TagVisitorNBTPrinterSerialized()).visit(packStructureTemplate(compound));
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
            listTag5.stream().map(NBTTagList.class::cast).forEach((nbt) -> {
                NBTTagCompound compoundTag = new NBTTagCompound();

                for(int i = 0; i < nbt.size(); ++i) {
                    compoundTag.setString(listTag3.getString(i), packBlockState(nbt.getCompound(i)));
                }

                listTag4.add(compoundTag);
            });
            compound.set("palettes", listTag4);
        }

        if (compound.hasKeyOfType("entities", 10)) {
            NBTTagList listTag6 = compound.getList("entities", 10);
            NBTTagList listTag7 = listTag6.stream().map(NBTTagCompound.class::cast).sorted(Comparator.comparing((nbt) -> {
                return nbt.getList("pos", 6);
            }, YXZ_LISTTAG_DOUBLE_COMPARATOR)).collect(Collectors.toCollection(NBTTagList::new));
            compound.set("entities", listTag7);
        }

        NBTTagList listTag8 = compound.getList("blocks", 10).stream().map(NBTTagCompound.class::cast).sorted(Comparator.comparing((nbt) -> {
            return nbt.getList("pos", 3);
        }, YXZ_LISTTAG_INT_COMPARATOR)).peek((nbt) -> {
            nbt.setString("state", listTag3.getString(nbt.getInt("state")));
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
            compound.set("palettes", compound.getList("palettes", 10).stream().map(NBTTagCompound.class::cast).map((nbt) -> {
                return map.keySet().stream().map(nbt::getString).map(GameProfileSerializer::unpackBlockState).collect(Collectors.toCollection(NBTTagList::new));
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
            String string = compoundTag.getKeys().stream().sorted().map((key) -> {
                return key + ":" + compoundTag.get(key).asString();
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
                COMMA_SPLITTER.split(string3).forEach((property) -> {
                    List<String> list = COLON_SPLITTER.splitToList(property);
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
