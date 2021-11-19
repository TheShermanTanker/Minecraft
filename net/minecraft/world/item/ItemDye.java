package net.minecraft.world.item;

import com.google.common.collect.Maps;
import java.util.Map;
import net.minecraft.sounds.EnumSoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.animal.EntitySheep;
import net.minecraft.world.entity.player.EntityHuman;

public class ItemDye extends Item {
    private static final Map<EnumColor, ItemDye> ITEM_BY_COLOR = Maps.newEnumMap(EnumColor.class);
    private final EnumColor dyeColor;

    public ItemDye(EnumColor color, Item.Info settings) {
        super(settings);
        this.dyeColor = color;
        ITEM_BY_COLOR.put(color, this);
    }

    @Override
    public EnumInteractionResult interactLivingEntity(ItemStack stack, EntityHuman user, EntityLiving entity, EnumHand hand) {
        if (entity instanceof EntitySheep) {
            EntitySheep sheep = (EntitySheep)entity;
            if (sheep.isAlive() && !sheep.isSheared() && sheep.getColor() != this.dyeColor) {
                sheep.level.playSound(user, sheep, SoundEffects.DYE_USE, EnumSoundCategory.PLAYERS, 1.0F, 1.0F);
                if (!user.level.isClientSide) {
                    sheep.setColor(this.dyeColor);
                    stack.subtract(1);
                }

                return EnumInteractionResult.sidedSuccess(user.level.isClientSide);
            }
        }

        return EnumInteractionResult.PASS;
    }

    public EnumColor getDyeColor() {
        return this.dyeColor;
    }

    public static ItemDye byColor(EnumColor color) {
        return ITEM_BY_COLOR.get(color);
    }
}
