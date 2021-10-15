package net.minecraft.world.entity.monster;

import java.util.List;
import net.minecraft.network.protocol.game.PacketPlayOutGameStateChange;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectList;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.AttributeProvider;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.level.World;

public class EntityGuardianElder extends EntityGuardian {
    public static final float ELDER_SIZE_SCALE = EntityTypes.ELDER_GUARDIAN.getWidth() / EntityTypes.GUARDIAN.getWidth();

    public EntityGuardianElder(EntityTypes<? extends EntityGuardianElder> type, World world) {
        super(type, world);
        this.setPersistent();
        if (this.randomStrollGoal != null) {
            this.randomStrollGoal.setTimeBetweenMovement(400);
        }

    }

    public static AttributeProvider.Builder createAttributes() {
        return EntityGuardian.createAttributes().add(GenericAttributes.MOVEMENT_SPEED, (double)0.3F).add(GenericAttributes.ATTACK_DAMAGE, 8.0D).add(GenericAttributes.MAX_HEALTH, 80.0D);
    }

    @Override
    public int getAttackDuration() {
        return 60;
    }

    @Override
    protected SoundEffect getSoundAmbient() {
        return this.isInWaterOrBubble() ? SoundEffects.ELDER_GUARDIAN_AMBIENT : SoundEffects.ELDER_GUARDIAN_AMBIENT_LAND;
    }

    @Override
    protected SoundEffect getSoundHurt(DamageSource source) {
        return this.isInWaterOrBubble() ? SoundEffects.ELDER_GUARDIAN_HURT : SoundEffects.ELDER_GUARDIAN_HURT_LAND;
    }

    @Override
    public SoundEffect getSoundDeath() {
        return this.isInWaterOrBubble() ? SoundEffects.ELDER_GUARDIAN_DEATH : SoundEffects.ELDER_GUARDIAN_DEATH_LAND;
    }

    @Override
    protected SoundEffect getSoundFlop() {
        return SoundEffects.ELDER_GUARDIAN_FLOP;
    }

    @Override
    protected void mobTick() {
        super.mobTick();
        int i = 1200;
        if ((this.tickCount + this.getId()) % 1200 == 0) {
            MobEffectList mobEffect = MobEffects.DIG_SLOWDOWN;
            List<EntityPlayer> list = ((WorldServer)this.level).getPlayers((player) -> {
                return this.distanceToSqr(player) < 2500.0D && player.gameMode.isSurvival();
            });
            int j = 2;
            int k = 6000;
            int l = 1200;

            for(EntityPlayer serverPlayer : list) {
                if (!serverPlayer.hasEffect(mobEffect) || serverPlayer.getEffect(mobEffect).getAmplifier() < 2 || serverPlayer.getEffect(mobEffect).getDuration() < 1200) {
                    serverPlayer.connection.sendPacket(new PacketPlayOutGameStateChange(PacketPlayOutGameStateChange.GUARDIAN_ELDER_EFFECT, this.isSilent() ? 0.0F : 1.0F));
                    serverPlayer.addEffect(new MobEffect(mobEffect, 6000, 2), this);
                }
            }
        }

        if (!this.hasRestriction()) {
            this.restrictTo(this.getChunkCoordinates(), 16);
        }

    }
}
