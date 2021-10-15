package net.minecraft.world.damagesource;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.tags.TagsBlock;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;

public class CombatTracker {
    public static final int RESET_DAMAGE_STATUS_TIME = 100;
    public static final int RESET_COMBAT_STATUS_TIME = 300;
    private final List<CombatEntry> entries = Lists.newArrayList();
    private final EntityLiving mob;
    private int lastDamageTime;
    private int combatStartTime;
    private int combatEndTime;
    private boolean inCombat;
    private boolean takingDamage;
    @Nullable
    private String nextLocation;

    public CombatTracker(EntityLiving entity) {
        this.mob = entity;
    }

    public void prepareForDamage() {
        this.resetPreparedStatus();
        Optional<BlockPosition> optional = this.mob.getLastClimbablePos();
        if (optional.isPresent()) {
            IBlockData blockState = this.mob.level.getType(optional.get());
            if (!blockState.is(Blocks.LADDER) && !blockState.is(TagsBlock.TRAPDOORS)) {
                if (blockState.is(Blocks.VINE)) {
                    this.nextLocation = "vines";
                } else if (!blockState.is(Blocks.WEEPING_VINES) && !blockState.is(Blocks.WEEPING_VINES_PLANT)) {
                    if (!blockState.is(Blocks.TWISTING_VINES) && !blockState.is(Blocks.TWISTING_VINES_PLANT)) {
                        if (blockState.is(Blocks.SCAFFOLDING)) {
                            this.nextLocation = "scaffolding";
                        } else {
                            this.nextLocation = "other_climbable";
                        }
                    } else {
                        this.nextLocation = "twisting_vines";
                    }
                } else {
                    this.nextLocation = "weeping_vines";
                }
            } else {
                this.nextLocation = "ladder";
            }
        } else if (this.mob.isInWater()) {
            this.nextLocation = "water";
        }

    }

    public void trackDamage(DamageSource damageSource, float originalHealth, float damage) {
        this.recheckStatus();
        this.prepareForDamage();
        CombatEntry combatEntry = new CombatEntry(damageSource, this.mob.tickCount, originalHealth, damage, this.nextLocation, this.mob.fallDistance);
        this.entries.add(combatEntry);
        this.lastDamageTime = this.mob.tickCount;
        this.takingDamage = true;
        if (combatEntry.isCombatRelated() && !this.inCombat && this.mob.isAlive()) {
            this.inCombat = true;
            this.combatStartTime = this.mob.tickCount;
            this.combatEndTime = this.combatStartTime;
            this.mob.enterCombat();
        }

    }

    public IChatBaseComponent getDeathMessage() {
        if (this.entries.isEmpty()) {
            return new ChatMessage("death.attack.generic", this.mob.getScoreboardDisplayName());
        } else {
            CombatEntry combatEntry = this.getMostSignificantFall();
            CombatEntry combatEntry2 = this.entries.get(this.entries.size() - 1);
            IChatBaseComponent component = combatEntry2.getAttackerName();
            Entity entity = combatEntry2.getSource().getEntity();
            IChatBaseComponent component4;
            if (combatEntry != null && combatEntry2.getSource() == DamageSource.FALL) {
                IChatBaseComponent component2 = combatEntry.getAttackerName();
                if (combatEntry.getSource() != DamageSource.FALL && combatEntry.getSource() != DamageSource.OUT_OF_WORLD) {
                    if (component2 != null && !component2.equals(component)) {
                        Entity entity2 = combatEntry.getSource().getEntity();
                        ItemStack itemStack = entity2 instanceof EntityLiving ? ((EntityLiving)entity2).getItemInMainHand() : ItemStack.EMPTY;
                        if (!itemStack.isEmpty() && itemStack.hasName()) {
                            component4 = new ChatMessage("death.fell.assist.item", this.mob.getScoreboardDisplayName(), component2, itemStack.getDisplayName());
                        } else {
                            component4 = new ChatMessage("death.fell.assist", this.mob.getScoreboardDisplayName(), component2);
                        }
                    } else if (component != null) {
                        ItemStack itemStack2 = entity instanceof EntityLiving ? ((EntityLiving)entity).getItemInMainHand() : ItemStack.EMPTY;
                        if (!itemStack2.isEmpty() && itemStack2.hasName()) {
                            component4 = new ChatMessage("death.fell.finish.item", this.mob.getScoreboardDisplayName(), component, itemStack2.getDisplayName());
                        } else {
                            component4 = new ChatMessage("death.fell.finish", this.mob.getScoreboardDisplayName(), component);
                        }
                    } else {
                        component4 = new ChatMessage("death.fell.killer", this.mob.getScoreboardDisplayName());
                    }
                } else {
                    component4 = new ChatMessage("death.fell.accident." + this.getFallLocation(combatEntry), this.mob.getScoreboardDisplayName());
                }
            } else {
                component4 = combatEntry2.getSource().getLocalizedDeathMessage(this.mob);
            }

            return component4;
        }
    }

