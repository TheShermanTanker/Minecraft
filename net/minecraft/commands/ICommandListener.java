package net.minecraft.commands;

import java.util.UUID;
import net.minecraft.network.chat.IChatBaseComponent;

public interface ICommandListener {
    ICommandListener NULL = new ICommandListener() {
        @Override
        public void sendMessage(IChatBaseComponent message, UUID sender) {
        }

        @Override
        public boolean shouldSendSuccess() {
            return false;
        }

        @Override
        public boolean shouldSendFailure() {
            return false;
        }

        @Override
        public boolean shouldBroadcastCommands() {
            return false;
        }
    };

    void sendMessage(IChatBaseComponent message, UUID sender);

    boolean shouldSendSuccess();

    boolean shouldSendFailure();

    boolean shouldBroadcastCommands();

    default boolean alwaysAccepts() {
        return false;
    }
}
