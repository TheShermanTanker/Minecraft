package net.minecraft.network.syncher;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.IRegistry;
import net.minecraft.core.Vector3f;
import net.minecraft.core.particles.Particle;
import net.minecraft.core.particles.ParticleParam;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.util.RegistryID;
import net.minecraft.world.entity.EntityPose;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.IBlockData;

public class DataWatcherRegistry {
    private static final RegistryID<DataWatcherSerializer<?>> SERIALIZERS = RegistryID.create(16);
    public static final DataWatcherSerializer<Byte> BYTE = new DataWatcherSerializer<Byte>() {
        @Override
        public void write(PacketDataSerializer buf, Byte value) {
            buf.writeByte(value);
        }

        @Override
        public Byte read(PacketDataSerializer friendlyByteBuf) {
            return friendlyByteBuf.readByte();
        }

        @Override
        public Byte copy(Byte value) {
            return value;
        }
    };
    public static final DataWatcherSerializer<Integer> INT = new DataWatcherSerializer<Integer>() {
        @Override
        public void write(PacketDataSerializer buf, Integer value) {
            buf.writeVarInt(value);
        }

        @Override
        public Integer read(PacketDataSerializer friendlyByteBuf) {
            return friendlyByteBuf.readVarInt();
        }

        @Override
        public Integer copy(Integer value) {
            return value;
        }
    };
    public static final DataWatcherSerializer<Float> FLOAT = new DataWatcherSerializer<Float>() {
        @Override
        public void write(PacketDataSerializer buf, Float value) {
            buf.writeFloat(value);
        }

        @Override
        public Float read(PacketDataSerializer friendlyByteBuf) {
            return friendlyByteBuf.readFloat();
        }

        @Override
        public Float copy(Float value) {
            return value;
        }
    };
    public static final DataWatcherSerializer<String> STRING = new DataWatcherSerializer<String>() {
        @Override
        public void write(PacketDataSerializer buf, String value) {
            buf.writeUtf(value);
        }

        @Override
        public String read(PacketDataSerializer friendlyByteBuf) {
            return friendlyByteBuf.readUtf();
        }

        @Override
        public String copy(String value) {
            return value;
        }
    };
    public static final DataWatcherSerializer<IChatBaseComponent> COMPONENT = new DataWatcherSerializer<IChatBaseComponent>() {
        @Override
        public void write(PacketDataSerializer buf, IChatBaseComponent value) {
            buf.writeComponent(value);
        }

        @Override
        public IChatBaseComponent read(PacketDataSerializer friendlyByteBuf) {
            return friendlyByteBuf.readComponent();
        }

        @Override
        public IChatBaseComponent copy(IChatBaseComponent value) {
            return value;
        }
    };
    public static final DataWatcherSerializer<Optional<IChatBaseComponent>> OPTIONAL_COMPONENT = new DataWatcherSerializer<Optional<IChatBaseComponent>>() {
        @Override
        public void write(PacketDataSerializer buf, Optional<IChatBaseComponent> value) {
            if (value.isPresent()) {
                buf.writeBoolean(true);
                buf.writeComponent(value.get());
            } else {
                buf.writeBoolean(false);
            }

        }

        @Override
        public Optional<IChatBaseComponent> read(PacketDataSerializer friendlyByteBuf) {
            return friendlyByteBuf.readBoolean() ? Optional.of(friendlyByteBuf.readComponent()) : Optional.empty();
        }

        @Override
        public Optional<IChatBaseComponent> copy(Optional<IChatBaseComponent> value) {
            return value;
        }
    };
    public static final DataWatcherSerializer<ItemStack> ITEM_STACK = new DataWatcherSerializer<ItemStack>() {
        @Override
        public void write(PacketDataSerializer buf, ItemStack value) {
            buf.writeItem(value);
        }

        @Override
        public ItemStack read(PacketDataSerializer friendlyByteBuf) {
            return friendlyByteBuf.readItem();
        }

        @Override
        public ItemStack copy(ItemStack value) {
            return value.cloneItemStack();
        }
    };
    public static final DataWatcherSerializer<Optional<IBlockData>> BLOCK_STATE = new DataWatcherSerializer<Optional<IBlockData>>() {
        @Override
        public void write(PacketDataSerializer buf, Optional<IBlockData> value) {
            if (value.isPresent()) {
                buf.writeVarInt(Block.getCombinedId(value.get()));
            } else {
                buf.writeVarInt(0);
            }

        }

        @Override
        public Optional<IBlockData> read(PacketDataSerializer friendlyByteBuf) {
            int i = friendlyByteBuf.readVarInt();
            return i == 0 ? Optional.empty() : Optional.of(Block.getByCombinedId(i));
        }

        @Override
        public Optional<IBlockData> copy(Optional<IBlockData> value) {
            return value;
        }
    };
    public static final DataWatcherSerializer<Boolean> BOOLEAN = new DataWatcherSerializer<Boolean>() {
        @Override
        public void write(PacketDataSerializer buf, Boolean value) {
            buf.writeBoolean(value);
        }

        @Override
        public Boolean read(PacketDataSerializer friendlyByteBuf) {
            return friendlyByteBuf.readBoolean();
        }

        @Override
        public Boolean copy(Boolean value) {
            return value;
        }
    };
    public static final DataWatcherSerializer<ParticleParam> PARTICLE = new DataWatcherSerializer<ParticleParam>() {
        @Override
        public void write(PacketDataSerializer buf, ParticleParam value) {
            buf.writeVarInt(IRegistry.PARTICLE_TYPE.getId(value.getParticle()));
            value.writeToNetwork(buf);
        }

        @Override
        public ParticleParam read(PacketDataSerializer friendlyByteBuf) {
            return this.readParticle(friendlyByteBuf, IRegistry.PARTICLE_TYPE.fromId(friendlyByteBuf.readVarInt()));
        }

        private <T extends ParticleParam> T readParticle(PacketDataSerializer buf, Particle<T> type) {
            return type.getDeserializer().fromNetwork(type, buf);
        }

        @Override
        public ParticleParam copy(ParticleParam value) {
            return value;
        }
    };
    public static final DataWatcherSerializer<Vector3f> ROTATIONS = new DataWatcherSerializer<Vector3f>() {
        @Override
        public void write(PacketDataSerializer buf, Vector3f value) {
            buf.writeFloat(value.getX());
            buf.writeFloat(value.getY());
            buf.writeFloat(value.getZ());
        }

        @Override
        public Vector3f read(PacketDataSerializer friendlyByteBuf) {
            return new Vector3f(friendlyByteBuf.readFloat(), friendlyByteBuf.readFloat(), friendlyByteBuf.readFloat());
        }

        @Override
        public Vector3f copy(Vector3f value) {
            return value;
        }
    };
    public static final DataWatcherSerializer<BlockPosition> BLOCK_POS = new DataWatcherSerializer<BlockPosition>() {
        @Override
        public void write(PacketDataSerializer buf, BlockPosition value) {
            buf.writeBlockPos(value);
        }

        @Override
        public BlockPosition read(PacketDataSerializer friendlyByteBuf) {
            return friendlyByteBuf.readBlockPos();
        }

        @Override
        public BlockPosition copy(BlockPosition value) {
            return value;
        }
    };
    public static final DataWatcherSerializer<Optional<BlockPosition>> OPTIONAL_BLOCK_POS = new DataWatcherSerializer<Optional<BlockPosition>>() {
        @Override
        public void write(PacketDataSerializer buf, Optional<BlockPosition> value) {
            buf.writeBoolean(value.isPresent());
            if (value.isPresent()) {
                buf.writeBlockPos(value.get());
            }

        }

        @Override
        public Optional<BlockPosition> read(PacketDataSerializer friendlyByteBuf) {
            return !friendlyByteBuf.readBoolean() ? Optional.empty() : Optional.of(friendlyByteBuf.readBlockPos());
        }

        @Override
        public Optional<BlockPosition> copy(Optional<BlockPosition> value) {
            return value;
        }
    };
    public static final DataWatcherSerializer<EnumDirection> DIRECTION = new DataWatcherSerializer<EnumDirection>() {
        @Override
        public void write(PacketDataSerializer buf, EnumDirection value) {
            buf.writeEnum(value);
        }

        @Override
        public EnumDirection read(PacketDataSerializer friendlyByteBuf) {
            return friendlyByteBuf.readEnum(EnumDirection.class);
        }

        @Override
        public EnumDirection copy(EnumDirection value) {
            return value;
        }
    };
    public static final DataWatcherSerializer<Optional<UUID>> OPTIONAL_UUID = new DataWatcherSerializer<Optional<UUID>>() {
        @Override
        public void write(PacketDataSerializer buf, Optional<UUID> value) {
            buf.writeBoolean(value.isPresent());
            if (value.isPresent()) {
                buf.writeUUID(value.get());
            }

        }

        @Override
        public Optional<UUID> read(PacketDataSerializer friendlyByteBuf) {
            return !friendlyByteBuf.readBoolean() ? Optional.empty() : Optional.of(friendlyByteBuf.readUUID());
        }

        @Override
        public Optional<UUID> copy(Optional<UUID> value) {
            return value;
        }
    };
    public static final DataWatcherSerializer<NBTTagCompound> COMPOUND_TAG = new DataWatcherSerializer<NBTTagCompound>() {
        @Override
        public void write(PacketDataSerializer buf, NBTTagCompound value) {
            buf.writeNbt(value);
        }

        @Override
        public NBTTagCompound read(PacketDataSerializer friendlyByteBuf) {
            return friendlyByteBuf.readNbt();
        }

        @Override
        public NBTTagCompound copy(NBTTagCompound value) {
            return value.copy();
        }
    };
    public static final DataWatcherSerializer<VillagerData> VILLAGER_DATA = new DataWatcherSerializer<VillagerData>() {
        @Override
        public void write(PacketDataSerializer buf, VillagerData value) {
            buf.writeVarInt(IRegistry.VILLAGER_TYPE.getId(value.getType()));
            buf.writeVarInt(IRegistry.VILLAGER_PROFESSION.getId(value.getProfession()));
            buf.writeVarInt(value.getLevel());
        }

        @Override
        public VillagerData read(PacketDataSerializer friendlyByteBuf) {
            return new VillagerData(IRegistry.VILLAGER_TYPE.fromId(friendlyByteBuf.readVarInt()), IRegistry.VILLAGER_PROFESSION.fromId(friendlyByteBuf.readVarInt()), friendlyByteBuf.readVarInt());
        }

        @Override
        public VillagerData copy(VillagerData value) {
            return value;
        }
    };
    public static final DataWatcherSerializer<OptionalInt> OPTIONAL_UNSIGNED_INT = new DataWatcherSerializer<OptionalInt>() {
        @Override
        public void write(PacketDataSerializer buf, OptionalInt value) {
            buf.writeVarInt(value.orElse(-1) + 1);
        }

        @Override
        public OptionalInt read(PacketDataSerializer friendlyByteBuf) {
            int i = friendlyByteBuf.readVarInt();
            return i == 0 ? OptionalInt.empty() : OptionalInt.of(i - 1);
        }

        @Override
        public OptionalInt copy(OptionalInt value) {
            return value;
        }
    };
    public static final DataWatcherSerializer<EntityPose> POSE = new DataWatcherSerializer<EntityPose>() {
        @Override
        public void write(PacketDataSerializer buf, EntityPose value) {
            buf.writeEnum(value);
        }

        @Override
        public EntityPose read(PacketDataSerializer friendlyByteBuf) {
            return friendlyByteBuf.readEnum(EntityPose.class);
        }

        @Override
        public EntityPose copy(EntityPose value) {
            return value;
        }
    };

