package net.minecraft.world.level.block.state.properties;

import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.tags.TagsBlock;
import net.minecraft.util.INamable;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.material.Material;

public enum BlockPropertyInstrument implements INamable {
    HARP("harp", SoundEffects.NOTE_BLOCK_HARP),
    BASEDRUM("basedrum", SoundEffects.NOTE_BLOCK_BASEDRUM),
    SNARE("snare", SoundEffects.NOTE_BLOCK_SNARE),
    HAT("hat", SoundEffects.NOTE_BLOCK_HAT),
    BASS("bass", SoundEffects.NOTE_BLOCK_BASS),
    FLUTE("flute", SoundEffects.NOTE_BLOCK_FLUTE),
    BELL("bell", SoundEffects.NOTE_BLOCK_BELL),
    GUITAR("guitar", SoundEffects.NOTE_BLOCK_GUITAR),
    CHIME("chime", SoundEffects.NOTE_BLOCK_CHIME),
    XYLOPHONE("xylophone", SoundEffects.NOTE_BLOCK_XYLOPHONE),
    IRON_XYLOPHONE("iron_xylophone", SoundEffects.NOTE_BLOCK_IRON_XYLOPHONE),
    COW_BELL("cow_bell", SoundEffects.NOTE_BLOCK_COW_BELL),
    DIDGERIDOO("didgeridoo", SoundEffects.NOTE_BLOCK_DIDGERIDOO),
    BIT("bit", SoundEffects.NOTE_BLOCK_BIT),
    BANJO("banjo", SoundEffects.NOTE_BLOCK_BANJO),
    PLING("pling", SoundEffects.NOTE_BLOCK_PLING);

    private final String name;
    private final SoundEffect soundEvent;

    private BlockPropertyInstrument(String name, SoundEffect sound) {
        this.name = name;
        this.soundEvent = sound;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public SoundEffect getSoundEvent() {
        return this.soundEvent;
    }

    public static BlockPropertyInstrument byState(IBlockData state) {
        if (state.is(Blocks.CLAY)) {
            return FLUTE;
        } else if (state.is(Blocks.GOLD_BLOCK)) {
            return BELL;
        } else if (state.is(TagsBlock.WOOL)) {
            return GUITAR;
        } else if (state.is(Blocks.PACKED_ICE)) {
            return CHIME;
        } else if (state.is(Blocks.BONE_BLOCK)) {
            return XYLOPHONE;
        } else if (state.is(Blocks.IRON_BLOCK)) {
            return IRON_XYLOPHONE;
        } else if (state.is(Blocks.SOUL_SAND)) {
            return COW_BELL;
        } else if (state.is(Blocks.PUMPKIN)) {
            return DIDGERIDOO;
        } else if (state.is(Blocks.EMERALD_BLOCK)) {
            return BIT;
        } else if (state.is(Blocks.HAY_BLOCK)) {
            return BANJO;
        } else if (state.is(Blocks.GLOWSTONE)) {
            return PLING;
        } else {
            Material material = state.getMaterial();
            if (material == Material.STONE) {
                return BASEDRUM;
            } else if (material == Material.SAND) {
                return SNARE;
            } else if (material == Material.GLASS) {
                return HAT;
            } else {
                return material != Material.WOOD && material != Material.NETHER_WOOD ? HARP : BASS;
            }
        }
    }
}
