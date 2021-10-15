package net.minecraft.util;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import net.minecraft.SharedConstants;
import net.minecraft.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class WeightedRandom {
    static final Logger LOGGER = LogManager.getLogger();

    public static int getTotalWeight(List<? extends WeightedRandom.WeightedRandomChoice> list) {
        long l = 0L;

        for(WeightedRandom.WeightedRandomChoice weighedRandomItem : list) {
            l += (long)weighedRandomItem.weight;
        }

        if (l > 2147483647L) {
            throw new IllegalArgumentException("Sum of weights must be <= 2147483647");
        } else {
            return (int)l;
        }
    }

    public static <T extends WeightedRandom.WeightedRandomChoice> Optional<T> getRandomItem(Random random, List<T> list, int weightSum) {
        if (weightSum < 0) {
            throw (IllegalArgumentException)SystemUtils.pauseInIde(new IllegalArgumentException("Negative total weight in getRandomItem"));
        } else if (weightSum == 0) {
            return Optional.empty();
        } else {
            int i = random.nextInt(weightSum);
            return getWeightedItem(list, i);
        }
    }

    public static <T extends WeightedRandom.WeightedRandomChoice> Optional<T> getWeightedItem(List<T> list, int weightMark) {
        for(T weighedRandomItem : list) {
            weightMark -= weighedRandomItem.weight;
            if (weightMark < 0) {
                return Optional.of(weighedRandomItem);
            }
        }

        return Optional.empty();
    }

    public static <T extends WeightedRandom.WeightedRandomChoice> Optional<T> getRandomItem(Random random, List<T> list) {
        return getRandomItem(random, list, getTotalWeight(list));
    }

    public static class WeightedRandomChoice {
        protected final int weight;

        public WeightedRandomChoice(int weight) {
            if (weight < 0) {
                throw (IllegalArgumentException)SystemUtils.pauseInIde(new IllegalArgumentException("Weight should be >= 0"));
            } else {
                if (weight == 0 && SharedConstants.IS_RUNNING_IN_IDE) {
                    WeightedRandom.LOGGER.warn("Found 0 weight, make sure this is intentional!");
                }

                this.weight = weight;
            }
        }
    }
}
