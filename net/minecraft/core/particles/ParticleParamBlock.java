package net.minecraft.core.particles;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import net.minecraft.commands.arguments.blocks.ArgumentBlock;
import net.minecraft.core.IRegistry;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.IBlockData;

public class ParticleParamBlock implements ParticleParam {
    public static final ParticleParam.Deserializer<ParticleParamBlock> DESERIALIZER = new ParticleParam.Deserializer<ParticleParamBlock>() {
        @Override
        public ParticleParamBlock fromCommand(Particle<ParticleParamBlock> particleType, StringReader stringReader) throws CommandSyntaxException {
            stringReader.expect(' ');
            return new ParticleParamBlock(particleType, (new ArgumentBlock(stringReader, false)).parse(false).getBlockData());
        }

        @Override
        public ParticleParamBlock fromNetwork(Particle<ParticleParamBlock> particleType, PacketDataSerializer friendlyByteBuf) {
            return new ParticleParamBlock(particleType, Block.BLOCK_STATE_REGISTRY.fromId(friendlyByteBuf.readVarInt()));
        }
    };
    private final Particle<ParticleParamBlock> type;
    private final IBlockData state;

    public static Codec<ParticleParamBlock> codec(Particle<ParticleParamBlock> type) {
        return IBlockData.CODEC.xmap((state) -> {
            return new ParticleParamBlock(type, state);
        }, (effect) -> {
            return effect.state;
        });
    }

    public ParticleParamBlock(Particle<ParticleParamBlock> type, IBlockData blockState) {
        this.type = type;
        this.state = blockState;
    }

    @Override
    public void writeToNetwork(PacketDataSerializer buf) {
        buf.writeVarInt(Block.BLOCK_STATE_REGISTRY.getId(this.state));
    }

    @Override
    public String writeToString() {
        return IRegistry.PARTICLE_TYPE.getKey(this.getParticle()) + " " + ArgumentBlock.serialize(this.state);
    }

    @Override
    public Particle<ParticleParamBlock> getParticle() {
        return this.type;
    }

    public IBlockData getState() {
        return this.state;
    }
}
