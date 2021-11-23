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
    private static final RegistryID<DataWatcherSerializer<?>> SERIALIZERS = new RegistryID<>(16);
    public static final DataWatcherSerializer<Byte> BYTE = new DataWatcherSerializer<Byte>() {
        @Override
        public void a(PacketDataSerializer buf, Byte value) {
            buf.writeByte(value);
        }

        @Override
        public Byte read(PacketDataSerializer friendlyByteBuf) {
            return friendlyByteBuf.readByte();
        }

        @Override
        public Byte a(Byte value) {
            return value;
        }
    };
    public static final DataWatcherSerializer<Integer> INT = new DataWatcherSerializer<Integer>() {
        @Override
        public void a(PacketDataSerializer buf, Integer value) {
            buf.writeVarInt(value);
        }

        @Override
        public Integer b(PacketDataSerializer friendlyByteBuf) {
            return friendlyByteBuf.readVarInt();
        }

        @Override
        public Integer a(Integer value) {
            return value;
        }
    };
    public static final DataWatcherSerializer<Float> FLOAT = new DataWatcherSerializer<Float>() {
        @Override
        public void a(PacketDataSerializer buf, Float value) {
            buf.writeFloat(value);
        }

        @Override
        public Float b(PacketDataSerializer friendlyByteBuf) {
            return friendlyByteBuf.readFloat();
        }

        @Override
        public Float a(Float value) {
            return value;
        }
    };
    public static final DataWatcherSerializer<String> STRING = new DataWatcherSerializer<String>() {
        @Override
        public void a(PacketDataSerializer buf, String value) {
            buf.writeUtf(value);
        }

        @Override
        public String b(PacketDataSerializer friendlyByteBuf) {
            return friendlyByteBuf.readUtf();
        }

        @Override
        public String a(String value) {
            return value;
        }
    };
    public static final DataWatcherSerializer<IChatBaseComponent> COMPONENT = new DataWatcherSerializer<IChatBaseComponent>() {
        @Override
        public void a(PacketDataSerializer buf, IChatBaseComponent value) {
            buf.writeComponent(value);
        }

        @Override
        public IChatBaseComponent b(PacketDataSerializer friendlyByteBuf) {
            return friendlyByteBuf.readComponent();
        }

        @Override
        public IChatBaseComponent a(IChatBaseComponent value) {
            return value;
        }
    };
    public static final DataWatcherSerializer<Optional<IChatBaseComponent>> OPTIONAL_COMPONENT = new DataWatcherSerializer<Optional<IChatBaseComponent>>() {
        @Override
        public void a(PacketDataSerializer buf, Optional<IChatBaseComponent> value) {
            if (value.isPresent()) {
                buf.writeBoolean(true);
                buf.writeComponent(value.get());
            } else {
                buf.writeBoolean(false);
            }

        }

        @Override
        public Optional<IChatBaseComponent> b(PacketDataSerializer friendlyByteBuf) {
            return friendlyByteBuf.readBoolean() ? Optional.of(friendlyByteBuf.readComponent()) : Optional.empty();
        }

        @Override
        public Optional<IChatBaseComponent> a(Optional<IChatBaseComponent> value) {
            return value;
        }
    };
    public static final DataWatcherSerializer<ItemStack> ITEM_STACK = new DataWatcherSerializer<ItemStack>() {
        @Override
        public void a(PacketDataSerializer buf, ItemStack value) {
            buf.writeItem(value);
        }

        @Override
        public ItemStack b(PacketDataSerializer friendlyByteBuf) {
            return friendlyByteBuf.readItem();
        }

        @Override
        public ItemStack a(ItemStack value) {
            return value.cloneItemStack();
        }
    };
    public static final DataWatcherSerializer<Optional<IBlockData>> BLOCK_STATE = new DataWatcherSerializer<Optional<IBlockData>>() {
        @Override
        public void a(PacketDataSerializer buf, Optional<IBlockData> value) {
            if (value.isPresent()) {
                buf.writeVarInt(Block.getCombinedId(value.get()));
            } else {
                buf.writeVarInt(0);
            }

        }

        @Override
        public Optional<IBlockData> b(PacketDataSerializer friendlyByteBuf) {
            int i = friendlyByteBuf.readVarInt();
            return i == 0 ? Optional.empty() : Optional.of(Block.getByCombinedId(i));
        }

        @Override
        public Optional<IBlockData> a(Optional<IBlockData> value) {
            return value;
        }
    };
    public static final DataWatcherSerializer<Boolean> BOOLEAN = new DataWatcherSerializer<Boolean>() {
        @Override
        public void a(PacketDataSerializer buf, Boolean value) {
            buf.writeBoolean(value);
        }

        @Override
        public Boolean b(PacketDataSerializer friendlyByteBuf) {
            return friendlyByteBuf.readBoolean();
        }

        @Override
        public Boolean a(Boolean value) {
            return value;
        }
    };
    public static final DataWatcherSerializer<ParticleParam> PARTICLE = new DataWatcherSerializer<ParticleParam>() {
        @Override
        public void a(PacketDataSerializer buf, ParticleParam value) {
            buf.writeVarInt(IRegistry.PARTICLE_TYPE.getId(value.getParticle()));
            value.writeToNetwork(buf);
        }

        @Override
        public ParticleParam b(PacketDataSerializer friendlyByteBuf) {
            return this.a(friendlyByteBuf, IRegistry.PARTICLE_TYPE.fromId(friendlyByteBuf.readVarInt()));
        }

        private <T extends ParticleParam> T a(PacketDataSerializer friendlyByteBuf, Particle<T> particleType) {
            return particleType.getDeserializer().fromNetwork(particleType, friendlyByteBuf);
        }

        @Override
        public ParticleParam a(ParticleParam value) {
            return value;
        }
    };
    public static final DataWatcherSerializer<Vector3f> ROTATIONS = new DataWatcherSerializer<Vector3f>() {
        @Override
        public void a(PacketDataSerializer buf, Vector3f value) {
            buf.writeFloat(value.getX());
            buf.writeFloat(value.getY());
            buf.writeFloat(value.getZ());
        }

        @Override
        public Vector3f b(PacketDataSerializer friendlyByteBuf) {
            return new Vector3f(friendlyByteBuf.readFloat(), friendlyByteBuf.readFloat(), friendlyByteBuf.readFloat());
        }

        @Override
        public Vector3f a(Vector3f value) {
            return value;
        }
    };
    public static final DataWatcherSerializer<BlockPosition> BLOCK_POS = new DataWatcherSerializer<BlockPosition>() {
        @Override
        public void a(PacketDataSerializer buf, BlockPosition value) {
            buf.writeBlockPos(value);
        }

        @Override
        public BlockPosition b(PacketDataSerializer friendlyByteBuf) {
            return friendlyByteBuf.readBlockPos();
        }

        @Override
        public BlockPosition a(BlockPosition value) {
            return value;
        }
    };
    public static final DataWatcherSerializer<Optional<BlockPosition>> OPTIONAL_BLOCK_POS = new DataWatcherSerializer<Optional<BlockPosition>>() {
        @Override
        public void a(PacketDataSerializer buf, Optional<BlockPosition> value) {
            buf.writeBoolean(value.isPresent());
            if (value.isPresent()) {
                buf.writeBlockPos(value.get());
            }

        }

        @Override
        public Optional<BlockPosition> b(PacketDataSerializer friendlyByteBuf) {
            return !friendlyByteBuf.readBoolean() ? Optional.empty() : Optional.of(friendlyByteBuf.readBlockPos());
        }

        @Override
        public Optional<BlockPosition> a(Optional<BlockPosition> value) {
            return value;
        }
    };
    public static final DataWatcherSerializer<EnumDirection> DIRECTION = new DataWatcherSerializer<EnumDirection>() {
        @Override
        public void a(PacketDataSerializer buf, EnumDirection value) {
            buf.writeEnum(value);
        }

        @Override
        public EnumDirection b(PacketDataSerializer friendlyByteBuf) {
            return friendlyByteBuf.readEnum(EnumDirection.class);
        }

        @Override
        public EnumDirection a(EnumDirection value) {
            return value;
        }
    };
    public static final DataWatcherSerializer<Optional<UUID>> OPTIONAL_UUID = new DataWatcherSerializer<Optional<UUID>>() {
        @Override
        public void a(PacketDataSerializer buf, Optional<UUID> value) {
            buf.writeBoolean(value.isPresent());
            if (value.isPresent()) {
                buf.writeUUID(value.get());
            }

        }

        @Override
        public Optional<UUID> b(PacketDataSerializer friendlyByteBuf) {
            return !friendlyByteBuf.readBoolean() ? Optional.empty() : Optional.of(friendlyByteBuf.readUUID());
        }

        @Override
        public Optional<UUID> a(Optional<UUID> value) {
            return value;
        }
    };
    public static final DataWatcherSerializer<NBTTagCompound> COMPOUND_TAG = new DataWatcherSerializer<NBTTagCompound>() {
        @Override
        public void a(PacketDataSerializer buf, NBTTagCompound value) {
            buf.writeNbt(value);
        }

        @Override
        public NBTTagCompound b(PacketDataSerializer friendlyByteBuf) {
            return friendlyByteBuf.readNbt();
        }

        @Override
        public NBTTagCompound a(NBTTagCompound value) {
            return value.c();
        }
    };
    public static final DataWatcherSerializer<VillagerData> VILLAGER_DATA = new DataWatcherSerializer<VillagerData>() {
        @Override
        public void a(PacketDataSerializer buf, VillagerData value) {
            buf.writeVarInt(IRegistry.VILLAGER_TYPE.getId(value.getType()));
            buf.writeVarInt(IRegistry.VILLAGER_PROFESSION.getId(value.getProfession()));
            buf.writeVarInt(value.getLevel());
        }

        @Override
        public VillagerData b(PacketDataSerializer friendlyByteBuf) {
            return new VillagerData(IRegistry.VILLAGER_TYPE.fromId(friendlyByteBuf.readVarInt()), IRegistry.VILLAGER_PROFESSION.fromId(friendlyByteBuf.readVarInt()), friendlyByteBuf.readVarInt());
        }

        @Override
        public VillagerData a(VillagerData value) {
            return value;
        }
    };
    public static final DataWatcherSerializer<OptionalInt> OPTIONAL_UNSIGNED_INT = new DataWatcherSerializer<OptionalInt>() {
        @Override
        public void a(PacketDataSerializer buf, OptionalInt value) {
            buf.writeVarInt(value.orElse(-1) + 1);
        }

        @Override
        public OptionalInt b(PacketDataSerializer friendlyByteBuf) {
            int i = friendlyByteBuf.readVarInt();
            return i == 0 ? OptionalInt.empty() : OptionalInt.of(i - 1);
        }

        @Override
        public OptionalInt a(OptionalInt value) {
            return value;
        }
    };
    public static final DataWatcherSerializer<EntityPose> POSE = new DataWatcherSerializer<EntityPose>() {
        @Override
        public void a(PacketDataSerializer buf, EntityPose value) {
            buf.writeEnum(value);
        }

        @Override
        public EntityPose b(PacketDataSerializer friendlyByteBuf) {
            return friendlyByteBuf.readEnum(EntityPose.class);
        }

        @Override
        public EntityPose a(EntityPose value) {
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
