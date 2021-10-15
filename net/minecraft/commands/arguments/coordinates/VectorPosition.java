package net.minecraft.commands.arguments.coordinates;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.world.phys.Vec2F;
import net.minecraft.world.phys.Vec3D;

public class VectorPosition implements IVectorPosition {
    private final ArgumentParserPosition x;
    private final ArgumentParserPosition y;
    private final ArgumentParserPosition z;

    public VectorPosition(ArgumentParserPosition x, ArgumentParserPosition y, ArgumentParserPosition z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public Vec3D getPosition(CommandListenerWrapper source) {
        Vec3D vec3 = source.getPosition();
        return new Vec3D(this.x.get(vec3.x), this.y.get(vec3.y), this.z.get(vec3.z));
    }

    @Override
    public Vec2F getRotation(CommandListenerWrapper source) {
        Vec2F vec2 = source.getRotation();
        return new Vec2F((float)this.x.get((double)vec2.x), (float)this.y.get((double)vec2.y));
    }

    @Override
    public boolean isXRelative() {
        return this.x.isRelative();
    }

    @Override
    public boolean isYRelative() {
        return this.y.isRelative();
    }

    @Override
    public boolean isZRelative() {
        return this.z.isRelative();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (!(object instanceof VectorPosition)) {
            return false;
        } else {
            VectorPosition worldCoordinates = (VectorPosition)object;
            if (!this.x.equals(worldCoordinates.x)) {
                return false;
            } else {
                return !this.y.equals(worldCoordinates.y) ? false : this.z.equals(worldCoordinates.z);
            }
        }
    }

    public static VectorPosition parseInt(StringReader reader) throws CommandSyntaxException {
        int i = reader.getCursor();
        ArgumentParserPosition worldCoordinate = ArgumentParserPosition.parseInt(reader);
        if (reader.canRead() && reader.peek() == ' ') {
            reader.skip();
            ArgumentParserPosition worldCoordinate2 = ArgumentParserPosition.parseInt(reader);
            if (reader.canRead() && reader.peek() == ' ') {
                reader.skip();
                ArgumentParserPosition worldCoordinate3 = ArgumentParserPosition.parseInt(reader);
                return new VectorPosition(worldCoordinate, worldCoordinate2, worldCoordinate3);
            } else {
                reader.setCursor(i);
                throw ArgumentVec3.ERROR_NOT_COMPLETE.createWithContext(reader);
            }
        } else {
            reader.setCursor(i);
            throw ArgumentVec3.ERROR_NOT_COMPLETE.createWithContext(reader);
        }
    }

    public static VectorPosition parseDouble(StringReader reader, boolean centerIntegers) throws CommandSyntaxException {
        int i = reader.getCursor();
        ArgumentParserPosition worldCoordinate = ArgumentParserPosition.parseDouble(reader, centerIntegers);
        if (reader.canRead() && reader.peek() == ' ') {
            reader.skip();
            ArgumentParserPosition worldCoordinate2 = ArgumentParserPosition.parseDouble(reader, false);
            if (reader.canRead() && reader.peek() == ' ') {
                reader.skip();
                ArgumentParserPosition worldCoordinate3 = ArgumentParserPosition.parseDouble(reader, centerIntegers);
                return new VectorPosition(worldCoordinate, worldCoordinate2, worldCoordinate3);
            } else {
                reader.setCursor(i);
                throw ArgumentVec3.ERROR_NOT_COMPLETE.createWithContext(reader);
            }
        } else {
            reader.setCursor(i);
            throw ArgumentVec3.ERROR_NOT_COMPLETE.createWithContext(reader);
        }
    }

    public static VectorPosition absolute(double x, double y, double z) {
        return new VectorPosition(new ArgumentParserPosition(false, x), new ArgumentParserPosition(false, y), new ArgumentParserPosition(false, z));
    }

    public static VectorPosition absolute(Vec2F vec) {
        return new VectorPosition(new ArgumentParserPosition(false, (double)vec.x), new ArgumentParserPosition(false, (double)vec.y), new ArgumentParserPosition(true, 0.0D));
    }

    public static VectorPosition current() {
        return new VectorPosition(new ArgumentParserPosition(true, 0.0D), new ArgumentParserPosition(true, 0.0D), new ArgumentParserPosition(true, 0.0D));
    }

    @Override
    public int hashCode() {
        int i = this.x.hashCode();
        i = 31 * i + this.y.hashCode();
        return 31 * i + this.z.hashCode();
    }
}
