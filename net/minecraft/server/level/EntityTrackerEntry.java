package net.minecraft.server.level;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.PacketPlayOutAttachEntity;
import net.minecraft.network.protocol.game.PacketPlayOutEntity;
import net.minecraft.network.protocol.game.PacketPlayOutEntityDestroy;
import net.minecraft.network.protocol.game.PacketPlayOutEntityEffect;
import net.minecraft.network.protocol.game.PacketPlayOutEntityEquipment;
import net.minecraft.network.protocol.game.PacketPlayOutEntityHeadRotation;
import net.minecraft.network.protocol.game.PacketPlayOutEntityMetadata;
import net.minecraft.network.protocol.game.PacketPlayOutEntityTeleport;
import net.minecraft.network.protocol.game.PacketPlayOutEntityVelocity;
import net.minecraft.network.protocol.game.PacketPlayOutMount;
import net.minecraft.network.protocol.game.PacketPlayOutSpawnEntityLiving;
import net.minecraft.network.protocol.game.PacketPlayOutUpdateAttributes;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.util.MathHelper;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifiable;
import net.minecraft.world.entity.decoration.EntityItemFrame;
import net.minecraft.world.entity.projectile.EntityArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemWorldMap;
import net.minecraft.world.level.saveddata.maps.WorldMap;
import net.minecraft.world.phys.Vec3D;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class EntityTrackerEntry {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final int TOLERANCE_LEVEL_ROTATION = 1;
    private final WorldServer level;
    private final Entity entity;
    private final int updateInterval;
    private final boolean trackDelta;
    private final Consumer<Packet<?>> broadcast;
    private long xp;
    private long yp;
    private long zp;
    private int yRotp;
    private int xRotp;
    private int yHeadRotp;
    private Vec3D ap = Vec3D.ZERO;
    private int tickCount;
    private int teleportDelay;
    private List<Entity> lastPassengers = Collections.emptyList();
    private boolean wasRiding;
    private boolean wasOnGround;

    public EntityTrackerEntry(WorldServer world, Entity entity, int tickInterval, boolean alwaysUpdateVelocity, Consumer<Packet<?>> receiver) {
        this.level = world;
        this.broadcast = receiver;
        this.entity = entity;
        this.updateInterval = tickInterval;
        this.trackDelta = alwaysUpdateVelocity;
        this.updateSentPos();
        this.yRotp = MathHelper.floor(entity.getYRot() * 256.0F / 360.0F);
        this.xRotp = MathHelper.floor(entity.getXRot() * 256.0F / 360.0F);
        this.yHeadRotp = MathHelper.floor(entity.getHeadRotation() * 256.0F / 360.0F);
        this.wasOnGround = entity.isOnGround();
    }

    public void sendChanges() {
        List<Entity> list = this.entity.getPassengers();
        if (!list.equals(this.lastPassengers)) {
            this.lastPassengers = list;
            this.broadcast.accept(new PacketPlayOutMount(this.entity));
        }

        if (this.entity instanceof EntityItemFrame && this.tickCount % 10 == 0) {
            EntityItemFrame itemFrame = (EntityItemFrame)this.entity;
            ItemStack itemStack = itemFrame.getItem();
            if (itemStack.getItem() instanceof ItemWorldMap) {
                Integer integer = ItemWorldMap.getMapId(itemStack);
                WorldMap mapItemSavedData = ItemWorldMap.getSavedData(integer, this.level);
                if (mapItemSavedData != null) {
                    for(EntityPlayer serverPlayer : this.level.getPlayers()) {
                        mapItemSavedData.tickCarriedBy(serverPlayer, itemStack);
                        Packet<?> packet = mapItemSavedData.getUpdatePacket(integer, serverPlayer);
                        if (packet != null) {
                            serverPlayer.connection.sendPacket(packet);
                        }
                    }
                }
            }

            this.sendDirtyEntityData();
        }

        if (this.tickCount % this.updateInterval == 0 || this.entity.hasImpulse || this.entity.getDataWatcher().isDirty()) {
            if (this.entity.isPassenger()) {
                int i = MathHelper.floor(this.entity.getYRot() * 256.0F / 360.0F);
                int j = MathHelper.floor(this.entity.getXRot() * 256.0F / 360.0F);
                boolean bl = Math.abs(i - this.yRotp) >= 1 || Math.abs(j - this.xRotp) >= 1;
                if (bl) {
                    this.broadcast.accept(new PacketPlayOutEntity.PacketPlayOutEntityLook(this.entity.getId(), (byte)i, (byte)j, this.entity.isOnGround()));
                    this.yRotp = i;
                    this.xRotp = j;
                }

                this.updateSentPos();
                this.sendDirtyEntityData();
                this.wasRiding = true;
            } else {
                ++this.teleportDelay;
                int k = MathHelper.floor(this.entity.getYRot() * 256.0F / 360.0F);
                int l = MathHelper.floor(this.entity.getXRot() * 256.0F / 360.0F);
                Vec3D vec3 = this.entity.getPositionVector().subtract(PacketPlayOutEntity.packetToEntity(this.xp, this.yp, this.zp));
                boolean bl2 = vec3.lengthSqr() >= (double)7.6293945E-6F;
                Packet<?> packet2 = null;
                boolean bl3 = bl2 || this.tickCount % 60 == 0;
                boolean bl4 = Math.abs(k - this.yRotp) >= 1 || Math.abs(l - this.xRotp) >= 1;
                if (this.tickCount > 0 || this.entity instanceof EntityArrow) {
                    long m = PacketPlayOutEntity.entityToPacket(vec3.x);
                    long n = PacketPlayOutEntity.entityToPacket(vec3.y);
                    long o = PacketPlayOutEntity.entityToPacket(vec3.z);
                    boolean bl5 = m < -32768L || m > 32767L || n < -32768L || n > 32767L || o < -32768L || o > 32767L;
                    if (!bl5 && this.teleportDelay <= 400 && !this.wasRiding && this.wasOnGround == this.entity.isOnGround()) {
                        if ((!bl3 || !bl4) && !(this.entity instanceof EntityArrow)) {
                            if (bl3) {
                                packet2 = new PacketPlayOutEntity.PacketPlayOutRelEntityMove(this.entity.getId(), (short)((int)m), (short)((int)n), (short)((int)o), this.entity.isOnGround());
                            } else if (bl4) {
                                packet2 = new PacketPlayOutEntity.PacketPlayOutEntityLook(this.entity.getId(), (byte)k, (byte)l, this.entity.isOnGround());
                            }
                        } else {
                            packet2 = new PacketPlayOutEntity.PacketPlayOutRelEntityMoveLook(this.entity.getId(), (short)((int)m), (short)((int)n), (short)((int)o), (byte)k, (byte)l, this.entity.isOnGround());
                        }
                    } else {
                        this.wasOnGround = this.entity.isOnGround();
                        this.teleportDelay = 0;
                        packet2 = new PacketPlayOutEntityTeleport(this.entity);
                    }
                }

                if ((this.trackDelta || this.entity.hasImpulse || this.entity instanceof EntityLiving && ((EntityLiving)this.entity).isGliding()) && this.tickCount > 0) {
                    Vec3D vec32 = this.entity.getMot();
                    double d = vec32.distanceSquared(this.ap);
                    if (d > 1.0E-7D || d > 0.0D && vec32.lengthSqr() == 0.0D) {
                        this.ap = vec32;
                        this.broadcast.accept(new PacketPlayOutEntityVelocity(this.entity.getId(), this.ap));
                    }
                }

                if (packet2 != null) {
                    this.broadcast.accept(packet2);
                }

                this.sendDirtyEntityData();
                if (bl3) {
                    this.updateSentPos();
                }

                if (bl4) {
                    this.yRotp = k;
                    this.xRotp = l;
                }

                this.wasRiding = false;
            }

            int p = MathHelper.floor(this.entity.getHeadRotation() * 256.0F / 360.0F);
            if (Math.abs(p - this.yHeadRotp) >= 1) {
                this.broadcast.accept(new PacketPlayOutEntityHeadRotation(this.entity, (byte)p));
                this.yHeadRotp = p;
            }

            this.entity.hasImpulse = false;
        }

        ++this.tickCount;
        if (this.entity.hurtMarked) {
            this.broadcastIncludingSelf(new PacketPlayOutEntityVelocity(this.entity));
            this.entity.hurtMarked = false;
        }

    }

    public void removePairing(EntityPlayer player) {
        this.entity.stopSeenByPlayer(player);
        player.connection.sendPacket(new PacketPlayOutEntityDestroy(this.entity.getId()));
    }

    public void addPairing(EntityPlayer player) {
        this.sendPairingData(player.connection::sendPacket);
        this.entity.startSeenByPlayer(player);
    }

    public void sendPairingData(Consumer<Packet<?>> sender) {
        if (this.entity.isRemoved()) {
            LOGGER.warn("Fetching packet for removed entity {}", (Object)this.entity);
        }

        Packet<?> packet = this.entity.getPacket();
        this.yHeadRotp = MathHelper.floor(this.entity.getHeadRotation() * 256.0F / 360.0F);
        sender.accept(packet);
        if (!this.entity.getDataWatcher().isEmpty()) {
            sender.accept(new PacketPlayOutEntityMetadata(this.entity.getId(), this.entity.getDataWatcher(), true));
        }

        boolean bl = this.trackDelta;
        if (this.entity instanceof EntityLiving) {
            Collection<AttributeModifiable> collection = ((EntityLiving)this.entity).getAttributeMap().getSyncableAttributes();
            if (!collection.isEmpty()) {
                sender.accept(new PacketPlayOutUpdateAttributes(this.entity.getId(), collection));
            }

            if (((EntityLiving)this.entity).isGliding()) {
                bl = true;
            }
        }

        this.ap = this.entity.getMot();
        if (bl && !(packet instanceof PacketPlayOutSpawnEntityLiving)) {
            sender.accept(new PacketPlayOutEntityVelocity(this.entity.getId(), this.ap));
        }

        if (this.entity instanceof EntityLiving) {
            List<Pair<EnumItemSlot, ItemStack>> list = Lists.newArrayList();

            for(EnumItemSlot equipmentSlot : EnumItemSlot.values()) {
                ItemStack itemStack = ((EntityLiving)this.entity).getEquipment(equipmentSlot);
                if (!itemStack.isEmpty()) {
                    list.add(Pair.of(equipmentSlot, itemStack.cloneItemStack()));
                }
            }

            if (!list.isEmpty()) {
                sender.accept(new PacketPlayOutEntityEquipment(this.entity.getId(), list));
            }
        }

        if (this.entity instanceof EntityLiving) {
            EntityLiving livingEntity = (EntityLiving)this.entity;

            for(MobEffect mobEffectInstance : livingEntity.getEffects()) {
                sender.accept(new PacketPlayOutEntityEffect(this.entity.getId(), mobEffectInstance));
            }
        }

        if (!this.entity.getPassengers().isEmpty()) {
            sender.accept(new PacketPlayOutMount(this.entity));
        }

        if (this.entity.isPassenger()) {
            sender.accept(new PacketPlayOutMount(this.entity.getVehicle()));
        }

        if (this.entity instanceof EntityInsentient) {
            EntityInsentient mob = (EntityInsentient)this.entity;
            if (mob.isLeashed()) {
                sender.accept(new PacketPlayOutAttachEntity(mob, mob.getLeashHolder()));
            }
        }

    }

    private void sendDirtyEntityData() {
        DataWatcher synchedEntityData = this.entity.getDataWatcher();
        if (synchedEntityData.isDirty()) {
            this.broadcastIncludingSelf(new PacketPlayOutEntityMetadata(this.entity.getId(), synchedEntityData, false));
        }

        if (this.entity instanceof EntityLiving) {
            Set<AttributeModifiable> set = ((EntityLiving)this.entity).getAttributeMap().getAttributes();
            if (!set.isEmpty()) {
                this.broadcastIncludingSelf(new PacketPlayOutUpdateAttributes(this.entity.getId(), set));
            }

            set.clear();
        }

    }

    private void updateSentPos() {
        this.xp = PacketPlayOutEntity.entityToPacket(this.entity.locX());
        this.yp = PacketPlayOutEntity.entityToPacket(this.entity.locY());
        this.zp = PacketPlayOutEntity.entityToPacket(this.entity.locZ());
    }

    public Vec3D sentPos() {
        return PacketPlayOutEntity.packetToEntity(this.xp, this.yp, this.zp);
    }

    private void broadcastIncludingSelf(Packet<?> packet) {
        this.broadcast.accept(packet);
        if (this.entity instanceof EntityPlayer) {
            ((EntityPlayer)this.entity).connection.sendPacket(packet);
        }

    }
}
