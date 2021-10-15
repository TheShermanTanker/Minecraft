package net.minecraft.world.level.block;

import com.google.common.collect.Lists;
import com.mojang.math.PointGroupO;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import net.minecraft.SystemUtils;
import net.minecraft.core.EnumDirection;

public enum EnumBlockRotation {
    NONE(PointGroupO.IDENTITY),
    CLOCKWISE_90(PointGroupO.ROT_90_Y_NEG),
    CLOCKWISE_180(PointGroupO.ROT_180_FACE_XZ),
    COUNTERCLOCKWISE_90(PointGroupO.ROT_90_Y_POS);

    private final PointGroupO rotation;

    private EnumBlockRotation(PointGroupO directionTransformation) {
        this.rotation = directionTransformation;
    }

    public EnumBlockRotation getRotated(EnumBlockRotation rotation) {
        switch(rotation) {
        case CLOCKWISE_180:
            switch(this) {
            case NONE:
                return CLOCKWISE_180;
            case CLOCKWISE_90:
                return COUNTERCLOCKWISE_90;
            case CLOCKWISE_180:
                return NONE;
            case COUNTERCLOCKWISE_90:
                return CLOCKWISE_90;
            }
        case COUNTERCLOCKWISE_90:
            switch(this) {
            case NONE:
                return COUNTERCLOCKWISE_90;
            case CLOCKWISE_90:
                return NONE;
            case CLOCKWISE_180:
                return CLOCKWISE_90;
            case COUNTERCLOCKWISE_90:
                return CLOCKWISE_180;
            }
        case CLOCKWISE_90:
            switch(this) {
            case NONE:
                return CLOCKWISE_90;
            case CLOCKWISE_90:
                return CLOCKWISE_180;
            case CLOCKWISE_180:
                return COUNTERCLOCKWISE_90;
            case COUNTERCLOCKWISE_90:
                return NONE;
            }
        default:
            return this;
        }
    }

    public PointGroupO rotation() {
        return this.rotation;
    }

    public EnumDirection rotate(EnumDirection direction) {
        if (direction.getAxis() == EnumDirection.EnumAxis.Y) {
            return direction;
        } else {
            switch(this) {
            case CLOCKWISE_90:
                return direction.getClockWise();
            case CLOCKWISE_180:
                return direction.opposite();
            case COUNTERCLOCKWISE_90:
                return direction.getCounterClockWise();
            default:
                return direction;
            }
        }
    }

    public int rotate(int rotation, int fullTurn) {
        switch(this) {
        case CLOCKWISE_90:
            return (rotation + fullTurn / 4) % fullTurn;
        case CLOCKWISE_180:
            return (rotation + fullTurn / 2) % fullTurn;
        case COUNTERCLOCKWISE_90:
            return (rotation + fullTurn * 3 / 4) % fullTurn;
        default:
            return rotation;
        }
    }

    public static EnumBlockRotation getRandom(Random random) {
        return SystemUtils.getRandom(values(), random);
    }

    public static List<EnumBlockRotation> getShuffled(Random random) {
        List<EnumBlockRotation> list = Lists.newArrayList(values());
        Collections.shuffle(list, random);
        return list;
    }
}
