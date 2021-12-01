package net.minecraft.server.commands.data;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.arguments.ArgumentNBTBase;
import net.minecraft.commands.arguments.ArgumentNBTKey;
import net.minecraft.commands.arguments.ArgumentNBTTag;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTList;
import net.minecraft.nbt.NBTNumber;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.util.MathHelper;

public class CommandData {
    private static final SimpleCommandExceptionType ERROR_MERGE_UNCHANGED = new SimpleCommandExceptionType(new ChatMessage("commands.data.merge.failed"));
    private static final DynamicCommandExceptionType ERROR_GET_NOT_NUMBER = new DynamicCommandExceptionType((path) -> {
        return new ChatMessage("commands.data.get.invalid", path);
    });
    private static final DynamicCommandExceptionType ERROR_GET_NON_EXISTENT = new DynamicCommandExceptionType((path) -> {
        return new ChatMessage("commands.data.get.unknown", path);
    });
    private static final SimpleCommandExceptionType ERROR_MULTIPLE_TAGS = new SimpleCommandExceptionType(new ChatMessage("commands.data.get.multiple"));
    private static final DynamicCommandExceptionType ERROR_EXPECTED_LIST = new DynamicCommandExceptionType((nbt) -> {
        return new ChatMessage("commands.data.modify.expected_list", nbt);
    });
    private static final DynamicCommandExceptionType ERROR_EXPECTED_OBJECT = new DynamicCommandExceptionType((nbt) -> {
        return new ChatMessage("commands.data.modify.expected_object", nbt);
    });
    private static final DynamicCommandExceptionType ERROR_INVALID_INDEX = new DynamicCommandExceptionType((index) -> {
        return new ChatMessage("commands.data.modify.invalid_index", index);
    });
    public static final List<Function<String, CommandData.DataProvider>> ALL_PROVIDERS = ImmutableList.of(CommandDataAccessorEntity.PROVIDER, CommandDataAccessorTile.PROVIDER, CommandDataStorage.PROVIDER);
    public static final List<CommandData.DataProvider> TARGET_PROVIDERS = ALL_PROVIDERS.stream().map((factory) -> {
        return factory.apply("target");
    }).collect(ImmutableList.toImmutableList());
    public static final List<CommandData.DataProvider> SOURCE_PROVIDERS = ALL_PROVIDERS.stream().map((factory) -> {
        return factory.apply("source");
    }).collect(ImmutableList.toImmutableList());

    public static void register(CommandDispatcher<CommandListenerWrapper> dispatcher) {
        LiteralArgumentBuilder<CommandListenerWrapper> literalArgumentBuilder = net.minecraft.commands.CommandDispatcher.literal("data").requires((source) -> {
            return source.hasPermission(2);
        });

        for(CommandData.DataProvider dataProvider : TARGET_PROVIDERS) {
            literalArgumentBuilder.then(dataProvider.wrap(net.minecraft.commands.CommandDispatcher.literal("merge"), (builder) -> {
                return builder.then(net.minecraft.commands.CommandDispatcher.argument("nbt", ArgumentNBTTag.compoundTag()).executes((context) -> {
                    return mergeData(context.getSource(), dataProvider.access(context), ArgumentNBTTag.getCompoundTag(context, "nbt"));
                }));
            })).then(dataProvider.wrap(net.minecraft.commands.CommandDispatcher.literal("get"), (builder) -> {
                return builder.executes((context) -> {
                    return getData(context.getSource(), dataProvider.access(context));
                }).then(net.minecraft.commands.CommandDispatcher.argument("path", ArgumentNBTKey.nbtPath()).executes((context) -> {
                    return getData(context.getSource(), dataProvider.access(context), ArgumentNBTKey.getPath(context, "path"));
                }).then(net.minecraft.commands.CommandDispatcher.argument("scale", DoubleArgumentType.doubleArg()).executes((context) -> {
                    return getNumeric(context.getSource(), dataProvider.access(context), ArgumentNBTKey.getPath(context, "path"), DoubleArgumentType.getDouble(context, "scale"));
                })));
            })).then(dataProvider.wrap(net.minecraft.commands.CommandDispatcher.literal("remove"), (builder) -> {
                return builder.then(net.minecraft.commands.CommandDispatcher.argument("path", ArgumentNBTKey.nbtPath()).executes((context) -> {
                    return removeData(context.getSource(), dataProvider.access(context), ArgumentNBTKey.getPath(context, "path"));
                }));
            })).then(decorateModification((builder, modifier) -> {
                builder.then(net.minecraft.commands.CommandDispatcher.literal("insert").then(net.minecraft.commands.CommandDispatcher.argument("index", IntegerArgumentType.integer()).then(modifier.create((context, sourceNbt, path, elements) -> {
                    int i = IntegerArgumentType.getInteger(context, "index");
                    return insertAtIndex(i, sourceNbt, path, elements);
                })))).then(net.minecraft.commands.CommandDispatcher.literal("prepend").then(modifier.create((context, sourceNbt, path, elements) -> {
                    return insertAtIndex(0, sourceNbt, path, elements);
                }))).then(net.minecraft.commands.CommandDispatcher.literal("append").then(modifier.create((context, sourceNbt, path, elements) -> {
                    return insertAtIndex(-1, sourceNbt, path, elements);
                }))).then(net.minecraft.commands.CommandDispatcher.literal("set").then(modifier.create((context, sourceNbt, path, elements) -> {
                    return path.set(sourceNbt, Iterables.getLast(elements)::clone);
                }))).then(net.minecraft.commands.CommandDispatcher.literal("merge").then(modifier.create((context, sourceNbt, path, elements) -> {
                    Collection<NBTBase> collection = path.getOrCreate(sourceNbt, NBTTagCompound::new);
                    int i = 0;

                    for(NBTBase tag : collection) {
                        if (!(tag instanceof NBTTagCompound)) {
                            throw ERROR_EXPECTED_OBJECT.create(tag);
                        }

                        NBTTagCompound compoundTag = (NBTTagCompound)tag;
                        NBTTagCompound compoundTag2 = compoundTag.copy();

                        for(NBTBase tag2 : elements) {
                            if (!(tag2 instanceof NBTTagCompound)) {
                                throw ERROR_EXPECTED_OBJECT.create(tag2);
                            }

                            compoundTag.merge((NBTTagCompound)tag2);
                        }

                        i += compoundTag2.equals(compoundTag) ? 0 : 1;
                    }

                    return i;
                })));
            }));
        }

        dispatcher.register(literalArgumentBuilder);
    }

