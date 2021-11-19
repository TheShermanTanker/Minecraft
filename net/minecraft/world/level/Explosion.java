package net.minecraft.world.level;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.particles.Particles;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.EnumSoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.util.MathHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.item.EntityItem;
import net.minecraft.world.entity.item.EntityTNTPrimed;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.projectile.IProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentProtection;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BlockFireAbstract;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.loot.LootTableInfo;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameters;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.MovingObjectPosition;
import net.minecraft.world.phys.Vec3D;

public class Explosion {
    private static final ExplosionDamageCalculator EXPLOSION_DAMAGE_CALCULATOR = new ExplosionDamageCalculator();
    private static final int MAX_DROPS_PER_COMBINED_STACK = 16;
    private final boolean fire;
    private final Explosion.Effect blockInteraction;
    private final Random random = new Random();
    private final World level;
    private final double x;
    private final double y;
    private final double z;
    @Nullable
    public final Entity source;
    private final float radius;
    private final DamageSource damageSource;
    private final ExplosionDamageCalculator damageCalculator;
    private final List<BlockPosition> toBlow = Lists.newArrayList();
    private final Map<EntityHuman, Vec3D> hitPlayers = Maps.newHashMap();

    public Explosion(World world, @Nullable Entity entity, double x, double y, double z, float power) {
        this(world, entity, x, y, z, power, false, Explosion.Effect.DESTROY);
    }

    public Explosion(World world, @Nullable Entity entity, double x, double y, double z, float power, List<BlockPosition> affectedBlocks) {
        this(world, entity, x, y, z, power, false, Explosion.Effect.DESTROY, affectedBlocks);
    }

    public Explosion(World world, @Nullable Entity entity, double x, double y, double z, float power, boolean createFire, Explosion.Effect destructionType, List<BlockPosition> affectedBlocks) {
        this(world, entity, x, y, z, power, createFire, destructionType);
        this.toBlow.addAll(affectedBlocks);
    }

    public Explosion(World world, @Nullable Entity entity, double x, double y, double z, float power, boolean createFire, Explosion.Effect destructionType) {
        this(world, entity, (DamageSource)null, (ExplosionDamageCalculator)null, x, y, z, power, createFire, destructionType);
    }

    public Explosion(World world, @Nullable Entity entity, @Nullable DamageSource damageSource, @Nullable ExplosionDamageCalculator behavior, double x, double y, double z, float power, boolean createFire, Explosion.Effect destructionType) {
        this.level = world;
        this.source = entity;
        this.radius = power;
        this.x = x;
        this.y = y;
        this.z = z;
        this.fire = createFire;
        this.blockInteraction = destructionType;
        this.damageSource = damageSource == null ? DamageSource.explosion(this) : damageSource;
        this.damageCalculator = behavior == null ? this.makeDamageCalculator(entity) : behavior;
    }

    private ExplosionDamageCalculator makeDamageCalculator(@Nullable Entity entity) {
        return (ExplosionDamageCalculator)(entity == null ? EXPLOSION_DAMAGE_CALCULATOR : new ExplosionDamageCalculatorEntity(entity));
    }

    public static float getSeenPercent(Vec3D source, Entity entity) {
        AxisAlignedBB aABB = entity.getBoundingBox();
        double d = 1.0D / ((aABB.maxX - aABB.minX) * 2.0D + 1.0D);
        double e = 1.0D / ((aABB.maxY - aABB.minY) * 2.0D + 1.0D);
        double f = 1.0D / ((aABB.maxZ - aABB.minZ) * 2.0D + 1.0D);
        double g = (1.0D - Math.floor(1.0D / d) * d) / 2.0D;
        double h = (1.0D - Math.floor(1.0D / f) * f) / 2.0D;
        if (!(d < 0.0D) && !(e < 0.0D) && !(f < 0.0D)) {
            int i = 0;
            int j = 0;

            for(float k = 0.0F; k <= 1.0F; k = (float)((double)k + d)) {
                for(float l = 0.0F; l <= 1.0F; l = (float)((double)l + e)) {
                    for(float m = 0.0F; m <= 1.0F; m = (float)((double)m + f)) {
                        double n = MathHelper.lerp((double)k, aABB.minX, aABB.maxX);
                        double o = MathHelper.lerp((double)l, aABB.minY, aABB.maxY);
                        double p = MathHelper.lerp((double)m, aABB.minZ, aABB.maxZ);
                        Vec3D vec3 = new Vec3D(n + g, o, p + h);
                        if (entity.level.rayTrace(new RayTrace(vec3, source, RayTrace.BlockCollisionOption.COLLIDER, RayTrace.FluidCollisionOption.NONE, entity)).getType() == MovingObjectPosition.EnumMovingObjectType.MISS) {
                            ++i;
                        }

                        ++j;
                    }
                }
            }

            return (float)i / (float)j;
        } else {
            return 0.0F;
        }
    }

