package net.minecraft.data;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.stream.Collectors;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import net.minecraft.SharedConstants;
import net.minecraft.data.advancements.DebugReportProviderAdvancement;
import net.minecraft.data.info.DebugReportProviderBlockList;
import net.minecraft.data.info.DebugReportProviderCommands;
import net.minecraft.data.info.DebugReportProviderRegistryDump;
import net.minecraft.data.info.WorldgenRegistryDumpReport;
import net.minecraft.data.loot.DebugReportProviderLootTable;
import net.minecraft.data.models.DebugReportProviderModel;
import net.minecraft.data.recipes.DebugReportProviderRecipe;
import net.minecraft.data.structures.DebugReportProviderStructureFromNBT;
import net.minecraft.data.structures.DebugReportProviderStructureToNBT;
import net.minecraft.data.structures.StructureUpdater;
import net.minecraft.data.tags.TagsProviderBlock;
import net.minecraft.data.tags.TagsProviderEntityType;
import net.minecraft.data.tags.TagsProviderFluid;
import net.minecraft.data.tags.TagsProviderGameEvent;
import net.minecraft.data.tags.TagsProviderItem;
import net.minecraft.obfuscate.DontObfuscate;

public class Main {
    @DontObfuscate
    public static void main(String[] args) throws IOException {
        SharedConstants.tryDetectVersion();
        OptionParser optionParser = new OptionParser();
        OptionSpec<Void> optionSpec = optionParser.accepts("help", "Show the help menu").forHelp();
        OptionSpec<Void> optionSpec2 = optionParser.accepts("server", "Include server generators");
        OptionSpec<Void> optionSpec3 = optionParser.accepts("client", "Include client generators");
        OptionSpec<Void> optionSpec4 = optionParser.accepts("dev", "Include development tools");
        OptionSpec<Void> optionSpec5 = optionParser.accepts("reports", "Include data reports");
        OptionSpec<Void> optionSpec6 = optionParser.accepts("validate", "Validate inputs");
        OptionSpec<Void> optionSpec7 = optionParser.accepts("all", "Include all generators");
        OptionSpec<String> optionSpec8 = optionParser.accepts("output", "Output folder").withRequiredArg().defaultsTo("generated");
        OptionSpec<String> optionSpec9 = optionParser.accepts("input", "Input folder").withRequiredArg();
        OptionSet optionSet = optionParser.parse(args);
        if (!optionSet.has(optionSpec) && optionSet.hasOptions()) {
            Path path = Paths.get(optionSpec8.value(optionSet));
            boolean bl = optionSet.has(optionSpec7);
            boolean bl2 = bl || optionSet.has(optionSpec3);
            boolean bl3 = bl || optionSet.has(optionSpec2);
            boolean bl4 = bl || optionSet.has(optionSpec4);
            boolean bl5 = bl || optionSet.has(optionSpec5);
            boolean bl6 = bl || optionSet.has(optionSpec6);
            DebugReportGenerator dataGenerator = createStandardGenerator(path, optionSet.valuesOf(optionSpec9).stream().map((string) -> {
                return Paths.get(string);
            }).collect(Collectors.toList()), bl2, bl3, bl4, bl5, bl6);
            dataGenerator.run();
        } else {
            optionParser.printHelpOn(System.out);
        }
    }

    public static DebugReportGenerator createStandardGenerator(Path output, Collection<Path> inputs, boolean includeClient, boolean includeServer, boolean includeDev, boolean includeReports, boolean validate) {
        DebugReportGenerator dataGenerator = new DebugReportGenerator(output, inputs);
        if (includeClient || includeServer) {
            dataGenerator.addProvider((new DebugReportProviderStructureToNBT(dataGenerator)).addFilter(new StructureUpdater()));
        }

        if (includeClient) {
            dataGenerator.addProvider(new DebugReportProviderModel(dataGenerator));
        }

        if (includeServer) {
            dataGenerator.addProvider(new TagsProviderFluid(dataGenerator));
            TagsProviderBlock blockTagsProvider = new TagsProviderBlock(dataGenerator);
            dataGenerator.addProvider(blockTagsProvider);
            dataGenerator.addProvider(new TagsProviderItem(dataGenerator, blockTagsProvider));
            dataGenerator.addProvider(new TagsProviderEntityType(dataGenerator));
            dataGenerator.addProvider(new DebugReportProviderRecipe(dataGenerator));
            dataGenerator.addProvider(new DebugReportProviderAdvancement(dataGenerator));
            dataGenerator.addProvider(new DebugReportProviderLootTable(dataGenerator));
            dataGenerator.addProvider(new TagsProviderGameEvent(dataGenerator));
        }

        if (includeDev) {
            dataGenerator.addProvider(new DebugReportProviderStructureFromNBT(dataGenerator));
        }

        if (includeReports) {
            dataGenerator.addProvider(new DebugReportProviderBlockList(dataGenerator));
            dataGenerator.addProvider(new DebugReportProviderRegistryDump(dataGenerator));
            dataGenerator.addProvider(new DebugReportProviderCommands(dataGenerator));
            dataGenerator.addProvider(new WorldgenRegistryDumpReport(dataGenerator));
        }

        return dataGenerator;
    }
}
