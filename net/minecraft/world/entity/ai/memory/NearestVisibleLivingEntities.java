package net.minecraft.world.entity.ai.memory;

import com.google.common.collect.Iterables;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.ai.sensing.Sensor;

public class NearestVisibleLivingEntities {
    private static final NearestVisibleLivingEntities EMPTY = new NearestVisibleLivingEntities();
    private final List<EntityLiving> nearbyEntities;
    private final Predicate<EntityLiving> lineOfSightTest;

    private NearestVisibleLivingEntities() {
        this.nearbyEntities = List.of();
        this.lineOfSightTest = (entity) -> {
            return false;
        };
    }

    public NearestVisibleLivingEntities(EntityLiving owner, List<EntityLiving> entities) {
        this.nearbyEntities = entities;
        Object2BooleanOpenHashMap<EntityLiving> object2BooleanOpenHashMap = new Object2BooleanOpenHashMap<>(entities.size());
        Predicate<EntityLiving> predicate = (entity) -> {
            return Sensor.isEntityTargetable(owner, entity);
        };
        this.lineOfSightTest = (entity) -> {
            return object2BooleanOpenHashMap.computeBooleanIfAbsent(entity, predicate);
        };
    }

    public static NearestVisibleLivingEntities empty() {
        return EMPTY;
    }

    public Optional<EntityLiving> findClosest(Predicate<EntityLiving> predicate) {
        for(EntityLiving livingEntity : this.nearbyEntities) {
            if (predicate.test(livingEntity) && this.lineOfSightTest.test(livingEntity)) {
                return Optional.of(livingEntity);
            }
        }

        return Optional.empty();
    }

    public Iterable<EntityLiving> findAll(Predicate<EntityLiving> predicate) {
        return Iterables.filter(this.nearbyEntities, (entity) -> {
            return predicate.test(entity) && this.lineOfSightTest.test(entity);
        });
    }

    public Stream<EntityLiving> find(Predicate<EntityLiving> predicate) {
        return this.nearbyEntities.stream().filter((entity) -> {
            return predicate.test(entity) && this.lineOfSightTest.test(entity);
        });
    }

    public boolean contains(EntityLiving entity) {
        return this.nearbyEntities.contains(entity) && this.lineOfSightTest.test(entity);
    }

    public boolean contains(Predicate<EntityLiving> predicate) {
        for(EntityLiving livingEntity : this.nearbyEntities) {
            if (predicate.test(livingEntity) && this.lineOfSightTest.test(livingEntity)) {
                return true;
            }
        }

        return false;
    }
}