    public static void registerSerializer(DataWatcherSerializer<?> handler) {
        SERIALIZERS.add(handler);
    }

    @Nullable
    public static DataWatcherSerializer<?> getSerializer(int id) {
        return SERIALIZERS.fromId(id);
    }

    public static int getSerializedId(DataWatcherSerializer<?> handler) {
        return SERIALIZERS.getId(handler);
    }

    private DataWatcherRegistry() {
    }

    static {
        registerSerializer(BYTE);
        registerSerializer(INT);
        registerSerializer(FLOAT);
        registerSerializer(STRING);
        registerSerializer(COMPONENT);
        registerSerializer(OPTIONAL_COMPONENT);
        registerSerializer(ITEM_STACK);
        registerSerializer(BOOLEAN);
        registerSerializer(ROTATIONS);
        registerSerializer(BLOCK_POS);
        registerSerializer(OPTIONAL_BLOCK_POS);
        registerSerializer(DIRECTION);
        registerSerializer(OPTIONAL_UUID);
        registerSerializer(BLOCK_STATE);
        registerSerializer(COMPOUND_TAG);
        registerSerializer(PARTICLE);
        registerSerializer(VILLAGER_DATA);
        registerSerializer(OPTIONAL_UNSIGNED_INT);
        registerSerializer(POSE);
    }
}
