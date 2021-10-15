package net.minecraft.world.entity.vehicle;

import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.ChatComponentText;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.CommandBlockListenerAbstract;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.phys.Vec3D;

public class EntityMinecartCommandBlock extends EntityMinecartAbstract {
    public static final DataWatcherObject<String> DATA_ID_COMMAND_NAME = DataWatcher.defineId(EntityMinecartCommandBlock.class, DataWatcherRegistry.STRING);
    static final DataWatcherObject<IChatBaseComponent> DATA_ID_LAST_OUTPUT = DataWatcher.defineId(EntityMinecartCommandBlock.class, DataWatcherRegistry.COMPONENT);
    private final CommandBlockListenerAbstract commandBlock = new EntityMinecartCommandBlock.MinecartCommandBase();
    private static final int ACTIVATION_DELAY = 4;
    private int lastActivated;

    public EntityMinecartCommandBlock(EntityTypes<? extends EntityMinecartCommandBlock> type, World world) {
        super(type, world);
    }

    public EntityMinecartCommandBlock(World world, double x, double y, double z) {
        super(EntityTypes.COMMAND_BLOCK_MINECART, world, x, y, z);
    }

    @Override
    protected void initDatawatcher() {
        super.initDatawatcher();
        this.getDataWatcher().register(DATA_ID_COMMAND_NAME, "");
        this.getDataWatcher().register(DATA_ID_LAST_OUTPUT, ChatComponentText.EMPTY);
    }

    @Override
    protected void loadData(NBTTagCompound nbt) {
        super.loadData(nbt);
        this.commandBlock.load(nbt);
        this.getDataWatcher().set(DATA_ID_COMMAND_NAME, this.getCommandBlock().getCommand());
        this.getDataWatcher().set(DATA_ID_LAST_OUTPUT, this.getCommandBlock().getLastOutput());
    }

    @Override
    protected void saveData(NBTTagCompound nbt) {
        super.saveData(nbt);
        this.commandBlock.save(nbt);
    }

    @Override
    public EntityMinecartAbstract.EnumMinecartType getMinecartType() {
        return EntityMinecartAbstract.EnumMinecartType.COMMAND_BLOCK;
    }

    @Override
    public IBlockData getDefaultDisplayBlockState() {
        return Blocks.COMMAND_BLOCK.getBlockData();
    }

    public CommandBlockListenerAbstract getCommandBlock() {
        return this.commandBlock;
    }

    @Override
    public void activateMinecart(int x, int y, int z, boolean powered) {
        if (powered && this.tickCount - this.lastActivated >= 4) {
            this.getCommandBlock().performCommand(this.level);
            this.lastActivated = this.tickCount;
        }

    }

    @Override
    public EnumInteractionResult interact(EntityHuman player, EnumHand hand) {
        return this.commandBlock.usedBy(player);
    }

    @Override
    public void onSyncedDataUpdated(DataWatcherObject<?> data) {
        super.onSyncedDataUpdated(data);
        if (DATA_ID_LAST_OUTPUT.equals(data)) {
            try {
                this.commandBlock.setLastOutput(this.getDataWatcher().get(DATA_ID_LAST_OUTPUT));
            } catch (Throwable var3) {
            }
        } else if (DATA_ID_COMMAND_NAME.equals(data)) {
            this.commandBlock.setCommand(this.getDataWatcher().get(DATA_ID_COMMAND_NAME));
        }

    }

    @Override
    public boolean onlyOpCanSetNbt() {
        return true;
    }

    public class MinecartCommandBase extends CommandBlockListenerAbstract {
        @Override
        public WorldServer getLevel() {
            return (WorldServer)EntityMinecartCommandBlock.this.level;
        }

        @Override
        public void onUpdated() {
            EntityMinecartCommandBlock.this.getDataWatcher().set(EntityMinecartCommandBlock.DATA_ID_COMMAND_NAME, this.getCommand());
            EntityMinecartCommandBlock.this.getDataWatcher().set(EntityMinecartCommandBlock.DATA_ID_LAST_OUTPUT, this.getLastOutput());
        }

        @Override
        public Vec3D getPosition() {
            return EntityMinecartCommandBlock.this.getPositionVector();
        }

        public EntityMinecartCommandBlock getMinecart() {
            return EntityMinecartCommandBlock.this;
        }

        @Override
        public CommandListenerWrapper getWrapper() {
            return new CommandListenerWrapper(this, EntityMinecartCommandBlock.this.getPositionVector(), EntityMinecartCommandBlock.this.getRotationVector(), this.getLevel(), 2, this.getName().getString(), EntityMinecartCommandBlock.this.getScoreboardDisplayName(), this.getLevel().getMinecraftServer(), EntityMinecartCommandBlock.this);
        }
    }
}
