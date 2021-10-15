package net.minecraft.world.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.SystemUtils;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistry;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.InteractionResultWrapper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.ai.attributes.AttributeBase;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.item.EntityItem;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.food.FoodInfo;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.context.ItemActionContext;
import net.minecraft.world.level.IMaterial;
import net.minecraft.world.level.RayTrace;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import net.minecraft.world.phys.Vec3D;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Item implements IMaterial {
    private static final Logger LOGGER = LogManager.getLogger();
    public static final Map<Block, Item> BY_BLOCK = Maps.newHashMap();
    protected static final UUID BASE_ATTACK_DAMAGE_UUID = UUID.fromString("CB3F55D3-645C-4F38-A497-9C13A33DB5CF");
    protected static final UUID BASE_ATTACK_SPEED_UUID = UUID.fromString("FA233E1C-4180-4865-B01B-BCCE9785ACA3");
    public static final int MAX_STACK_SIZE = 64;
    public static final int EAT_DURATION = 32;
    public static final int MAX_BAR_WIDTH = 13;
    protected final CreativeModeTab category;
    public final EnumItemRarity rarity;
    private final int maxStackSize;
    private final int maxDamage;
    private final boolean isFireResistant;
    private final Item craftingRemainingItem;
    @Nullable
    private String descriptionId;
    @Nullable
    private final FoodInfo foodProperties;

    public static int getId(Item item) {
        return item == null ? 0 : IRegistry.ITEM.getId(item);
    }

    public static Item getById(int id) {
        return IRegistry.ITEM.fromId(id);
    }

    @Deprecated
    public static Item getItemOf(Block block) {
        return BY_BLOCK.getOrDefault(block, Items.AIR);
    }

    public Item(Item.Info settings) {
        this.category = settings.category;
        this.rarity = settings.rarity;
        this.craftingRemainingItem = settings.craftingRemainingItem;
        this.maxDamage = settings.maxDamage;
        this.maxStackSize = settings.maxStackSize;
        this.foodProperties = settings.foodProperties;
        this.isFireResistant = settings.isFireResistant;
        if (SharedConstants.IS_RUNNING_IN_IDE) {
            String string = this.getClass().getSimpleName();
            if (!string.endsWith("Item")) {
                LOGGER.error("Item classes should end with Item and {} doesn't.", (Object)string);
            }
        }

    }

    public void onUseTick(World world, EntityLiving user, ItemStack stack, int remainingUseTicks) {
    }

    public void onDestroyed(EntityItem entity) {
    }

    public void verifyTagAfterLoad(NBTTagCompound nbt) {
    }

    public boolean canAttackBlock(IBlockData state, World world, BlockPosition pos, EntityHuman miner) {
        return true;
    }

    @Override
    public Item getItem() {
        return this;
    }

    public EnumInteractionResult useOn(ItemActionContext context) {
        return EnumInteractionResult.PASS;
    }

    public float getDestroySpeed(ItemStack stack, IBlockData state) {
        return 1.0F;
    }

    public InteractionResultWrapper<ItemStack> use(World world, EntityHuman user, EnumHand hand) {
        if (this.isFood()) {
            ItemStack itemStack = user.getItemInHand(hand);
            if (user.canEat(this.getFoodInfo().canAlwaysEat())) {
                user.startUsingItem(hand);
                return InteractionResultWrapper.consume(itemStack);
            } else {
                return InteractionResultWrapper.fail(itemStack);
            }
        } else {
            return InteractionResultWrapper.pass(user.getItemInHand(hand));
        }
    }

    public ItemStack finishUsingItem(ItemStack stack, World world, EntityLiving user) {
        return this.isFood() ? user.eat(world, stack) : stack;
    }

    public final int getMaxStackSize() {
        return this.maxStackSize;
    }

    public final int getMaxDurability() {
        return this.maxDamage;
    }

    public boolean usesDurability() {
        return this.maxDamage > 0;
    }

    public boolean isBarVisible(ItemStack stack) {
        return stack.isDamaged();
    }

    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F - (float)stack.getDamage() * 13.0F / (float)this.maxDamage);
    }

    public int getBarColor(ItemStack stack) {
        float f = Math.max(0.0F, ((float)this.maxDamage - (float)stack.getDamage()) / (float)this.maxDamage);
        return MathHelper.hsvToRgb(f / 3.0F, 1.0F, 1.0F);
    }

    public boolean overrideStackedOnOther(ItemStack stack, Slot slot, ClickAction clickType, EntityHuman player) {
        return false;
    }

    public boolean overrideOtherStackedOnMe(ItemStack stack, ItemStack otherStack, Slot slot, ClickAction clickType, EntityHuman player, SlotAccess cursorStackReference) {
        return false;
    }

    public boolean hurtEnemy(ItemStack stack, EntityLiving target, EntityLiving attacker) {
        return false;
    }

    public boolean mineBlock(ItemStack stack, World world, IBlockData state, BlockPosition pos, EntityLiving miner) {
        return false;
    }

    public boolean canDestroySpecialBlock(IBlockData state) {
        return false;
    }

    public EnumInteractionResult interactLivingEntity(ItemStack stack, EntityHuman user, EntityLiving entity, EnumHand hand) {
        return EnumInteractionResult.PASS;
    }

    public IChatBaseComponent getDescription() {
        return new ChatMessage(this.getName());
    }

    @Override
    public String toString() {
        return IRegistry.ITEM.getKey(this).getKey();
    }

    protected String getOrCreateDescriptionId() {
        if (this.descriptionId == null) {
            this.descriptionId = SystemUtils.makeDescriptionId("item", IRegistry.ITEM.getKey(this));
        }

        return this.descriptionId;
    }

    public String getName() {
        return this.getOrCreateDescriptionId();
    }

    public String getDescriptionId(ItemStack stack) {
        return this.getName();
    }

    public boolean shouldOverrideMultiplayerNbt() {
        return true;
    }

    @Nullable
    public final Item getCraftingRemainingItem() {
        return this.craftingRemainingItem;
    }

    public boolean hasCraftingRemainingItem() {
        return this.craftingRemainingItem != null;
    }

    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
    }

    public void onCraftedBy(ItemStack stack, World world, EntityHuman player) {
    }

    public boolean isComplex() {
        return false;
    }

    public EnumAnimation getUseAnimation(ItemStack stack) {
        return stack.getItem().isFood() ? EnumAnimation.EAT : EnumAnimation.NONE;
    }

    public int getUseDuration(ItemStack stack) {
        if (stack.getItem().isFood()) {
            return this.getFoodInfo().isFastFood() ? 16 : 32;
        } else {
            return 0;
        }
    }

    public void releaseUsing(ItemStack stack, World world, EntityLiving user, int remainingUseTicks) {
    }

    public void appendHoverText(ItemStack stack, @Nullable World world, List<IChatBaseComponent> tooltip, TooltipFlag context) {
    }

    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        return Optional.empty();
    }

    public IChatBaseComponent getName(ItemStack stack) {
        return new ChatMessage(this.getDescriptionId(stack));
    }

    public boolean isFoil(ItemStack stack) {
        return stack.hasEnchantments();
    }

    public EnumItemRarity getRarity(ItemStack stack) {
        if (!stack.hasEnchantments()) {
            return this.rarity;
        } else {
            switch(this.rarity) {
            case COMMON:
            case UNCOMMON:
                return EnumItemRarity.RARE;
            case RARE:
                return EnumItemRarity.EPIC;
            case EPIC:
            default:
                return this.rarity;
            }
        }
    }

    public boolean isEnchantable(ItemStack stack) {
        return this.getMaxStackSize() == 1 && this.usesDurability();
    }

    protected static MovingObjectPositionBlock getPlayerPOVHitResult(World world, EntityHuman player, RayTrace.FluidCollisionOption fluidHandling) {
        float f = player.getXRot();
        float g = player.getYRot();
        Vec3D vec3 = player.getEyePosition();
        float h = MathHelper.cos(-g * ((float)Math.PI / 180F) - (float)Math.PI);
        float i = MathHelper.sin(-g * ((float)Math.PI / 180F) - (float)Math.PI);
        float j = -MathHelper.cos(-f * ((float)Math.PI / 180F));
        float k = MathHelper.sin(-f * ((float)Math.PI / 180F));
        float l = i * j;
        float n = h * j;
        double d = 5.0D;
        Vec3D vec32 = vec3.add((double)l * 5.0D, (double)k * 5.0D, (double)n * 5.0D);
        return world.rayTrace(new RayTrace(vec3, vec32, RayTrace.BlockCollisionOption.OUTLINE, fluidHandling, player));
    }

    public int getEnchantmentValue() {
        return 0;
    }

    public void fillItemCategory(CreativeModeTab group, NonNullList<ItemStack> stacks) {
        if (this.allowdedIn(group)) {
            stacks.add(new ItemStack(this));
        }

    }

    protected boolean allowdedIn(CreativeModeTab group) {
        CreativeModeTab creativeModeTab = this.getItemCategory();
        return creativeModeTab != null && (group == CreativeModeTab.TAB_SEARCH || group == creativeModeTab);
    }

    @Nullable
    public final CreativeModeTab getItemCategory() {
        return this.category;
    }

    public boolean isValidRepairItem(ItemStack stack, ItemStack ingredient) {
        return false;
    }

    public Multimap<AttributeBase, AttributeModifier> getDefaultAttributeModifiers(EnumItemSlot slot) {
        return ImmutableMultimap.of();
    }

    public boolean useOnRelease(ItemStack stack) {
        return false;
    }

    public ItemStack createItemStack() {
        return new ItemStack(this);
    }

    public boolean isFood() {
        return this.foodProperties != null;
    }

    @Nullable
    public FoodInfo getFoodInfo() {
        return this.foodProperties;
    }

    public SoundEffect getDrinkingSound() {
        return SoundEffects.GENERIC_DRINK;
    }

    public SoundEffect getEatingSound() {
        return SoundEffects.GENERIC_EAT;
    }

    public boolean isFireResistant() {
        return this.isFireResistant;
    }

    public boolean canBeHurtBy(DamageSource source) {
        return !this.isFireResistant || !source.isFire();
    }

    @Nullable
    public SoundEffect getEquipSound() {
        return null;
    }

    public boolean canFitInsideContainerItems() {
        return true;
    }

    public static class Info {
        int maxStackSize = 64;
        int maxDamage;
        Item craftingRemainingItem;
        CreativeModeTab category;
        EnumItemRarity rarity = EnumItemRarity.COMMON;
        FoodInfo foodProperties;
        boolean isFireResistant;

        public Item.Info food(FoodInfo foodComponent) {
            this.foodProperties = foodComponent;
            return this;
        }

        public Item.Info stacksTo(int maxCount) {
            if (this.maxDamage > 0) {
                throw new RuntimeException("Unable to have damage AND stack.");
            } else {
                this.maxStackSize = maxCount;
                return this;
            }
        }

        public Item.Info defaultDurability(int maxDamage) {
            return this.maxDamage == 0 ? this.durability(maxDamage) : this;
        }

        public Item.Info durability(int maxDamage) {
            this.maxDamage = maxDamage;
            this.maxStackSize = 1;
            return this;
        }

        public Item.Info craftRemainder(Item recipeRemainder) {
            this.craftingRemainingItem = recipeRemainder;
            return this;
        }

        public Item.Info tab(CreativeModeTab group) {
            this.category = group;
            return this;
        }

        public Item.Info rarity(EnumItemRarity rarity) {
            this.rarity = rarity;
            return this;
        }

        public Item.Info fireResistant() {
            this.isFireResistant = true;
            return this;
        }
    }
}