    public void explode() {
        this.level.gameEvent(this.source, GameEvent.EXPLODE, new BlockPosition(this.x, this.y, this.z));
        Set<BlockPosition> set = Sets.newHashSet();
        int i = 16;

        for(int j = 0; j < 16; ++j) {
            for(int k = 0; k < 16; ++k) {
                for(int l = 0; l < 16; ++l) {
                    if (j == 0 || j == 15 || k == 0 || k == 15 || l == 0 || l == 15) {
                        double d = (double)((float)j / 15.0F * 2.0F - 1.0F);
                        double e = (double)((float)k / 15.0F * 2.0F - 1.0F);
                        double f = (double)((float)l / 15.0F * 2.0F - 1.0F);
                        double g = Math.sqrt(d * d + e * e + f * f);
                        d = d / g;
                        e = e / g;
                        f = f / g;
                        float h = this.radius * (0.7F + this.level.random.nextFloat() * 0.6F);
                        double m = this.x;
                        double n = this.y;
                        double o = this.z;

                        for(float p = 0.3F; h > 0.0F; h -= 0.22500001F) {
                            BlockPosition blockPos = new BlockPosition(m, n, o);
                            IBlockData blockState = this.level.getType(blockPos);
                            Fluid fluidState = this.level.getFluid(blockPos);
                            if (!this.level.isValidLocation(blockPos)) {
                                break;
                            }

                            Optional<Float> optional = this.damageCalculator.getBlockExplosionResistance(this, this.level, blockPos, blockState, fluidState);
                            if (optional.isPresent()) {
                                h -= (optional.get() + 0.3F) * 0.3F;
                            }

                            if (h > 0.0F && this.damageCalculator.shouldBlockExplode(this, this.level, blockPos, blockState, h)) {
                                set.add(blockPos);
                            }

                            m += d * (double)0.3F;
                            n += e * (double)0.3F;
                            o += f * (double)0.3F;
                        }
                    }
                }
            }
        }

        this.toBlow.addAll(set);
        float q = this.radius * 2.0F;
        int r = MathHelper.floor(this.x - (double)q - 1.0D);
        int s = MathHelper.floor(this.x + (double)q + 1.0D);
        int t = MathHelper.floor(this.y - (double)q - 1.0D);
        int u = MathHelper.floor(this.y + (double)q + 1.0D);
        int v = MathHelper.floor(this.z - (double)q - 1.0D);
        int w = MathHelper.floor(this.z + (double)q + 1.0D);
        List<Entity> list = this.level.getEntities(this.source, new AxisAlignedBB((double)r, (double)t, (double)v, (double)s, (double)u, (double)w));
        Vec3D vec3 = new Vec3D(this.x, this.y, this.z);

        for(int x = 0; x < list.size(); ++x) {
            Entity entity = list.get(x);
            if (!entity.ignoreExplosion()) {
                double y = Math.sqrt(entity.distanceToSqr(vec3)) / (double)q;
                if (y <= 1.0D) {
                    double z = entity.locX() - this.x;
                    double aa = (entity instanceof EntityTNTPrimed ? entity.locY() : entity.getHeadY()) - this.y;
                    double ab = entity.locZ() - this.z;
                    double ac = Math.sqrt(z * z + aa * aa + ab * ab);
                    if (ac != 0.0D) {
                        z = z / ac;
                        aa = aa / ac;
                        ab = ab / ac;
                        double ad = (double)getSeenPercent(vec3, entity);
                        double ae = (1.0D - y) * ad;
                        entity.damageEntity(this.getDamageSource(), (float)((int)((ae * ae + ae) / 2.0D * 7.0D * (double)q + 1.0D)));
                        double af = ae;
                        if (entity instanceof EntityLiving) {
                            af = EnchantmentProtection.getExplosionKnockbackAfterDampener((EntityLiving)entity, ae);
                        }

                        entity.setMot(entity.getMot().add(z * af, aa * af, ab * af));
                        if (entity instanceof EntityHuman) {
                            EntityHuman player = (EntityHuman)entity;
                            if (!player.isSpectator() && (!player.isCreative() || !player.getAbilities().flying)) {
                                this.hitPlayers.put(player, new Vec3D(z * ae, aa * ae, ab * ae));
                            }
                        }
                    }
                }
            }
        }

    }

