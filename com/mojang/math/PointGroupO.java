package com.mojang.math;

import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.booleans.BooleanArrayList;
import it.unimi.dsi.fastutil.booleans.BooleanList;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.SystemUtils;
import net.minecraft.core.BlockPropertyJigsawOrientation;
import net.minecraft.core.EnumDirection;
import net.minecraft.util.INamable;

public enum PointGroupO implements INamable {
    IDENTITY("identity", PointGroupS.P123, false, false, false),
    ROT_180_FACE_XY("rot_180_face_xy", PointGroupS.P123, true, true, false),
    ROT_180_FACE_XZ("rot_180_face_xz", PointGroupS.P123, true, false, true),
    ROT_180_FACE_YZ("rot_180_face_yz", PointGroupS.P123, false, true, true),
    ROT_120_NNN("rot_120_nnn", PointGroupS.P231, false, false, false),
    ROT_120_NNP("rot_120_nnp", PointGroupS.P312, true, false, true),
    ROT_120_NPN("rot_120_npn", PointGroupS.P312, false, true, true),
    ROT_120_NPP("rot_120_npp", PointGroupS.P231, true, false, true),
    ROT_120_PNN("rot_120_pnn", PointGroupS.P312, true, true, false),
    ROT_120_PNP("rot_120_pnp", PointGroupS.P231, true, true, false),
    ROT_120_PPN("rot_120_ppn", PointGroupS.P231, false, true, true),
    ROT_120_PPP("rot_120_ppp", PointGroupS.P312, false, false, false),
    ROT_180_EDGE_XY_NEG("rot_180_edge_xy_neg", PointGroupS.P213, true, true, true),
    ROT_180_EDGE_XY_POS("rot_180_edge_xy_pos", PointGroupS.P213, false, false, true),
    ROT_180_EDGE_XZ_NEG("rot_180_edge_xz_neg", PointGroupS.P321, true, true, true),
    ROT_180_EDGE_XZ_POS("rot_180_edge_xz_pos", PointGroupS.P321, false, true, false),
    ROT_180_EDGE_YZ_NEG("rot_180_edge_yz_neg", PointGroupS.P132, true, true, true),
    ROT_180_EDGE_YZ_POS("rot_180_edge_yz_pos", PointGroupS.P132, true, false, false),
    ROT_90_X_NEG("rot_90_x_neg", PointGroupS.P132, false, false, true),
    ROT_90_X_POS("rot_90_x_pos", PointGroupS.P132, false, true, false),
    ROT_90_Y_NEG("rot_90_y_neg", PointGroupS.P321, true, false, false),
    ROT_90_Y_POS("rot_90_y_pos", PointGroupS.P321, false, false, true),
    ROT_90_Z_NEG("rot_90_z_neg", PointGroupS.P213, false, true, false),
    ROT_90_Z_POS("rot_90_z_pos", PointGroupS.P213, true, false, false),
    INVERSION("inversion", PointGroupS.P123, true, true, true),
    INVERT_X("invert_x", PointGroupS.P123, true, false, false),
    INVERT_Y("invert_y", PointGroupS.P123, false, true, false),
    INVERT_Z("invert_z", PointGroupS.P123, false, false, true),
    ROT_60_REF_NNN("rot_60_ref_nnn", PointGroupS.P312, true, true, true),
    ROT_60_REF_NNP("rot_60_ref_nnp", PointGroupS.P231, true, false, false),
    ROT_60_REF_NPN("rot_60_ref_npn", PointGroupS.P231, false, false, true),
    ROT_60_REF_NPP("rot_60_ref_npp", PointGroupS.P312, false, false, true),
    ROT_60_REF_PNN("rot_60_ref_pnn", PointGroupS.P231, false, true, false),
    ROT_60_REF_PNP("rot_60_ref_pnp", PointGroupS.P312, true, false, false),
    ROT_60_REF_PPN("rot_60_ref_ppn", PointGroupS.P312, false, true, false),
    ROT_60_REF_PPP("rot_60_ref_ppp", PointGroupS.P231, true, true, true),
    SWAP_XY("swap_xy", PointGroupS.P213, false, false, false),
    SWAP_YZ("swap_yz", PointGroupS.P132, false, false, false),
    SWAP_XZ("swap_xz", PointGroupS.P321, false, false, false),
    SWAP_NEG_XY("swap_neg_xy", PointGroupS.P213, true, true, false),
    SWAP_NEG_YZ("swap_neg_yz", PointGroupS.P132, false, true, true),
    SWAP_NEG_XZ("swap_neg_xz", PointGroupS.P321, true, false, true),
    ROT_90_REF_X_NEG("rot_90_ref_x_neg", PointGroupS.P132, true, false, true),
    ROT_90_REF_X_POS("rot_90_ref_x_pos", PointGroupS.P132, true, true, false),
    ROT_90_REF_Y_NEG("rot_90_ref_y_neg", PointGroupS.P321, true, true, false),
    ROT_90_REF_Y_POS("rot_90_ref_y_pos", PointGroupS.P321, false, true, true),
    ROT_90_REF_Z_NEG("rot_90_ref_z_neg", PointGroupS.P213, false, true, true),
    ROT_90_REF_Z_POS("rot_90_ref_z_pos", PointGroupS.P213, true, false, true);

