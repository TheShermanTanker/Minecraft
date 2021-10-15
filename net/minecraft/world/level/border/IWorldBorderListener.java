package net.minecraft.world.level.border;

public interface IWorldBorderListener {
    void onBorderSizeSet(WorldBorder border, double size);

    void onBorderSizeLerping(WorldBorder border, double fromSize, double toSize, long time);

    void onBorderCenterSet(WorldBorder border, double centerX, double centerZ);

    void onBorderSetWarningTime(WorldBorder border, int warningTime);

    void onBorderSetWarningBlocks(WorldBorder border, int warningBlockDistance);

    void onBorderSetDamagePerBlock(WorldBorder border, double damagePerBlock);

    void onBorderSetDamageSafeZOne(WorldBorder border, double safeZoneRadius);

    public static class DelegateBorderChangeListener implements IWorldBorderListener {
        private final WorldBorder worldBorder;

        public DelegateBorderChangeListener(WorldBorder border) {
            this.worldBorder = border;
        }

        @Override
        public void onBorderSizeSet(WorldBorder border, double size) {
            this.worldBorder.setSize(size);
        }

        @Override
        public void onBorderSizeLerping(WorldBorder border, double fromSize, double toSize, long time) {
            this.worldBorder.transitionSizeBetween(fromSize, toSize, time);
        }

        @Override
        public void onBorderCenterSet(WorldBorder border, double centerX, double centerZ) {
            this.worldBorder.setCenter(centerX, centerZ);
        }

        @Override
        public void onBorderSetWarningTime(WorldBorder border, int warningTime) {
            this.worldBorder.setWarningTime(warningTime);
        }

        @Override
        public void onBorderSetWarningBlocks(WorldBorder border, int warningBlockDistance) {
            this.worldBorder.setWarningDistance(warningBlockDistance);
        }

        @Override
        public void onBorderSetDamagePerBlock(WorldBorder border, double damagePerBlock) {
            this.worldBorder.setDamageAmount(damagePerBlock);
        }

        @Override
        public void onBorderSetDamageSafeZOne(WorldBorder border, double safeZoneRadius) {
            this.worldBorder.setDamageBuffer(safeZoneRadius);
        }
    }
}