    public void finalizeExplosion(boolean particles) {
        if (this.level.isClientSide) {
            this.level.playLocalSound(this.x, this.y, this.z, SoundEffects.GENERIC_EXPLODE, EnumSoundCategory.BLOCKS, 4.0F, (1.0F + (this.level.random.nextFloat() - this.level.random.nextFloat()) * 0.2F) * 0.7F, false);
        }

        boolean bl = this.blockInteraction != Explosion.Effect.NONE;
        if (particles) {
            if (!(this.radius < 2.0F) && bl) {
                this.level.addParticle(Particles.EXPLOSION_EMITTER, this.x, this.y, this.z, 1.0D, 0.0D, 0.0D);
            } else {
                this.level.addParticle(Particles.EXPLOSION, this.x, this.y, this.z, 1.0D, 0.0D, 0.0D);
            }
        }

        if (bl) {
            ObjectArrayList<Pair<ItemStack, BlockPosition>> objectArrayList = new ObjectArrayList<>();
            Collections.shuffle(this.toBlow, this.level.random);

            for(BlockPosition blockPos : this.toBlow) {
                IBlockData blockState = this.level.getType(blockPos);
                Block block = blockState.getBlock();
                if (!blockState.isAir()) {
                    BlockPosition blockPos2 = blockPos.immutableCopy();
                    this.level.getMethodProfiler().enter("explosion_blocks");
                    if (block.dropFromExplosion(this) && this.level instanceof WorldServer) {
                        TileEntity blockEntity = blockState.isTileEntity() ? this.level.getTileEntity(blockPos) : null;
                        LootTableInfo.Builder builder = (new LootTableInfo.Builder((WorldServer)this.level)).withRandom(this.level.random).set(LootContextParameters.ORIGIN, Vec3D.atCenterOf(blockPos)).set(LootContextParameters.TOOL, ItemStack.EMPTY).setOptional(LootContextParameters.BLOCK_ENTITY, blockEntity).setOptional(LootContextParameters.THIS_ENTITY, this.source);
                        if (this.blockInteraction == Explosion.Effect.DESTROY) {
                            builder.set(LootContextParameters.EXPLOSION_RADIUS, this.radius);
                        }

                        blockState.getDrops(builder).forEach((stack) -> {
                            addBlockDrops(objectArrayList, stack, blockPos2);
                        });
                    }

                    this.level.setTypeAndData(blockPos, Blocks.AIR.getBlockData(), 3);
                    block.wasExploded(this.level, blockPos, this);
                    this.level.getMethodProfiler().exit();
                }
            }

            for(Pair<ItemStack, BlockPosition> pair : objectArrayList) {
                Block.popResource(this.level, pair.getSecond(), pair.getFirst());
            }
        }

        if (this.fire) {
            for(BlockPosition blockPos3 : this.toBlow) {
                if (this.random.nextInt(3) == 0 && this.level.getType(blockPos3).isAir() && this.level.getType(blockPos3.below()).isSolidRender(this.level, blockPos3.below())) {
                    this.level.setTypeUpdate(blockPos3, BlockFireAbstract.getState(this.level, blockPos3));
                }
            }
        }

    }

    private static void addBlockDrops(ObjectArrayList<Pair<ItemStack, BlockPosition>> stacks, ItemStack stack, BlockPosition pos) {
        int i = stacks.size();

        for(int j = 0; j < i; ++j) {
            Pair<ItemStack, BlockPosition> pair = stacks.get(j);
            ItemStack itemStack = pair.getFirst();
            if (EntityItem.areMergable(itemStack, stack)) {
                ItemStack itemStack2 = EntityItem.merge(itemStack, stack, 16);
                stacks.set(j, Pair.of(itemStack2, pair.getSecond()));
                if (stack.isEmpty()) {
                    return;
                }
            }
        }

        stacks.add(Pair.of(stack, pos));
    }

    public DamageSource getDamageSource() {
        return this.damageSource;
    }

    public Map<EntityHuman, Vec3D> getHitPlayers() {
        return this.hitPlayers;
    }

    @Nullable
    public EntityLiving getSource() {
        if (this.source == null) {
            return null;
        } else if (this.source instanceof EntityTNTPrimed) {
            return ((EntityTNTPrimed)this.source).getSource();
        } else if (this.source instanceof EntityLiving) {
            return (EntityLiving)this.source;
        } else {
            if (this.source instanceof IProjectile) {
                Entity entity = ((IProjectile)this.source).getShooter();
                if (entity instanceof EntityLiving) {
                    return (EntityLiving)entity;
                }
            }

            return null;
        }
    }

    public void clearBlocks() {
        this.toBlow.clear();
    }

    public List<BlockPosition> getBlocks() {
        return this.toBlow;
    }

    public static enum Effect {
        NONE,
        BREAK,
        DESTROY;
    }
}
