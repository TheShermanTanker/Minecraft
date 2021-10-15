package net.minecraft.world.entity.boss.enderdragon;

import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.GameProfileSerializer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.PacketPlayOutSpawnEntity;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.BlockFireAbstract;
import net.minecraft.world.level.dimension.end.EnderDragonBattle;

public class EntityEnderCrystal extends Entity {
    private static final DataWatcherObject<Optional<BlockPosition>> DATA_BEAM_TARGET = DataWatcher.defineId(EntityEnderCrystal.class, DataWatcherRegistry.OPTIONAL_BLOCK_POS);
    private static final DataWatcherObject<Boolean> DATA_SHOW_BOTTOM = DataWatcher.defineId(EntityEnderCrystal.class, DataWatcherRegistry.BOOLEAN);
    public int time;

    public EntityEnderCrystal(EntityTypes<? extends EntityEnderCrystal> type, World world) {
        super(type, world);
        this.blocksBuilding = true;
        this.time = this.random.nextInt(100000);
    }

    public EntityEnderCrystal(World world, double x, double y, double z) {
        this(EntityTypes.END_CRYSTAL, world);
        this.setPosition(x, y, z);
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.NONE;
    }

    @Override
    protected void initDatawatcher() {
        this.getDataWatcher().register(DATA_BEAM_TARGET, Optional.empty());
        this.getDataWatcher().register(DATA_SHOW_BOTTOM, true);
    }

    @Override
    public void tick() {
        ++this.time;
        if (this.level instanceof WorldServer) {
            BlockPosition blockPos = this.getChunkCoordinates();
            if (((WorldServer)this.level).getDragonBattle() != null && this.level.getType(blockPos).isAir()) {
                this.level.setTypeUpdate(blockPos, BlockFireAbstract.getState(this.level, blockPos));
            }
        }

    }

    @Override
    protected void saveData(NBTTagCompound nbt) {
        if (this.getBeamTarget() != null) {
            nbt.set("BeamTarget", GameProfileSerializer.writeBlockPos(this.getBeamTarget()));
        }

        nbt.setBoolean("ShowBottom", this.isShowingBottom());
    }

    @Override
    protected void loadData(NBTTagCompound nbt) {
        if (nbt.hasKeyOfType("BeamTarget", 10)) {
            this.setBeamTarget(GameProfileSerializer.readBlockPos(nbt.getCompound("BeamTarget")));
        }

        if (nbt.hasKeyOfType("ShowBottom", 1)) {
            this.setShowingBottom(nbt.getBoolean("ShowBottom"));
        }

    }

    @Override
    public boolean isInteractable() {
        return true;
    }

    @Override
    public boolean damageEntity(DamageSource source, float amount) {
        if (this.isInvulnerable(source)) {
            return false;
        } else if (source.getEntity() instanceof EntityEnderDragon) {
            return false;
        } else {
            if (!this.isRemoved() && !this.level.isClientSide) {
                this.remove(Entity.RemovalReason.KILLED);
                if (!source.isExplosion()) {
                    this.level.explode((Entity)null, this.locX(), this.locY(), this.locZ(), 6.0F, Explosion.Effect.DESTROY);
                }

                this.onDestroyedBy(source);
            }

            return true;
        }
    }

    @Override
    public void killEntity() {
        this.onDestroyedBy(DamageSource.GENERIC);
        super.killEntity();
    }

    private void onDestroyedBy(DamageSource source) {
        if (this.level instanceof WorldServer) {
            EnderDragonBattle endDragonFight = ((WorldServer)this.level).getDragonBattle();
            if (endDragonFight != null) {
                endDragonFight.onCrystalDestroyed(this, source);
            }
        }

    }

    public void setBeamTarget(@Nullable BlockPosition beamTarget) {
        this.getDataWatcher().set(DATA_BEAM_TARGET, Optional.ofNullable(beamTarget));
    }

    @Nullable
    public BlockPosition getBeamTarget() {
        return this.getDataWatcher().get(DATA_BEAM_TARGET).orElse((BlockPosition)null);
    }

    public void setShowingBottom(boolean showBottom) {
        this.getDataWatcher().set(DATA_SHOW_BOTTOM, showBottom);
    }

    public boolean isShowingBottom() {
        return this.getDataWatcher().get(DATA_SHOW_BOTTOM);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return super.shouldRenderAtSqrDistance(distance) || this.getBeamTarget() != null;
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(Items.END_CRYSTAL);
    }

    @Override
    public Packet<?> getPacket() {
        return new PacketPlayOutSpawnEntity(this);
    }
}
