package net.minecraft.world.level.block;

import com.google.common.collect.Lists;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockPropertyTrackPosition;

public class MinecartTrackLogic {
    private final World level;
    private final BlockPosition pos;
    private final BlockMinecartTrackAbstract block;
    private IBlockData state;
    private final boolean isStraight;
    private final List<BlockPosition> connections = Lists.newArrayList();

    public MinecartTrackLogic(World world, BlockPosition pos, IBlockData state) {
        this.level = world;
        this.pos = pos;
        this.state = state;
        this.block = (BlockMinecartTrackAbstract)state.getBlock();
        BlockPropertyTrackPosition railShape = state.get(this.block.getShapeProperty());
        this.isStraight = this.block.isStraight();
        this.updateConnections(railShape);
    }

    public List<BlockPosition> getConnections() {
        return this.connections;
    }

    private void updateConnections(BlockPropertyTrackPosition shape) {
        this.connections.clear();
        switch(shape) {
        case NORTH_SOUTH:
            this.connections.add(this.pos.north());
            this.connections.add(this.pos.south());
            break;
        case EAST_WEST:
            this.connections.add(this.pos.west());
            this.connections.add(this.pos.east());
            break;
        case ASCENDING_EAST:
            this.connections.add(this.pos.west());
            this.connections.add(this.pos.east().above());
            break;
        case ASCENDING_WEST:
            this.connections.add(this.pos.west().above());
            this.connections.add(this.pos.east());
            break;
        case ASCENDING_NORTH:
            this.connections.add(this.pos.north().above());
            this.connections.add(this.pos.south());
            break;
        case ASCENDING_SOUTH:
            this.connections.add(this.pos.north());
            this.connections.add(this.pos.south().above());
            break;
        case SOUTH_EAST:
            this.connections.add(this.pos.east());
            this.connections.add(this.pos.south());
            break;
        case SOUTH_WEST:
            this.connections.add(this.pos.west());
            this.connections.add(this.pos.south());
            break;
        case NORTH_WEST:
            this.connections.add(this.pos.west());
            this.connections.add(this.pos.north());
            break;
        case NORTH_EAST:
            this.connections.add(this.pos.east());
            this.connections.add(this.pos.north());
        }

    }

    private void removeSoftConnections() {
        for(int i = 0; i < this.connections.size(); ++i) {
            MinecartTrackLogic railState = this.getRail(this.connections.get(i));
            if (railState != null && railState.connectsTo(this)) {
                this.connections.set(i, railState.pos);
            } else {
                this.connections.remove(i--);
            }
        }

    }

    private boolean hasRail(BlockPosition pos) {
        return BlockMinecartTrackAbstract.isRail(this.level, pos) || BlockMinecartTrackAbstract.isRail(this.level, pos.above()) || BlockMinecartTrackAbstract.isRail(this.level, pos.below());
    }

    @Nullable
    private MinecartTrackLogic getRail(BlockPosition pos) {
        IBlockData blockState = this.level.getType(pos);
        if (BlockMinecartTrackAbstract.isRail(blockState)) {
            return new MinecartTrackLogic(this.level, pos, blockState);
        } else {
            BlockPosition blockPos = pos.above();
            blockState = this.level.getType(blockPos);
            if (BlockMinecartTrackAbstract.isRail(blockState)) {
                return new MinecartTrackLogic(this.level, blockPos, blockState);
            } else {
                blockPos = pos.below();
                blockState = this.level.getType(blockPos);
                return BlockMinecartTrackAbstract.isRail(blockState) ? new MinecartTrackLogic(this.level, blockPos, blockState) : null;
            }
        }
    }

    private boolean connectsTo(MinecartTrackLogic other) {
        return this.hasConnection(other.pos);
    }

    private boolean hasConnection(BlockPosition pos) {
        for(int i = 0; i < this.connections.size(); ++i) {
            BlockPosition blockPos = this.connections.get(i);
            if (blockPos.getX() == pos.getX() && blockPos.getZ() == pos.getZ()) {
                return true;
            }
        }

        return false;
    }

