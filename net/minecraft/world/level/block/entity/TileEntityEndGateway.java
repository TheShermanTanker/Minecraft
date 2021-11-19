package net.minecraft.world.level.block.entity;

import java.util.List;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriterionTriggers;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.data.worldgen.WorldGenBiomeDecoratorGroups;
import net.minecraft.nbt.GameProfileSerializer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.protocol.game.PacketPlayOutTileEntityData;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.util.MathHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.IEntitySelector;
import net.minecraft.world.entity.projectile.EntityEnderPearl;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.chunk.Chunk;
import net.minecraft.world.level.levelgen.feature.WorldGenerator;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenEndGatewayConfiguration;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.Vec3D;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TileEntityEndGateway extends TileEntityEnderPortal {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final int SPAWN_TIME = 200;
    private static final int COOLDOWN_TIME = 40;
    private static final int ATTENTION_INTERVAL = 2400;
    private static final int EVENT_COOLDOWN = 1;
    private static final int GATEWAY_HEIGHT_ABOVE_SURFACE = 10;
    public long age;
    private int teleportCooldown;
    @Nullable
    public BlockPosition exitPortal;
    public boolean exactTeleport;

    public TileEntityEndGateway(BlockPosition pos, IBlockData state) {
        super(TileEntityTypes.END_GATEWAY, pos, state);
    }

    @Override
    public NBTTagCompound save(NBTTagCompound nbt) {
        super.save(nbt);
        nbt.setLong("Age", this.age);
        if (this.exitPortal != null) {
            nbt.set("ExitPortal", GameProfileSerializer.writeBlockPos(this.exitPortal));
        }

        if (this.exactTeleport) {
            nbt.setBoolean("ExactTeleport", this.exactTeleport);
        }

        return nbt;
    }

    @Override
    public void load(NBTTagCompound nbt) {
        super.load(nbt);
        this.age = nbt.getLong("Age");
        if (nbt.hasKeyOfType("ExitPortal", 10)) {
            BlockPosition blockPos = GameProfileSerializer.readBlockPos(nbt.getCompound("ExitPortal"));
            if (World.isInSpawnableBounds(blockPos)) {
                this.exitPortal = blockPos;
            }
        }

        this.exactTeleport = nbt.getBoolean("ExactTeleport");
    }

    public static void beamAnimationTick(World world, BlockPosition pos, IBlockData state, TileEntityEndGateway blockEntity) {
        ++blockEntity.age;
        if (blockEntity.isCoolingDown()) {
            --blockEntity.teleportCooldown;
        }

    }

    public static void teleportTick(World world, BlockPosition pos, IBlockData state, TileEntityEndGateway blockEntity) {
        boolean bl = blockEntity.isSpawning();
        boolean bl2 = blockEntity.isCoolingDown();
        ++blockEntity.age;
        if (bl2) {
            --blockEntity.teleportCooldown;
        } else {
            List<Entity> list = world.getEntitiesOfClass(Entity.class, new AxisAlignedBB(pos), TileEntityEndGateway::canEntityTeleport);
            if (!list.isEmpty()) {
                teleportEntity(world, pos, state, list.get(world.random.nextInt(list.size())), blockEntity);
            }

            if (blockEntity.age % 2400L == 0L) {
                triggerCooldown(world, pos, state, blockEntity);
            }
        }

        if (bl != blockEntity.isSpawning() || bl2 != blockEntity.isCoolingDown()) {
            setChanged(world, pos, state);
        }

    }

    public static boolean canEntityTeleport(Entity entity) {
        return IEntitySelector.NO_SPECTATORS.test(entity) && !entity.getRootVehicle().isOnPortalCooldown();
    }

    public boolean isSpawning() {
        return this.age < 200L;
    }

    public boolean isCoolingDown() {
        return this.teleportCooldown > 0;
    }

    public float getSpawnPercent(float tickDelta) {
        return MathHelper.clamp(((float)this.age + tickDelta) / 200.0F, 0.0F, 1.0F);
    }

    public float getCooldownPercent(float tickDelta) {
        return 1.0F - MathHelper.clamp(((float)this.teleportCooldown - tickDelta) / 40.0F, 0.0F, 1.0F);
    }

    @Nullable
    @Override
    public PacketPlayOutTileEntityData getUpdatePacket() {
        return new PacketPlayOutTileEntityData(this.worldPosition, 8, this.getUpdateTag());
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return this.save(new NBTTagCompound());
    }

    private static void triggerCooldown(World world, BlockPosition pos, IBlockData state, TileEntityEndGateway blockEntity) {
        if (!world.isClientSide) {
            blockEntity.teleportCooldown = 40;
            world.playBlockAction(pos, state.getBlock(), 1, 0);
            setChanged(world, pos, state);
        }

    }

    @Override
    public boolean setProperty(int type, int data) {
        if (type == 1) {
            this.teleportCooldown = 40;
            return true;
        } else {
            return super.setProperty(type, data);
        }
    }

    public static void teleportEntity(World world, BlockPosition pos, IBlockData state, Entity entity, TileEntityEndGateway blockEntity) {
        if (world instanceof WorldServer && !blockEntity.isCoolingDown()) {
            WorldServer serverLevel = (WorldServer)world;
            blockEntity.teleportCooldown = 100;
            if (blockEntity.exitPortal == null && world.getDimensionKey() == World.END) {
                BlockPosition blockPos = findOrCreateValidTeleportPos(serverLevel, pos);
                blockPos = blockPos.above(10);
                LOGGER.debug("Creating portal at {}", (Object)blockPos);
                spawnGatewayPortal(serverLevel, blockPos, WorldGenEndGatewayConfiguration.knownExit(pos, false));
                blockEntity.exitPortal = blockPos;
            }

            if (blockEntity.exitPortal != null) {
                BlockPosition blockPos2 = blockEntity.exactTeleport ? blockEntity.exitPortal : findExitPosition(world, blockEntity.exitPortal);
                Entity entity3;
                if (entity instanceof EntityEnderPearl) {
                    Entity entity2 = ((EntityEnderPearl)entity).getShooter();
                    if (entity2 instanceof EntityPlayer) {
                        CriterionTriggers.ENTER_BLOCK.trigger((EntityPlayer)entity2, state);
                    }

                    if (entity2 != null) {
                        entity3 = entity2;
                        entity.die();
                    } else {
                        entity3 = entity;
                    }
                } else {
                    entity3 = entity.getRootVehicle();
                }

                entity3.resetPortalCooldown();
                entity3.enderTeleportAndLoad((double)blockPos2.getX() + 0.5D, (double)blockPos2.getY(), (double)blockPos2.getZ() + 0.5D);
            }

            triggerCooldown(world, pos, state, blockEntity);
        }
    }

    private static BlockPosition findExitPosition(World world, BlockPosition pos) {
        BlockPosition blockPos = findTallestBlock(world, pos.offset(0, 2, 0), 5, false);
        LOGGER.debug("Best exit position for portal at {} is {}", pos, blockPos);
        return blockPos.above();
    }

    private static BlockPosition findOrCreateValidTeleportPos(WorldServer world, BlockPosition pos) {
        Vec3D vec3 = findExitPortalXZPosTentative(world, pos);
        Chunk levelChunk = getChunk(world, vec3);
        BlockPosition blockPos = findValidSpawnInChunk(levelChunk);
        if (blockPos == null) {
            blockPos = new BlockPosition(vec3.x + 0.5D, 75.0D, vec3.z + 0.5D);
            LOGGER.debug("Failed to find a suitable block to teleport to, spawning an island on {}", (Object)blockPos);
            WorldGenBiomeDecoratorGroups.END_ISLAND.place(world, world.getChunkSource().getChunkGenerator(), new Random(blockPos.asLong()), blockPos);
        } else {
            LOGGER.debug("Found suitable block to teleport to: {}", (Object)blockPos);
        }

        return findTallestBlock(world, blockPos, 16, true);
    }

    private static Vec3D findExitPortalXZPosTentative(WorldServer world, BlockPosition pos) {
        Vec3D vec3 = (new Vec3D((double)pos.getX(), 0.0D, (double)pos.getZ())).normalize();
        int i = 1024;
        Vec3D vec32 = vec3.scale(1024.0D);

        for(int j = 16; !isChunkEmpty(world, vec32) && j-- > 0; vec32 = vec32.add(vec3.scale(-16.0D))) {
            LOGGER.debug("Skipping backwards past nonempty chunk at {}", (Object)vec32);
        }

        for(int var6 = 16; isChunkEmpty(world, vec32) && var6-- > 0; vec32 = vec32.add(vec3.scale(16.0D))) {
            LOGGER.debug("Skipping forward past empty chunk at {}", (Object)vec32);
        }

        LOGGER.debug("Found chunk at {}", (Object)vec32);
        return vec32;
    }

    private static boolean isChunkEmpty(WorldServer world, Vec3D pos) {
        return getChunk(world, pos).getHighestSectionPosition() <= world.getMinBuildHeight();
    }

    private static BlockPosition findTallestBlock(IBlockAccess world, BlockPosition pos, int searchRadius, boolean force) {
        BlockPosition blockPos = null;

        for(int i = -searchRadius; i <= searchRadius; ++i) {
            for(int j = -searchRadius; j <= searchRadius; ++j) {
                if (i != 0 || j != 0 || force) {
                    for(int k = world.getMaxBuildHeight() - 1; k > (blockPos == null ? world.getMinBuildHeight() : blockPos.getY()); --k) {
                        BlockPosition blockPos2 = new BlockPosition(pos.getX() + i, k, pos.getZ() + j);
                        IBlockData blockState = world.getType(blockPos2);
                        if (blockState.isCollisionShapeFullBlock(world, blockPos2) && (force || !blockState.is(Blocks.BEDROCK))) {
                            blockPos = blockPos2;
                            break;
                        }
                    }
                }
            }
        }

        return blockPos == null ? pos : blockPos;
    }

    private static Chunk getChunk(World world, Vec3D pos) {
        return world.getChunk(MathHelper.floor(pos.x / 16.0D), MathHelper.floor(pos.z / 16.0D));
    }

    @Nullable
    private static BlockPosition findValidSpawnInChunk(Chunk chunk) {
        ChunkCoordIntPair chunkPos = chunk.getPos();
        BlockPosition blockPos = new BlockPosition(chunkPos.getMinBlockX(), 30, chunkPos.getMinBlockZ());
        int i = chunk.getHighestSectionPosition() + 16 - 1;
        BlockPosition blockPos2 = new BlockPosition(chunkPos.getMaxBlockX(), i, chunkPos.getMaxBlockZ());
        BlockPosition blockPos3 = null;
        double d = 0.0D;

        for(BlockPosition blockPos4 : BlockPosition.betweenClosed(blockPos, blockPos2)) {
            IBlockData blockState = chunk.getType(blockPos4);
            BlockPosition blockPos5 = blockPos4.above();
            BlockPosition blockPos6 = blockPos4.above(2);
            if (blockState.is(Blocks.END_STONE) && !chunk.getType(blockPos5).isCollisionShapeFullBlock(chunk, blockPos5) && !chunk.getType(blockPos6).isCollisionShapeFullBlock(chunk, blockPos6)) {
                double e = blockPos4.distanceSquared(0.0D, 0.0D, 0.0D, true);
                if (blockPos3 == null || e < d) {
                    blockPos3 = blockPos4;
                    d = e;
                }
            }
        }

        return blockPos3;
    }

    private static void spawnGatewayPortal(WorldServer world, BlockPosition pos, WorldGenEndGatewayConfiguration config) {
        WorldGenerator.END_GATEWAY.configured(config).place(world, world.getChunkSource().getChunkGenerator(), new Random(), pos);
    }

    @Override
    public boolean shouldRenderFace(EnumDirection direction) {
        return Block.shouldRenderFace(this.getBlock(), this.level, this.getPosition(), direction, this.getPosition().relative(direction));
    }

    public int getParticleAmount() {
        int i = 0;

        for(EnumDirection direction : EnumDirection.values()) {
            i += this.shouldRenderFace(direction) ? 1 : 0;
        }

        return i;
    }

    public void setExitPosition(BlockPosition pos, boolean exactTeleport) {
        this.exactTeleport = exactTeleport;
        this.exitPortal = pos;
    }
}
