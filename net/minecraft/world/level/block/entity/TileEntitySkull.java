package net.minecraft.world.level.block.entity;

import com.google.common.collect.Iterables;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.properties.Property;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.SystemUtils;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.GameProfileSerializer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.protocol.game.PacketPlayOutTileEntityData;
import net.minecraft.server.players.UserCache;
import net.minecraft.util.UtilColor;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.IBlockData;

public class TileEntitySkull extends TileEntity {
    public static final String TAG_SKULL_OWNER = "SkullOwner";
    @Nullable
    private static UserCache profileCache;
    @Nullable
    private static MinecraftSessionService sessionService;
    @Nullable
    private static Executor mainThreadExecutor;
    @Nullable
    public GameProfile owner;
    private int mouthTickCount;
    private boolean isMovingMouth;

    public TileEntitySkull(BlockPosition pos, IBlockData state) {
        super(TileEntityTypes.SKULL, pos, state);
    }

    public static void setProfileCache(UserCache value) {
        profileCache = value;
    }

    public static void setSessionService(MinecraftSessionService value) {
        sessionService = value;
    }

    public static void setMainThreadExecutor(Executor executor) {
        mainThreadExecutor = executor;
    }

    @Override
    public NBTTagCompound save(NBTTagCompound nbt) {
        super.save(nbt);
        if (this.owner != null) {
            NBTTagCompound compoundTag = new NBTTagCompound();
            GameProfileSerializer.serialize(compoundTag, this.owner);
            nbt.set("SkullOwner", compoundTag);
        }

        return nbt;
    }

    @Override
    public void load(NBTTagCompound nbt) {
        super.load(nbt);
        if (nbt.hasKeyOfType("SkullOwner", 10)) {
            this.setGameProfile(GameProfileSerializer.deserialize(nbt.getCompound("SkullOwner")));
        } else if (nbt.hasKeyOfType("ExtraType", 8)) {
            String string = nbt.getString("ExtraType");
            if (!UtilColor.isNullOrEmpty(string)) {
                this.setGameProfile(new GameProfile((UUID)null, string));
            }
        }

    }

    public static void dragonHeadAnimation(World world, BlockPosition pos, IBlockData state, TileEntitySkull blockEntity) {
        if (world.isBlockIndirectlyPowered(pos)) {
            blockEntity.isMovingMouth = true;
            ++blockEntity.mouthTickCount;
        } else {
            blockEntity.isMovingMouth = false;
        }

    }

    public float getMouthAnimation(float tickDelta) {
        return this.isMovingMouth ? (float)this.mouthTickCount + tickDelta : (float)this.mouthTickCount;
    }

    @Nullable
    public GameProfile getOwnerProfile() {
        return this.owner;
    }

    @Nullable
    @Override
    public PacketPlayOutTileEntityData getUpdatePacket() {
        return new PacketPlayOutTileEntityData(this.worldPosition, 4, this.getUpdateTag());
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return this.save(new NBTTagCompound());
    }

    public void setGameProfile(@Nullable GameProfile owner) {
        synchronized(this) {
            this.owner = owner;
        }

        this.updateOwnerProfile();
    }

    private void updateOwnerProfile() {
        updateGameprofile(this.owner, (owner) -> {
            this.owner = owner;
            this.update();
        });
    }

    public static void updateGameprofile(@Nullable GameProfile owner, Consumer<GameProfile> callback) {
        if (owner != null && !UtilColor.isNullOrEmpty(owner.getName()) && (!owner.isComplete() || !owner.getProperties().containsKey("textures")) && profileCache != null && sessionService != null) {
            profileCache.getAsync(owner.getName(), (profile) -> {
                SystemUtils.backgroundExecutor().execute(() -> {
                    SystemUtils.ifElse(profile, (profilex) -> {
                        Property property = Iterables.getFirst(profilex.getProperties().get("textures"), (Property)null);
                        if (property == null) {
                            profilex = sessionService.fillProfileProperties(profilex, true);
                        }

                        GameProfile gameProfile = profilex;
                        mainThreadExecutor.execute(() -> {
                            profileCache.add(gameProfile);
                            callback.accept(gameProfile);
                        });
                    }, () -> {
                        mainThreadExecutor.execute(() -> {
                            callback.accept(owner);
                        });
                    });
                });
            });
        } else {
            callback.accept(owner);
        }
    }
}
