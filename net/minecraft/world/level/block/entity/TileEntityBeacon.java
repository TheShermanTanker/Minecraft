package net.minecraft.world.level.block.entity;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriterionTriggers;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.game.PacketPlayOutTileEntityData;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.sounds.EnumSoundCategory;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.tags.TagsBlock;
import net.minecraft.world.ChestLock;
import net.minecraft.world.ITileInventory;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectBase;
import net.minecraft.world.effect.MobEffectList;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.inventory.Container;
import net.minecraft.world.inventory.ContainerAccess;
import net.minecraft.world.inventory.ContainerBeacon;
import net.minecraft.world.inventory.IContainerProperties;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.IBeaconBeam;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.phys.AxisAlignedBB;

public class TileEntityBeacon extends TileEntity implements ITileInventory {
    private static final int MAX_LEVELS = 4;
    public static final MobEffectBase[][] BEACON_EFFECTS = new MobEffectBase[][]{{MobEffectList.MOVEMENT_SPEED, MobEffectList.DIG_SPEED}, {MobEffectList.DAMAGE_RESISTANCE, MobEffectList.JUMP}, {MobEffectList.DAMAGE_BOOST}, {MobEffectList.REGENERATION}};
    private static final Set<MobEffectBase> VALID_EFFECTS = Arrays.stream(BEACON_EFFECTS).flatMap(Arrays::stream).collect(Collectors.toSet());
    public static final int DATA_LEVELS = 0;
    public static final int DATA_PRIMARY = 1;
    public static final int DATA_SECONDARY = 2;
    public static final int NUM_DATA_VALUES = 3;
    private static final int BLOCKS_CHECK_PER_TICK = 10;
    List<TileEntityBeacon.BeaconColorTracker> beamSections = Lists.newArrayList();
    private List<TileEntityBeacon.BeaconColorTracker> checkingBeamSections = Lists.newArrayList();
    public int levels;
    private int lastCheckY;
    @Nullable
    public MobEffectBase primaryPower;
    @Nullable
    public MobEffectBase secondaryPower;
    @Nullable
    public IChatBaseComponent name;
    public ChestLock lockKey = ChestLock.NO_LOCK;
    private final IContainerProperties dataAccess = new IContainerProperties() {
        @Override
        public int getProperty(int index) {
            switch(index) {
            case 0:
                return TileEntityBeacon.this.levels;
            case 1:
                return MobEffectBase.getId(TileEntityBeacon.this.primaryPower);
            case 2:
                return MobEffectBase.getId(TileEntityBeacon.this.secondaryPower);
            default:
                return 0;
            }
        }

        @Override
        public void setProperty(int index, int value) {
            switch(index) {
            case 0:
                TileEntityBeacon.this.levels = value;
                break;
            case 1:
                if (!TileEntityBeacon.this.level.isClientSide && !TileEntityBeacon.this.beamSections.isEmpty()) {
                    TileEntityBeacon.playSound(TileEntityBeacon.this.level, TileEntityBeacon.this.worldPosition, SoundEffects.BEACON_POWER_SELECT);
                }

                TileEntityBeacon.this.primaryPower = TileEntityBeacon.getValidEffectById(value);
                break;
            case 2:
                TileEntityBeacon.this.secondaryPower = TileEntityBeacon.getValidEffectById(value);
            }

        }

        @Override
        public int getCount() {
            return 3;
        }
    };

    public TileEntityBeacon(BlockPosition pos, IBlockData state) {
        super(TileEntityTypes.BEACON, pos, state);
    }

