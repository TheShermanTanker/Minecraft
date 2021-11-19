package net.minecraft.world.entity;

import java.util.List;
import java.util.Map.Entry;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.PacketPlayOutSpawnEntityExperienceOrb;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.EnumSoundCategory;
import net.minecraft.tags.TagsFluid;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentManager;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.World;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.Vec3D;

public class EntityExperienceOrb extends Entity {
    private static final int LIFETIME = 6000;
    private static final int ENTITY_SCAN_PERIOD = 20;
    private static final int MAX_FOLLOW_DIST = 8;
    private static final int ORB_GROUPS_PER_AREA = 40;
    private static final double ORB_MERGE_DISTANCE = 0.5D;
    private int age;
    private int health = 5;
    public int value;
    private int count = 1;
    private EntityHuman followingPlayer;

    public EntityExperienceOrb(World world, double x, double y, double z, int amount) {
        this(EntityTypes.EXPERIENCE_ORB, world);
        this.setPosition(x, y, z);
        this.setYRot((float)(this.random.nextDouble() * 360.0D));
        this.setMot((this.random.nextDouble() * (double)0.2F - (double)0.1F) * 2.0D, this.random.nextDouble() * 0.2D * 2.0D, (this.random.nextDouble() * (double)0.2F - (double)0.1F) * 2.0D);
        this.value = amount;
    }

    public EntityExperienceOrb(EntityTypes<? extends EntityExperienceOrb> type, World world) {
        super(type, world);
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.NONE;
    }

    @Override
    protected void initDatawatcher() {
    }

    @Override
    public void tick() {
        super.tick();
        this.xo = this.locX();
        this.yo = this.locY();
        this.zo = this.locZ();
        if (this.isEyeInFluid(TagsFluid.WATER)) {
            this.setUnderwaterMovement();
        } else if (!this.isNoGravity()) {
            this.setMot(this.getMot().add(0.0D, -0.03D, 0.0D));
        }

        if (this.level.getFluid(this.getChunkCoordinates()).is(TagsFluid.LAVA)) {
            this.setMot((double)((this.random.nextFloat() - this.random.nextFloat()) * 0.2F), (double)0.2F, (double)((this.random.nextFloat() - this.random.nextFloat()) * 0.2F));
        }

        if (!this.level.noCollision(this.getBoundingBox())) {
            this.moveTowardsClosestSpace(this.locX(), (this.getBoundingBox().minY + this.getBoundingBox().maxY) / 2.0D, this.locZ());
        }

        if (this.tickCount % 20 == 1) {
            this.scanForEntities();
        }

        if (this.followingPlayer != null && (this.followingPlayer.isSpectator() || this.followingPlayer.isDeadOrDying())) {
            this.followingPlayer = null;
        }

        if (this.followingPlayer != null) {
            Vec3D vec3 = new Vec3D(this.followingPlayer.locX() - this.locX(), this.followingPlayer.locY() + (double)this.followingPlayer.getHeadHeight() / 2.0D - this.locY(), this.followingPlayer.locZ() - this.locZ());
            double d = vec3.lengthSqr();
            if (d < 64.0D) {
                double e = 1.0D - Math.sqrt(d) / 8.0D;
                this.setMot(this.getMot().add(vec3.normalize().scale(e * e * 0.1D)));
            }
        }

        this.move(EnumMoveType.SELF, this.getMot());
        float f = 0.98F;
        if (this.onGround) {
            f = this.level.getType(new BlockPosition(this.locX(), this.locY() - 1.0D, this.locZ())).getBlock().getFrictionFactor() * 0.98F;
        }

        this.setMot(this.getMot().multiply((double)f, 0.98D, (double)f));
        if (this.onGround) {
            this.setMot(this.getMot().multiply(1.0D, -0.9D, 1.0D));
        }

        ++this.age;
        if (this.age >= 6000) {
            this.die();
        }

    }

    private void scanForEntities() {
        if (this.followingPlayer == null || this.followingPlayer.distanceToSqr(this) > 64.0D) {
            this.followingPlayer = this.level.findNearbyPlayer(this, 8.0D);
        }

        if (this.level instanceof WorldServer) {
            for(EntityExperienceOrb experienceOrb : this.level.getEntities(EntityTypeTest.forClass(EntityExperienceOrb.class), this.getBoundingBox().inflate(0.5D), this::canMerge)) {
                this.merge(experienceOrb);
            }
        }

    }

    public static void award(WorldServer world, Vec3D pos, int amount) {
        while(amount > 0) {
            int i = getOrbValue(amount);
            amount -= i;
            if (!tryMergeToExisting(world, pos, i)) {
                world.addEntity(new EntityExperienceOrb(world, pos.getX(), pos.getY(), pos.getZ(), i));
            }
        }

    }

