package net.minecraft.world.entity.vehicle;

import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.particles.Particles;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.BlockFurnaceFurace;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.phys.Vec3D;

public class EntityMinecartFurnace extends EntityMinecartAbstract {
    private static final DataWatcherObject<Boolean> DATA_ID_FUEL = DataWatcher.defineId(EntityMinecartFurnace.class, DataWatcherRegistry.BOOLEAN);
    public int fuel;
    public double xPush;
    public double zPush;
    private static final RecipeItemStack INGREDIENT = RecipeItemStack.of(Items.COAL, Items.CHARCOAL);

    public EntityMinecartFurnace(EntityTypes<? extends EntityMinecartFurnace> type, World world) {
        super(type, world);
    }

    public EntityMinecartFurnace(World world, double x, double y, double z) {
        super(EntityTypes.FURNACE_MINECART, world, x, y, z);
    }

    @Override
    public EntityMinecartAbstract.EnumMinecartType getMinecartType() {
        return EntityMinecartAbstract.EnumMinecartType.FURNACE;
    }

    @Override
    protected void initDatawatcher() {
        super.initDatawatcher();
        this.entityData.register(DATA_ID_FUEL, false);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level.isClientSide()) {
            if (this.fuel > 0) {
                --this.fuel;
            }

            if (this.fuel <= 0) {
                this.xPush = 0.0D;
                this.zPush = 0.0D;
            }

            this.setHasFuel(this.fuel > 0);
        }

        if (this.hasFuel() && this.random.nextInt(4) == 0) {
            this.level.addParticle(Particles.LARGE_SMOKE, this.locX(), this.locY() + 0.8D, this.locZ(), 0.0D, 0.0D, 0.0D);
        }

    }

    @Override
    protected double getMaxSpeed() {
        return (this.isInWater() ? 3.0D : 4.0D) / 20.0D;
    }

    @Override
    public void destroy(DamageSource damageSource) {
        super.destroy(damageSource);
        if (!damageSource.isExplosion() && this.level.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
            this.spawnAtLocation(Blocks.FURNACE);
        }

    }

    @Override
    protected void moveAlongTrack(BlockPosition pos, IBlockData state) {
        double d = 1.0E-4D;
        double e = 0.001D;
        super.moveAlongTrack(pos, state);
        Vec3D vec3 = this.getMot();
        double f = vec3.horizontalDistanceSqr();
        double g = this.xPush * this.xPush + this.zPush * this.zPush;
        if (g > 1.0E-4D && f > 0.001D) {
            double h = Math.sqrt(f);
            double i = Math.sqrt(g);
            this.xPush = vec3.x / h * i;
            this.zPush = vec3.z / h * i;
        }

    }

    @Override
    protected void decelerate() {
        double d = this.xPush * this.xPush + this.zPush * this.zPush;
        if (d > 1.0E-7D) {
            d = Math.sqrt(d);
            this.xPush /= d;
            this.zPush /= d;
            Vec3D vec3 = this.getMot().multiply(0.8D, 0.0D, 0.8D).add(this.xPush, 0.0D, this.zPush);
            if (this.isInWater()) {
                vec3 = vec3.scale(0.1D);
            }

            this.setMot(vec3);
        } else {
            this.setMot(this.getMot().multiply(0.98D, 0.0D, 0.98D));
        }

        super.decelerate();
    }

    @Override
    public EnumInteractionResult interact(EntityHuman player, EnumHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (INGREDIENT.test(itemStack) && this.fuel + 3600 <= 32000) {
            if (!player.getAbilities().instabuild) {
                itemStack.subtract(1);
            }

            this.fuel += 3600;
        }

        if (this.fuel > 0) {
            this.xPush = this.locX() - player.locX();
            this.zPush = this.locZ() - player.locZ();
        }

        return EnumInteractionResult.sidedSuccess(this.level.isClientSide);
    }

    @Override
    protected void saveData(NBTTagCompound nbt) {
        super.saveData(nbt);
        nbt.setDouble("PushX", this.xPush);
        nbt.setDouble("PushZ", this.zPush);
        nbt.setShort("Fuel", (short)this.fuel);
    }

    @Override
    protected void loadData(NBTTagCompound nbt) {
        super.loadData(nbt);
        this.xPush = nbt.getDouble("PushX");
        this.zPush = nbt.getDouble("PushZ");
        this.fuel = nbt.getShort("Fuel");
    }

    protected boolean hasFuel() {
        return this.entityData.get(DATA_ID_FUEL);
    }

    protected void setHasFuel(boolean lit) {
        this.entityData.set(DATA_ID_FUEL, lit);
    }

    @Override
    public IBlockData getDefaultDisplayBlockState() {
        return Blocks.FURNACE.getBlockData().set(BlockFurnaceFurace.FACING, EnumDirection.NORTH).set(BlockFurnaceFurace.LIT, Boolean.valueOf(this.hasFuel()));
    }
}
