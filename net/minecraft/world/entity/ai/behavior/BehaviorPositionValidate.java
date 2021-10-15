package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.function.Predicate;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.protocol.game.PacketDebug;
import net.minecraft.server.level.WorldServer;
import net.minecraft.tags.TagsBlock;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.ai.BehaviorController;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.village.poi.VillagePlaceType;
import net.minecraft.world.level.block.BlockBed;
import net.minecraft.world.level.block.state.IBlockData;

public class BehaviorPositionValidate extends Behavior<EntityLiving> {
    private static final int MAX_DISTANCE = 16;
    private final MemoryModuleType<GlobalPos> memoryType;
    private final Predicate<VillagePlaceType> poiPredicate;

    public BehaviorPositionValidate(VillagePlaceType poiType, MemoryModuleType<GlobalPos> memoryModule) {
        super(ImmutableMap.of(memoryModule, MemoryStatus.VALUE_PRESENT));
        this.poiPredicate = poiType.getPredicate();
        this.memoryType = memoryModule;
    }

    @Override
    protected boolean checkExtraStartConditions(WorldServer world, EntityLiving entity) {
        GlobalPos globalPos = entity.getBehaviorController().getMemory(this.memoryType).get();
        return world.getDimensionKey() == globalPos.getDimensionManager() && globalPos.getBlockPosition().closerThan(entity.getPositionVector(), 16.0D);
    }

    @Override
    protected void start(WorldServer world, EntityLiving entity, long time) {
        BehaviorController<?> brain = entity.getBehaviorController();
        GlobalPos globalPos = brain.getMemory(this.memoryType).get();
        BlockPosition blockPos = globalPos.getBlockPosition();
        WorldServer serverLevel = world.getMinecraftServer().getWorldServer(globalPos.getDimensionManager());
        if (serverLevel != null && !this.poiDoesntExist(serverLevel, blockPos)) {
            if (this.bedIsOccupied(serverLevel, blockPos, entity)) {
                brain.removeMemory(this.memoryType);
                world.getPoiManager().release(blockPos);
                PacketDebug.sendPoiTicketCountPacket(world, blockPos);
            }
        } else {
            brain.removeMemory(this.memoryType);
        }

    }

    private boolean bedIsOccupied(WorldServer world, BlockPosition pos, EntityLiving entity) {
        IBlockData blockState = world.getType(pos);
        return blockState.is(TagsBlock.BEDS) && blockState.get(BlockBed.OCCUPIED) && !entity.isSleeping();
    }

    private boolean poiDoesntExist(WorldServer world, BlockPosition pos) {
        return !world.getPoiManager().exists(pos, this.poiPredicate);
    }
}
