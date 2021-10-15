package net.minecraft.world.damagesource;

import javax.annotation.Nullable;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.projectile.EntityArrow;
import net.minecraft.world.entity.projectile.EntityFireballFireball;
import net.minecraft.world.entity.projectile.EntityFireworks;
import net.minecraft.world.entity.projectile.EntityWitherSkull;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.phys.Vec3D;

public class DamageSource {
    public static final DamageSource IN_FIRE = (new DamageSource("inFire")).setIgnoreArmor().setFire();
    public static final DamageSource LIGHTNING_BOLT = new DamageSource("lightningBolt");
    public static final DamageSource ON_FIRE = (new DamageSource("onFire")).setIgnoreArmor().setFire();
    public static final DamageSource LAVA = (new DamageSource("lava")).setFire();
    public static final DamageSource HOT_FLOOR = (new DamageSource("hotFloor")).setFire();
    public static final DamageSource IN_WALL = (new DamageSource("inWall")).setIgnoreArmor();
    public static final DamageSource CRAMMING = (new DamageSource("cramming")).setIgnoreArmor();
    public static final DamageSource DROWN = (new DamageSource("drown")).setIgnoreArmor();
    public static final DamageSource STARVE = (new DamageSource("starve")).setIgnoreArmor().setStarvation();
    public static final DamageSource CACTUS = new DamageSource("cactus");
    public static final DamageSource FALL = (new DamageSource("fall")).setIgnoreArmor().setIsFall();
    public static final DamageSource FLY_INTO_WALL = (new DamageSource("flyIntoWall")).setIgnoreArmor();
    public static final DamageSource OUT_OF_WORLD = (new DamageSource("outOfWorld")).setIgnoreArmor().setIgnoresInvulnerability();
    public static final DamageSource GENERIC = (new DamageSource("generic")).setIgnoreArmor();
    public static final DamageSource MAGIC = (new DamageSource("magic")).setIgnoreArmor().setMagic();
    public static final DamageSource WITHER = (new DamageSource("wither")).setIgnoreArmor();
    public static final DamageSource ANVIL = (new DamageSource("anvil")).damageHelmet();
    public static final DamageSource FALLING_BLOCK = (new DamageSource("fallingBlock")).damageHelmet();
    public static final DamageSource DRAGON_BREATH = (new DamageSource("dragonBreath")).setIgnoreArmor();
    public static final DamageSource DRY_OUT = new DamageSource("dryout");
    public static final DamageSource SWEET_BERRY_BUSH = new DamageSource("sweetBerryBush");
    public static final DamageSource FREEZE = (new DamageSource("freeze")).setIgnoreArmor();
    public static final DamageSource FALLING_STALACTITE = (new DamageSource("fallingStalactite")).damageHelmet();
    public static final DamageSource STALAGMITE = (new DamageSource("stalagmite")).setIgnoreArmor().setIsFall();
    private boolean damageHelmet;
    private boolean bypassArmor;
    private boolean bypassInvul;
    private boolean bypassMagic;
    private float exhaustion = 0.1F;
    private boolean isFireSource;
    private boolean isProjectile;
    private boolean scalesWithDifficulty;
    private boolean isMagic;
    private boolean isExplosion;
    private boolean isFall;
    private boolean noAggro;
    public final String msgId;

    public static DamageSource sting(EntityLiving attacker) {
        return new EntityDamageSource("sting", attacker);
    }

    public static DamageSource mobAttack(EntityLiving attacker) {
        return new EntityDamageSource("mob", attacker);
    }

    public static DamageSource indirectMobAttack(Entity projectile, @Nullable EntityLiving attacker) {
        return new EntityDamageSourceIndirect("mob", projectile, attacker);
    }

    public static DamageSource playerAttack(EntityHuman attacker) {
        return new EntityDamageSource("player", attacker);
    }

    public static DamageSource arrow(EntityArrow projectile, @Nullable Entity attacker) {
        return (new EntityDamageSourceIndirect("arrow", projectile, attacker)).setProjectile();
    }

    public static DamageSource trident(Entity trident, @Nullable Entity attacker) {
        return (new EntityDamageSourceIndirect("trident", trident, attacker)).setProjectile();
    }

    public static DamageSource fireworks(EntityFireworks firework, @Nullable Entity attacker) {
        return (new EntityDamageSourceIndirect("fireworks", firework, attacker)).setExplosion();
    }

