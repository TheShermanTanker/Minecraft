package net.minecraft.server;

import java.io.PrintStream;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandDispatcher;
import net.minecraft.commands.arguments.selector.options.PlayerSelector;
import net.minecraft.commands.synchronization.ArgumentRegistry;
import net.minecraft.core.IRegistry;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.core.dispenser.IDispenseBehavior;
import net.minecraft.locale.LocaleLanguage;
import net.minecraft.tags.TagStatic;
import net.minecraft.world.effect.MobEffectList;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.AttributeBase;
import net.minecraft.world.entity.ai.attributes.AttributeDefaults;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.alchemy.PotionBrewer;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BlockComposter;
import net.minecraft.world.level.block.BlockFire;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DispenserRegistry {
    public static final PrintStream STDOUT = System.out;
    private static volatile boolean isBootstrapped;
    private static final Logger LOGGER = LogManager.getLogger();

    public static void init() {
        if (!isBootstrapped) {
            isBootstrapped = true;
            if (IRegistry.REGISTRY.keySet().isEmpty()) {
                throw new IllegalStateException("Unable to load registries");
            } else {
                BlockFire.bootStrap();
                BlockComposter.bootStrap();
                if (EntityTypes.getName(EntityTypes.PLAYER) == null) {
                    throw new IllegalStateException("Failed loading EntityTypes");
                } else {
                    PotionBrewer.bootStrap();
                    PlayerSelector.bootStrap();
                    IDispenseBehavior.bootStrap();
                    CauldronInteraction.bootStrap();
                    ArgumentRegistry.bootStrap();
                    TagStatic.bootStrap();
                    wrapStreams();
                }
            }
        }
    }

    private static <T> void checkTranslations(Iterable<T> registry, Function<T, String> keyExtractor, Set<String> translationKeys) {
        LocaleLanguage language = LocaleLanguage.getInstance();
        registry.forEach((object) -> {
            String string = keyExtractor.apply(object);
            if (!language.has(string)) {
                translationKeys.add(string);
            }

        });
    }

    private static void checkGameruleTranslations(Set<String> translations) {
        final LocaleLanguage language = LocaleLanguage.getInstance();
        GameRules.visitGameRuleTypes(new GameRules.GameRuleVisitor() {
            @Override
            public <T extends GameRules.GameRuleValue<T>> void visit(GameRules.GameRuleKey<T> key, GameRules.GameRuleDefinition<T> type) {
                if (!language.has(key.getDescriptionId())) {
                    translations.add(key.getId());
                }

            }
        });
    }

    public static Set<String> getMissingTranslations() {
        Set<String> set = new TreeSet<>();
        checkTranslations(IRegistry.ATTRIBUTE, AttributeBase::getName, set);
        checkTranslations(IRegistry.ENTITY_TYPE, EntityTypes::getDescriptionId, set);
        checkTranslations(IRegistry.MOB_EFFECT, MobEffectList::getDescriptionId, set);
        checkTranslations(IRegistry.ITEM, Item::getName, set);
        checkTranslations(IRegistry.ENCHANTMENT, Enchantment::getDescriptionId, set);
        checkTranslations(IRegistry.BLOCK, Block::getDescriptionId, set);
        checkTranslations(IRegistry.CUSTOM_STAT, (stat) -> {
            return "stat." + stat.toString().replace(':', '.');
        }, set);
        checkGameruleTranslations(set);
        return set;
    }

    public static void checkBootstrapCalled(Supplier<String> callerGetter) {
        if (!isBootstrapped) {
            throw createBootstrapException(callerGetter);
        }
    }

    private static RuntimeException createBootstrapException(Supplier<String> callerGetter) {
        try {
            String string = callerGetter.get();
            return new IllegalArgumentException("Not bootstrapped (called from " + string + ")");
        } catch (Exception var3) {
            RuntimeException runtimeException = new IllegalArgumentException("Not bootstrapped (failed to resolve location)");
            runtimeException.addSuppressed(var3);
            return runtimeException;
        }
    }

    public static void validate() {
        checkBootstrapCalled(() -> {
            return "validate";
        });
        if (SharedConstants.IS_RUNNING_IN_IDE) {
            getMissingTranslations().forEach((key) -> {
                LOGGER.error("Missing translations: {}", (Object)key);
            });
            CommandDispatcher.validate();
        }

        AttributeDefaults.validate();
    }

    private static void wrapStreams() {
        if (LOGGER.isDebugEnabled()) {
            System.setErr(new DebugOutputStream("STDERR", System.err));
            System.setOut(new DebugOutputStream("STDOUT", STDOUT));
        } else {
            System.setErr(new RedirectStream("STDERR", System.err));
            System.setOut(new RedirectStream("STDOUT", STDOUT));
        }

    }

    public static void realStdoutPrintln(String str) {
        STDOUT.println(str);
    }
}
