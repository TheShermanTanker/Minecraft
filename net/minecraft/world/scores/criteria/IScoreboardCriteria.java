package net.minecraft.world.scores.criteria;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.ImmutableMap.Builder;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.EnumChatFormat;
import net.minecraft.core.IRegistry;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.stats.StatisticWrapper;

public class IScoreboardCriteria {
    private static final Map<String, IScoreboardCriteria> CUSTOM_CRITERIA = Maps.newHashMap();
    public static final Map<String, IScoreboardCriteria> CRITERIA_CACHE = Maps.newHashMap();
    public static final IScoreboardCriteria DUMMY = registerCustom("dummy");
    public static final IScoreboardCriteria TRIGGER = registerCustom("trigger");
    public static final IScoreboardCriteria DEATH_COUNT = registerCustom("deathCount");
    public static final IScoreboardCriteria KILL_COUNT_PLAYERS = registerCustom("playerKillCount");
    public static final IScoreboardCriteria KILL_COUNT_ALL = registerCustom("totalKillCount");
    public static final IScoreboardCriteria HEALTH = registerCustom("health", true, IScoreboardCriteria.EnumScoreboardHealthDisplay.HEARTS);
    public static final IScoreboardCriteria FOOD = registerCustom("food", true, IScoreboardCriteria.EnumScoreboardHealthDisplay.INTEGER);
    public static final IScoreboardCriteria AIR = registerCustom("air", true, IScoreboardCriteria.EnumScoreboardHealthDisplay.INTEGER);
    public static final IScoreboardCriteria ARMOR = registerCustom("armor", true, IScoreboardCriteria.EnumScoreboardHealthDisplay.INTEGER);
    public static final IScoreboardCriteria EXPERIENCE = registerCustom("xp", true, IScoreboardCriteria.EnumScoreboardHealthDisplay.INTEGER);
    public static final IScoreboardCriteria LEVEL = registerCustom("level", true, IScoreboardCriteria.EnumScoreboardHealthDisplay.INTEGER);
    public static final IScoreboardCriteria[] TEAM_KILL = new IScoreboardCriteria[]{registerCustom("teamkill." + EnumChatFormat.BLACK.getName()), registerCustom("teamkill." + EnumChatFormat.DARK_BLUE.getName()), registerCustom("teamkill." + EnumChatFormat.DARK_GREEN.getName()), registerCustom("teamkill." + EnumChatFormat.DARK_AQUA.getName()), registerCustom("teamkill." + EnumChatFormat.DARK_RED.getName()), registerCustom("teamkill." + EnumChatFormat.DARK_PURPLE.getName()), registerCustom("teamkill." + EnumChatFormat.GOLD.getName()), registerCustom("teamkill." + EnumChatFormat.GRAY.getName()), registerCustom("teamkill." + EnumChatFormat.DARK_GRAY.getName()), registerCustom("teamkill." + EnumChatFormat.BLUE.getName()), registerCustom("teamkill." + EnumChatFormat.GREEN.getName()), registerCustom("teamkill." + EnumChatFormat.AQUA.getName()), registerCustom("teamkill." + EnumChatFormat.RED.getName()), registerCustom("teamkill." + EnumChatFormat.LIGHT_PURPLE.getName()), registerCustom("teamkill." + EnumChatFormat.YELLOW.getName()), registerCustom("teamkill." + EnumChatFormat.WHITE.getName())};
    public static final IScoreboardCriteria[] KILLED_BY_TEAM = new IScoreboardCriteria[]{registerCustom("killedByTeam." + EnumChatFormat.BLACK.getName()), registerCustom("killedByTeam." + EnumChatFormat.DARK_BLUE.getName()), registerCustom("killedByTeam." + EnumChatFormat.DARK_GREEN.getName()), registerCustom("killedByTeam." + EnumChatFormat.DARK_AQUA.getName()), registerCustom("killedByTeam." + EnumChatFormat.DARK_RED.getName()), registerCustom("killedByTeam." + EnumChatFormat.DARK_PURPLE.getName()), registerCustom("killedByTeam." + EnumChatFormat.GOLD.getName()), registerCustom("killedByTeam." + EnumChatFormat.GRAY.getName()), registerCustom("killedByTeam." + EnumChatFormat.DARK_GRAY.getName()), registerCustom("killedByTeam." + EnumChatFormat.BLUE.getName()), registerCustom("killedByTeam." + EnumChatFormat.GREEN.getName()), registerCustom("killedByTeam." + EnumChatFormat.AQUA.getName()), registerCustom("killedByTeam." + EnumChatFormat.RED.getName()), registerCustom("killedByTeam." + EnumChatFormat.LIGHT_PURPLE.getName()), registerCustom("killedByTeam." + EnumChatFormat.YELLOW.getName()), registerCustom("killedByTeam." + EnumChatFormat.WHITE.getName())};
    private final String name;
    private final boolean readOnly;
    private final IScoreboardCriteria.EnumScoreboardHealthDisplay renderType;