    @Nullable
    public EntityLiving getKiller() {
        EntityLiving livingEntity = null;
        EntityHuman player = null;
        float f = 0.0F;
        float g = 0.0F;

        for(CombatEntry combatEntry : this.entries) {
            if (combatEntry.getSource().getEntity() instanceof EntityHuman && (player == null || combatEntry.getDamage() > g)) {
                g = combatEntry.getDamage();
                player = (EntityHuman)combatEntry.getSource().getEntity();
            }

            if (combatEntry.getSource().getEntity() instanceof EntityLiving && (livingEntity == null || combatEntry.getDamage() > f)) {
                f = combatEntry.getDamage();
                livingEntity = (EntityLiving)combatEntry.getSource().getEntity();
            }
        }

        return (EntityLiving)(player != null && g >= f / 3.0F ? player : livingEntity);
    }

    @Nullable
    private CombatEntry getMostSignificantFall() {
        CombatEntry combatEntry = null;
        CombatEntry combatEntry2 = null;
        float f = 0.0F;
        float g = 0.0F;

        for(int i = 0; i < this.entries.size(); ++i) {
            CombatEntry combatEntry3 = this.entries.get(i);
            CombatEntry combatEntry4 = i > 0 ? this.entries.get(i - 1) : null;
            if ((combatEntry3.getSource() == DamageSource.FALL || combatEntry3.getSource() == DamageSource.OUT_OF_WORLD) && combatEntry3.getFallDistance() > 0.0F && (combatEntry == null || combatEntry3.getFallDistance() > g)) {
                if (i > 0) {
                    combatEntry = combatEntry4;
                } else {
                    combatEntry = combatEntry3;
                }

                g = combatEntry3.getFallDistance();
            }

            if (combatEntry3.getLocation() != null && (combatEntry2 == null || combatEntry3.getDamage() > f)) {
                combatEntry2 = combatEntry3;
                f = combatEntry3.getDamage();
            }
        }

        if (g > 5.0F && combatEntry != null) {
            return combatEntry;
        } else {
            return f > 5.0F && combatEntry2 != null ? combatEntry2 : null;
        }
    }

    private String getFallLocation(CombatEntry damageRecord) {
        return damageRecord.getLocation() == null ? "generic" : damageRecord.getLocation();
    }

    public boolean isTakingDamage() {
        this.recheckStatus();
        return this.takingDamage;
    }

    public boolean isInCombat() {
        this.recheckStatus();
        return this.inCombat;
    }

    public int getCombatDuration() {
        return this.inCombat ? this.mob.tickCount - this.combatStartTime : this.combatEndTime - this.combatStartTime;
    }

    private void resetPreparedStatus() {
        this.nextLocation = null;
    }

    public void recheckStatus() {
        int i = this.inCombat ? 300 : 100;
        if (this.takingDamage && (!this.mob.isAlive() || this.mob.tickCount - this.lastDamageTime > i)) {
            boolean bl = this.inCombat;
            this.takingDamage = false;
            this.inCombat = false;
            this.combatEndTime = this.mob.tickCount;
            if (bl) {
                this.mob.exitCombat();
            }

            this.entries.clear();
        }

    }

    public EntityLiving getMob() {
        return this.mob;
    }

    @Nullable
    public CombatEntry getLastEntry() {
        return this.entries.isEmpty() ? null : this.entries.get(this.entries.size() - 1);
    }

    public int getKillerId() {
        EntityLiving livingEntity = this.getKiller();
        return livingEntity == null ? -1 : livingEntity.getId();
    }
}
