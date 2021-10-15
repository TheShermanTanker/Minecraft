package net.minecraft.server.level;

import net.minecraft.SystemUtils;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.protocol.game.PacketPlayInBlockDig;
import net.minecraft.network.protocol.game.PacketPlayOutGameStateChange;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.World;
import net.minecraft.world.phys.MovingObjectPositionBlock;

public class DemoPlayerInteractManager extends PlayerInteractManager {
    public static final int DEMO_DAYS = 5;
    public static final int TOTAL_PLAY_TICKS = 120500;
    private boolean displayedIntro;
    private boolean demoHasEnded;
    private int demoEndedReminder;
    private int gameModeTicks;

    public DemoPlayerInteractManager(EntityPlayer player) {
        super(player);
    }

    @Override
    public void tick() {
        super.tick();
        ++this.gameModeTicks;
        long l = this.level.getTime();
        long m = l / 24000L + 1L;
        if (!this.displayedIntro && this.gameModeTicks > 20) {
            this.displayedIntro = true;
            this.player.connection.sendPacket(new PacketPlayOutGameStateChange(PacketPlayOutGameStateChange.DEMO_EVENT, 0.0F));
        }

        this.demoHasEnded = l > 120500L;
        if (this.demoHasEnded) {
            ++this.demoEndedReminder;
        }

        if (l % 24000L == 500L) {
            if (m <= 6L) {
                if (m == 6L) {
                    this.player.connection.sendPacket(new PacketPlayOutGameStateChange(PacketPlayOutGameStateChange.DEMO_EVENT, 104.0F));
                } else {
                    this.player.sendMessage(new ChatMessage("demo.day." + m), SystemUtils.NIL_UUID);
                }
            }
        } else if (m == 1L) {
            if (l == 100L) {
                this.player.connection.sendPacket(new PacketPlayOutGameStateChange(PacketPlayOutGameStateChange.DEMO_EVENT, 101.0F));
            } else if (l == 175L) {
                this.player.connection.sendPacket(new PacketPlayOutGameStateChange(PacketPlayOutGameStateChange.DEMO_EVENT, 102.0F));
            } else if (l == 250L) {
                this.player.connection.sendPacket(new PacketPlayOutGameStateChange(PacketPlayOutGameStateChange.DEMO_EVENT, 103.0F));
            }
        } else if (m == 5L && l % 24000L == 22000L) {
            this.player.sendMessage(new ChatMessage("demo.day.warning"), SystemUtils.NIL_UUID);
        }

    }

    private void outputDemoReminder() {
        if (this.demoEndedReminder > 100) {
            this.player.sendMessage(new ChatMessage("demo.reminder"), SystemUtils.NIL_UUID);
            this.demoEndedReminder = 0;
        }

    }

    @Override
    public void handleBlockBreakAction(BlockPosition pos, PacketPlayInBlockDig.EnumPlayerDigType action, EnumDirection direction, int worldHeight) {
        if (this.demoHasEnded) {
            this.outputDemoReminder();
        } else {
            super.handleBlockBreakAction(pos, action, direction, worldHeight);
        }
    }

    @Override
    public EnumInteractionResult useItem(EntityPlayer player, World world, ItemStack stack, EnumHand hand) {
        if (this.demoHasEnded) {
            this.outputDemoReminder();
            return EnumInteractionResult.PASS;
        } else {
            return super.useItem(player, world, stack, hand);
        }
    }

    @Override
    public EnumInteractionResult useItemOn(EntityPlayer player, World world, ItemStack stack, EnumHand hand, MovingObjectPositionBlock hitResult) {
        if (this.demoHasEnded) {
            this.outputDemoReminder();
            return EnumInteractionResult.PASS;
        } else {
            return super.useItemOn(player, world, stack, hand, hitResult);
        }
    }
}
