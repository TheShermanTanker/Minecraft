package net.minecraft.world.level.block.entity;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MathHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.BlockSculkSensor;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.gameevent.BlockPositionSource;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.gameevent.vibrations.VibrationListener;

public class SculkSensorBlockEntity extends TileEntity implements VibrationListener.VibrationListenerConfig {
    private final VibrationListener listener;
    public int lastVibrationFrequency;

    public SculkSensorBlockEntity(BlockPosition pos, IBlockData state) {
        super(TileEntityTypes.SCULK_SENSOR, pos, state);
        this.listener = new VibrationListener(new BlockPositionSource(this.worldPosition), ((BlockSculkSensor)state.getBlock()).getListenerRange(), this);
    }

    @Override
    public void load(NBTTagCompound nbt) {
        super.load(nbt);
        this.lastVibrationFrequency = nbt.getInt("last_vibration_frequency");
    }

    @Override
    public NBTTagCompound save(NBTTagCompound nbt) {
        super.save(nbt);
        nbt.setInt("last_vibration_frequency", this.lastVibrationFrequency);
        return nbt;
    }

    public VibrationListener getListener() {
        return this.listener;
    }

    public int getLastVibrationFrequency() {
        return this.lastVibrationFrequency;
    }

    @Override
    public boolean shouldListen(World world, GameEventListener listener, BlockPosition pos, GameEvent event, @Nullable Entity entity) {
        boolean bl = event == GameEvent.BLOCK_DESTROY && pos.equals(this.getPosition());
        boolean bl2 = event == GameEvent.BLOCK_PLACE && pos.equals(this.getPosition());
        return !bl && !bl2 && BlockSculkSensor.canActivate(this.getBlock());
    }

    @Override
    public void onSignalReceive(World world, GameEventListener listener, GameEvent event, int distance) {
        IBlockData blockState = this.getBlock();
        if (!world.isClientSide() && BlockSculkSensor.canActivate(blockState)) {
            this.lastVibrationFrequency = BlockSculkSensor.VIBRATION_STRENGTH_FOR_EVENT.getInt(event);
            BlockSculkSensor.activate(world, this.worldPosition, blockState, getRedstoneStrengthForDistance(distance, listener.getListenerRadius()));
        }

    }

    public static int getRedstoneStrengthForDistance(int distance, int range) {
        double d = (double)distance / (double)range;
        return Math.max(1, 15 - MathHelper.floor(d * 15.0D));
    }
}
