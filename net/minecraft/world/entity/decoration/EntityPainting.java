package net.minecraft.world.entity.decoration;

import com.google.common.collect.Lists;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.IRegistry;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.PacketPlayOutSpawnEntityPainting;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.World;

public class EntityPainting extends EntityHanging {
    public Paintings motive;

    public EntityPainting(EntityTypes<? extends EntityPainting> type, World world) {
        super(type, world);
    }

    public EntityPainting(World world, BlockPosition pos, EnumDirection direction) {
        super(EntityTypes.PAINTING, world, pos);
        List<Paintings> list = Lists.newArrayList();
        int i = 0;

        for(Paintings motive : IRegistry.MOTIVE) {
            this.motive = motive;
            this.setDirection(direction);
            if (this.survives()) {
                list.add(motive);
                int j = motive.getWidth() * motive.getHeight();
                if (j > i) {
                    i = j;
                }
            }
        }

        if (!list.isEmpty()) {
            Iterator<Paintings> iterator = list.iterator();

            while(iterator.hasNext()) {
                Paintings motive2 = iterator.next();
                if (motive2.getWidth() * motive2.getHeight() < i) {
                    iterator.remove();
                }
            }

            this.motive = list.get(this.random.nextInt(list.size()));
        }

        this.setDirection(direction);
    }

    public EntityPainting(World world, BlockPosition pos, EnumDirection direction, Paintings motive) {
        this(world, pos, direction);
        this.motive = motive;
        this.setDirection(direction);
    }

    @Override
    public void saveData(NBTTagCompound nbt) {
        nbt.setString("Motive", IRegistry.MOTIVE.getKey(this.motive).toString());
        nbt.setByte("Facing", (byte)this.direction.get2DRotationValue());
        super.saveData(nbt);
    }

    @Override
    public void loadData(NBTTagCompound nbt) {
        this.motive = IRegistry.MOTIVE.get(MinecraftKey.tryParse(nbt.getString("Motive")));
        this.direction = EnumDirection.fromType2(nbt.getByte("Facing"));
        super.loadData(nbt);
        this.setDirection(this.direction);
    }

    @Override
    public int getHangingWidth() {
        return this.motive == null ? 1 : this.motive.getWidth();
    }

    @Override
    public int getHangingHeight() {
        return this.motive == null ? 1 : this.motive.getHeight();
    }

    @Override
    public void dropItem(@Nullable Entity entity) {
        if (this.level.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
            this.playSound(SoundEffects.PAINTING_BREAK, 1.0F, 1.0F);
            if (entity instanceof EntityHuman) {
                EntityHuman player = (EntityHuman)entity;
                if (player.getAbilities().instabuild) {
                    return;
                }
            }

            this.spawnAtLocation(Items.PAINTING);
        }
    }

    @Override
    public void playPlaceSound() {
        this.playSound(SoundEffects.PAINTING_PLACE, 1.0F, 1.0F);
    }

    @Override
    public void setPositionRotation(double x, double y, double z, float yaw, float pitch) {
        this.setPosition(x, y, z);
    }

    @Override
    public void lerpTo(double x, double y, double z, float yaw, float pitch, int interpolationSteps, boolean interpolate) {
        BlockPosition blockPos = this.pos.offset(x - this.locX(), y - this.locY(), z - this.locZ());
        this.setPosition((double)blockPos.getX(), (double)blockPos.getY(), (double)blockPos.getZ());
    }

    @Override
    public Packet<?> getPacket() {
        return new PacketPlayOutSpawnEntityPainting(this);
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(Items.PAINTING);
    }
}
