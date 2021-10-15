package net.minecraft.core;

import com.google.common.collect.Maps;
import com.mojang.math.Matrix4f;
import com.mojang.math.Quaternion;
import com.mojang.math.Transformation;
import com.mojang.math.Vector3fa;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BlockMath {
    private static final Logger LOGGER = LogManager.getLogger();
    public static final Map<EnumDirection, Transformation> VANILLA_UV_TRANSFORM_LOCAL_TO_GLOBAL = SystemUtils.make(Maps.newEnumMap(EnumDirection.class), (enumMap) -> {
        enumMap.put(EnumDirection.SOUTH, Transformation.identity());
        enumMap.put(EnumDirection.EAST, new Transformation((Vector3fa)null, Vector3fa.YP.rotationDegrees(90.0F), (Vector3fa)null, (Quaternion)null));
        enumMap.put(EnumDirection.WEST, new Transformation((Vector3fa)null, Vector3fa.YP.rotationDegrees(-90.0F), (Vector3fa)null, (Quaternion)null));
        enumMap.put(EnumDirection.NORTH, new Transformation((Vector3fa)null, Vector3fa.YP.rotationDegrees(180.0F), (Vector3fa)null, (Quaternion)null));
        enumMap.put(EnumDirection.UP, new Transformation((Vector3fa)null, Vector3fa.XP.rotationDegrees(-90.0F), (Vector3fa)null, (Quaternion)null));
        enumMap.put(EnumDirection.DOWN, new Transformation((Vector3fa)null, Vector3fa.XP.rotationDegrees(90.0F), (Vector3fa)null, (Quaternion)null));
    });
    public static final Map<EnumDirection, Transformation> VANILLA_UV_TRANSFORM_GLOBAL_TO_LOCAL = SystemUtils.make(Maps.newEnumMap(EnumDirection.class), (enumMap) -> {
        for(EnumDirection direction : EnumDirection.values()) {
            enumMap.put(direction, VANILLA_UV_TRANSFORM_LOCAL_TO_GLOBAL.get(direction).inverse());
        }

    });

    public static Transformation blockCenterToCorner(Transformation transformation) {
        Matrix4f matrix4f = Matrix4f.createTranslateMatrix(0.5F, 0.5F, 0.5F);
        matrix4f.multiply(transformation.getMatrix());
        matrix4f.multiply(Matrix4f.createTranslateMatrix(-0.5F, -0.5F, -0.5F));
        return new Transformation(matrix4f);
    }

    public static Transformation blockCornerToCenter(Transformation transformation) {
        Matrix4f matrix4f = Matrix4f.createTranslateMatrix(-0.5F, -0.5F, -0.5F);
        matrix4f.multiply(transformation.getMatrix());
        matrix4f.multiply(Matrix4f.createTranslateMatrix(0.5F, 0.5F, 0.5F));
        return new Transformation(matrix4f);
    }

    public static Transformation getUVLockTransform(Transformation transformation, EnumDirection direction, Supplier<String> supplier) {
        EnumDirection direction2 = EnumDirection.rotate(transformation.getMatrix(), direction);
        Transformation transformation2 = transformation.inverse();
        if (transformation2 == null) {
            LOGGER.warn(supplier.get());
            return new Transformation((Vector3fa)null, (Quaternion)null, new Vector3fa(0.0F, 0.0F, 0.0F), (Quaternion)null);
        } else {
            Transformation transformation3 = VANILLA_UV_TRANSFORM_GLOBAL_TO_LOCAL.get(direction).compose(transformation2).compose(VANILLA_UV_TRANSFORM_LOCAL_TO_GLOBAL.get(direction2));
            return blockCenterToCorner(transformation3);
        }
    }
}
