package net.minecraft.world.entity.ai.village;

public interface ReputationEvent {
    ReputationEvent ZOMBIE_VILLAGER_CURED = register("zombie_villager_cured");
    ReputationEvent GOLEM_KILLED = register("golem_killed");
    ReputationEvent VILLAGER_HURT = register("villager_hurt");
    ReputationEvent VILLAGER_KILLED = register("villager_killed");
    ReputationEvent TRADE = register("trade");

    static ReputationEvent register(String key) {
        return new ReputationEvent() {
            @Override
            public String toString() {
                return key;
            }
        };
    }
}