    public static DamageSource fireball(EntityFireballFireball fireball, @Nullable Entity attacker) {
        return attacker == null ? (new EntityDamageSourceIndirect("onFire", fireball, fireball)).setFire().setProjectile() : (new EntityDamageSourceIndirect("fireball", fireball, attacker)).setFire().setProjectile();
    }

    public static DamageSource witherSkull(EntityWitherSkull witherSkull, Entity attacker) {
        return (new EntityDamageSourceIndirect("witherSkull", witherSkull, attacker)).setProjectile();
    }

    public static DamageSource projectile(Entity projectile, @Nullable Entity attacker) {
        return (new EntityDamageSourceIndirect("thrown", projectile, attacker)).setProjectile();
    }

    public static DamageSource indirectMagic(Entity magic, @Nullable Entity attacker) {
        return (new EntityDamageSourceIndirect("indirectMagic", magic, attacker)).setIgnoreArmor().setMagic();
    }

    public static DamageSource thorns(Entity attacker) {
        return (new EntityDamageSource("thorns", attacker)).setThorns().setMagic();
    }

    public static DamageSource explosion(@Nullable Explosion explosion) {
        return explosion(explosion != null ? explosion.getSource() : null);
    }

    public static DamageSource explosion(@Nullable EntityLiving attacker) {
        return attacker != null ? (new EntityDamageSource("explosion.player", attacker)).setScalesWithDifficulty().setExplosion() : (new DamageSource("explosion")).setScalesWithDifficulty().setExplosion();
    }

    public static DamageSource badRespawnPointExplosion() {
        return new DamageSourceNetherBed();
    }

    @Override
    public String toString() {
        return "DamageSource (" + this.msgId + ")";
    }

    public boolean isProjectile() {
        return this.isProjectile;
    }

    public DamageSource setProjectile() {
        this.isProjectile = true;
        return this;
    }

    public boolean isExplosion() {
        return this.isExplosion;
    }

    public DamageSource setExplosion() {
        this.isExplosion = true;
        return this;
    }

    public boolean ignoresArmor() {
        return this.bypassArmor;
    }

    public boolean isDamageHelmet() {
        return this.damageHelmet;
    }

    public float getExhaustionCost() {
        return this.exhaustion;
    }

    public boolean ignoresInvulnerability() {
        return this.bypassInvul;
    }

    public boolean isStarvation() {
        return this.bypassMagic;
    }

    protected DamageSource(String name) {
        this.msgId = name;
    }

    @Nullable
    public Entity getDirectEntity() {
        return this.getEntity();
    }

    @Nullable
    public Entity getEntity() {
        return null;
    }

    protected DamageSource setIgnoreArmor() {
        this.bypassArmor = true;
        this.exhaustion = 0.0F;
        return this;
    }

    protected DamageSource damageHelmet() {
        this.damageHelmet = true;
        return this;
    }

    protected DamageSource setIgnoresInvulnerability() {
        this.bypassInvul = true;
        return this;
    }

    protected DamageSource setStarvation() {
        this.bypassMagic = true;
        this.exhaustion = 0.0F;
        return this;
    }

    protected DamageSource setFire() {
        this.isFireSource = true;
        return this;
    }

    public DamageSource setNoAggro() {
        this.noAggro = true;
        return this;
    }

    public IChatBaseComponent getLocalizedDeathMessage(EntityLiving entity) {
        EntityLiving livingEntity = entity.getKillingEntity();
        String string = "death.attack." + this.msgId;
        String string2 = string + ".player";
        return livingEntity != null ? new ChatMessage(string2, entity.getScoreboardDisplayName(), livingEntity.getScoreboardDisplayName()) : new ChatMessage(string, entity.getScoreboardDisplayName());
    }

    public boolean isFire() {
        return this.isFireSource;
    }

    public boolean isNoAggro() {
        return this.noAggro;
    }

    public String getMsgId() {
        return this.msgId;
    }

    public DamageSource setScalesWithDifficulty() {
        this.scalesWithDifficulty = true;
        return this;
    }

    public boolean scalesWithDifficulty() {
        return this.scalesWithDifficulty;
    }

    public boolean isMagic() {
        return this.isMagic;
    }

    public DamageSource setMagic() {
        this.isMagic = true;
        return this;
    }

    public boolean isFall() {
        return this.isFall;
    }

    public DamageSource setIsFall() {
        this.isFall = true;
        return this;
    }

    public boolean isCreativePlayer() {
        Entity entity = this.getEntity();
        return entity instanceof EntityHuman && ((EntityHuman)entity).getAbilities().instabuild;
    }

    @Nullable
    public Vec3D getSourcePosition() {
        return null;
    }
}