    private static IScoreboardCriteria registerCustom(String name, boolean readOnly, IScoreboardCriteria.EnumScoreboardHealthDisplay defaultRenderType) {
        IScoreboardCriteria objectiveCriteria = new IScoreboardCriteria(name, readOnly, defaultRenderType);
        CUSTOM_CRITERIA.put(name, objectiveCriteria);
        return objectiveCriteria;
    }

    private static IScoreboardCriteria registerCustom(String name) {
        return registerCustom(name, false, IScoreboardCriteria.EnumScoreboardHealthDisplay.INTEGER);
    }

    protected IScoreboardCriteria(String name) {
        this(name, false, IScoreboardCriteria.EnumScoreboardHealthDisplay.INTEGER);
    }

    protected IScoreboardCriteria(String name, boolean readOnly, IScoreboardCriteria.EnumScoreboardHealthDisplay defaultRenderType) {
        this.name = name;
        this.readOnly = readOnly;
        this.renderType = defaultRenderType;
        CRITERIA_CACHE.put(name, this);
    }

    public static Set<String> getCustomCriteriaNames() {
        return ImmutableSet.copyOf(CUSTOM_CRITERIA.keySet());
    }

    public static Optional<IScoreboardCriteria> byName(String name) {
        IScoreboardCriteria objectiveCriteria = CRITERIA_CACHE.get(name);
        if (objectiveCriteria != null) {
            return Optional.of(objectiveCriteria);
        } else {
            int i = name.indexOf(58);
            return i < 0 ? Optional.empty() : IRegistry.STAT_TYPE.getOptional(MinecraftKey.of(name.substring(0, i), '.')).flatMap((statType) -> {
                return getStat(statType, MinecraftKey.of(name.substring(i + 1), '.'));
            });
        }
    }

    private static <T> Optional<IScoreboardCriteria> getStat(StatisticWrapper<T> statType, MinecraftKey id) {
        return statType.getRegistry().getOptional(id).map(statType::get);
    }

    public String getName() {
        return this.name;
    }

    public boolean isReadOnly() {
        return this.readOnly;
    }

    public IScoreboardCriteria.EnumScoreboardHealthDisplay getDefaultRenderType() {
        return this.renderType;
    }

    public static enum EnumScoreboardHealthDisplay {
        INTEGER("integer"),
        HEARTS("hearts");

        private final String id;
        private static final Map<String, IScoreboardCriteria.EnumScoreboardHealthDisplay> BY_ID;

        private EnumScoreboardHealthDisplay(String name) {
            this.id = name;
        }

        public String getId() {
            return this.id;
        }

        public static IScoreboardCriteria.EnumScoreboardHealthDisplay byId(String name) {
            return BY_ID.getOrDefault(name, INTEGER);
        }

        static {
            Builder<String, IScoreboardCriteria.EnumScoreboardHealthDisplay> builder = ImmutableMap.builder();

            for(IScoreboardCriteria.EnumScoreboardHealthDisplay renderType : values()) {
                builder.put(renderType.id, renderType);
            }

            BY_ID = builder.build();
        }
    }
}