    protected int countPotentialConnections() {
        int i = 0;

        for(EnumDirection direction : EnumDirection.EnumDirectionLimit.HORIZONTAL) {
            if (this.hasRail(this.pos.relative(direction))) {
                ++i;
            }
        }

        return i;
    }

    private boolean canConnectTo(MinecartTrackLogic placementHelper) {
        return this.connectsTo(placementHelper) || this.connections.size() != 2;
    }

    private void connectTo(MinecartTrackLogic placementHelper) {
        this.connections.add(placementHelper.pos);
        BlockPosition blockPos = this.pos.north();
        BlockPosition blockPos2 = this.pos.south();
        BlockPosition blockPos3 = this.pos.west();
        BlockPosition blockPos4 = this.pos.east();
        boolean bl = this.hasConnection(blockPos);
        boolean bl2 = this.hasConnection(blockPos2);
        boolean bl3 = this.hasConnection(blockPos3);
        boolean bl4 = this.hasConnection(blockPos4);
        BlockPropertyTrackPosition railShape = null;
        if (bl || bl2) {
            railShape = BlockPropertyTrackPosition.NORTH_SOUTH;
        }

        if (bl3 || bl4) {
            railShape = BlockPropertyTrackPosition.EAST_WEST;
        }

        if (!this.isStraight) {
            if (bl2 && bl4 && !bl && !bl3) {
                railShape = BlockPropertyTrackPosition.SOUTH_EAST;
            }

            if (bl2 && bl3 && !bl && !bl4) {
                railShape = BlockPropertyTrackPosition.SOUTH_WEST;
            }

            if (bl && bl3 && !bl2 && !bl4) {
                railShape = BlockPropertyTrackPosition.NORTH_WEST;
            }

            if (bl && bl4 && !bl2 && !bl3) {
                railShape = BlockPropertyTrackPosition.NORTH_EAST;
            }
        }

        if (railShape == BlockPropertyTrackPosition.NORTH_SOUTH) {
            if (BlockMinecartTrackAbstract.isRail(this.level, blockPos.above())) {
                railShape = BlockPropertyTrackPosition.ASCENDING_NORTH;
            }

            if (BlockMinecartTrackAbstract.isRail(this.level, blockPos2.above())) {
                railShape = BlockPropertyTrackPosition.ASCENDING_SOUTH;
            }
        }

        if (railShape == BlockPropertyTrackPosition.EAST_WEST) {
            if (BlockMinecartTrackAbstract.isRail(this.level, blockPos4.above())) {
                railShape = BlockPropertyTrackPosition.ASCENDING_EAST;
            }

            if (BlockMinecartTrackAbstract.isRail(this.level, blockPos3.above())) {
                railShape = BlockPropertyTrackPosition.ASCENDING_WEST;
            }
        }

        if (railShape == null) {
            railShape = BlockPropertyTrackPosition.NORTH_SOUTH;
        }

        this.state = this.state.set(this.block.getShapeProperty(), railShape);
        this.level.setTypeAndData(this.pos, this.state, 3);
    }

    private boolean hasNeighborRail(BlockPosition pos) {
        MinecartTrackLogic railState = this.getRail(pos);
        if (railState == null) {
            return false;
        } else {
            railState.removeSoftConnections();
            return railState.canConnectTo(this);
        }
    }

