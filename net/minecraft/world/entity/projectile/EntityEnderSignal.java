package net.minecraft.world.entity.projectile;

import net.minecraft.SystemUtils;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.particles.Particles;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.PacketPlayOutSpawnEntity;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.util.MathHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.EntityItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.World;
import net.minecraft.world.phys.Vec3D;

public class EntityEnderSignal extends Entity implements ItemSupplier {
    private static final DataWatcherObject<ItemStack> DATA_ITEM_STACK = DataWatcher.defineId(EntityEnderSignal.class, DataWatcherRegistry.ITEM_STACK);
    public double tx;
    public double ty;
    public double tz;
    public int life;
    public boolean surviveAfterDeath;

    public EntityEnderSignal(EntityTypes<? extends EntityEnderSignal> type, World world) {
        super(type, world);
    }

    public EntityEnderSignal(World world, double x, double y, double z) {
        this(EntityTypes.EYE_OF_ENDER, world);
        this.setPosition(x, y, z);
    }

    public void setItem(ItemStack stack) {
        if (!stack.is(Items.ENDER_EYE) || stack.hasTag()) {
            this.getDataWatcher().set(DATA_ITEM_STACK, SystemUtils.make(stack.cloneItemStack(), (stackx) -> {
                stackx.setCount(1);
            }));
        }

    }

    private ItemStack getItemRaw() {
        return this.getDataWatcher().get(DATA_ITEM_STACK);
    }

    @Override
    public ItemStack getSuppliedItem() {
        ItemStack itemStack = this.getItemRaw();
        return itemStack.isEmpty() ? new ItemStack(Items.ENDER_EYE) : itemStack;
    }

    @Override
    protected void initDatawatcher() {
        this.getDataWatcher().register(DATA_ITEM_STACK, ItemStack.EMPTY);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double d = this.getBoundingBox().getSize() * 4.0D;
        if (Double.isNaN(d)) {
            d = 4.0D;
        }

        d = d * 64.0D;
        return distance < d * d;
    }

    public void signalTo(BlockPosition pos) {
        double d = (double)pos.getX();
        int i = pos.getY();
        double e = (double)pos.getZ();
        double f = d - this.locX();
        double g = e - this.locZ();
        double h = Math.sqrt(f * f + g * g);
        if (h > 12.0D) {
            this.tx = this.locX() + f / h * 12.0D;
            this.tz = this.locZ() + g / h * 12.0D;
            this.ty = this.locY() + 8.0D;
        } else {
            this.tx = d;
            this.ty = (double)i;
            this.tz = e;
        }

        this.life = 0;
        this.surviveAfterDeath = this.random.nextInt(5) > 0;
    }

    @Override
    public void lerpMotion(double x, double y, double z) {
        this.setMot(x, y, z);
        if (this.xRotO == 0.0F && this.yRotO == 0.0F) {
            double d = Math.sqrt(x * x + z * z);
            this.setYRot((float)(MathHelper.atan2(x, z) * (double)(180F / (float)Math.PI)));
            this.setXRot((float)(MathHelper.atan2(y, d) * (double)(180F / (float)Math.PI)));
            this.yRotO = this.getYRot();
            this.xRotO = this.getXRot();
        }

    }

    @Override
    public void tick() {
        super.tick();
        Vec3D vec3 = this.getMot();
        double d = this.locX() + vec3.x;
        double e = this.locY() + vec3.y;
        double f = this.locZ() + vec3.z;
        double g = vec3.horizontalDistance();
        this.setXRot(IProjectile.lerpRotation(this.xRotO, (float)(MathHelper.atan2(vec3.y, g) * (double)(180F / (float)Math.PI))));
        this.setYRot(IProjectile.lerpRotation(this.yRotO, (float)(MathHelper.atan2(vec3.x, vec3.z) * (double)(180F / (float)Math.PI))));
        if (!this.level.isClientSide) {
            double h = this.tx - d;
            double i = this.tz - f;
            float j = (float)Math.sqrt(h * h + i * i);
            float k = (float)MathHelper.atan2(i, h);
            double l = MathHelper.lerp(0.0025D, g, (double)j);
            double m = vec3.y;
            if (j < 1.0F) {
                l *= 0.8D;
                m *= 0.8D;
            }

            int n = this.locY() < this.ty ? 1 : -1;
            vec3 = new Vec3D(Math.cos((double)k) * l, m + ((double)n - m) * (double)0.015F, Math.sin((double)k) * l);
            this.setMot(vec3);
        }

        float o = 0.25F;
        if (this.isInWater()) {
            for(int p = 0; p < 4; ++p) {
                this.level.addParticle(Particles.BUBBLE, d - vec3.x * 0.25D, e - vec3.y * 0.25D, f - vec3.z * 0.25D, vec3.x, vec3.y, vec3.z);
            }
        } else {
            this.level.addParticle(Particles.PORTAL, d - vec3.x * 0.25D + this.random.nextDouble() * 0.6D - 0.3D, e - vec3.y * 0.25D - 0.5D, f - vec3.z * 0.25D + this.random.nextDouble() * 0.6D - 0.3D, vec3.x, vec3.y, vec3.z);
        }

        if (!this.level.isClientSide) {
            this.setPosition(d, e, f);
            ++this.life;
            if (this.life > 80 && !this.level.isClientSide) {
                this.playSound(SoundEffects.ENDER_EYE_DEATH, 1.0F, 1.0F);
                this.die();
                if (this.surviveAfterDeath) {
                    this.level.addEntity(new EntityItem(this.level, this.locX(), this.locY(), this.locZ(), this.getSuppliedItem()));
                } else {
                    this.level.triggerEffect(2003, this.getChunkCoordinates(), 0);
                }
            }
        } else {
            this.setPositionRaw(d, e, f);
        }

    }

    @Override
    public void saveData(NBTTagCompound nbt) {
        ItemStack itemStack = this.getItemRaw();
        if (!itemStack.isEmpty()) {
            nbt.set("Item", itemStack.save(new NBTTagCompound()));
        }

    }

    @Override
    public void loadData(NBTTagCompound nbt) {
        ItemStack itemStack = ItemStack.of(nbt.getCompound("Item"));
        this.setItem(itemStack);
    }

    @Override
    public float getBrightness() {
        return 1.0F;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public Packet<?> getPacket() {
        return new PacketPlayOutSpawnEntity(this);
    }
}
