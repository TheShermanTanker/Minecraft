package net.minecraft.world.entity.ai.village.poi;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.minecraft.SystemUtils;
import net.minecraft.core.IRegistry;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BlockBed;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockPropertyBedPart;

public class VillagePlaceType {
    private static final Supplier<Set<VillagePlaceType>> ALL_JOB_POI_TYPES = Suppliers.memoize(() -> {
        return IRegistry.VILLAGER_PROFESSION.stream().map(VillagerProfession::getJobPoiType).collect(Collectors.toSet());
    });
    public static final Predicate<VillagePlaceType> ALL_JOBS = (poiType) -> {
        return ALL_JOB_POI_TYPES.get().contains(poiType);
    };
    public static final Predicate<VillagePlaceType> ALL = (poiType) -> {
        return true;
    };
    private static final Set<IBlockData> BEDS = ImmutableList.of(Blocks.RED_BED, Blocks.BLACK_BED, Blocks.BLUE_BED, Blocks.BROWN_BED, Blocks.CYAN_BED, Blocks.GRAY_BED, Blocks.GREEN_BED, Blocks.LIGHT_BLUE_BED, Blocks.LIGHT_GRAY_BED, Blocks.LIME_BED, Blocks.MAGENTA_BED, Blocks.ORANGE_BED, Blocks.PINK_BED, Blocks.PURPLE_BED, Blocks.WHITE_BED, Blocks.YELLOW_BED).stream().flatMap((block) -> {
        return block.getStates().getPossibleStates().stream();
    }).filter((state) -> {
        return state.get(BlockBed.PART) == BlockPropertyBedPart.HEAD;
    }).collect(ImmutableSet.toImmutableSet());
    private static final Set<IBlockData> CAULDRONS = ImmutableList.of(Blocks.CAULDRON, Blocks.LAVA_CAULDRON, Blocks.WATER_CAULDRON, Blocks.POWDER_SNOW_CAULDRON).stream().flatMap((block) -> {
        return block.getStates().getPossibleStates().stream();
    }).collect(ImmutableSet.toImmutableSet());
    private static final Map<IBlockData, VillagePlaceType> TYPE_BY_STATE = Maps.newHashMap();
    public static final VillagePlaceType UNEMPLOYED = register("unemployed", ImmutableSet.of(), 1, ALL_JOBS, 1);
    public static final VillagePlaceType ARMORER = register("armorer", getBlockStates(Blocks.BLAST_FURNACE), 1, 1);
    public static final VillagePlaceType BUTCHER = register("butcher", getBlockStates(Blocks.SMOKER), 1, 1);
    public static final VillagePlaceType CARTOGRAPHER = register("cartographer", getBlockStates(Blocks.CARTOGRAPHY_TABLE), 1, 1);
    public static final VillagePlaceType CLERIC = register("cleric", getBlockStates(Blocks.BREWING_STAND), 1, 1);
    public static final VillagePlaceType FARMER = register("farmer", getBlockStates(Blocks.COMPOSTER), 1, 1);
    public static final VillagePlaceType FISHERMAN = register("fisherman", getBlockStates(Blocks.BARREL), 1, 1);
    public static final VillagePlaceType FLETCHER = register("fletcher", getBlockStates(Blocks.FLETCHING_TABLE), 1, 1);
    public static final VillagePlaceType LEATHERWORKER = register("leatherworker", CAULDRONS, 1, 1);
    public static final VillagePlaceType LIBRARIAN = register("librarian", getBlockStates(Blocks.LECTERN), 1, 1);
    public static final VillagePlaceType MASON = register("mason", getBlockStates(Blocks.STONECUTTER), 1, 1);
    public static final VillagePlaceType NITWIT = register("nitwit", ImmutableSet.of(), 1, 1);
    public static final VillagePlaceType SHEPHERD = register("shepherd", getBlockStates(Blocks.LOOM), 1, 1);
    public static final VillagePlaceType TOOLSMITH = register("toolsmith", getBlockStates(Blocks.SMITHING_TABLE), 1, 1);
    public static final VillagePlaceType WEAPONSMITH = register("weaponsmith", getBlockStates(Blocks.GRINDSTONE), 1, 1);
    public static final VillagePlaceType HOME = register("home", BEDS, 1, 1);
    public static final VillagePlaceType MEETING = register("meeting", getBlockStates(Blocks.BELL), 32, 6);
    public static final VillagePlaceType BEEHIVE = register("beehive", getBlockStates(Blocks.BEEHIVE), 0, 1);
    public static final VillagePlaceType BEE_NEST = register("bee_nest", getBlockStates(Blocks.BEE_NEST), 0, 1);
    public static final VillagePlaceType NETHER_PORTAL = register("nether_portal", getBlockStates(Blocks.NETHER_PORTAL), 0, 1);
    public static final VillagePlaceType LODESTONE = register("lodestone", getBlockStates(Blocks.LODESTONE), 0, 1);
    public static final VillagePlaceType LIGHTNING_ROD = register("lightning_rod", getBlockStates(Blocks.LIGHTNING_ROD), 0, 1);
    protected static final Set<IBlockData> ALL_STATES = new ObjectOpenHashSet<>(TYPE_BY_STATE.keySet());
    private final String name;
    private final Set<IBlockData> matchingStates;
    private final int maxTickets;
    private final Predicate<VillagePlaceType> predicate;
    private final int validRange;

