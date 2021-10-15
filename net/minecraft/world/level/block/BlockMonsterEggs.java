package net.minecraft.world.level.block;

import com.google.common.collect.Maps;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.BlockPosition;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.EntitySilverfish;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentManager;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.IBlockState;

public class BlockMonsterEggs extends Block {
    private final Block hostBlock;
    private static final Map<Block, Block> BLOCK_BY_HOST_BLOCK = Maps.newIdentityHashMap();
    private static final Map<IBlockData, IBlockData> HOST_TO_INFESTED_STATES = Maps.newIdentityHashMap();
    private static final Map<IBlockData, IBlockData> INFESTED_TO_HOST_STATES = Maps.newIdentityHashMap();

    public BlockMonsterEggs(Block regularBlock, BlockBase.Info settings) {
        super(settings.destroyTime(regularBlock.defaultDestroyTime() / 2.0F).explosionResistance(0.75F));
        this.hostBlock = regularBlock;
        BLOCK_BY_HOST_BLOCK.put(regularBlock, this);
    }

    public Block getHostBlock() {
        return this.hostBlock;
    }

    public static boolean isCompatibleHostBlock(IBlockData block) {
        return BLOCK_BY_HOST_BLOCK.containsKey(block.getBlock());
    }

    private void spawnInfestation(WorldServer world, BlockPosition pos) {
        EntitySilverfish silverfish = EntityTypes.SILVERFISH.create(world);
        silverfish.setPositionRotation((double)pos.getX() + 0.5D, (double)pos.getY(), (double)pos.getZ() + 0.5D, 0.0F, 0.0F);
        world.addEntity(silverfish);
        silverfish.doSpawnEffect();
    }

    @Override
    public void dropNaturally(IBlockData state, WorldServer world, BlockPosition pos, ItemStack stack) {
        super.dropNaturally(state, world, pos, stack);
        if (world.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS) && EnchantmentManager.getEnchantmentLevel(Enchantments.SILK_TOUCH, stack) == 0) {
            this.spawnInfestation(world, pos);
        }

    }

    @Override
    public void wasExploded(World world, BlockPosition pos, Explosion explosion) {
        if (world instanceof WorldServer) {
            this.spawnInfestation((WorldServer)world, pos);
        }

    }

    public static IBlockData infestedStateByHost(IBlockData regularState) {
        return getNewStateWithProperties(HOST_TO_INFESTED_STATES, regularState, () -> {
            return BLOCK_BY_HOST_BLOCK.get(regularState.getBlock()).getBlockData();
        });
    }

    public IBlockData hostStateByInfested(IBlockData infestedState) {
        return getNewStateWithProperties(INFESTED_TO_HOST_STATES, infestedState, () -> {
            return this.getHostBlock().getBlockData();
        });
    }

    private static IBlockData getNewStateWithProperties(Map<IBlockData, IBlockData> stateMap, IBlockData fromState, Supplier<IBlockData> toStateSupplier) {
        return stateMap.computeIfAbsent(fromState, (infestedState) -> {
            IBlockData blockState = toStateSupplier.get();

            for(IBlockState property : infestedState.getProperties()) {
                blockState = blockState.hasProperty(property) ? blockState.set(property, infestedState.get(property)) : blockState;
            }

            return blockState;
        });
    }
}
