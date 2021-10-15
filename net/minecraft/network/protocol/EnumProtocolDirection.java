package net.minecraft.network.protocol;

public enum EnumProtocolDirection {
    SERVERBOUND,
    CLIENTBOUND;

    public EnumProtocolDirection getOpposite() {
        return this == CLIENTBOUND ? SERVERBOUND : CLIENTBOUND;
    }
}
