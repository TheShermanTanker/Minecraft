package net.minecraft.world.entity.ai.village.poi;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistry;
import net.minecraft.util.VisibleForDebug;

public class VillagePlaceRecord {
    private final BlockPosition pos;
    private final VillagePlaceType poiType;
    private int freeTickets;
    private final Runnable setDirty;

    public static Codec<VillagePlaceRecord> codec(Runnable updateListener) {
        return RecordCodecBuilder.create((instance) -> {
            return instance.group(BlockPosition.CODEC.fieldOf("pos").forGetter((poi) -> {
                return poi.pos;
            }), IRegistry.POINT_OF_INTEREST_TYPE.fieldOf("type").forGetter((poi) -> {
                return poi.poiType;
            }), Codec.INT.fieldOf("free_tickets").orElse(0).forGetter((poi) -> {
                return poi.freeTickets;
            }), RecordCodecBuilder.point(updateListener)).apply(instance, VillagePlaceRecord::new);
        });
    }

    private VillagePlaceRecord(BlockPosition pos, VillagePlaceType type, int freeTickets, Runnable updateListener) {
        this.pos = pos.immutableCopy();
        this.poiType = type;
        this.freeTickets = freeTickets;
        this.setDirty = updateListener;
    }

    public VillagePlaceRecord(BlockPosition pos, VillagePlaceType type, Runnable updateListener) {
        this(pos, type, type.getMaxTickets(), updateListener);
    }

    @Deprecated
    @VisibleForDebug
    public int getFreeTickets() {
        return this.freeTickets;
    }

    protected boolean acquireTicket() {
        if (this.freeTickets <= 0) {
            return false;
        } else {
            --this.freeTickets;
            this.setDirty.run();
            return true;
        }
    }

    protected boolean releaseTicket() {
        if (this.freeTickets >= this.poiType.getMaxTickets()) {
            return false;
        } else {
            ++this.freeTickets;
            this.setDirty.run();
            return true;
        }
    }

    public boolean hasSpace() {
        return this.freeTickets > 0;
    }

    public boolean isOccupied() {
        return this.freeTickets != this.poiType.getMaxTickets();
    }

    public BlockPosition getPos() {
        return this.pos;
    }

    public VillagePlaceType getPoiType() {
        return this.poiType;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else {
            return object != null && this.getClass() == object.getClass() ? Objects.equals(this.pos, ((VillagePlaceRecord)object).pos) : false;
        }
    }

    @Override
    public int hashCode() {
        return this.pos.hashCode();
    }
}
