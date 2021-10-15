package net.minecraft.world.level.material;

public final class Material {
    public static final Material AIR = (new Material.Builder(MaterialMapColor.NONE)).noCollider().notSolidBlocking().nonSolid().replaceable().build();
    public static final Material STRUCTURAL_AIR = (new Material.Builder(MaterialMapColor.NONE)).noCollider().notSolidBlocking().nonSolid().replaceable().build();
    public static final Material PORTAL = (new Material.Builder(MaterialMapColor.NONE)).noCollider().notSolidBlocking().nonSolid().notPushable().build();
    public static final Material CLOTH_DECORATION = (new Material.Builder(MaterialMapColor.WOOL)).noCollider().notSolidBlocking().nonSolid().flammable().build();
    public static final Material PLANT = (new Material.Builder(MaterialMapColor.PLANT)).noCollider().notSolidBlocking().nonSolid().destroyOnPush().build();
    public static final Material WATER_PLANT = (new Material.Builder(MaterialMapColor.WATER)).noCollider().notSolidBlocking().nonSolid().destroyOnPush().build();
    public static final Material REPLACEABLE_PLANT = (new Material.Builder(MaterialMapColor.PLANT)).noCollider().notSolidBlocking().nonSolid().destroyOnPush().replaceable().flammable().build();
    public static final Material REPLACEABLE_FIREPROOF_PLANT = (new Material.Builder(MaterialMapColor.PLANT)).noCollider().notSolidBlocking().nonSolid().destroyOnPush().replaceable().build();
    public static final Material REPLACEABLE_WATER_PLANT = (new Material.Builder(MaterialMapColor.WATER)).noCollider().notSolidBlocking().nonSolid().destroyOnPush().replaceable().build();
    public static final Material WATER = (new Material.Builder(MaterialMapColor.WATER)).noCollider().notSolidBlocking().nonSolid().destroyOnPush().replaceable().liquid().build();
    public static final Material BUBBLE_COLUMN = (new Material.Builder(MaterialMapColor.WATER)).noCollider().notSolidBlocking().nonSolid().destroyOnPush().replaceable().liquid().build();
    public static final Material LAVA = (new Material.Builder(MaterialMapColor.FIRE)).noCollider().notSolidBlocking().nonSolid().destroyOnPush().replaceable().liquid().build();
    public static final Material TOP_SNOW = (new Material.Builder(MaterialMapColor.SNOW)).noCollider().notSolidBlocking().nonSolid().destroyOnPush().replaceable().build();
    public static final Material FIRE = (new Material.Builder(MaterialMapColor.NONE)).noCollider().notSolidBlocking().nonSolid().destroyOnPush().replaceable().build();
    public static final Material DECORATION = (new Material.Builder(MaterialMapColor.NONE)).noCollider().notSolidBlocking().nonSolid().destroyOnPush().build();
    public static final Material WEB = (new Material.Builder(MaterialMapColor.WOOL)).noCollider().notSolidBlocking().destroyOnPush().build();
    public static final Material SCULK = (new Material.Builder(MaterialMapColor.COLOR_BLACK)).build();
    public static final Material BUILDABLE_GLASS = (new Material.Builder(MaterialMapColor.NONE)).build();
    public static final Material CLAY = (new Material.Builder(MaterialMapColor.CLAY)).build();
    public static final Material DIRT = (new Material.Builder(MaterialMapColor.DIRT)).build();
    public static final Material GRASS = (new Material.Builder(MaterialMapColor.GRASS)).build();
    public static final Material ICE_SOLID = (new Material.Builder(MaterialMapColor.ICE)).build();
    public static final Material SAND = (new Material.Builder(MaterialMapColor.SAND)).build();
    public static final Material SPONGE = (new Material.Builder(MaterialMapColor.COLOR_YELLOW)).build();
    public static final Material SHULKER_SHELL = (new Material.Builder(MaterialMapColor.COLOR_PURPLE)).build();
    public static final Material WOOD = (new Material.Builder(MaterialMapColor.WOOD)).flammable().build();
    public static final Material NETHER_WOOD = (new Material.Builder(MaterialMapColor.WOOD)).build();
    public static final Material BAMBOO_SAPLING = (new Material.Builder(MaterialMapColor.WOOD)).flammable().destroyOnPush().noCollider().build();
    public static final Material BAMBOO = (new Material.Builder(MaterialMapColor.WOOD)).flammable().destroyOnPush().build();
    public static final Material WOOL = (new Material.Builder(MaterialMapColor.WOOL)).flammable().build();
    public static final Material EXPLOSIVE = (new Material.Builder(MaterialMapColor.FIRE)).flammable().notSolidBlocking().build();
    public static final Material LEAVES = (new Material.Builder(MaterialMapColor.PLANT)).flammable().notSolidBlocking().destroyOnPush().build();
    public static final Material GLASS = (new Material.Builder(MaterialMapColor.NONE)).notSolidBlocking().build();
    public static final Material ICE = (new Material.Builder(MaterialMapColor.ICE)).notSolidBlocking().build();
    public static final Material CACTUS = (new Material.Builder(MaterialMapColor.PLANT)).notSolidBlocking().destroyOnPush().build();
    public static final Material STONE = (new Material.Builder(MaterialMapColor.STONE)).build();
    public static final Material METAL = (new Material.Builder(MaterialMapColor.METAL)).build();
    public static final Material SNOW = (new Material.Builder(MaterialMapColor.SNOW)).build();
    public static final Material HEAVY_METAL = (new Material.Builder(MaterialMapColor.METAL)).notPushable().build();
    public static final Material BARRIER = (new Material.Builder(MaterialMapColor.NONE)).notPushable().build();
    public static final Material PISTON = (new Material.Builder(MaterialMapColor.STONE)).notPushable().build();
    public static final Material MOSS = (new Material.Builder(MaterialMapColor.PLANT)).destroyOnPush().build();
    public static final Material VEGETABLE = (new Material.Builder(MaterialMapColor.PLANT)).destroyOnPush().build();
    public static final Material EGG = (new Material.Builder(MaterialMapColor.PLANT)).destroyOnPush().build();
    public static final Material CAKE = (new Material.Builder(MaterialMapColor.NONE)).destroyOnPush().build();
    public static final Material AMETHYST = (new Material.Builder(MaterialMapColor.COLOR_PURPLE)).build();
    public static final Material POWDER_SNOW = (new Material.Builder(MaterialMapColor.SNOW)).nonSolid().noCollider().build();
    private final MaterialMapColor color;
    private final EnumPistonReaction pushReaction;
    private final boolean blocksMotion;
    private final boolean flammable;
    private final boolean liquid;
    private final boolean solidBlocking;
    private final boolean replaceable;
    private final boolean solid;

