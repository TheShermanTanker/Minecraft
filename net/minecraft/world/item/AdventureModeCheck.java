package net.minecraft.world.item;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Objects;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.commands.arguments.blocks.ArgumentBlockPredicate;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tags.ITagRegistry;
import net.minecraft.world.level.block.state.pattern.ShapeDetectorBlock;

public class AdventureModeCheck {
    public static final ArgumentBlockPredicate PREDICATE_PARSER = ArgumentBlockPredicate.blockPredicate();
    private final String tagName;
    @Nullable
    private ShapeDetectorBlock lastCheckedBlock;
    private boolean lastResult;
    private boolean checksBlockEntity;

    public AdventureModeCheck(String key) {
        this.tagName = key;
    }

    private static boolean areSameBlocks(ShapeDetectorBlock pos, @Nullable ShapeDetectorBlock cachedPos, boolean nbtAware) {
        if (cachedPos != null && pos.getState() == cachedPos.getState()) {
            if (!nbtAware) {
                return true;
            } else if (pos.getEntity() == null && cachedPos.getEntity() == null) {
                return true;
            } else {
                return pos.getEntity() != null && cachedPos.getEntity() != null ? Objects.equals(pos.getEntity().saveWithId(), cachedPos.getEntity().saveWithId()) : false;
            }
        } else {
            return false;
        }
    }

    public boolean test(ItemStack stack, ITagRegistry tagManager, ShapeDetectorBlock pos) {
        if (areSameBlocks(pos, this.lastCheckedBlock, this.checksBlockEntity)) {
            return this.lastResult;
        } else {
            this.lastCheckedBlock = pos;
            this.checksBlockEntity = false;
            NBTTagCompound compoundTag = stack.getTag();
            if (compoundTag != null && compoundTag.hasKeyOfType(this.tagName, 9)) {
                NBTTagList listTag = compoundTag.getList(this.tagName, 8);

                for(int i = 0; i < listTag.size(); ++i) {
                    String string = listTag.getString(i);

                    try {
                        ArgumentBlockPredicate.Result result = PREDICATE_PARSER.parse(new StringReader(string));
                        this.checksBlockEntity |= result.requiresNbt();
                        Predicate<ShapeDetectorBlock> predicate = result.create(tagManager);
                        if (predicate.test(pos)) {
                            this.lastResult = true;
                            return true;
                        }
                    } catch (CommandSyntaxException var10) {
                    }
                }
            }

            this.lastResult = false;
            return false;
        }
    }
}