    public static void tick(World world, BlockPosition pos, IBlockData state, TileEntityBeacon blockEntity) {
        int i = pos.getX();
        int j = pos.getY();
        int k = pos.getZ();
        BlockPosition blockPos;
        if (blockEntity.lastCheckY < j) {
            blockPos = pos;
            blockEntity.checkingBeamSections = Lists.newArrayList();
            blockEntity.lastCheckY = pos.getY() - 1;
        } else {
            blockPos = new BlockPosition(i, blockEntity.lastCheckY + 1, k);
        }

        TileEntityBeacon.BeaconColorTracker beaconBeamSection = blockEntity.checkingBeamSections.isEmpty() ? null : blockEntity.checkingBeamSections.get(blockEntity.checkingBeamSections.size() - 1);
        int l = world.getHeight(HeightMap.Type.WORLD_SURFACE, i, k);

        for(int m = 0; m < 10 && blockPos.getY() <= l; ++m) {
            IBlockData blockState = world.getType(blockPos);
            Block block = blockState.getBlock();
            if (block instanceof IBeaconBeam) {
                float[] fs = ((IBeaconBeam)block).getColor().getColor();
                if (blockEntity.checkingBeamSections.size() <= 1) {
                    beaconBeamSection = new TileEntityBeacon.BeaconColorTracker(fs);
                    blockEntity.checkingBeamSections.add(beaconBeamSection);
                } else if (beaconBeamSection != null) {
                    if (Arrays.equals(fs, beaconBeamSection.color)) {
                        beaconBeamSection.increaseHeight();
                    } else {
                        beaconBeamSection = new TileEntityBeacon.BeaconColorTracker(new float[]{(beaconBeamSection.color[0] + fs[0]) / 2.0F, (beaconBeamSection.color[1] + fs[1]) / 2.0F, (beaconBeamSection.color[2] + fs[2]) / 2.0F});
                        blockEntity.checkingBeamSections.add(beaconBeamSection);
                    }
                }
            } else {
                if (beaconBeamSection == null || blockState.getLightBlock(world, blockPos) >= 15 && !blockState.is(Blocks.BEDROCK)) {
                    blockEntity.checkingBeamSections.clear();
                    blockEntity.lastCheckY = l;
                    break;
                }

                beaconBeamSection.increaseHeight();
            }

            blockPos = blockPos.above();
            ++blockEntity.lastCheckY;
        }

        int n = blockEntity.levels;
        if (world.getTime() % 80L == 0L) {
            if (!blockEntity.beamSections.isEmpty()) {
                blockEntity.levels = updateBase(world, i, j, k);
            }

            if (blockEntity.levels > 0 && !blockEntity.beamSections.isEmpty()) {
                applyEffects(world, pos, blockEntity.levels, blockEntity.primaryPower, blockEntity.secondaryPower);
                playSound(world, pos, SoundEffects.BEACON_AMBIENT);
            }
        }

        if (blockEntity.lastCheckY >= l) {
            blockEntity.lastCheckY = world.getMinBuildHeight() - 1;
            boolean bl = n > 0;
            blockEntity.beamSections = blockEntity.checkingBeamSections;
            if (!world.isClientSide) {
                boolean bl2 = blockEntity.levels > 0;
                if (!bl && bl2) {
                    playSound(world, pos, SoundEffects.BEACON_ACTIVATE);

                    for(EntityPlayer serverPlayer : world.getEntitiesOfClass(EntityPlayer.class, (new AxisAlignedBB((double)i, (double)j, (double)k, (double)i, (double)(j - 4), (double)k)).grow(10.0D, 5.0D, 10.0D))) {
                        CriterionTriggers.CONSTRUCT_BEACON.trigger(serverPlayer, blockEntity.levels);
                    }
                } else if (bl && !bl2) {
                    playSound(world, pos, SoundEffects.BEACON_DEACTIVATE);
                }
            }
        }

    }

    private static int updateBase(World world, int x, int y, int z) {
        int i = 0;

        for(int j = 1; j <= 4; i = j++) {
            int k = y - j;
            if (k < world.getMinBuildHeight()) {
                break;
            }

            boolean bl = true;

            for(int l = x - j; l <= x + j && bl; ++l) {
                for(int m = z - j; m <= z + j; ++m) {
                    if (!world.getType(new BlockPosition(l, k, m)).is(TagsBlock.BEACON_BASE_BLOCKS)) {
                        bl = false;
                        break;
                    }
                }
            }

            if (!bl) {
                break;
            }
        }

        return i;
    }

    @Override
    public void setRemoved() {
        playSound(this.level, this.worldPosition, SoundEffects.BEACON_DEACTIVATE);
        super.setRemoved();
    }

