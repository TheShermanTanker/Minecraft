package net.minecraft.world.item;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.gson.JsonParseException;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.EnumChatFormat;
import net.minecraft.SystemUtils;
import net.minecraft.advancements.CriterionTriggers;
import net.minecraft.commands.arguments.blocks.ArgumentBlock;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistry;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.chat.ChatComponentText;
import net.minecraft.network.chat.ChatComponentUtils;
import net.minecraft.network.chat.ChatHoverable;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.ChatModifier;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.chat.IChatMutableComponent;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.stats.StatisticList;
import net.minecraft.tags.ITagRegistry;
import net.minecraft.tags.Tag;
import net.minecraft.tags.TagsBlock;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.InteractionResultWrapper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.entity.EnumMonsterType;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.ai.attributes.AttributeBase;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.entity.decoration.EntityItemFrame;
import net.minecraft.world.entity.item.EntityItem;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.context.ItemActionContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentDurability;
import net.minecraft.world.item.enchantment.EnchantmentManager;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.IMaterial;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.pattern.ShapeDetectorBlock;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class ItemStack {
    public static final Codec<ItemStack> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(IRegistry.ITEM.byNameCodec().fieldOf("id").forGetter((stack) -> {
            return stack.item;
        }), Codec.INT.fieldOf("Count").forGetter((stack) -> {
            return stack.count;
        }), NBTTagCompound.CODEC.optionalFieldOf("tag").forGetter((stack) -> {
            return Optional.ofNullable(stack.tag);
        })).apply(instance, ItemStack::new);
    });
    private static final Logger LOGGER = LogManager.getLogger();
    public static final ItemStack EMPTY = new ItemStack((Item)null);
    public static final DecimalFormat ATTRIBUTE_MODIFIER_FORMAT = SystemUtils.make(new DecimalFormat("#.##"), (decimalFormat) -> {
        decimalFormat.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.ROOT));
    });
    public static final String TAG_ENCH = "Enchantments";
    public static final String TAG_DISPLAY = "display";
    public static final String TAG_DISPLAY_NAME = "Name";
    public static final String TAG_LORE = "Lore";
    public static final String TAG_DAMAGE = "Damage";
    public static final String TAG_COLOR = "color";
    private static final String TAG_UNBREAKABLE = "Unbreakable";
    private static final String TAG_REPAIR_COST = "RepairCost";
    private static final String TAG_CAN_DESTROY_BLOCK_LIST = "CanDestroy";
    private static final String TAG_CAN_PLACE_ON_BLOCK_LIST = "CanPlaceOn";
    private static final String TAG_HIDE_FLAGS = "HideFlags";
    private static final int DONT_HIDE_TOOLTIP = 0;
    private static final ChatModifier LORE_STYLE = ChatModifier.EMPTY.setColor(EnumChatFormat.DARK_PURPLE).setItalic(true);
    private int count;
    private int popTime;
    /** @deprecated */
    @Deprecated
    private Item item;
    @Nullable
    public NBTTagCompound tag;
    private boolean emptyCacheFlag;
    @Nullable
    private Entity entityRepresentation;
    @Nullable
    private AdventureModeCheck adventureBreakCheck;
    @Nullable
    private AdventureModeCheck adventurePlaceCheck;

    public Optional<TooltipComponent> getTooltipImage() {
        return this.getItem().getTooltipImage(this);
    }

    public ItemStack(IMaterial item) {
        this(item, 1);
    }

    private ItemStack(IMaterial item, int count, Optional<NBTTagCompound> nbt) {
        this(item, count);
        nbt.ifPresent(this::setTag);
    }

    public ItemStack(IMaterial item, int count) {
        this.item = item == null ? null : item.getItem();
        this.count = count;
        if (this.item != null && this.item.usesDurability()) {
            this.setDamage(this.getDamage());
        }

        this.checkEmpty();
    }

    private void checkEmpty() {
        this.emptyCacheFlag = false;
        this.emptyCacheFlag = this.isEmpty();
    }

    private ItemStack(NBTTagCompound nbt) {
        this.item = IRegistry.ITEM.get(new MinecraftKey(nbt.getString("id")));
        this.count = nbt.getByte("Count");
        if (nbt.hasKeyOfType("tag", 10)) {
            this.tag = nbt.getCompound("tag");
            this.getItem().verifyTagAfterLoad(this.tag);
        }

        if (this.getItem().usesDurability()) {
            this.setDamage(this.getDamage());
        }

        this.checkEmpty();
    }

    public static ItemStack of(NBTTagCompound nbt) {
        try {
            return new ItemStack(nbt);
        } catch (RuntimeException var2) {
            LOGGER.debug("Tried to load invalid item: {}", nbt, var2);
            return EMPTY;
        }
    }

    public boolean isEmpty() {
        if (this == EMPTY) {
            return true;
        } else if (this.getItem() != null && !this.is(Items.AIR)) {
            return this.count <= 0;
        } else {
            return true;
        }
    }

    public ItemStack cloneAndSubtract(int amount) {
        int i = Math.min(amount, this.count);
        ItemStack itemStack = this.cloneItemStack();
        itemStack.setCount(i);
        this.subtract(i);
        return itemStack;
    }

    public Item getItem() {
        return this.emptyCacheFlag ? Items.AIR : this.item;
    }

    public boolean is(Tag<Item> tag) {
        return tag.isTagged(this.getItem());
    }

    public boolean is(Item item) {
        return this.getItem() == item;
    }

    public EnumInteractionResult placeItem(ItemActionContext context) {
        EntityHuman player = context.getEntity();
        BlockPosition blockPos = context.getClickPosition();
        ShapeDetectorBlock blockInWorld = new ShapeDetectorBlock(context.getWorld(), blockPos, false);
        if (player != null && !player.getAbilities().mayBuild && !this.hasAdventureModePlaceTagForBlock(context.getWorld().getTagManager(), blockInWorld)) {
            return EnumInteractionResult.PASS;
        } else {
            Item item = this.getItem();
            EnumInteractionResult interactionResult = item.useOn(context);
            if (player != null && interactionResult.shouldAwardStats()) {
                player.awardStat(StatisticList.ITEM_USED.get(item));
            }

            return interactionResult;
        }
    }

    public float getDestroySpeed(IBlockData state) {
        return this.getItem().getDestroySpeed(this, state);
    }

    public InteractionResultWrapper<ItemStack> use(World world, EntityHuman user, EnumHand hand) {
        return this.getItem().use(world, user, hand);
    }

    public ItemStack finishUsingItem(World world, EntityLiving user) {
        return this.getItem().finishUsingItem(this, world, user);
    }

    public NBTTagCompound save(NBTTagCompound nbt) {
        MinecraftKey resourceLocation = IRegistry.ITEM.getKey(this.getItem());
        nbt.setString("id", resourceLocation == null ? "minecraft:air" : resourceLocation.toString());
        nbt.setByte("Count", (byte)this.count);
        if (this.tag != null) {
            nbt.set("tag", this.tag.copy());
        }

        return nbt;
    }

    public int getMaxStackSize() {
        return this.getItem().getMaxStackSize();
    }

    public boolean isStackable() {
        return this.getMaxStackSize() > 1 && (!this.isDamageableItem() || !this.isDamaged());
    }

    public boolean isDamageableItem() {
        if (!this.emptyCacheFlag && this.getItem().getMaxDurability() > 0) {
            NBTTagCompound compoundTag = this.getTag();
            return compoundTag == null || !compoundTag.getBoolean("Unbreakable");
        } else {
            return false;
        }
    }

    public boolean isDamaged() {
        return this.isDamageableItem() && this.getDamage() > 0;
    }

    public int getDamage() {
        return this.tag == null ? 0 : this.tag.getInt("Damage");
    }

    public void setDamage(int damage) {
        this.getOrCreateTag().setInt("Damage", Math.max(0, damage));
    }

    public int getMaxDamage() {
        return this.getItem().getMaxDurability();
    }

    public boolean isDamaged(int amount, Random random, @Nullable EntityPlayer player) {
        if (!this.isDamageableItem()) {
            return false;
        } else {
            if (amount > 0) {
                int i = EnchantmentManager.getEnchantmentLevel(Enchantments.UNBREAKING, this);
                int j = 0;

                for(int k = 0; i > 0 && k < amount; ++k) {
                    if (EnchantmentDurability.shouldIgnoreDurabilityDrop(this, i, random)) {
                        ++j;
                    }
                }

                amount -= j;
                if (amount <= 0) {
                    return false;
                }
            }

            if (player != null && amount != 0) {
                CriterionTriggers.ITEM_DURABILITY_CHANGED.trigger(player, this, this.getDamage() + amount);
            }

            int l = this.getDamage() + amount;
            this.setDamage(l);
            return l >= this.getMaxDamage();
        }
    }

    public <T extends EntityLiving> void damage(int amount, T entity, Consumer<T> breakCallback) {
        if (!entity.level.isClientSide && (!(entity instanceof EntityHuman) || !((EntityHuman)entity).getAbilities().instabuild)) {
            if (this.isDamageableItem()) {
                if (this.isDamaged(amount, entity.getRandom(), entity instanceof EntityPlayer ? (EntityPlayer)entity : null)) {
                    breakCallback.accept(entity);
                    Item item = this.getItem();
                    this.subtract(1);
                    if (entity instanceof EntityHuman) {
                        ((EntityHuman)entity).awardStat(StatisticList.ITEM_BROKEN.get(item));
                    }

                    this.setDamage(0);
                }

            }
        }
    }

    public boolean isBarVisible() {
        return this.item.isBarVisible(this);
    }

    public int getBarWidth() {
        return this.item.getBarWidth(this);
    }

    public int getBarColor() {
        return this.item.getBarColor(this);
    }

    public boolean overrideStackedOnOther(Slot slot, ClickAction clickType, EntityHuman player) {
        return this.getItem().overrideStackedOnOther(this, slot, clickType, player);
    }

    public boolean overrideOtherStackedOnMe(ItemStack stack, Slot slot, ClickAction clickType, EntityHuman player, SlotAccess cursorStackReference) {
        return this.getItem().overrideOtherStackedOnMe(this, stack, slot, clickType, player, cursorStackReference);
    }

    public void hurtEnemy(EntityLiving target, EntityHuman attacker) {
        Item item = this.getItem();
        if (item.hurtEnemy(this, target, attacker)) {
            attacker.awardStat(StatisticList.ITEM_USED.get(item));
        }

    }

    public void mineBlock(World world, IBlockData state, BlockPosition pos, EntityHuman miner) {
        Item item = this.getItem();
        if (item.mineBlock(this, world, state, pos, miner)) {
            miner.awardStat(StatisticList.ITEM_USED.get(item));
        }

    }

    public boolean canDestroySpecialBlock(IBlockData state) {
        return this.getItem().canDestroySpecialBlock(state);
    }

    public EnumInteractionResult interactLivingEntity(EntityHuman user, EntityLiving entity, EnumHand hand) {
        return this.getItem().interactLivingEntity(this, user, entity, hand);
    }

    public ItemStack cloneItemStack() {
        if (this.isEmpty()) {
            return EMPTY;
        } else {
            ItemStack itemStack = new ItemStack(this.getItem(), this.count);
            itemStack.setPopTime(this.getPopTime());
            if (this.tag != null) {
                itemStack.tag = this.tag.copy();
            }

            return itemStack;
        }
    }

    public static boolean equals(ItemStack left, ItemStack right) {
        if (left.isEmpty() && right.isEmpty()) {
            return true;
        } else if (!left.isEmpty() && !right.isEmpty()) {
            if (left.tag == null && right.tag != null) {
                return false;
            } else {
                return left.tag == null || left.tag.equals(right.tag);
            }
        } else {
            return false;
        }
    }

    public static boolean matches(ItemStack left, ItemStack right) {
        if (left.isEmpty() && right.isEmpty()) {
            return true;
        } else {
            return !left.isEmpty() && !right.isEmpty() ? left.matches(right) : false;
        }
    }

    private boolean matches(ItemStack stack) {
        if (this.count != stack.count) {
            return false;
        } else if (!this.is(stack.getItem())) {
            return false;
        } else if (this.tag == null && stack.tag != null) {
            return false;
        } else {
            return this.tag == null || this.tag.equals(stack.tag);
        }
    }

    public static boolean isSame(ItemStack left, ItemStack right) {
        if (left == right) {
            return true;
        } else {
            return !left.isEmpty() && !right.isEmpty() ? left.doMaterialsMatch(right) : false;
        }
    }

    public static boolean isSameIgnoreDurability(ItemStack left, ItemStack right) {
        if (left == right) {
            return true;
        } else {
            return !left.isEmpty() && !right.isEmpty() ? left.sameItemStackIgnoreDurability(right) : false;
        }
    }

    public boolean doMaterialsMatch(ItemStack stack) {
        return !stack.isEmpty() && this.is(stack.getItem());
    }

    public boolean sameItemStackIgnoreDurability(ItemStack stack) {
        if (!this.isDamageableItem()) {
            return this.doMaterialsMatch(stack);
        } else {
            return !stack.isEmpty() && this.is(stack.getItem());
        }
    }

    public static boolean isSameItemSameTags(ItemStack stack, ItemStack otherStack) {
        return stack.is(otherStack.getItem()) && equals(stack, otherStack);
    }

    public String getDescriptionId() {
        return this.getItem().getDescriptionId(this);
    }

    @Override
    public String toString() {
        return this.count + " " + this.getItem();
    }

    public void inventoryTick(World world, Entity entity, int slot, boolean selected) {
        if (this.popTime > 0) {
            --this.popTime;
        }

        if (this.getItem() != null) {
            this.getItem().inventoryTick(this, world, entity, slot, selected);
        }

    }

    public void onCraftedBy(World world, EntityHuman player, int amount) {
        player.awardStat(StatisticList.ITEM_CRAFTED.get(this.getItem()), amount);
        this.getItem().onCraftedBy(this, world, player);
    }

    public int getUseDuration() {
        return this.getItem().getUseDuration(this);
    }

    public EnumAnimation getUseAnimation() {
        return this.getItem().getUseAnimation(this);
    }

    public void releaseUsing(World world, EntityLiving user, int remainingUseTicks) {
        this.getItem().releaseUsing(this, world, user, remainingUseTicks);
    }

    public boolean useOnRelease() {
        return this.getItem().useOnRelease(this);
    }

    public boolean hasTag() {
        return !this.emptyCacheFlag && this.tag != null && !this.tag.isEmpty();
    }

    @Nullable
    public NBTTagCompound getTag() {
        return this.tag;
    }

    public NBTTagCompound getOrCreateTag() {
        if (this.tag == null) {
            this.setTag(new NBTTagCompound());
        }

        return this.tag;
    }

    public NBTTagCompound getOrCreateTagElement(String key) {
        if (this.tag != null && this.tag.hasKeyOfType(key, 10)) {
            return this.tag.getCompound(key);
        } else {
            NBTTagCompound compoundTag = new NBTTagCompound();
            this.addTagElement(key, compoundTag);
            return compoundTag;
        }
    }

    @Nullable
    public NBTTagCompound getTagElement(String key) {
        return this.tag != null && this.tag.hasKeyOfType(key, 10) ? this.tag.getCompound(key) : null;
    }

    public void removeTag(String key) {
        if (this.tag != null && this.tag.hasKey(key)) {
            this.tag.remove(key);
            if (this.tag.isEmpty()) {
                this.tag = null;
            }
        }

    }

    public NBTTagList getEnchantments() {
        return this.tag != null ? this.tag.getList("Enchantments", 10) : new NBTTagList();
    }

    public void setTag(@Nullable NBTTagCompound nbt) {
        this.tag = nbt;
        if (this.getItem().usesDurability()) {
            this.setDamage(this.getDamage());
        }

        if (nbt != null) {
            this.getItem().verifyTagAfterLoad(nbt);
        }

    }

    public IChatBaseComponent getName() {
        NBTTagCompound compoundTag = this.getTagElement("display");
        if (compoundTag != null && compoundTag.hasKeyOfType("Name", 8)) {
            try {
                IChatBaseComponent component = IChatBaseComponent.ChatSerializer.fromJson(compoundTag.getString("Name"));
                if (component != null) {
                    return component;
                }

                compoundTag.remove("Name");
            } catch (JsonParseException var3) {
                compoundTag.remove("Name");
            }
        }

        return this.getItem().getName(this);
    }

    public ItemStack setHoverName(@Nullable IChatBaseComponent name) {
        NBTTagCompound compoundTag = this.getOrCreateTagElement("display");
        if (name != null) {
            compoundTag.setString("Name", IChatBaseComponent.ChatSerializer.toJson(name));
        } else {
            compoundTag.remove("Name");
        }

        return this;
    }

    public void resetHoverName() {
        NBTTagCompound compoundTag = this.getTagElement("display");
        if (compoundTag != null) {
            compoundTag.remove("Name");
            if (compoundTag.isEmpty()) {
                this.removeTag("display");
            }
        }

        if (this.tag != null && this.tag.isEmpty()) {
            this.tag = null;
        }

    }

    public boolean hasName() {
        NBTTagCompound compoundTag = this.getTagElement("display");
        return compoundTag != null && compoundTag.hasKeyOfType("Name", 8);
    }

    public List<IChatBaseComponent> getTooltipLines(@Nullable EntityHuman player, TooltipFlag context) {
        List<IChatBaseComponent> list = Lists.newArrayList();
        IChatMutableComponent mutableComponent = (new ChatComponentText("")).addSibling(this.getName()).withStyle(this.getRarity().color);
        if (this.hasName()) {
            mutableComponent.withStyle(EnumChatFormat.ITALIC);
        }

        list.add(mutableComponent);
        if (!context.isAdvanced() && !this.hasName() && this.is(Items.FILLED_MAP)) {
            Integer integer = ItemWorldMap.getMapId(this);
            if (integer != null) {
                list.add((new ChatComponentText("#" + integer)).withStyle(EnumChatFormat.GRAY));
            }
        }

        int i = this.getHideFlags();
        if (shouldShowInTooltip(i, ItemStack.HideFlags.ADDITIONAL)) {
            this.getItem().appendHoverText(this, player == null ? null : player.level, list, context);
        }

        if (this.hasTag()) {
            if (shouldShowInTooltip(i, ItemStack.HideFlags.ENCHANTMENTS)) {
                appendEnchantmentNames(list, this.getEnchantments());
            }

            if (this.tag.hasKeyOfType("display", 10)) {
                NBTTagCompound compoundTag = this.tag.getCompound("display");
                if (shouldShowInTooltip(i, ItemStack.HideFlags.DYE) && compoundTag.hasKeyOfType("color", 99)) {
                    if (context.isAdvanced()) {
                        list.add((new ChatMessage("item.color", String.format("#%06X", compoundTag.getInt("color")))).withStyle(EnumChatFormat.GRAY));
                    } else {
                        list.add((new ChatMessage("item.dyed")).withStyle(new EnumChatFormat[]{EnumChatFormat.GRAY, EnumChatFormat.ITALIC}));
                    }
                }

                if (compoundTag.getTagType("Lore") == 9) {
                    NBTTagList listTag = compoundTag.getList("Lore", 8);

                    for(int j = 0; j < listTag.size(); ++j) {
                        String string = listTag.getString(j);

                        try {
                            IChatMutableComponent mutableComponent2 = IChatBaseComponent.ChatSerializer.fromJson(string);
                            if (mutableComponent2 != null) {
                                list.add(ChatComponentUtils.mergeStyles(mutableComponent2, LORE_STYLE));
                            }
                        } catch (JsonParseException var19) {
                            compoundTag.remove("Lore");
                        }
                    }
                }
            }
        }

        if (shouldShowInTooltip(i, ItemStack.HideFlags.MODIFIERS)) {
            for(EnumItemSlot equipmentSlot : EnumItemSlot.values()) {
                Multimap<AttributeBase, AttributeModifier> multimap = this.getAttributeModifiers(equipmentSlot);
                if (!multimap.isEmpty()) {
                    list.add(ChatComponentText.EMPTY);
                    list.add((new ChatMessage("item.modifiers." + equipmentSlot.getSlotName())).withStyle(EnumChatFormat.GRAY));

                    for(Entry<AttributeBase, AttributeModifier> entry : multimap.entries()) {
                        AttributeModifier attributeModifier = entry.getValue();
                        double d = attributeModifier.getAmount();
                        boolean bl = false;
                        if (player != null) {
                            if (attributeModifier.getUniqueId() == Item.BASE_ATTACK_DAMAGE_UUID) {
                                d = d + player.getAttributeBaseValue(GenericAttributes.ATTACK_DAMAGE);
                                d = d + (double)EnchantmentManager.getDamageBonus(this, EnumMonsterType.UNDEFINED);
                                bl = true;
                            } else if (attributeModifier.getUniqueId() == Item.BASE_ATTACK_SPEED_UUID) {
                                d += player.getAttributeBaseValue(GenericAttributes.ATTACK_SPEED);
                                bl = true;
                            }
                        }

                        double f;
                        if (attributeModifier.getOperation() != AttributeModifier.Operation.MULTIPLY_BASE && attributeModifier.getOperation() != AttributeModifier.Operation.MULTIPLY_TOTAL) {
                            if (entry.getKey().equals(GenericAttributes.KNOCKBACK_RESISTANCE)) {
                                f = d * 10.0D;
                            } else {
                                f = d;
                            }
                        } else {
                            f = d * 100.0D;
                        }

                        if (bl) {
                            list.add((new ChatComponentText(" ")).addSibling(new ChatMessage("attribute.modifier.equals." + attributeModifier.getOperation().toValue(), ATTRIBUTE_MODIFIER_FORMAT.format(f), new ChatMessage(entry.getKey().getName()))).withStyle(EnumChatFormat.DARK_GREEN));
                        } else if (d > 0.0D) {
                            list.add((new ChatMessage("attribute.modifier.plus." + attributeModifier.getOperation().toValue(), ATTRIBUTE_MODIFIER_FORMAT.format(f), new ChatMessage(entry.getKey().getName()))).withStyle(EnumChatFormat.BLUE));
                        } else if (d < 0.0D) {
                            f = f * -1.0D;
                            list.add((new ChatMessage("attribute.modifier.take." + attributeModifier.getOperation().toValue(), ATTRIBUTE_MODIFIER_FORMAT.format(f), new ChatMessage(entry.getKey().getName()))).withStyle(EnumChatFormat.RED));
                        }
                    }
                }
            }
        }

        if (this.hasTag()) {
            if (shouldShowInTooltip(i, ItemStack.HideFlags.UNBREAKABLE) && this.tag.getBoolean("Unbreakable")) {
                list.add((new ChatMessage("item.unbreakable")).withStyle(EnumChatFormat.BLUE));
            }

            if (shouldShowInTooltip(i, ItemStack.HideFlags.CAN_DESTROY) && this.tag.hasKeyOfType("CanDestroy", 9)) {
                NBTTagList listTag2 = this.tag.getList("CanDestroy", 8);
                if (!listTag2.isEmpty()) {
                    list.add(ChatComponentText.EMPTY);
                    list.add((new ChatMessage("item.canBreak")).withStyle(EnumChatFormat.GRAY));

                    for(int k = 0; k < listTag2.size(); ++k) {
                        list.addAll(expandBlockState(listTag2.getString(k)));
                    }
                }
            }

            if (shouldShowInTooltip(i, ItemStack.HideFlags.CAN_PLACE) && this.tag.hasKeyOfType("CanPlaceOn", 9)) {
                NBTTagList listTag3 = this.tag.getList("CanPlaceOn", 8);
                if (!listTag3.isEmpty()) {
                    list.add(ChatComponentText.EMPTY);
                    list.add((new ChatMessage("item.canPlace")).withStyle(EnumChatFormat.GRAY));

                    for(int l = 0; l < listTag3.size(); ++l) {
                        list.addAll(expandBlockState(listTag3.getString(l)));
                    }
                }
            }
        }

        if (context.isAdvanced()) {
            if (this.isDamaged()) {
                list.add(new ChatMessage("item.durability", this.getMaxDamage() - this.getDamage(), this.getMaxDamage()));
            }

            list.add((new ChatComponentText(IRegistry.ITEM.getKey(this.getItem()).toString())).withStyle(EnumChatFormat.DARK_GRAY));
            if (this.hasTag()) {
                list.add((new ChatMessage("item.nbt_tags", this.tag.getKeys().size())).withStyle(EnumChatFormat.DARK_GRAY));
            }
        }

        return list;
    }

    private static boolean shouldShowInTooltip(int flags, ItemStack.HideFlags tooltipSection) {
        return (flags & tooltipSection.getMask()) == 0;
    }

    private int getHideFlags() {
        return this.hasTag() && this.tag.hasKeyOfType("HideFlags", 99) ? this.tag.getInt("HideFlags") : 0;
    }

    public void hideTooltipPart(ItemStack.HideFlags tooltipSection) {
        NBTTagCompound compoundTag = this.getOrCreateTag();
        compoundTag.setInt("HideFlags", compoundTag.getInt("HideFlags") | tooltipSection.getMask());
    }

    public static void appendEnchantmentNames(List<IChatBaseComponent> tooltip, NBTTagList enchantments) {
        for(int i = 0; i < enchantments.size(); ++i) {
            NBTTagCompound compoundTag = enchantments.getCompound(i);
            IRegistry.ENCHANTMENT.getOptional(EnchantmentManager.getEnchantmentId(compoundTag)).ifPresent((e) -> {
                tooltip.add(e.getFullname(EnchantmentManager.getEnchantmentLevel(compoundTag)));
            });
        }

    }

    private static Collection<IChatBaseComponent> expandBlockState(String tag) {
        try {
            ArgumentBlock blockStateParser = (new ArgumentBlock(new StringReader(tag), true)).parse(true);
            IBlockData blockState = blockStateParser.getBlockData();
            MinecraftKey resourceLocation = blockStateParser.getTag();
            boolean bl = blockState != null;
            boolean bl2 = resourceLocation != null;
            if (bl || bl2) {
                if (bl) {
                    return Lists.newArrayList(blockState.getBlock().getName().withStyle(EnumChatFormat.DARK_GRAY));
                }

                Tag<Block> tag2 = TagsBlock.getAllTags().getTag(resourceLocation);
                if (tag2 != null) {
                    Collection<Block> collection = tag2.getTagged();
                    if (!collection.isEmpty()) {
                        return collection.stream().map(Block::getName).map((text) -> {
                            return text.withStyle(EnumChatFormat.DARK_GRAY);
                        }).collect(Collectors.toList());
                    }
                }
            }
        } catch (CommandSyntaxException var8) {
        }

        return Lists.newArrayList((new ChatComponentText("missingno")).withStyle(EnumChatFormat.DARK_GRAY));
    }

    public boolean hasFoil() {
        return this.getItem().isFoil(this);
    }

    public EnumItemRarity getRarity() {
        return this.getItem().getRarity(this);
    }

    public boolean canEnchant() {
        if (!this.getItem().isEnchantable(this)) {
            return false;
        } else {
            return !this.hasEnchantments();
        }
    }

    public void addEnchantment(Enchantment enchantment, int level) {
        this.getOrCreateTag();
        if (!this.tag.hasKeyOfType("Enchantments", 9)) {
            this.tag.set("Enchantments", new NBTTagList());
        }

        NBTTagList listTag = this.tag.getList("Enchantments", 10);
        listTag.add(EnchantmentManager.storeEnchantment(EnchantmentManager.getEnchantmentId(enchantment), (byte)level));
    }

    public boolean hasEnchantments() {
        if (this.tag != null && this.tag.hasKeyOfType("Enchantments", 9)) {
            return !this.tag.getList("Enchantments", 10).isEmpty();
        } else {
            return false;
        }
    }

    public void addTagElement(String key, NBTBase element) {
        this.getOrCreateTag().set(key, element);
    }

    public boolean isFramed() {
        return this.entityRepresentation instanceof EntityItemFrame;
    }

    public void setEntityRepresentation(@Nullable Entity holder) {
        this.entityRepresentation = holder;
    }

    @Nullable
    public EntityItemFrame getFrame() {
        return this.entityRepresentation instanceof EntityItemFrame ? (EntityItemFrame)this.getEntityRepresentation() : null;
    }

    @Nullable
    public Entity getEntityRepresentation() {
        return !this.emptyCacheFlag ? this.entityRepresentation : null;
    }

    public int getRepairCost() {
        return this.hasTag() && this.tag.hasKeyOfType("RepairCost", 3) ? this.tag.getInt("RepairCost") : 0;
    }

    public void setRepairCost(int repairCost) {
        this.getOrCreateTag().setInt("RepairCost", repairCost);
    }

    public Multimap<AttributeBase, AttributeModifier> getAttributeModifiers(EnumItemSlot slot) {
        Multimap<AttributeBase, AttributeModifier> multimap;
        if (this.hasTag() && this.tag.hasKeyOfType("AttributeModifiers", 9)) {
            multimap = HashMultimap.create();
            NBTTagList listTag = this.tag.getList("AttributeModifiers", 10);

            for(int i = 0; i < listTag.size(); ++i) {
                NBTTagCompound compoundTag = listTag.getCompound(i);
                if (!compoundTag.hasKeyOfType("Slot", 8) || compoundTag.getString("Slot").equals(slot.getSlotName())) {
                    Optional<AttributeBase> optional = IRegistry.ATTRIBUTE.getOptional(MinecraftKey.tryParse(compoundTag.getString("AttributeName")));
                    if (optional.isPresent()) {
                        AttributeModifier attributeModifier = AttributeModifier.load(compoundTag);
                        if (attributeModifier != null && attributeModifier.getUniqueId().getLeastSignificantBits() != 0L && attributeModifier.getUniqueId().getMostSignificantBits() != 0L) {
                            multimap.put(optional.get(), attributeModifier);
                        }
                    }
                }
            }
        } else {
            multimap = this.getItem().getDefaultAttributeModifiers(slot);
        }

        return multimap;
    }

    public void addAttributeModifier(AttributeBase attribute, AttributeModifier modifier, @Nullable EnumItemSlot slot) {
        this.getOrCreateTag();
        if (!this.tag.hasKeyOfType("AttributeModifiers", 9)) {
            this.tag.set("AttributeModifiers", new NBTTagList());
        }

        NBTTagList listTag = this.tag.getList("AttributeModifiers", 10);
        NBTTagCompound compoundTag = modifier.save();
        compoundTag.setString("AttributeName", IRegistry.ATTRIBUTE.getKey(attribute).toString());
        if (slot != null) {
            compoundTag.setString("Slot", slot.getSlotName());
        }

        listTag.add(compoundTag);
    }

    public IChatBaseComponent getDisplayName() {
        IChatMutableComponent mutableComponent = (new ChatComponentText("")).addSibling(this.getName());
        if (this.hasName()) {
            mutableComponent.withStyle(EnumChatFormat.ITALIC);
        }

        IChatMutableComponent mutableComponent2 = ChatComponentUtils.wrapInSquareBrackets(mutableComponent);
        if (!this.emptyCacheFlag) {
            mutableComponent2.withStyle(this.getRarity().color).format((style) -> {
                return style.setChatHoverable(new ChatHoverable(ChatHoverable.EnumHoverAction.SHOW_ITEM, new ChatHoverable.ItemStackInfo(this)));
            });
        }

        return mutableComponent2;
    }

    public boolean hasAdventureModePlaceTagForBlock(ITagRegistry tagManager, ShapeDetectorBlock pos) {
        if (this.adventurePlaceCheck == null) {
            this.adventurePlaceCheck = new AdventureModeCheck("CanPlaceOn");
        }

        return this.adventurePlaceCheck.test(this, tagManager, pos);
    }

    public boolean hasAdventureModeBreakTagForBlock(ITagRegistry tagManager, ShapeDetectorBlock pos) {
        if (this.adventureBreakCheck == null) {
            this.adventureBreakCheck = new AdventureModeCheck("CanDestroy");
        }

        return this.adventureBreakCheck.test(this, tagManager, pos);
    }

    public int getPopTime() {
        return this.popTime;
    }

    public void setPopTime(int bobbingAnimationTime) {
        this.popTime = bobbingAnimationTime;
    }

    public int getCount() {
        return this.emptyCacheFlag ? 0 : this.count;
    }

    public void setCount(int count) {
        this.count = count;
        this.checkEmpty();
    }

    public void add(int amount) {
        this.setCount(this.count + amount);
    }

    public void subtract(int amount) {
        this.add(-amount);
    }

    public void onUseTick(World world, EntityLiving user, int remainingUseTicks) {
        this.getItem().onUseTick(world, user, this, remainingUseTicks);
    }

    public void onDestroyed(EntityItem entity) {
        this.getItem().onDestroyed(entity);
    }

    public boolean isEdible() {
        return this.getItem().isFood();
    }

    public SoundEffect getDrinkingSound() {
        return this.getItem().getDrinkingSound();
    }

    public SoundEffect getEatingSound() {
        return this.getItem().getEatingSound();
    }

    @Nullable
    public SoundEffect getEquipSound() {
        return this.getItem().getEquipSound();
    }

    public static enum HideFlags {
        ENCHANTMENTS,
        MODIFIERS,
        UNBREAKABLE,
        CAN_DESTROY,
        CAN_PLACE,
        ADDITIONAL,
        DYE;

        private final int mask = 1 << this.ordinal();

        public int getMask() {
            return this.mask;
        }
    }
}
