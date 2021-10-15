package net.minecraft.world.level.block;

import com.mojang.math.PointGroupO;
import net.minecraft.core.EnumDirection;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;

public enum EnumBlockMirror {
    NONE(new ChatMessage("mirror.none"), PointGroupO.IDENTITY),
    LEFT_RIGHT(new ChatMessage("mirror.left_right"), PointGroupO.INVERT_Z),
    FRONT_BACK(new ChatMessage("mirror.front_back"), PointGroupO.INVERT_X);

    private final IChatBaseComponent symbol;
    private final PointGroupO rotation;

    private EnumBlockMirror(IChatBaseComponent name, PointGroupO directionTransformation) {
        this.symbol = name;
        this.rotation = directionTransformation;
    }

    public int mirror(int rotation, int fullTurn) {
        int i = fullTurn / 2;
        int j = rotation > i ? rotation - fullTurn : rotation;
        switch(this) {
        case FRONT_BACK:
            return (fullTurn - j) % fullTurn;
        case LEFT_RIGHT:
            return (i - j + fullTurn) % fullTurn;
        default:
            return rotation;
        }
    }

    public EnumBlockRotation getRotation(EnumDirection direction) {
        EnumDirection.EnumAxis axis = direction.getAxis();
        return (this != LEFT_RIGHT || axis != EnumDirection.EnumAxis.Z) && (this != FRONT_BACK || axis != EnumDirection.EnumAxis.X) ? EnumBlockRotation.NONE : EnumBlockRotation.CLOCKWISE_180;
    }

    public EnumDirection mirror(EnumDirection direction) {
        if (this == FRONT_BACK && direction.getAxis() == EnumDirection.EnumAxis.X) {
            return direction.opposite();
        } else {
            return this == LEFT_RIGHT && direction.getAxis() == EnumDirection.EnumAxis.Z ? direction.opposite() : direction;
        }
    }

    public PointGroupO rotation() {
        return this.rotation;
    }

    public IChatBaseComponent symbol() {
        return this.symbol;
    }
}
