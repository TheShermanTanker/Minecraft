package net.minecraft.server.level;

import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriterionTriggers;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.network.protocol.game.PacketPlayInBlockDig;
import net.minecraft.network.protocol.game.PacketPlayOutBlockBreak;
import net.minecraft.network.protocol.game.PacketPlayOutPlayerInfo;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.ITileInventory;
import net.minecraft.world.InteractionResultWrapper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.ItemActionContext;
import net.minecraft.world.level.EnumGamemode;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.GameMasterBlock;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PlayerInteractManager {
    private static final Logger LOGGER = LogManager.getLogger();
    protected WorldServer level;
    protected final EntityPlayer player;
    private EnumGamemode gameModeForPlayer = EnumGamemode.DEFAULT_MODE;
    @Nullable
    private EnumGamemode previousGameModeForPlayer;
    private boolean isDestroyingBlock;
    private int destroyProgressStart;
    private BlockPosition destroyPos = BlockPosition.ZERO;
    private int gameTicks;
    private boolean hasDelayedDestroy;
    private BlockPosition delayedDestroyPos = BlockPosition.ZERO;
    private int delayedTickStart;
    private int lastSentState = -1;

    public PlayerInteractManager(EntityPlayer player) {
        this.player = player;
        this.level = player.getWorldServer();
    }

    public boolean setGameMode(EnumGamemode gameMode) {
        if (gameMode == this.gameModeForPlayer) {
            return false;
        } else {
            this.setGameModeForPlayer(gameMode, this.gameModeForPlayer);
            return true;
        }
    }

    protected void setGameModeForPlayer(EnumGamemode gameMode, @Nullable EnumGamemode previousGameMode) {
        this.previousGameModeForPlayer = previousGameMode;
        this.gameModeForPlayer = gameMode;
        gameMode.updatePlayerAbilities(this.player.getAbilities());
        this.player.updateAbilities();
        this.player.server.getPlayerList().sendAll(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.UPDATE_GAME_MODE, this.player));
        this.level.everyoneSleeping();
    }

    public EnumGamemode getGameMode() {
        return this.gameModeForPlayer;
    }

    @Nullable
    public EnumGamemode getPreviousGameModeForPlayer() {
        return this.previousGameModeForPlayer;
    }

    public boolean isSurvival() {
        return this.gameModeForPlayer.isSurvival();
    }

    public boolean isCreative() {
        return this.gameModeForPlayer.isCreative();
    }

    public void tick() {
        ++this.gameTicks;
        if (this.hasDelayedDestroy) {
            IBlockData blockState = this.level.getType(this.delayedDestroyPos);
            if (blockState.isAir()) {
                this.hasDelayedDestroy = false;
            } else {
                float f = this.incrementDestroyProgress(blockState, this.delayedDestroyPos, this.delayedTickStart);
                if (f >= 1.0F) {
                    this.hasDelayedDestroy = false;
                    this.breakBlock(this.delayedDestroyPos);
                }
            }
        } else if (this.isDestroyingBlock) {
            IBlockData blockState2 = this.level.getType(this.destroyPos);
            if (blockState2.isAir()) {
                this.level.destroyBlockProgress(this.player.getId(), this.destroyPos, -1);
                this.lastSentState = -1;
                this.isDestroyingBlock = false;
            } else {
                this.incrementDestroyProgress(blockState2, this.destroyPos, this.destroyProgressStart);
            }
        }

    }

    private float incrementDestroyProgress(IBlockData state, BlockPosition pos, int i) {
        int j = this.gameTicks - i;
        float f = state.getDamage(this.player, this.player.level, pos) * (float)(j + 1);
        int k = (int)(f * 10.0F);
        if (k != this.lastSentState) {
            this.level.destroyBlockProgress(this.player.getId(), pos, k);
            this.lastSentState = k;
        }

        return f;
    }

    public void handleBlockBreakAction(BlockPosition pos, PacketPlayInBlockDig.EnumPlayerDigType action, EnumDirection direction, int worldHeight) {
        double d = this.player.locX() - ((double)pos.getX() + 0.5D);
        double e = this.player.locY() - ((double)pos.getY() + 0.5D) + 1.5D;
        double f = this.player.locZ() - ((double)pos.getZ() + 0.5D);
        double g = d * d + e * e + f * f;
        if (g > 36.0D) {
            this.player.connection.sendPacket(new PacketPlayOutBlockBreak(pos, this.level.getType(pos), action, false, "too far"));
        } else if (pos.getY() >= worldHeight) {
            this.player.connection.sendPacket(new PacketPlayOutBlockBreak(pos, this.level.getType(pos), action, false, "too high"));
        } else {
            if (action == PacketPlayInBlockDig.EnumPlayerDigType.START_DESTROY_BLOCK) {
                if (!this.level.mayInteract(this.player, pos)) {
                    this.player.connection.sendPacket(new PacketPlayOutBlockBreak(pos, this.level.getType(pos), action, false, "may not interact"));
                    return;
                }

                if (this.isCreative()) {
                    this.destroyAndAck(pos, action, "creative destroy");
                    return;
                }

                if (this.player.blockActionRestricted(this.level, pos, this.gameModeForPlayer)) {
                    this.player.connection.sendPacket(new PacketPlayOutBlockBreak(pos, this.level.getType(pos), action, false, "block action restricted"));
                    return;
                }

                this.destroyProgressStart = this.gameTicks;
                float h = 1.0F;
                IBlockData blockState = this.level.getType(pos);
                if (!blockState.isAir()) {
                    blockState.attack(this.level, pos, this.player);
                    h = blockState.getDamage(this.player, this.player.level, pos);
                }

                if (!blockState.isAir() && h >= 1.0F) {
                    this.destroyAndAck(pos, action, "insta mine");
                } else {
                    if (this.isDestroyingBlock) {
                        this.player.connection.sendPacket(new PacketPlayOutBlockBreak(this.destroyPos, this.level.getType(this.destroyPos), PacketPlayInBlockDig.EnumPlayerDigType.START_DESTROY_BLOCK, false, "abort destroying since another started (client insta mine, server disagreed)"));
                    }

                    this.isDestroyingBlock = true;
                    this.destroyPos = pos.immutableCopy();
                    int i = (int)(h * 10.0F);
                    this.level.destroyBlockProgress(this.player.getId(), pos, i);
                    this.player.connection.sendPacket(new PacketPlayOutBlockBreak(pos, this.level.getType(pos), action, true, "actual start of destroying"));
                    this.lastSentState = i;
                }
            } else if (action == PacketPlayInBlockDig.EnumPlayerDigType.STOP_DESTROY_BLOCK) {
                if (pos.equals(this.destroyPos)) {
                    int j = this.gameTicks - this.destroyProgressStart;
                    IBlockData blockState2 = this.level.getType(pos);
                    if (!blockState2.isAir()) {
                        float k = blockState2.getDamage(this.player, this.player.level, pos) * (float)(j + 1);
                        if (k >= 0.7F) {
                            this.isDestroyingBlock = false;
                            this.level.destroyBlockProgress(this.player.getId(), pos, -1);
                            this.destroyAndAck(pos, action, "destroyed");
                            return;
                        }

                        if (!this.hasDelayedDestroy) {
                            this.isDestroyingBlock = false;
                            this.hasDelayedDestroy = true;
                            this.delayedDestroyPos = pos;
                            this.delayedTickStart = this.destroyProgressStart;
                        }
                    }
                }

                this.player.connection.sendPacket(new PacketPlayOutBlockBreak(pos, this.level.getType(pos), action, true, "stopped destroying"));
            } else if (action == PacketPlayInBlockDig.EnumPlayerDigType.ABORT_DESTROY_BLOCK) {
                this.isDestroyingBlock = false;
                if (!Objects.equals(this.destroyPos, pos)) {
                    LOGGER.warn("Mismatch in destroy block pos: {} {}", this.destroyPos, pos);
                    this.level.destroyBlockProgress(this.player.getId(), this.destroyPos, -1);
                    this.player.connection.sendPacket(new PacketPlayOutBlockBreak(this.destroyPos, this.level.getType(this.destroyPos), action, true, "aborted mismatched destroying"));
                }

                this.level.destroyBlockProgress(this.player.getId(), pos, -1);
                this.player.connection.sendPacket(new PacketPlayOutBlockBreak(pos, this.level.getType(pos), action, true, "aborted destroying"));
            }

        }
    }

    public void destroyAndAck(BlockPosition pos, PacketPlayInBlockDig.EnumPlayerDigType action, String reason) {
        if (this.breakBlock(pos)) {
            this.player.connection.sendPacket(new PacketPlayOutBlockBreak(pos, this.level.getType(pos), action, true, reason));
        } else {
            this.player.connection.sendPacket(new PacketPlayOutBlockBreak(pos, this.level.getType(pos), action, false, reason));
        }

    }

    public boolean breakBlock(BlockPosition pos) {
        IBlockData blockState = this.level.getType(pos);
        if (!this.player.getItemInMainHand().getItem().canAttackBlock(blockState, this.level, pos, this.player)) {
            return false;
        } else {
            TileEntity blockEntity = this.level.getTileEntity(pos);
            Block block = blockState.getBlock();
            if (block instanceof GameMasterBlock && !this.player.isCreativeAndOp()) {
                this.level.notify(pos, blockState, blockState, 3);
                return false;
            } else if (this.player.blockActionRestricted(this.level, pos, this.gameModeForPlayer)) {
                return false;
            } else {
                block.playerWillDestroy(this.level, pos, blockState, this.player);
                boolean bl = this.level.removeBlock(pos, false);
                if (bl) {
                    block.postBreak(this.level, pos, blockState);
                }

                if (this.isCreative()) {
                    return true;
                } else {
                    ItemStack itemStack = this.player.getItemInMainHand();
                    ItemStack itemStack2 = itemStack.cloneItemStack();
                    boolean bl2 = this.player.hasBlock(blockState);
                    itemStack.mineBlock(this.level, blockState, pos, this.player);
                    if (bl && bl2) {
                        block.playerDestroy(this.level, this.player, pos, blockState, blockEntity, itemStack2);
                    }

                    return true;
                }
            }
        }
    }

    public EnumInteractionResult useItem(EntityPlayer player, World world, ItemStack stack, EnumHand hand) {
        if (this.gameModeForPlayer == EnumGamemode.SPECTATOR) {
            return EnumInteractionResult.PASS;
        } else if (player.getCooldownTracker().hasCooldown(stack.getItem())) {
            return EnumInteractionResult.PASS;
        } else {
            int i = stack.getCount();
            int j = stack.getDamage();
            InteractionResultWrapper<ItemStack> interactionResultHolder = stack.use(world, player, hand);
            ItemStack itemStack = interactionResultHolder.getObject();
            if (itemStack == stack && itemStack.getCount() == i && itemStack.getUseDuration() <= 0 && itemStack.getDamage() == j) {
                return interactionResultHolder.getResult();
            } else if (interactionResultHolder.getResult() == EnumInteractionResult.FAIL && itemStack.getUseDuration() > 0 && !player.isHandRaised()) {
                return interactionResultHolder.getResult();
            } else {
                player.setItemInHand(hand, itemStack);
                if (this.isCreative()) {
                    itemStack.setCount(i);
                    if (itemStack.isDamageableItem() && itemStack.getDamage() != j) {
                        itemStack.setDamage(j);
                    }
                }

                if (itemStack.isEmpty()) {
                    player.setItemInHand(hand, ItemStack.EMPTY);
                }

                if (!player.isHandRaised()) {
                    player.inventoryMenu.updateInventory();
                }

                return interactionResultHolder.getResult();
            }
        }
    }

    public EnumInteractionResult useItemOn(EntityPlayer player, World world, ItemStack stack, EnumHand hand, MovingObjectPositionBlock hitResult) {
        BlockPosition blockPos = hitResult.getBlockPosition();
        IBlockData blockState = world.getType(blockPos);
        if (this.gameModeForPlayer == EnumGamemode.SPECTATOR) {
            ITileInventory menuProvider = blockState.getMenuProvider(world, blockPos);
            if (menuProvider != null) {
                player.openContainer(menuProvider);
                return EnumInteractionResult.SUCCESS;
            } else {
                return EnumInteractionResult.PASS;
            }
        } else {
            boolean bl = !player.getItemInMainHand().isEmpty() || !player.getItemInOffHand().isEmpty();
            boolean bl2 = player.isSecondaryUseActive() && bl;
            ItemStack itemStack = stack.cloneItemStack();
            if (!bl2) {
                EnumInteractionResult interactionResult = blockState.interact(world, player, hand, hitResult);
                if (interactionResult.consumesAction()) {
                    CriterionTriggers.ITEM_USED_ON_BLOCK.trigger(player, blockPos, itemStack);
                    return interactionResult;
                }
            }

            if (!stack.isEmpty() && !player.getCooldownTracker().hasCooldown(stack.getItem())) {
                ItemActionContext useOnContext = new ItemActionContext(player, hand, hitResult);
                EnumInteractionResult interactionResult2;
                if (this.isCreative()) {
                    int i = stack.getCount();
                    interactionResult2 = stack.placeItem(useOnContext);
                    stack.setCount(i);
                } else {
                    interactionResult2 = stack.placeItem(useOnContext);
                }

                if (interactionResult2.consumesAction()) {
                    CriterionTriggers.ITEM_USED_ON_BLOCK.trigger(player, blockPos, itemStack);
                }

                return interactionResult2;
            } else {
                return EnumInteractionResult.PASS;
            }
        }
    }

    public void setLevel(WorldServer world) {
        this.level = world;
    }
}