    public MinecartTrackLogic place(boolean powered, boolean forceUpdate, BlockPropertyTrackPosition railShape) {
        BlockPosition blockPos = this.pos.north();
        BlockPosition blockPos2 = this.pos.south();
        BlockPosition blockPos3 = this.pos.west();
        BlockPosition blockPos4 = this.pos.east();
        boolean bl = this.hasNeighborRail(blockPos);
        boolean bl2 = this.hasNeighborRail(blockPos2);
        boolean bl3 = this.hasNeighborRail(blockPos3);
        boolean bl4 = this.hasNeighborRail(blockPos4);
        BlockPropertyTrackPosition railShape2 = null;
        boolean bl5 = bl || bl2;
        boolean bl6 = bl3 || bl4;
        if (bl5 && !bl6) {
            railShape2 = BlockPropertyTrackPosition.NORTH_SOUTH;
        }

        if (bl6 && !bl5) {
            railShape2 = BlockPropertyTrackPosition.EAST_WEST;
        }

        boolean bl7 = bl2 && bl4;
        boolean bl8 = bl2 && bl3;
        boolean bl9 = bl && bl4;
        boolean bl10 = bl && bl3;
        if (!this.isStraight) {
            if (bl7 && !bl && !bl3) {
                railShape2 = BlockPropertyTrackPosition.SOUTH_EAST;
            }

            if (bl8 && !bl && !bl4) {
                railShape2 = BlockPropertyTrackPosition.SOUTH_WEST;
            }

            if (bl10 && !bl2 && !bl4) {
                railShape2 = BlockPropertyTrackPosition.NORTH_WEST;
            }

            if (bl9 && !bl2 && !bl3) {
                railShape2 = BlockPropertyTrackPosition.NORTH_EAST;
            }
        }

        if (railShape2 == null) {
            if (bl5 && bl6) {
                railShape2 = railShape;
            } else if (bl5) {
                railShape2 = BlockPropertyTrackPosition.NORTH_SOUTH;
            } else if (bl6) {
                railShape2 = BlockPropertyTrackPosition.EAST_WEST;
            }

            if (!this.isStraight) {
                if (powered) {
                    if (bl7) {
                        railShape2 = BlockPropertyTrackPosition.SOUTH_EAST;
                    }

                    if (bl8) {
                        railShape2 = BlockPropertyTrackPosition.SOUTH_WEST;
                    }

                    if (bl9) {
                        railShape2 = BlockPropertyTrackPosition.NORTH_EAST;
                    }

                    if (bl10) {
                        railShape2 = BlockPropertyTrackPosition.NORTH_WEST;
                    }
                } else {
                    if (bl10) {
                        railShape2 = BlockPropertyTrackPosition.NORTH_WEST;
                    }

                    if (bl9) {
                        railShape2 = BlockPropertyTrackPosition.NORTH_EAST;
                    }

                    if (bl8) {
                        railShape2 = BlockPropertyTrackPosition.SOUTH_WEST;
                    }

                    if (bl7) {
                        railShape2 = BlockPropertyTrackPosition.SOUTH_EAST;
                    }
                }
            }
        }

        if (railShape2 == BlockPropertyTrackPosition.NORTH_SOUTH) {
            if (BlockMinecartTrackAbstract.isRail(this.level, blockPos.above())) {
                railShape2 = BlockPropertyTrackPosition.ASCENDING_NORTH;
            }

            if (BlockMinecartTrackAbstract.isRail(this.level, blockPos2.above())) {
                railShape2 = BlockPropertyTrackPosition.ASCENDING_SOUTH;
            }
        }

        if (railShape2 == BlockPropertyTrackPosition.EAST_WEST) {
            if (BlockMinecartTrackAbstract.isRail(this.level, blockPos4.above())) {
                railShape2 = BlockPropertyTrackPosition.ASCENDING_EAST;
            }

            if (BlockMinecartTrackAbstract.isRail(this.level, blockPos3.above())) {
                railShape2 = BlockPropertyTrackPosition.ASCENDING_WEST;
            }
        }

        if (railShape2 == null) {
            railShape2 = railShape;
        }

        this.updateConnections(railShape2);
        this.state = this.state.set(this.block.getShapeProperty(), railShape2);
        if (forceUpdate || this.level.getType(this.pos) != this.state) {
            this.level.setTypeAndData(this.pos, this.state, 3);

            for(int i = 0; i < this.connections.size(); ++i) {
                MinecartTrackLogic railState = this.getRail(this.connections.get(i));
                if (railState != null) {
                    railState.removeSoftConnections();
                    if (railState.canConnectTo(this)) {
                        railState.connectTo(this);
                    }
                }
            }
        }

        return this;
    }

    public IBlockData getState() {
        return this.state;
    }
}
