package net.minecraft.world.item;

import com.google.common.collect.Maps;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.util.MathHelper;

public class ItemCooldown {
    public final Map<Item, ItemCooldown.Info> cooldowns = Maps.newHashMap();
    public int tickCount;

    public boolean hasCooldown(Item item) {
        return this.getCooldownPercent(item, 0.0F) > 0.0F;
    }

    public float getCooldownPercent(Item item, float partialTicks) {
        ItemCooldown.Info cooldownInstance = this.cooldowns.get(item);
        if (cooldownInstance != null) {
            float f = (float)(cooldownInstance.endTime - cooldownInstance.startTime);
            float g = (float)cooldownInstance.endTime - ((float)this.tickCount + partialTicks);
            return MathHelper.clamp(g / f, 0.0F, 1.0F);
        } else {
            return 0.0F;
        }
    }

    public void tick() {
        ++this.tickCount;
        if (!this.cooldowns.isEmpty()) {
            Iterator<Entry<Item, ItemCooldown.Info>> iterator = this.cooldowns.entrySet().iterator();

            while(iterator.hasNext()) {
                Entry<Item, ItemCooldown.Info> entry = iterator.next();
                if ((entry.getValue()).endTime <= this.tickCount) {
                    iterator.remove();
                    this.onCooldownEnded(entry.getKey());
                }
            }
        }

    }

    public void setCooldown(Item item, int duration) {
        this.cooldowns.put(item, new ItemCooldown.Info(this.tickCount, this.tickCount + duration));
        this.onCooldownStarted(item, duration);
    }

    public void removeCooldown(Item item) {
        this.cooldowns.remove(item);
        this.onCooldownEnded(item);
    }

    protected void onCooldownStarted(Item item, int duration) {
    }

    protected void onCooldownEnded(Item item) {
    }

    public static class Info {
        final int startTime;
        public final int endTime;

        Info(int startTick, int endTick) {
            this.startTime = startTick;
            this.endTime = endTick;
        }
    }
}
