package net.minecraft.world.item;

import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.EnumChatFormat;
import net.minecraft.core.EnumDirection;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.chat.ChatComponentText;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.stats.StatisticList;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.InteractionResultWrapper;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.projectile.EntityFireworks;
import net.minecraft.world.item.context.ItemActionContext;
import net.minecraft.world.level.World;
import net.minecraft.world.phys.Vec3D;

public class ItemFireworks extends Item {
    public static final String TAG_FIREWORKS = "Fireworks";
    public static final String TAG_EXPLOSION = "Explosion";
    public static final String TAG_EXPLOSIONS = "Explosions";
    public static final String TAG_FLIGHT = "Flight";
    public static final String TAG_EXPLOSION_TYPE = "Type";
    public static final String TAG_EXPLOSION_TRAIL = "Trail";
    public static final String TAG_EXPLOSION_FLICKER = "Flicker";
    public static final String TAG_EXPLOSION_COLORS = "Colors";
    public static final String TAG_EXPLOSION_FADECOLORS = "FadeColors";
    public static final double ROCKET_PLACEMENT_OFFSET = 0.15D;

    public ItemFireworks(Item.Info settings) {
        super(settings);
    }

    @Override
    public EnumInteractionResult useOn(ItemActionContext context) {
        World level = context.getWorld();
        if (!level.isClientSide) {
            ItemStack itemStack = context.getItemStack();
            Vec3D vec3 = context.getPos();
            EnumDirection direction = context.getClickedFace();
            EntityFireworks fireworkRocketEntity = new EntityFireworks(level, context.getEntity(), vec3.x + (double)direction.getAdjacentX() * 0.15D, vec3.y + (double)direction.getAdjacentY() * 0.15D, vec3.z + (double)direction.getAdjacentZ() * 0.15D, itemStack);
            level.addEntity(fireworkRocketEntity);
            itemStack.subtract(1);
        }

        return EnumInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public InteractionResultWrapper<ItemStack> use(World world, EntityHuman user, EnumHand hand) {
        if (user.isGliding()) {
            ItemStack itemStack = user.getItemInHand(hand);
            if (!world.isClientSide) {
                EntityFireworks fireworkRocketEntity = new EntityFireworks(world, itemStack, user);
                world.addEntity(fireworkRocketEntity);
                if (!user.getAbilities().instabuild) {
                    itemStack.subtract(1);
                }

                user.awardStat(StatisticList.ITEM_USED.get(this));
            }

            return InteractionResultWrapper.sidedSuccess(user.getItemInHand(hand), world.isClientSide());
        } else {
            return InteractionResultWrapper.pass(user.getItemInHand(hand));
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable World world, List<IChatBaseComponent> tooltip, TooltipFlag context) {
        NBTTagCompound compoundTag = stack.getTagElement("Fireworks");
        if (compoundTag != null) {
            if (compoundTag.hasKeyOfType("Flight", 99)) {
                tooltip.add((new ChatMessage("item.minecraft.firework_rocket.flight")).append(" ").append(String.valueOf((int)compoundTag.getByte("Flight"))).withStyle(EnumChatFormat.GRAY));
            }

            NBTTagList listTag = compoundTag.getList("Explosions", 10);
            if (!listTag.isEmpty()) {
                for(int i = 0; i < listTag.size(); ++i) {
                    NBTTagCompound compoundTag2 = listTag.getCompound(i);
                    List<IChatBaseComponent> list = Lists.newArrayList();
                    ItemFireworksCharge.appendHoverText(compoundTag2, list);
                    if (!list.isEmpty()) {
                        for(int j = 1; j < list.size(); ++j) {
                            list.set(j, (new ChatComponentText("  ")).addSibling(list.get(j)).withStyle(EnumChatFormat.GRAY));
                        }

                        tooltip.addAll(list);
                    }
                }
            }

        }
    }

    @Override
    public ItemStack createItemStack() {
        ItemStack itemStack = new ItemStack(this);
        itemStack.getOrCreateTag().setByte("Flight", (byte)1);
        return itemStack;
    }

    public static enum EffectType {
        SMALL_BALL(0, "small_ball"),
        LARGE_BALL(1, "large_ball"),
        STAR(2, "star"),
        CREEPER(3, "creeper"),
        BURST(4, "burst");

        private static final ItemFireworks.EffectType[] BY_ID = Arrays.stream(values()).sorted(Comparator.comparingInt((type) -> {
            return type.id;
        })).toArray((i) -> {
            return new ItemFireworks.EffectType[i];
        });
        private final int id;
        private final String name;

        private EffectType(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public int getId() {
            return this.id;
        }

        public String getName() {
            return this.name;
        }

        public static ItemFireworks.EffectType byId(int id) {
            return id >= 0 && id < BY_ID.length ? BY_ID[id] : SMALL_BALL;
        }
    }
}
