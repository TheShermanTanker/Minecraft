package net.minecraft.commands.arguments.coordinates;

import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.phys.Vec2F;
import net.minecraft.world.phys.Vec3D;

public interface IVectorPosition {
    Vec3D getPosition(CommandListenerWrapper source);

    Vec2F getRotation(CommandListenerWrapper source);

    default BlockPosition getBlockPos(CommandListenerWrapper source) {
        return new BlockPosition(this.getPosition(source));
    }

    boolean isXRelative();

    boolean isYRelative();

    boolean isZRelative();
}
