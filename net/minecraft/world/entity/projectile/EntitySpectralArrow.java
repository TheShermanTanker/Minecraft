package net.minecraft.world.entity.projectile;

import net.minecraft.core.particles.Particles;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectList;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.World;

public class EntitySpectralArrow extends EntityArrow {
    public int duration = 200;

    public EntitySpectralArrow(EntityTypes<? extends EntitySpectralArrow> type, World world) {
        super(type, world);
    }

    public EntitySpectralArrow(World world, EntityLiving owner) {
        super(EntityTypes.SPECTRAL_ARROW, owner, world);
    }

    public EntitySpectralArrow(World world, double x, double y, double z) {
        super(EntityTypes.SPECTRAL_ARROW, x, y, z, world);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level.isClientSide && !this.inGround) {
            this.level.addParticle(Particles.INSTANT_EFFECT, this.locX(), this.locY(), this.locZ(), 0.0D, 0.0D, 0.0D);
        }

    }

    @Override
    public ItemStack getItemStack() {
        return new ItemStack(Items.SPECTRAL_ARROW);
    }

    @Override
    protected void doPostHurtEffects(EntityLiving target) {
        super.doPostHurtEffects(target);
        MobEffect mobEffectInstance = new MobEffect(MobEffectList.GLOWING, this.duration, 0);
        target.addEffect(mobEffectInstance, this.getEffectSource());
    }

    @Override
    public void loadData(NBTTagCompound nbt) {
        super.loadData(nbt);
        if (nbt.hasKey("Duration")) {
            this.duration = nbt.getInt("Duration");
        }

    }

    @Override
    public void saveData(NBTTagCompound nbt) {
        super.saveData(nbt);
        nbt.setInt("Duration", this.duration);
    }
}
