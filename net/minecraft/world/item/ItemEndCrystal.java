package net.minecraft.world.item;

import java.util.List;
import net.minecraft.core.BlockPosition;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EntityEnderCrystal;
import net.minecraft.world.item.context.ItemActionContext;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.dimension.end.EnderDragonBattle;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AxisAlignedBB;

public class ItemEndCrystal extends Item {
    public ItemEndCrystal(Item.Info settings) {
        super(settings);
    }

    @Override
    public EnumInteractionResult useOn(ItemActionContext context) {
        World level = context.getWorld();
        BlockPosition blockPos = context.getClickPosition();
        IBlockData blockState = level.getType(blockPos);
        if (!blockState.is(Blocks.OBSIDIAN) && !blockState.is(Blocks.BEDROCK)) {
            return EnumInteractionResult.FAIL;
        } else {
            BlockPosition blockPos2 = blockPos.above();
            if (!level.isEmpty(blockPos2)) {
                return EnumInteractionResult.FAIL;
            } else {
                double d = (double)blockPos2.getX();
                double e = (double)blockPos2.getY();
                double f = (double)blockPos2.getZ();
                List<Entity> list = level.getEntities((Entity)null, new AxisAlignedBB(d, e, f, d + 1.0D, e + 2.0D, f + 1.0D));
                if (!list.isEmpty()) {
                    return EnumInteractionResult.FAIL;
                } else {
                    if (level instanceof WorldServer) {
                        EntityEnderCrystal endCrystal = new EntityEnderCrystal(level, d + 0.5D, e, f + 0.5D);
                        endCrystal.setShowingBottom(false);
                        level.addEntity(endCrystal);
                        level.gameEvent(context.getEntity(), GameEvent.ENTITY_PLACE, blockPos2);
                        EnderDragonBattle endDragonFight = ((WorldServer)level).getDragonBattle();
                        if (endDragonFight != null) {
                            endDragonFight.initiateRespawn();
                        }
                    }

                    context.getItemStack().subtract(1);
                    return EnumInteractionResult.sidedSuccess(level.isClientSide);
                }
            }
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
