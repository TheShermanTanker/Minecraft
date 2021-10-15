package net.minecraft.world.level.block;

import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.world.level.block.state.BlockBase;

public class BlockStoneButton extends BlockButtonAbstract {
    protected BlockStoneButton(BlockBase.Info settings) {
        super(false, settings);
    }

    @Override
    protected SoundEffect getSound(boolean powered) {
        return powered ? SoundEffects.STONE_BUTTON_CLICK_ON : SoundEffects.STONE_BUTTON_CLICK_OFF;
    }
}