    private static Set<IBlockData> getBlockStates(Block block) {
        return ImmutableSet.copyOf(block.getStates().getPossibleStates());
    }

    private VillagePlaceType(String id, Set<IBlockData> blockStates, int ticketCount, Predicate<VillagePlaceType> completionCondition, int searchDistance) {
        this.name = id;
        this.matchingStates = ImmutableSet.copyOf(blockStates);
        this.maxTickets = ticketCount;
        this.predicate = completionCondition;
        this.validRange = searchDistance;
    }

    private VillagePlaceType(String id, Set<IBlockData> blockStates, int ticketCount, int searchDistance) {
        this.name = id;
        this.matchingStates = ImmutableSet.copyOf(blockStates);
        this.maxTickets = ticketCount;
        this.predicate = (poiType) -> {
            return poiType == this;
        };
        this.validRange = searchDistance;
    }

    public String getName() {
        return this.name;
    }

    public int getMaxTickets() {
        return this.maxTickets;
    }

    public Predicate<VillagePlaceType> getPredicate() {
        return this.predicate;
    }

    public boolean is(IBlockData state) {
        return this.matchingStates.contains(state);
    }

    public int getValidRange() {
        return this.validRange;
    }

    @Override
    public String toString() {
        return this.name;
    }

    private static VillagePlaceType register(String id, Set<IBlockData> workStationStates, int ticketCount, int searchDistance) {
        return registerBlockStates(IRegistry.register(IRegistry.POINT_OF_INTEREST_TYPE, new MinecraftKey(id), new VillagePlaceType(id, workStationStates, ticketCount, searchDistance)));
    }

    private static VillagePlaceType register(String id, Set<IBlockData> workStationStates, int ticketCount, Predicate<VillagePlaceType> completionCondition, int searchDistance) {
        return registerBlockStates(IRegistry.register(IRegistry.POINT_OF_INTEREST_TYPE, new MinecraftKey(id), new VillagePlaceType(id, workStationStates, ticketCount, completionCondition, searchDistance)));
    }

    private static VillagePlaceType registerBlockStates(VillagePlaceType poiType) {
        poiType.matchingStates.forEach((state) -> {
            VillagePlaceType poiType2 = TYPE_BY_STATE.put(state, poiType);
            if (poiType2 != null) {
                throw (IllegalStateException)SystemUtils.pauseInIde(new IllegalStateException(String.format("%s is defined in too many tags", state)));
            }
        });
        return poiType;
    }

    public static Optional<VillagePlaceType> forState(IBlockData state) {
        return Optional.ofNullable(TYPE_BY_STATE.get(state));
    }
}
