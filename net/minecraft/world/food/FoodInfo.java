package net.minecraft.world.food;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import java.util.List;
import net.minecraft.world.effect.MobEffect;

public class FoodInfo {
    private final int nutrition;
    private final float saturationModifier;
    private final boolean isMeat;
    private final boolean canAlwaysEat;
    private final boolean fastFood;
    private final List<Pair<MobEffect, Float>> effects;

    FoodInfo(int hunger, float saturationModifier, boolean meat, boolean alwaysEdible, boolean snack, List<Pair<MobEffect, Float>> statusEffects) {
        this.nutrition = hunger;
        this.saturationModifier = saturationModifier;
        this.isMeat = meat;
        this.canAlwaysEat = alwaysEdible;
        this.fastFood = snack;
        this.effects = statusEffects;
    }

    public int getNutrition() {
        return this.nutrition;
    }

    public float getSaturationModifier() {
        return this.saturationModifier;
    }

    public boolean isMeat() {
        return this.isMeat;
    }

    public boolean canAlwaysEat() {
        return this.canAlwaysEat;
    }

    public boolean isFastFood() {
        return this.fastFood;
    }

    public List<Pair<MobEffect, Float>> getEffects() {
        return this.effects;
    }

    public static class Builder {
        private int nutrition;
        private float saturationModifier;
        private boolean isMeat;
        private boolean canAlwaysEat;
        private boolean fastFood;
        private final List<Pair<MobEffect, Float>> effects = Lists.newArrayList();

        public FoodInfo.Builder nutrition(int hunger) {
            this.nutrition = hunger;
            return this;
        }

        public FoodInfo.Builder saturationMod(float saturationModifier) {
            this.saturationModifier = saturationModifier;
            return this;
        }

        public FoodInfo.Builder meat() {
            this.isMeat = true;
            return this;
        }

        public FoodInfo.Builder alwaysEat() {
            this.canAlwaysEat = true;
            return this;
        }

        public FoodInfo.Builder fast() {
            this.fastFood = true;
            return this;
        }

        public FoodInfo.Builder effect(MobEffect effect, float chance) {
            this.effects.add(Pair.of(effect, chance));
            return this;
        }

        public FoodInfo build() {
            return new FoodInfo(this.nutrition, this.saturationModifier, this.isMeat, this.canAlwaysEat, this.fastFood, this.effects);
        }
    }
}
