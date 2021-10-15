package net.minecraft.world.level.block.entity;

import javax.annotation.Nullable;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.protocol.game.PacketPlayOutTileEntityData;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.CommandBlockListenerAbstract;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BlockCommand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.phys.Vec2F;
import net.minecraft.world.phys.Vec3D;

public class TileEntityCommand extends TileEntity {
    private boolean powered;
    private boolean auto;
    private boolean conditionMet;
    private boolean sendToClient;
    private final CommandBlockListenerAbstract commandBlock = new CommandBlockListenerAbstract() {
        @Override
        public void setCommand(String command) {
            super.setCommand(command);
            TileEntityCommand.this.update();
        }

        @Override
        public WorldServer getLevel() {
            return (WorldServer)TileEntityCommand.this.level;
        }

        @Override
        public void onUpdated() {
            IBlockData blockState = TileEntityCommand.this.level.getType(TileEntityCommand.this.worldPosition);
            this.getLevel().notify(TileEntityCommand.this.worldPosition, blockState, blockState, 3);
        }

        @Override
        public Vec3D getPosition() {
            return Vec3D.atCenterOf(TileEntityCommand.this.worldPosition);
        }

        @Override
        public CommandListenerWrapper getWrapper() {
            return new CommandListenerWrapper(this, Vec3D.atCenterOf(TileEntityCommand.this.worldPosition), Vec2F.ZERO, this.getLevel(), 2, this.getName().getString(), this.getName(), this.getLevel().getMinecraftServer(), (Entity)null);
        }
    };

    public TileEntityCommand(BlockPosition pos, IBlockData state) {
        super(TileEntityTypes.COMMAND_BLOCK, pos, state);
    }

    @Override
    public NBTTagCompound save(NBTTagCompound nbt) {
        super.save(nbt);
        this.commandBlock.save(nbt);
        nbt.setBoolean("powered", this.isPowered());
        nbt.setBoolean("conditionMet", this.wasConditionMet());
        nbt.setBoolean("auto", this.isAutomatic());
        return nbt;
    }

    @Override
    public void load(NBTTagCompound nbt) {
        super.load(nbt);
        this.commandBlock.load(nbt);
        this.powered = nbt.getBoolean("powered");
        this.conditionMet = nbt.getBoolean("conditionMet");
        this.setAutomatic(nbt.getBoolean("auto"));
    }

    @Nullable
    @Override
    public PacketPlayOutTileEntityData getUpdatePacket() {
        if (this.isSendToClient()) {
            this.setSendToClient(false);
            NBTTagCompound compoundTag = this.save(new NBTTagCompound());
            return new PacketPlayOutTileEntityData(this.worldPosition, 2, compoundTag);
        } else {
            return null;
        }
    }

    @Override
    public boolean isFilteredNBT() {
        return true;
    }

    public CommandBlockListenerAbstract getCommandBlock() {
        return this.commandBlock;
    }

    public void setPowered(boolean powered) {
        this.powered = powered;
    }

    public boolean isPowered() {
        return this.powered;
    }

    public boolean isAutomatic() {
        return this.auto;
    }

    public void setAutomatic(boolean auto) {
        boolean bl = this.auto;
        this.auto = auto;
        if (!bl && auto && !this.powered && this.level != null && this.getMode() != TileEntityCommand.Type.SEQUENCE) {
            this.scheduleTick();
        }

    }

    public void onModeSwitch() {
        TileEntityCommand.Type mode = this.getMode();
        if (mode == TileEntityCommand.Type.AUTO && (this.powered || this.auto) && this.level != null) {
            this.scheduleTick();
        }

    }

    private void scheduleTick() {
        Block block = this.getBlock().getBlock();
        if (block instanceof BlockCommand) {
            this.markConditionMet();
            this.level.getBlockTickList().scheduleTick(this.worldPosition, block, 1);
        }

    }

    public boolean wasConditionMet() {
        return this.conditionMet;
    }

    public boolean markConditionMet() {
        this.conditionMet = true;
        if (this.isConditional()) {
            BlockPosition blockPos = this.worldPosition.relative(this.level.getType(this.worldPosition).get(BlockCommand.FACING).opposite());
            if (this.level.getType(blockPos).getBlock() instanceof BlockCommand) {
                TileEntity blockEntity = this.level.getTileEntity(blockPos);
                this.conditionMet = blockEntity instanceof TileEntityCommand && ((TileEntityCommand)blockEntity).getCommandBlock().getSuccessCount() > 0;
            } else {
                this.conditionMet = false;
            }
        }

        return this.conditionMet;
    }

    public boolean isSendToClient() {
        return this.sendToClient;
    }

    public void setSendToClient(boolean needsUpdatePacket) {
        this.sendToClient = needsUpdatePacket;
    }

    public TileEntityCommand.Type getMode() {
        IBlockData blockState = this.getBlock();
        if (blockState.is(Blocks.COMMAND_BLOCK)) {
            return TileEntityCommand.Type.REDSTONE;
        } else if (blockState.is(Blocks.REPEATING_COMMAND_BLOCK)) {
            return TileEntityCommand.Type.AUTO;
        } else {
            return blockState.is(Blocks.CHAIN_COMMAND_BLOCK) ? TileEntityCommand.Type.SEQUENCE : TileEntityCommand.Type.REDSTONE;
        }
    }

    public boolean isConditional() {
        IBlockData blockState = this.level.getType(this.getPosition());
        return blockState.getBlock() instanceof BlockCommand ? blockState.get(BlockCommand.CONDITIONAL) : false;
    }

    public static enum Type {
        SEQUENCE,
        AUTO,
        REDSTONE;
    }
}