    private static int insertAtIndex(int integer, NBTTagCompound sourceNbt, ArgumentNBTKey.NbtPath path, List<NBTBase> elements) throws CommandSyntaxException {
        Collection<NBTBase> collection = path.getOrCreate(sourceNbt, NBTTagList::new);
        int i = 0;

        for(NBTBase tag : collection) {
            if (!(tag instanceof NBTList)) {
                throw ERROR_EXPECTED_LIST.create(tag);
            }

            boolean bl = false;
            NBTList<?> collectionTag = (NBTList)tag;
            int j = integer < 0 ? collectionTag.size() + integer + 1 : integer;

            for(NBTBase tag2 : elements) {
                try {
                    if (collectionTag.addTag(j, tag2.clone())) {
                        ++j;
                        bl = true;
                    }
                } catch (IndexOutOfBoundsException var14) {
                    throw ERROR_INVALID_INDEX.create(j);
                }
            }

            i += bl ? 1 : 0;
        }

        return i;
    }

    private static ArgumentBuilder<CommandListenerWrapper, ?> decorateModification(BiConsumer<ArgumentBuilder<CommandListenerWrapper, ?>, CommandData.DataManipulatorDecorator> subArgumentAdder) {
        LiteralArgumentBuilder<CommandListenerWrapper> literalArgumentBuilder = net.minecraft.commands.CommandDispatcher.literal("modify");

        for(CommandData.DataProvider dataProvider : TARGET_PROVIDERS) {
            dataProvider.wrap(literalArgumentBuilder, (builder) -> {
                ArgumentBuilder<CommandListenerWrapper, ?> argumentBuilder = net.minecraft.commands.CommandDispatcher.argument("targetPath", ArgumentNBTKey.nbtPath());

                for(CommandData.DataProvider dataProvider2 : SOURCE_PROVIDERS) {
                    subArgumentAdder.accept(argumentBuilder, (modifier) -> {
                        return dataProvider2.wrap(net.minecraft.commands.CommandDispatcher.literal("from"), (builder) -> {
                            return builder.executes((context) -> {
                                List<NBTBase> list = Collections.singletonList(dataProvider2.access(context).getData());
                                return manipulateData(context, dataProvider, modifier, list);
                            }).then(net.minecraft.commands.CommandDispatcher.argument("sourcePath", ArgumentNBTKey.nbtPath()).executes((context) -> {
                                CommandDataAccessor dataAccessor = dataProvider2.access(context);
                                ArgumentNBTKey.NbtPath nbtPath = ArgumentNBTKey.getPath(context, "sourcePath");
                                List<NBTBase> list = nbtPath.get(dataAccessor.getData());
                                return manipulateData(context, dataProvider, modifier, list);
                            }));
                        });
                    });
                }

                subArgumentAdder.accept(argumentBuilder, (modifier) -> {
                    return net.minecraft.commands.CommandDispatcher.literal("value").then(net.minecraft.commands.CommandDispatcher.argument("value", ArgumentNBTBase.nbtTag()).executes((context) -> {
                        List<NBTBase> list = Collections.singletonList(ArgumentNBTBase.getNbtTag(context, "value"));
                        return manipulateData(context, dataProvider, modifier, list);
                    }));
                });
                return builder.then(argumentBuilder);
            });
        }

        return literalArgumentBuilder;
    }

