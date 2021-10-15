package net.minecraft.world.level.block;

import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.world.level.block.state.BlockBase;

public class BlockWoodButton extends BlockButtonAbstract {
    protected BlockWoodButton(BlockBase.Info settings) {
        super(true, settings);
    }

    @Override
    protected SoundEffect getSound(boolean powered) {
        return powered ? SoundEffects.WOODEN_BUTTON_CLICK_ON : SoundEffects.WOODEN_BUTTON_CLICK_OFF;
    }
}