    public Material(MaterialMapColor color, boolean liquid, boolean solid, boolean blocksMovement, boolean blocksLight, boolean breakByHand, boolean burnable, EnumPistonReaction pistonBehavior) {
        this.color = color;
        this.liquid = liquid;
        this.solid = solid;
        this.blocksMotion = blocksMovement;
        this.solidBlocking = blocksLight;
        this.flammable = breakByHand;
        this.replaceable = burnable;
        this.pushReaction = pistonBehavior;
    }

    public boolean isLiquid() {
        return this.liquid;
    }

    public boolean isBuildable() {
        return this.solid;
    }

    public boolean isSolid() {
        return this.blocksMotion;
    }

    public boolean isBurnable() {
        return this.flammable;
    }

    public boolean isReplaceable() {
        return this.replaceable;
    }

    public boolean isSolidBlocking() {
        return this.solidBlocking;
    }

    public EnumPistonReaction getPushReaction() {
        return this.pushReaction;
    }

    public MaterialMapColor getColor() {
        return this.color;
    }

    public static class Builder {
        private EnumPistonReaction pushReaction = EnumPistonReaction.NORMAL;
        private boolean blocksMotion = true;
        private boolean flammable;
        private boolean liquid;
        private boolean replaceable;
        private boolean solid = true;
        private final MaterialMapColor color;
        private boolean solidBlocking = true;

        public Builder(MaterialMapColor color) {
            this.color = color;
        }

        public Material.Builder liquid() {
            this.liquid = true;
            return this;
        }

        public Material.Builder nonSolid() {
            this.solid = false;
            return this;
        }

        public Material.Builder noCollider() {
            this.blocksMotion = false;
            return this;
        }

        Material.Builder notSolidBlocking() {
            this.solidBlocking = false;
            return this;
        }

        protected Material.Builder flammable() {
            this.flammable = true;
            return this;
        }

        public Material.Builder replaceable() {
            this.replaceable = true;
            return this;
        }

        protected Material.Builder destroyOnPush() {
            this.pushReaction = EnumPistonReaction.DESTROY;
            return this;
        }

        protected Material.Builder notPushable() {
            this.pushReaction = EnumPistonReaction.BLOCK;
            return this;
        }

        public Material build() {
            return new Material(this.color, this.liquid, this.solid, this.blocksMotion, this.solidBlocking, this.flammable, this.replaceable, this.pushReaction);
        }
    }
}
