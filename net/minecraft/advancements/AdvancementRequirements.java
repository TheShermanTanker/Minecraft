package net.minecraft.advancements;

import java.util.Collection;

public interface AdvancementRequirements {
    AdvancementRequirements AND = (collection) -> {
        String[][] strings = new String[collection.size()][];
        int i = 0;

        for(String string : collection) {
            strings[i++] = new String[]{string};
        }

        return strings;
    };
    AdvancementRequirements OR = (collection) -> {
        return new String[][]{collection.toArray(new String[0])};
    };

    String[][] createRequirements(Collection<String> criteriaNames);
}
