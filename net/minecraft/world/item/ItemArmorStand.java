package net.minecraft.world.item;

import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.Vector3f;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.EnumSoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumMobSpawn;
import net.minecraft.world.entity.decoration.EntityArmorStand;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.item.context.ItemActionContext;
import net.minecraft.world.level.World;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.Vec3D;

public class ItemArmorStand extends Item {
    public ItemArmorStand(Item.Info settings) {
        super(settings);
    }

    @Override
    public EnumInteractionResult useOn(ItemActionContext context) {
        EnumDirection direction = context.getClickedFace();
        if (direction == EnumDirection.DOWN) {
            return EnumInteractionResult.FAIL;
        } else {
            World level = context.getWorld();
            BlockActionContext blockPlaceContext = new BlockActionContext(context);
            BlockPosition blockPos = blockPlaceContext.getClickPosition();
            ItemStack itemStack = context.getItemStack();
            Vec3D vec3 = Vec3D.atBottomCenterOf(blockPos);
            AxisAlignedBB aABB = EntityTypes.ARMOR_STAND.getDimensions().makeBoundingBox(vec3.getX(), vec3.getY(), vec3.getZ());
            if (level.getCubes((Entity)null, aABB) && level.getEntities((Entity)null, aABB).isEmpty()) {
                if (level instanceof WorldServer) {
                    WorldServer serverLevel = (WorldServer)level;
                    EntityArmorStand armorStand = EntityTypes.ARMOR_STAND.createCreature(serverLevel, itemStack.getTag(), (IChatBaseComponent)null, context.getEntity(), blockPos, EnumMobSpawn.SPAWN_EGG, true, true);
                    if (armorStand == null) {
                        return EnumInteractionResult.FAIL;
                    }

                    float f = (float)MathHelper.floor((MathHelper.wrapDegrees(context.getRotation() - 180.0F) + 22.5F) / 45.0F) * 45.0F;
                    armorStand.setPositionRotation(armorStand.locX(), armorStand.locY(), armorStand.locZ(), f, 0.0F);
                    this.randomizePose(armorStand, level.random);
                    serverLevel.addAllEntities(armorStand);
                    level.playSound((EntityHuman)null, armorStand.locX(), armorStand.locY(), armorStand.locZ(), SoundEffects.ARMOR_STAND_PLACE, EnumSoundCategory.BLOCKS, 0.75F, 0.8F);
                    level.gameEvent(context.getEntity(), GameEvent.ENTITY_PLACE, armorStand);
                }

                itemStack.subtract(1);
                return EnumInteractionResult.sidedSuccess(level.isClientSide);
            } else {
                return EnumInteractionResult.FAIL;
            }
        }
    }

    private void randomizePose(EntityArmorStand stand, Random random) {
        Vector3f rotations = stand.getHeadPose();
        float f = random.nextFloat() * 5.0F;
        float g = random.nextFloat() * 20.0F - 10.0F;
        Vector3f rotations2 = new Vector3f(rotations.getX() + f, rotations.getY() + g, rotations.getZ());
        stand.setHeadPose(rotations2);
        rotations = stand.getBodyPose();
        f = random.nextFloat() * 10.0F - 5.0F;
        rotations2 = new Vector3f(rotations.getX(), rotations.getY() + f, rotations.getZ());
        stand.setBodyPose(rotations2);
    }
}
