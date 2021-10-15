package net.minecraft.server;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.CustomFunction;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.tags.Tag;
import net.minecraft.world.level.GameRules;

public class CustomFunctionData {
    private static final IChatBaseComponent NO_RECURSIVE_TRACES = new ChatMessage("commands.debug.function.noRecursion");
    private static final MinecraftKey TICK_FUNCTION_TAG = new MinecraftKey("tick");
    private static final MinecraftKey LOAD_FUNCTION_TAG = new MinecraftKey("load");
    final MinecraftServer server;
    @Nullable
    private CustomFunctionData.ExecutionContext context;
    private List<CustomFunction> ticking = ImmutableList.of();
    private boolean postReload;
    private CustomFunctionManager library;

    public CustomFunctionData(MinecraftServer server, CustomFunctionManager loader) {
        this.server = server;
        this.library = loader;
        this.postReload(loader);
    }

    public int getCommandLimit() {
        return this.server.getGameRules().getInt(GameRules.RULE_MAX_COMMAND_CHAIN_LENGTH);
    }

    public CommandDispatcher<CommandListenerWrapper> getCommandDispatcher() {
        return this.server.getCommandDispatcher().getDispatcher();
    }

    public void tick() {
        this.executeTagFunctions(this.ticking, TICK_FUNCTION_TAG);
        if (this.postReload) {
            this.postReload = false;
            Collection<CustomFunction> collection = this.library.getTags().getTagOrEmpty(LOAD_FUNCTION_TAG).getTagged();
            this.executeTagFunctions(collection, LOAD_FUNCTION_TAG);
        }

    }

    private void executeTagFunctions(Collection<CustomFunction> functions, MinecraftKey label) {
        this.server.getMethodProfiler().push(label::toString);

        for(CustomFunction commandFunction : functions) {
            this.execute(commandFunction, this.getGameLoopSender());
        }

        this.server.getMethodProfiler().exit();
    }

    public int execute(CustomFunction function, CommandListenerWrapper source) {
        return this.execute(function, source, (CustomFunctionData.TraceCallbacks)null);
    }

    public int execute(CustomFunction function, CommandListenerWrapper source, @Nullable CustomFunctionData.TraceCallbacks tracer) {
        if (this.context != null) {
            if (tracer != null) {
                this.context.reportError(NO_RECURSIVE_TRACES.getString());
                return 0;
            } else {
                this.context.delayFunctionCall(function, source);
                return 0;
            }
        } else {
            int var4;
            try {
                this.context = new CustomFunctionData.ExecutionContext(tracer);
                var4 = this.context.runTopCommand(function, source);
            } finally {
                this.context = null;
            }

            return var4;
        }
    }

    public void replaceLibrary(CustomFunctionManager loader) {
        this.library = loader;
        this.postReload(loader);
    }

    private void postReload(CustomFunctionManager loader) {
        this.ticking = ImmutableList.copyOf(loader.getTags().getTagOrEmpty(TICK_FUNCTION_TAG).getTagged());
        this.postReload = true;
    }

    public CommandListenerWrapper getGameLoopSender() {
        return this.server.getServerCommandListener().withPermission(2).withSuppressedOutput();
    }

    public Optional<CustomFunction> get(MinecraftKey id) {
        return this.library.getFunction(id);
    }

    public Tag<CustomFunction> getTag(MinecraftKey id) {
        return this.library.getTag(id);
    }

    public Iterable<MinecraftKey> getFunctionNames() {
        return this.library.getFunctions().keySet();
    }

    public Iterable<MinecraftKey> getTagNames() {
        return this.library.getTags().getAvailableTags();
    }

    class ExecutionContext {
        private int depth;
        @Nullable
        private final CustomFunctionData.TraceCallbacks tracer;
        private final Deque<CustomFunctionData.QueuedCommand> commandQueue = Queues.newArrayDeque();
        private final List<CustomFunctionData.QueuedCommand> nestedCalls = Lists.newArrayList();

        ExecutionContext(CustomFunctionData.TraceCallbacks tracer) {
            this.tracer = tracer;
        }

        void delayFunctionCall(CustomFunction function, CommandListenerWrapper source) {
            int i = CustomFunctionData.this.getCommandLimit();
            if (this.commandQueue.size() + this.nestedCalls.size() < i) {
                this.nestedCalls.add(new CustomFunctionData.QueuedCommand(source, this.depth, new CustomFunction.FunctionEntry(function)));
            }

        }

        int runTopCommand(CustomFunction function, CommandListenerWrapper source) {
            int i = CustomFunctionData.this.getCommandLimit();
            int j = 0;
            CustomFunction.Entry[] entrys = function.getEntries();

            for(int k = entrys.length - 1; k >= 0; --k) {
                this.commandQueue.push(new CustomFunctionData.QueuedCommand(source, 0, entrys[k]));
            }

            while(!this.commandQueue.isEmpty()) {
                try {
                    CustomFunctionData.QueuedCommand queuedCommand = this.commandQueue.removeFirst();
                    CustomFunctionData.this.server.getMethodProfiler().push(queuedCommand::toString);
                    this.depth = queuedCommand.depth;
                    queuedCommand.execute(CustomFunctionData.this, this.commandQueue, i, this.tracer);
                    if (!this.nestedCalls.isEmpty()) {
                        Lists.reverse(this.nestedCalls).forEach(this.commandQueue::addFirst);
                        this.nestedCalls.clear();
                    }
                } finally {
                    CustomFunctionData.this.server.getMethodProfiler().exit();
                }

                ++j;
                if (j >= i) {
                    return j;
                }
            }

            return j;
        }

        public void reportError(String message) {
            if (this.tracer != null) {
                this.tracer.onError(this.depth, message);
            }

        }
    }

    public static class QueuedCommand {
        private final CommandListenerWrapper sender;
        final int depth;
        private final CustomFunction.Entry entry;

        public QueuedCommand(CommandListenerWrapper source, int depth, CustomFunction.Entry element) {
            this.sender = source;
            this.depth = depth;
            this.entry = element;
        }

        public void execute(CustomFunctionData manager, Deque<CustomFunctionData.QueuedCommand> entries, int maxChainLength, @Nullable CustomFunctionData.TraceCallbacks tracer) {
            try {
                this.entry.execute(manager, this.sender, entries, maxChainLength, this.depth, tracer);
            } catch (CommandSyntaxException var6) {
                if (tracer != null) {
                    tracer.onError(this.depth, var6.getRawMessage().getString());
                }
            } catch (Exception var7) {
                if (tracer != null) {
                    tracer.onError(this.depth, var7.getMessage());
                }
            }

        }

        @Override
        public String toString() {
            return this.entry.toString();
        }
    }

    public interface TraceCallbacks {
        void onCommand(int depth, String command);

        void onReturn(int depth, String command, int result);

        void onError(int depth, String message);

        void onCall(int depth, MinecraftKey function, int size);
    }
}
