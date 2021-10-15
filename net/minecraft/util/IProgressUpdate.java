package net.minecraft.util;

import net.minecraft.network.chat.IChatBaseComponent;

public interface IProgressUpdate {
    void progressStartNoAbort(IChatBaseComponent title);

    void progressStart(IChatBaseComponent title);

    void progressStage(IChatBaseComponent task);

    void progressStagePercentage(int percentage);

    void stop();
}
