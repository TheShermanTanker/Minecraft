package net.minecraft.world.level.portal;

import java.util.Comparator;
import java.util.Optional;
import net.minecraft.BlockUtil;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.server.level.TicketType;
import net.minecraft.server.level.WorldServer;
import net.minecraft.util.MathHelper;
import net.minecraft.world.entity.ai.village.poi.VillagePlace;
import net.minecraft.world.entity.ai.village.poi.VillagePlaceRecord;
import net.minecraft.world.entity.ai.village.poi.VillagePlaceType;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.block.BlockPortal;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.levelgen.HeightMap;

public class PortalTravelAgent {
    private static final int TICKET_RADIUS = 3;
    private static final int SEARCH_RADIUS = 128;
    private static final int CREATE_RADIUS = 16;
    private static final int FRAME_HEIGHT = 5;
    private static final int FRAME_WIDTH = 4;
    private static final int FRAME_BOX = 3;
    private static final int FRAME_HEIGHT_START = -1;
    private static final int FRAME_HEIGHT_END = 4;
    private static final int FRAME_WIDTH_START = -1;
    private static final int FRAME_WIDTH_END = 3;
    private static final int FRAME_BOX_START = -1;
    private static final int FRAME_BOX_END = 2;
    private static final int NOTHING_FOUND = -1;
    private final WorldServer level;

    public PortalTravelAgent(WorldServer world) {
        this.level = world;
    }

    public Optional<BlockUtil.Rectangle> findPortal(BlockPosition destPos, boolean destIsNether) {
        VillagePlace poiManager = this.level.getPoiManager();
        int i = destIsNether ? 16 : 128;
        poiManager.ensureLoadedAndValid(this.level, destPos, i);
        Optional<VillagePlaceRecord> optional = poiManager.getInSquare((poiType) -> {
            return poiType == VillagePlaceType.NETHER_PORTAL;
        }, destPos, i, VillagePlace.Occupancy.ANY).sorted(Comparator.comparingDouble((poiRecord) -> {
            return poiRecord.getPos().distSqr(destPos);
        }).thenComparingInt((poiRecord) -> {
            return poiRecord.getPos().getY();
        })).filter((poiRecord) -> {
            return this.level.getType(poiRecord.getPos()).hasProperty(BlockProperties.HORIZONTAL_AXIS);
        }).findFirst();
        return optional.map((poiRecord) -> {
            BlockPosition blockPos = poiRecord.getPos();
            this.level.getChunkSource().addTicket(TicketType.PORTAL, new ChunkCoordIntPair(blockPos), 3, blockPos);
            IBlockData blockState = this.level.getType(blockPos);
            return BlockUtil.getLargestRectangleAround(blockPos, blockState.get(BlockProperties.HORIZONTAL_AXIS), 21, EnumDirection.EnumAxis.Y, 21, (blockPosx) -> {
                return this.level.getType(blockPosx) == blockState;
            });
        });
    }