    private static int manipulateData(CommandContext<CommandListenerWrapper> context, CommandData.DataProvider objectType, CommandData.DataManipulator modifier, List<NBTBase> elements) throws CommandSyntaxException {
        CommandDataAccessor dataAccessor = objectType.access(context);
        ArgumentNBTKey.NbtPath nbtPath = ArgumentNBTKey.getPath(context, "targetPath");
        NBTTagCompound compoundTag = dataAccessor.getData();
        int i = modifier.modify(context, compoundTag, nbtPath, elements);
        if (i == 0) {
            throw ERROR_MERGE_UNCHANGED.create();
        } else {
            dataAccessor.setData(compoundTag);
            context.getSource().sendMessage(dataAccessor.getModifiedSuccess(), true);
            return i;
        }
    }

    private static int removeData(CommandListenerWrapper source, CommandDataAccessor object, ArgumentNBTKey.NbtPath path) throws CommandSyntaxException {
        NBTTagCompound compoundTag = object.getData();
        int i = path.remove(compoundTag);
        if (i == 0) {
            throw ERROR_MERGE_UNCHANGED.create();
        } else {
            object.setData(compoundTag);
            source.sendMessage(object.getModifiedSuccess(), true);
            return i;
        }
    }

    private static NBTBase getSingleTag(ArgumentNBTKey.NbtPath path, CommandDataAccessor object) throws CommandSyntaxException {
        Collection<NBTBase> collection = path.get(object.getData());
        Iterator<NBTBase> iterator = collection.iterator();
        NBTBase tag = iterator.next();
        if (iterator.hasNext()) {
            throw ERROR_MULTIPLE_TAGS.create();
        } else {
            return tag;
        }
    }

    private static int getData(CommandListenerWrapper source, CommandDataAccessor object, ArgumentNBTKey.NbtPath path) throws CommandSyntaxException {
        NBTBase tag = getSingleTag(path, object);
        int i;
        if (tag instanceof NBTNumber) {
            i = MathHelper.floor(((NBTNumber)tag).asDouble());
        } else if (tag instanceof NBTList) {
            i = ((NBTList)tag).size();
        } else if (tag instanceof NBTTagCompound) {
            i = ((NBTTagCompound)tag).size();
        } else {
            if (!(tag instanceof NBTTagString)) {
                throw ERROR_GET_NON_EXISTENT.create(path.toString());
            }

            i = tag.asString().length();
        }

        source.sendMessage(object.getPrintSuccess(tag), false);
        return i;
    }

    private static int getNumeric(CommandListenerWrapper source, CommandDataAccessor object, ArgumentNBTKey.NbtPath path, double scale) throws CommandSyntaxException {
        NBTBase tag = getSingleTag(path, object);
        if (!(tag instanceof NBTNumber)) {
            throw ERROR_GET_NOT_NUMBER.create(path.toString());
        } else {
            int i = MathHelper.floor(((NBTNumber)tag).asDouble() * scale);
            source.sendMessage(object.getPrintSuccess(path, scale, i), false);
            return i;
        }
    }

    private static int getData(CommandListenerWrapper source, CommandDataAccessor object) throws CommandSyntaxException {
        source.sendMessage(object.getPrintSuccess(object.getData()), false);
        return 1;
    }

    private static int mergeData(CommandListenerWrapper source, CommandDataAccessor object, NBTTagCompound nbt) throws CommandSyntaxException {
        NBTTagCompound compoundTag = object.getData();
        NBTTagCompound compoundTag2 = compoundTag.copy().merge(nbt);
        if (compoundTag.equals(compoundTag2)) {
            throw ERROR_MERGE_UNCHANGED.create();
        } else {
            object.setData(compoundTag2);
            source.sendMessage(object.getModifiedSuccess(), true);
            return 1;
        }
    }

    interface DataManipulator {
        int modify(CommandContext<CommandListenerWrapper> context, NBTTagCompound sourceNbt, ArgumentNBTKey.NbtPath path, List<NBTBase> elements) throws CommandSyntaxException;
    }

    interface DataManipulatorDecorator {
        ArgumentBuilder<CommandListenerWrapper, ?> create(CommandData.DataManipulator modifier);
    }

    public interface DataProvider {
        CommandDataAccessor access(CommandContext<CommandListenerWrapper> context) throws CommandSyntaxException;

        ArgumentBuilder<CommandListenerWrapper, ?> wrap(ArgumentBuilder<CommandListenerWrapper, ?> argument, Function<ArgumentBuilder<CommandListenerWrapper, ?>, ArgumentBuilder<CommandListenerWrapper, ?>> argumentAdder);
    }
}
