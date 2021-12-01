package net.minecraft.world.entity.ai.village.poi;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.SystemUtils;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.SectionPosition;
import net.minecraft.util.VisibleForDebug;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class VillagePlaceSection {
    private static final Logger LOGGER = LogManager.getLogger();
    private final Short2ObjectMap<VillagePlaceRecord> records = new Short2ObjectOpenHashMap<>();
    private final Map<VillagePlaceType, Set<VillagePlaceRecord>> byType = Maps.newHashMap();
    private final Runnable setDirty;
    private boolean isValid;

    public static Codec<VillagePlaceSection> codec(Runnable updateListener) {
        return RecordCodecBuilder.create((instance) -> {
            return instance.group(RecordCodecBuilder.point(updateListener), Codec.BOOL.optionalFieldOf("Valid", Boolean.valueOf(false)).forGetter((poiSet) -> {
                return poiSet.isValid;
            }), VillagePlaceRecord.codec(updateListener).listOf().fieldOf("Records").forGetter((poiSet) -> {
                return ImmutableList.copyOf(poiSet.records.values());
            })).apply(instance, VillagePlaceSection::new);
        }).orElseGet(SystemUtils.prefix("Failed to read POI section: ", LOGGER::error), () -> {
            return new VillagePlaceSection(updateListener, false, ImmutableList.of());
        });
    }

    public VillagePlaceSection(Runnable updateListener) {
        this(updateListener, true, ImmutableList.of());
    }

    private VillagePlaceSection(Runnable updateListener, boolean valid, List<VillagePlaceRecord> pois) {
        this.setDirty = updateListener;
        this.isValid = valid;
        pois.forEach(this::add);
    }

    public Stream<VillagePlaceRecord> getRecords(Predicate<VillagePlaceType> predicate, VillagePlace.Occupancy occupationStatus) {
        return this.byType.entrySet().stream().filter((entry) -> {
            return predicate.test(entry.getKey());
        }).flatMap((entry) -> {
            return entry.getValue().stream();
        }).filter(occupationStatus.getTest());
    }

    public void add(BlockPosition pos, VillagePlaceType type) {
        if (this.add(new VillagePlaceRecord(pos, type, this.setDirty))) {
            LOGGER.debug("Added POI of type {} @ {}", () -> {
                return type;
            }, () -> {
                return pos;
            });
            this.setDirty.run();
        }

    }

    private boolean add(VillagePlaceRecord poi) {
        BlockPosition blockPos = poi.getPos();
        VillagePlaceType poiType = poi.getPoiType();
        short s = SectionPosition.sectionRelativePos(blockPos);
        VillagePlaceRecord poiRecord = this.records.get(s);
        if (poiRecord != null) {
            if (poiType.equals(poiRecord.getPoiType())) {
                return false;
            }

            SystemUtils.logAndPauseIfInIde("POI data mismatch: already registered at " + blockPos);
        }

        this.records.put(s, poi);
        this.byType.computeIfAbsent(poiType, (poiTypex) -> {
            return Sets.newHashSet();
        }).add(poi);
        return true;
    }

    public void remove(BlockPosition pos) {
        VillagePlaceRecord poiRecord = this.records.remove(SectionPosition.sectionRelativePos(pos));
        if (poiRecord == null) {
            LOGGER.error("POI data mismatch: never registered at {}", (Object)pos);
        } else {
            this.byType.get(poiRecord.getPoiType()).remove(poiRecord);
            LOGGER.debug("Removed POI of type {} @ {}", poiRecord::getPoiType, poiRecord::getPos);
            this.setDirty.run();
        }
    }

    /** @deprecated */
    @Deprecated
    @VisibleForDebug
    public int getFreeTickets(BlockPosition pos) {
        return this.getPoiRecord(pos).map(VillagePlaceRecord::getFreeTickets).orElse(0);
    }

    public boolean release(BlockPosition pos) {
        VillagePlaceRecord poiRecord = this.records.get(SectionPosition.sectionRelativePos(pos));
        if (poiRecord == null) {
            throw (IllegalStateException)SystemUtils.pauseInIde(new IllegalStateException("POI never registered at " + pos));
        } else {
            boolean bl = poiRecord.releaseTicket();
            this.setDirty.run();
            return bl;
        }
    }

    public boolean exists(BlockPosition pos, Predicate<VillagePlaceType> predicate) {
        return this.getType(pos).filter(predicate).isPresent();
    }

    public Optional<VillagePlaceType> getType(BlockPosition pos) {
        return this.getPoiRecord(pos).map(VillagePlaceRecord::getPoiType);
    }

    private Optional<VillagePlaceRecord> getPoiRecord(BlockPosition pos) {
        return Optional.ofNullable(this.records.get(SectionPosition.sectionRelativePos(pos)));
    }

    public void refresh(Consumer<BiConsumer<BlockPosition, VillagePlaceType>> consumer) {
        if (!this.isValid) {
            Short2ObjectMap<VillagePlaceRecord> short2ObjectMap = new Short2ObjectOpenHashMap<>(this.records);
            this.clear();
            consumer.accept((pos, poiType) -> {
                short s = SectionPosition.sectionRelativePos(pos);
                VillagePlaceRecord poiRecord = short2ObjectMap.computeIfAbsent(s, (sx) -> {
                    return new VillagePlaceRecord(pos, poiType, this.setDirty);
                });
                this.add(poiRecord);
            });
            this.isValid = true;
            this.setDirty.run();
        }

    }

    private void clear() {
        this.records.clear();
        this.byType.clear();
    }

    boolean isValid() {
        return this.isValid;
    }
}