    private static boolean tryMergeToExisting(WorldServer world, Vec3D pos, int amount) {
        AxisAlignedBB aABB = AxisAlignedBB.ofSize(pos, 1.0D, 1.0D, 1.0D);
        int i = world.getRandom().nextInt(40);
        List<EntityExperienceOrb> list = world.getEntities(EntityTypeTest.forClass(EntityExperienceOrb.class), aABB, (orb) -> {
            return canMerge(orb, i, amount);
        });
        if (!list.isEmpty()) {
            EntityExperienceOrb experienceOrb = list.get(0);
            ++experienceOrb.count;
            experienceOrb.age = 0;
            return true;
        } else {
            return false;
        }
    }

    private boolean canMerge(EntityExperienceOrb other) {
        return other != this && canMerge(other, this.getId(), this.value);
    }

    private static boolean canMerge(EntityExperienceOrb orb, int seed, int amount) {
        return !orb.isRemoved() && (orb.getId() - seed) % 40 == 0 && orb.value == amount;
    }

    private void merge(EntityExperienceOrb other) {
        this.count += other.count;
        this.age = Math.min(this.age, other.age);
        other.die();
    }

    private void setUnderwaterMovement() {
        Vec3D vec3 = this.getMot();
        this.setMot(vec3.x * (double)0.99F, Math.min(vec3.y + (double)5.0E-4F, (double)0.06F), vec3.z * (double)0.99F);
    }

    @Override
    protected void doWaterSplashEffect() {
    }

    @Override
    public boolean damageEntity(DamageSource source, float amount) {
        if (this.isInvulnerable(source)) {
            return false;
        } else {
            this.velocityChanged();
            this.health = (int)((float)this.health - amount);
            if (this.health <= 0) {
                this.die();
            }

            return true;
        }
    }

    @Override
    public void saveData(NBTTagCompound nbt) {
        nbt.setShort("Health", (short)this.health);
        nbt.setShort("Age", (short)this.age);
        nbt.setShort("Value", (short)this.value);
        nbt.setInt("Count", this.count);
    }

    @Override
    public void loadData(NBTTagCompound nbt) {
        this.health = nbt.getShort("Health");
        this.age = nbt.getShort("Age");
        this.value = nbt.getShort("Value");
        this.count = Math.max(nbt.getInt("Count"), 1);
    }

    @Override
    public void pickup(EntityHuman player) {
        if (!this.level.isClientSide) {
            if (player.takeXpDelay == 0) {
                player.takeXpDelay = 2;
                player.receive(this, 1);
                int i = this.repairPlayerItems(player, this.value);
                if (i > 0) {
                    player.giveExp(i);
                }

                --this.count;
                if (this.count == 0) {
                    this.die();
                }
            }

        }
    }

    private int repairPlayerItems(EntityHuman player, int amount) {
        Entry<EnumItemSlot, ItemStack> entry = EnchantmentManager.getRandomItemWith(Enchantments.MENDING, player, ItemStack::isDamaged);
        if (entry != null) {
            ItemStack itemStack = entry.getValue();
            int i = Math.min(this.xpToDurability(this.value), itemStack.getDamage());
            itemStack.setDamage(itemStack.getDamage() - i);
            int j = amount - this.durabilityToXp(i);
            return j > 0 ? this.repairPlayerItems(player, j) : 0;
        } else {
            return amount;
        }
    }

    public int durabilityToXp(int repairAmount) {
        return repairAmount / 2;
    }

    public int xpToDurability(int experienceAmount) {
        return experienceAmount * 2;
    }

    public int getValue() {
        return this.value;
    }

    public int getIcon() {
        if (this.value >= 2477) {
            return 10;
        } else if (this.value >= 1237) {
            return 9;
        } else if (this.value >= 617) {
            return 8;
        } else if (this.value >= 307) {
            return 7;
        } else if (this.value >= 149) {
            return 6;
        } else if (this.value >= 73) {
            return 5;
        } else if (this.value >= 37) {
            return 4;
        } else if (this.value >= 17) {
            return 3;
        } else if (this.value >= 7) {
            return 2;
        } else {
            return this.value >= 3 ? 1 : 0;
        }
    }

    public static int getOrbValue(int value) {
        if (value >= 2477) {
            return 2477;
        } else if (value >= 1237) {
            return 1237;
        } else if (value >= 617) {
            return 617;
        } else if (value >= 307) {
            return 307;
        } else if (value >= 149) {
            return 149;
        } else if (value >= 73) {
            return 73;
        } else if (value >= 37) {
            return 37;
        } else if (value >= 17) {
            return 17;
        } else if (value >= 7) {
            return 7;
        } else {
            return value >= 3 ? 3 : 1;
        }
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public Packet<?> getPacket() {
        return new PacketPlayOutSpawnEntityExperienceOrb(this);
    }

    @Override
    public EnumSoundCategory getSoundCategory() {
        return EnumSoundCategory.AMBIENT;
    }
}
