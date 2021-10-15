package net.minecraft.gametest.framework;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.level.block.EnumBlockRotation;

public class GameTestHarnessRegistry {
    private static final Collection<GameTestHarnessTestFunction> TEST_FUNCTIONS = Lists.newArrayList();
    private static final Set<String> TEST_CLASS_NAMES = Sets.newHashSet();
    private static final Map<String, Consumer<WorldServer>> BEFORE_BATCH_FUNCTIONS = Maps.newHashMap();
    private static final Map<String, Consumer<WorldServer>> AFTER_BATCH_FUNCTIONS = Maps.newHashMap();
    private static final Collection<GameTestHarnessTestFunction> LAST_FAILED_TESTS = Sets.newHashSet();

    public static void register(Class<?> testClass) {
        Arrays.stream(testClass.getDeclaredMethods()).forEach(GameTestHarnessRegistry::register);
    }

    public static void register(Method method) {
        String string = method.getDeclaringClass().getSimpleName();
        GameTest gameTest = method.getAnnotation(GameTest.class);
        if (gameTest != null) {
            TEST_FUNCTIONS.add(turnMethodIntoTestFunction(method));
            TEST_CLASS_NAMES.add(string);
        }

        GameTestGenerator gameTestGenerator = method.getAnnotation(GameTestGenerator.class);
        if (gameTestGenerator != null) {
            TEST_FUNCTIONS.addAll(useTestGeneratorMethod(method));
            TEST_CLASS_NAMES.add(string);
        }

        registerBatchFunction(method, BeforeBatch.class, BeforeBatch::batch, BEFORE_BATCH_FUNCTIONS);
        registerBatchFunction(method, AfterBatch.class, AfterBatch::batch, AFTER_BATCH_FUNCTIONS);
    }

    private static <T extends Annotation> void registerBatchFunction(Method method, Class<T> clazz, Function<T, String> batchIdFunction, Map<String, Consumer<WorldServer>> batchConsumerMap) {
        T annotation = method.getAnnotation(clazz);
        if (annotation != null) {
            String string = batchIdFunction.apply(annotation);
            Consumer<WorldServer> consumer = batchConsumerMap.putIfAbsent(string, turnMethodIntoConsumer(method));
            if (consumer != null) {
                throw new RuntimeException("Hey, there should only be one " + clazz + " method per batch. Batch '" + string + "' has more than one!");
            }
        }

    }

    public static Collection<GameTestHarnessTestFunction> getTestFunctionsForClassName(String testClass) {
        return TEST_FUNCTIONS.stream().filter((testFunction) -> {
            return isTestFunctionPartOfClass(testFunction, testClass);
        }).collect(Collectors.toList());
    }

    public static Collection<GameTestHarnessTestFunction> getAllTestFunctions() {
        return TEST_FUNCTIONS;
    }

    public static Collection<String> getAllTestClassNames() {
        return TEST_CLASS_NAMES;
    }

    public static boolean isTestClass(String testClass) {
        return TEST_CLASS_NAMES.contains(testClass);
    }

    @Nullable
    public static Consumer<WorldServer> getBeforeBatchFunction(String batchId) {
        return BEFORE_BATCH_FUNCTIONS.get(batchId);
    }

    @Nullable
    public static Consumer<WorldServer> getAfterBatchFunction(String batchId) {
        return AFTER_BATCH_FUNCTIONS.get(batchId);
    }

    public static Optional<GameTestHarnessTestFunction> findTestFunction(String structurePath) {
        return getAllTestFunctions().stream().filter((testFunction) -> {
            return testFunction.getTestName().equalsIgnoreCase(structurePath);
        }).findFirst();
    }

    public static GameTestHarnessTestFunction getTestFunction(String structurePath) {
        Optional<GameTestHarnessTestFunction> optional = findTestFunction(structurePath);
        if (!optional.isPresent()) {
            throw new IllegalArgumentException("Can't find the test function for " + structurePath);
        } else {
            return optional.get();
        }
    }

    private static Collection<GameTestHarnessTestFunction> useTestGeneratorMethod(Method method) {
        try {
            Object object = method.getDeclaringClass().newInstance();
            return (Collection)method.invoke(object);
        } catch (ReflectiveOperationException var2) {
            throw new RuntimeException(var2);
        }
    }

    private static GameTestHarnessTestFunction turnMethodIntoTestFunction(Method method) {
        GameTest gameTest = method.getAnnotation(GameTest.class);
        String string = method.getDeclaringClass().getSimpleName();
        String string2 = string.toLowerCase();
        String string3 = string2 + "." + method.getName().toLowerCase();
        String string4 = gameTest.template().isEmpty() ? string3 : string2 + "." + gameTest.template();
        String string5 = gameTest.batch();
        EnumBlockRotation rotation = GameTestHarnessStructures.getRotationForRotationSteps(gameTest.rotationSteps());
        return new GameTestHarnessTestFunction(string5, string3, string4, rotation, gameTest.timeoutTicks(), gameTest.setupTicks(), gameTest.required(), gameTest.requiredSuccesses(), gameTest.attempts(), turnMethodIntoConsumer(method));
    }

    private static Consumer<?> turnMethodIntoConsumer(Method method) {
        return (args) -> {
            try {
                Object object = method.getDeclaringClass().newInstance();
                method.invoke(object, args);
            } catch (InvocationTargetException var3) {
                if (var3.getCause() instanceof RuntimeException) {
                    throw (RuntimeException)var3.getCause();
                } else {
                    throw new RuntimeException(var3.getCause());
                }
            } catch (ReflectiveOperationException var4) {
                throw new RuntimeException(var4);
            }
        };
    }

    private static boolean isTestFunctionPartOfClass(GameTestHarnessTestFunction testFunction, String testClass) {
        return testFunction.getTestName().toLowerCase().startsWith(testClass.toLowerCase() + ".");
    }

    public static Collection<GameTestHarnessTestFunction> getLastFailedTests() {
        return LAST_FAILED_TESTS;
    }

    public static void rememberFailedTest(GameTestHarnessTestFunction testFunction) {
        LAST_FAILED_TESTS.add(testFunction);
    }

    public static void forgetFailedTests() {
        LAST_FAILED_TESTS.clear();
    }
}
