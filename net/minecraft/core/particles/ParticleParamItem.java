package net.minecraft.core.particles;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import net.minecraft.commands.arguments.item.ArgumentParserItemStack;
import net.minecraft.commands.arguments.item.ArgumentPredicateItemStack;
import net.minecraft.core.IRegistry;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.world.item.ItemStack;

public class ParticleParamItem implements ParticleParam {
    public static final ParticleParam.Deserializer<ParticleParamItem> DESERIALIZER = new ParticleParam.Deserializer<ParticleParamItem>() {
        @Override
        public ParticleParamItem fromCommand(Particle<ParticleParamItem> particleType, StringReader stringReader) throws CommandSyntaxException {
            stringReader.expect(' ');
            ArgumentParserItemStack itemParser = (new ArgumentParserItemStack(stringReader, false)).parse();
            ItemStack itemStack = (new ArgumentPredicateItemStack(itemParser.getItem(), itemParser.getNbt())).createItemStack(1, false);
            return new ParticleParamItem(particleType, itemStack);
        }

        @Override
        public ParticleParamItem fromNetwork(Particle<ParticleParamItem> particleType, PacketDataSerializer friendlyByteBuf) {
            return new ParticleParamItem(particleType, friendlyByteBuf.readItem());
        }
    };
    private final Particle<ParticleParamItem> type;
    private final ItemStack itemStack;

    public static Codec<ParticleParamItem> codec(Particle<ParticleParamItem> particleType) {
        return ItemStack.CODEC.xmap((itemStack) -> {
            return new ParticleParamItem(particleType, itemStack);
        }, (itemParticleOption) -> {
            return itemParticleOption.itemStack;
        });
    }

    public ParticleParamItem(Particle<ParticleParamItem> type, ItemStack stack) {
        this.type = type;
        this.itemStack = stack;
    }

    @Override
    public void writeToNetwork(PacketDataSerializer buf) {
        buf.writeItem(this.itemStack);
    }

    @Override
    public String writeToString() {
        return IRegistry.PARTICLE_TYPE.getKey(this.getParticle()) + " " + (new ArgumentPredicateItemStack(this.itemStack.getItem(), this.itemStack.getTag())).serialize();
    }

    @Override
    public Particle<ParticleParamItem> getParticle() {
        return this.type;
    }

    public ItemStack getItem() {
        return this.itemStack;
    }
}
