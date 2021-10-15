package net.minecraft.commands.arguments.coordinates;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Objects;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.util.MathHelper;
import net.minecraft.world.phys.Vec2F;
import net.minecraft.world.phys.Vec3D;

public class ArgumentVectorPosition implements IVectorPosition {
    public static final char PREFIX_LOCAL_COORDINATE = '^';
    private final double left;
    private final double up;
    private final double forwards;

    public ArgumentVectorPosition(double x, double y, double z) {
        this.left = x;
        this.up = y;
        this.forwards = z;
    }

    @Override
    public Vec3D getPosition(CommandListenerWrapper source) {
        Vec2F vec2 = source.getRotation();
        Vec3D vec3 = source.getAnchor().apply(source);
        float f = MathHelper.cos((vec2.y + 90.0F) * ((float)Math.PI / 180F));
        float g = MathHelper.sin((vec2.y + 90.0F) * ((float)Math.PI / 180F));
        float h = MathHelper.cos(-vec2.x * ((float)Math.PI / 180F));
        float i = MathHelper.sin(-vec2.x * ((float)Math.PI / 180F));
        float j = MathHelper.cos((-vec2.x + 90.0F) * ((float)Math.PI / 180F));
        float k = MathHelper.sin((-vec2.x + 90.0F) * ((float)Math.PI / 180F));
        Vec3D vec32 = new Vec3D((double)(f * h), (double)i, (double)(g * h));
        Vec3D vec33 = new Vec3D((double)(f * j), (double)k, (double)(g * j));
        Vec3D vec34 = vec32.cross(vec33).scale(-1.0D);
        double d = vec32.x * this.forwards + vec33.x * this.up + vec34.x * this.left;
        double e = vec32.y * this.forwards + vec33.y * this.up + vec34.y * this.left;
        double l = vec32.z * this.forwards + vec33.z * this.up + vec34.z * this.left;
        return new Vec3D(vec3.x + d, vec3.y + e, vec3.z + l);
    }

    @Override
    public Vec2F getRotation(CommandListenerWrapper source) {
        return Vec2F.ZERO;
    }

    @Override
    public boolean isXRelative() {
        return true;
    }

    @Override
    public boolean isYRelative() {
        return true;
    }

    @Override
    public boolean isZRelative() {
        return true;
    }

    public static ArgumentVectorPosition parse(StringReader reader) throws CommandSyntaxException {
        int i = reader.getCursor();
        double d = readDouble(reader, i);
        if (reader.canRead() && reader.peek() == ' ') {
            reader.skip();
            double e = readDouble(reader, i);
            if (reader.canRead() && reader.peek() == ' ') {
                reader.skip();
                double f = readDouble(reader, i);
                return new ArgumentVectorPosition(d, e, f);
            } else {
                reader.setCursor(i);
                throw ArgumentVec3.ERROR_NOT_COMPLETE.createWithContext(reader);
            }
        } else {
            reader.setCursor(i);
            throw ArgumentVec3.ERROR_NOT_COMPLETE.createWithContext(reader);
        }
    }

    private static double readDouble(StringReader reader, int startingCursorPos) throws CommandSyntaxException {
        if (!reader.canRead()) {
            throw ArgumentParserPosition.ERROR_EXPECTED_DOUBLE.createWithContext(reader);
        } else if (reader.peek() != '^') {
            reader.setCursor(startingCursorPos);
            throw ArgumentVec3.ERROR_MIXED_TYPE.createWithContext(reader);
        } else {
            reader.skip();
            return reader.canRead() && reader.peek() != ' ' ? reader.readDouble() : 0.0D;
        }
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (!(object instanceof ArgumentVectorPosition)) {
            return false;
        } else {
            ArgumentVectorPosition localCoordinates = (ArgumentVectorPosition)object;
            return this.left == localCoordinates.left && this.up == localCoordinates.up && this.forwards == localCoordinates.forwards;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.left, this.up, this.forwards);
    }
}