    private final Matrix3f transformation;
    private final String name;
    @Nullable
    private Map<EnumDirection, EnumDirection> rotatedDirections;
    private final boolean invertX;
    private final boolean invertY;
    private final boolean invertZ;
    private final PointGroupS permutation;
    private static final PointGroupO[][] cayleyTable = SystemUtils.make(new PointGroupO[values().length][values().length], (octahedralGroups) -> {
        Map<Pair<PointGroupS, BooleanList>, PointGroupO> map = Arrays.stream(values()).collect(Collectors.toMap((octahedralGroupx) -> {
            return Pair.of(octahedralGroupx.permutation, octahedralGroupx.packInversions());
        }, (octahedralGroupx) -> {
            return octahedralGroupx;
        }));

        for(PointGroupO octahedralGroup : values()) {
            for(PointGroupO octahedralGroup2 : values()) {
                BooleanList booleanList = octahedralGroup.packInversions();
                BooleanList booleanList2 = octahedralGroup2.packInversions();
                PointGroupS symmetricGroup3 = octahedralGroup2.permutation.compose(octahedralGroup.permutation);
                BooleanArrayList booleanArrayList = new BooleanArrayList(3);

                for(int i = 0; i < 3; ++i) {
                    booleanArrayList.add(booleanList.getBoolean(i) ^ booleanList2.getBoolean(octahedralGroup.permutation.permutation(i)));
                }

                octahedralGroups[octahedralGroup.ordinal()][octahedralGroup2.ordinal()] = map.get(Pair.of(symmetricGroup3, booleanArrayList));
            }
        }

    });
    private static final PointGroupO[] inverseTable = Arrays.stream(values()).map((octahedralGroup) -> {
        return Arrays.stream(values()).filter((octahedralGroup2) -> {
            return octahedralGroup.compose(octahedralGroup2) == IDENTITY;
        }).findAny().get();
    }).toArray((i) -> {
        return new PointGroupO[i];
    });

    private PointGroupO(String name, PointGroupS axisTransformation, boolean flipX, boolean flipY, boolean flipZ) {
        this.name = name;
        this.invertX = flipX;
        this.invertY = flipY;
        this.invertZ = flipZ;
        this.permutation = axisTransformation;
        this.transformation = new Matrix3f();
        this.transformation.m00 = flipX ? -1.0F : 1.0F;
        this.transformation.m11 = flipY ? -1.0F : 1.0F;
        this.transformation.m22 = flipZ ? -1.0F : 1.0F;
        this.transformation.mul(axisTransformation.transformation());
    }

    private BooleanList packInversions() {
        return new BooleanArrayList(new boolean[]{this.invertX, this.invertY, this.invertZ});
    }

    public PointGroupO compose(PointGroupO transformation) {
        return cayleyTable[this.ordinal()][transformation.ordinal()];
    }

    public PointGroupO inverse() {
        return inverseTable[this.ordinal()];
    }

    public Matrix3f transformation() {
        return this.transformation.copy();
    }

    @Override
    public String toString() {
        return this.name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public EnumDirection rotate(EnumDirection direction) {
        if (this.rotatedDirections == null) {
            this.rotatedDirections = Maps.newEnumMap(EnumDirection.class);

            for(EnumDirection direction2 : EnumDirection.values()) {
                EnumDirection.EnumAxis axis = direction2.getAxis();
                EnumDirection.EnumAxisDirection axisDirection = direction2.getAxisDirection();
                EnumDirection.EnumAxis axis2 = EnumDirection.EnumAxis.values()[this.permutation.permutation(axis.ordinal())];
                EnumDirection.EnumAxisDirection axisDirection2 = this.inverts(axis2) ? axisDirection.opposite() : axisDirection;
                EnumDirection direction3 = EnumDirection.fromAxisAndDirection(axis2, axisDirection2);
                this.rotatedDirections.put(direction2, direction3);
            }
        }

        return this.rotatedDirections.get(direction);
    }

    public boolean inverts(EnumDirection.EnumAxis axis) {
        switch(axis) {
        case X:
            return this.invertX;
        case Y:
            return this.invertY;
        case Z:
        default:
            return this.invertZ;
        }
    }

    public BlockPropertyJigsawOrientation rotate(BlockPropertyJigsawOrientation orientation) {
        return BlockPropertyJigsawOrientation.fromFrontAndTop(this.rotate(orientation.front()), this.rotate(orientation.top()));
    }
}