    private static void applyEffects(World world, BlockPosition pos, int beaconLevel, @Nullable MobEffectBase primaryEffect, @Nullable MobEffectBase secondaryEffect) {
        if (!world.isClientSide && primaryEffect != null) {
            double d = (double)(beaconLevel * 10 + 10);
            int i = 0;
            if (beaconLevel >= 4 && primaryEffect == secondaryEffect) {
                i = 1;
            }

            int j = (9 + beaconLevel * 2) * 20;
            AxisAlignedBB aABB = (new AxisAlignedBB(pos)).inflate(d).expandTowards(0.0D, (double)world.getHeight(), 0.0D);
            List<EntityHuman> list = world.getEntitiesOfClass(EntityHuman.class, aABB);

            for(EntityHuman player : list) {
                player.addEffect(new MobEffect(primaryEffect, j, i, true, true));
            }

            if (beaconLevel >= 4 && primaryEffect != secondaryEffect && secondaryEffect != null) {
                for(EntityHuman player2 : list) {
                    player2.addEffect(new MobEffect(secondaryEffect, j, 0, true, true));
                }
            }

        }
    }

    public static void playSound(World world, BlockPosition pos, SoundEffect sound) {
        world.playSound((EntityHuman)null, pos, sound, EnumSoundCategory.BLOCKS, 1.0F, 1.0F);
    }

    public List<TileEntityBeacon.BeaconColorTracker> getBeamSections() {
        return (List<TileEntityBeacon.BeaconColorTracker>)(this.levels == 0 ? ImmutableList.of() : this.beamSections);
    }

    @Nullable
    @Override
    public PacketPlayOutTileEntityData getUpdatePacket() {
        return new PacketPlayOutTileEntityData(this.worldPosition, 3, this.getUpdateTag());
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return this.save(new NBTTagCompound());
    }

    @Nullable
    static MobEffectBase getValidEffectById(int id) {
        MobEffectBase mobEffect = MobEffectBase.fromId(id);
        return VALID_EFFECTS.contains(mobEffect) ? mobEffect : null;
    }

    @Override
    public void load(NBTTagCompound nbt) {
        super.load(nbt);
        this.primaryPower = getValidEffectById(nbt.getInt("Primary"));
        this.secondaryPower = getValidEffectById(nbt.getInt("Secondary"));
        if (nbt.hasKeyOfType("CustomName", 8)) {
            this.name = IChatBaseComponent.ChatSerializer.fromJson(nbt.getString("CustomName"));
        }

        this.lockKey = ChestLock.fromTag(nbt);
    }

    @Override
    public NBTTagCompound save(NBTTagCompound nbt) {
        super.save(nbt);
        nbt.setInt("Primary", MobEffectBase.getId(this.primaryPower));
        nbt.setInt("Secondary", MobEffectBase.getId(this.secondaryPower));
        nbt.setInt("Levels", this.levels);
        if (this.name != null) {
            nbt.setString("CustomName", IChatBaseComponent.ChatSerializer.toJson(this.name));
        }

        this.lockKey.addToTag(nbt);
        return nbt;
    }

    public void setCustomName(@Nullable IChatBaseComponent customName) {
        this.name = customName;
    }

    @Nullable
    @Override
    public Container createMenu(int syncId, PlayerInventory inv, EntityHuman player) {
        return TileEntityContainer.canUnlock(player, this.lockKey, this.getScoreboardDisplayName()) ? new ContainerBeacon(syncId, inv, this.dataAccess, ContainerAccess.at(this.level, this.getPosition())) : null;
    }

    @Override
    public IChatBaseComponent getScoreboardDisplayName() {
        return (IChatBaseComponent)(this.name != null ? this.name : new ChatMessage("container.beacon"));
    }

    @Override
    public void setWorld(World world) {
        super.setWorld(world);
        this.lastCheckY = world.getMinBuildHeight() - 1;
    }

    public static class BeaconColorTracker {
        final float[] color;
        private int height;

        public BeaconColorTracker(float[] color) {
            this.color = color;
            this.height = 1;
        }

        protected void increaseHeight() {
            ++this.height;
        }

        public float[] getColor() {
            return this.color;
        }

        public int getHeight() {
            return this.height;
        }
    }
}