    public Optional<BlockUtil.Rectangle> createPortal(BlockPosition blockPos, EnumDirection.EnumAxis axis) {
        EnumDirection direction = EnumDirection.get(EnumDirection.EnumAxisDirection.POSITIVE, axis);
        double d = -1.0D;
        BlockPosition blockPos2 = null;
        double e = -1.0D;
        BlockPosition blockPos3 = null;
        WorldBorder worldBorder = this.level.getWorldBorder();
        int i = Math.min(this.level.getMaxBuildHeight(), this.level.getMinBuildHeight() + this.level.getLogicalHeight()) - 1;
        BlockPosition.MutableBlockPosition mutableBlockPos = blockPos.mutable();

        for(BlockPosition.MutableBlockPosition mutableBlockPos2 : BlockPosition.spiralAround(blockPos, 16, EnumDirection.EAST, EnumDirection.SOUTH)) {
            int j = Math.min(i, this.level.getHeight(HeightMap.Type.MOTION_BLOCKING, mutableBlockPos2.getX(), mutableBlockPos2.getZ()));
            int k = 1;
            if (worldBorder.isWithinBounds(mutableBlockPos2) && worldBorder.isWithinBounds(mutableBlockPos2.move(direction, 1))) {
                mutableBlockPos2.move(direction.opposite(), 1);

                for(int l = j; l >= this.level.getMinBuildHeight(); --l) {
                    mutableBlockPos2.setY(l);
                    if (this.level.isEmpty(mutableBlockPos2)) {
                        int m;
                        for(m = l; l > this.level.getMinBuildHeight() && this.level.isEmpty(mutableBlockPos2.move(EnumDirection.DOWN)); --l) {
                        }

                        if (l + 4 <= i) {
                            int n = m - l;
                            if (n <= 0 || n >= 3) {
                                mutableBlockPos2.setY(l);
                                if (this.canHostFrame(mutableBlockPos2, mutableBlockPos, direction, 0)) {
                                    double f = blockPos.distSqr(mutableBlockPos2);
                                    if (this.canHostFrame(mutableBlockPos2, mutableBlockPos, direction, -1) && this.canHostFrame(mutableBlockPos2, mutableBlockPos, direction, 1) && (d == -1.0D || d > f)) {
                                        d = f;
                                        blockPos2 = mutableBlockPos2.immutableCopy();
                                    }

                                    if (d == -1.0D && (e == -1.0D || e > f)) {
                                        e = f;
                                        blockPos3 = mutableBlockPos2.immutableCopy();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (d == -1.0D && e != -1.0D) {
            blockPos2 = blockPos3;
            d = e;
        }

        if (d == -1.0D) {
            int o = Math.max(this.level.getMinBuildHeight() - -1, 70);
            int p = i - 9;
            if (p < o) {
                return Optional.empty();
            }

            blockPos2 = (new BlockPosition(blockPos.getX(), MathHelper.clamp(blockPos.getY(), o, p), blockPos.getZ())).immutableCopy();
            EnumDirection direction2 = direction.getClockWise();
            if (!worldBorder.isWithinBounds(blockPos2)) {
                return Optional.empty();
            }

            for(int q = -1; q < 2; ++q) {
                for(int r = 0; r < 2; ++r) {
                    for(int s = -1; s < 3; ++s) {
                        IBlockData blockState = s < 0 ? Blocks.OBSIDIAN.getBlockData() : Blocks.AIR.getBlockData();
                        mutableBlockPos.setWithOffset(blockPos2, r * direction.getAdjacentX() + q * direction2.getAdjacentX(), s, r * direction.getAdjacentZ() + q * direction2.getAdjacentZ());
                        this.level.setTypeUpdate(mutableBlockPos, blockState);
                    }
                }
            }
        }

        for(int t = -1; t < 3; ++t) {
            for(int u = -1; u < 4; ++u) {
                if (t == -1 || t == 2 || u == -1 || u == 3) {
                    mutableBlockPos.setWithOffset(blockPos2, t * direction.getAdjacentX(), u, t * direction.getAdjacentZ());
                    this.level.setTypeAndData(mutableBlockPos, Blocks.OBSIDIAN.getBlockData(), 3);
                }
            }
        }

        IBlockData blockState2 = Blocks.NETHER_PORTAL.getBlockData().set(BlockPortal.AXIS, axis);

        for(int v = 0; v < 2; ++v) {
            for(int w = 0; w < 3; ++w) {
                mutableBlockPos.setWithOffset(blockPos2, v * direction.getAdjacentX(), w, v * direction.getAdjacentZ());
                this.level.setTypeAndData(mutableBlockPos, blockState2, 18);
            }
        }

        return Optional.of(new BlockUtil.Rectangle(blockPos2.immutableCopy(), 2, 3));
    }

    private boolean canHostFrame(BlockPosition pos, BlockPosition.MutableBlockPosition temp, EnumDirection portalDirection, int distanceOrthogonalToPortal) {
        EnumDirection direction = portalDirection.getClockWise();

        for(int i = -1; i < 3; ++i) {
            for(int j = -1; j < 4; ++j) {
                temp.setWithOffset(pos, portalDirection.getAdjacentX() * i + direction.getAdjacentX() * distanceOrthogonalToPortal, j, portalDirection.getAdjacentZ() * i + direction.getAdjacentZ() * distanceOrthogonalToPortal);
                if (j < 0 && !this.level.getType(temp).getMaterial().isBuildable()) {
                    return false;
                }

                if (j >= 0 && !this.level.isEmpty(temp)) {
                    return false;
                }
            }
        }

        return true;
    }
}
