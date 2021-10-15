package net.minecraft.world.level.storage.loot.providers.number;

import net.minecraft.world.level.storage.loot.LootItemUser;
import net.minecraft.world.level.storage.loot.LootTableInfo;

public interface NumberProvider extends LootItemUser {
    float getFloat(LootTableInfo context);

    default int getInt(LootTableInfo context) {
        return Math.round(this.getFloat(context));
    }

    LootNumberProviderType getType();
}
