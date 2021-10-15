package net.minecraft.world.phys.shapes;

public interface OperatorBoolean {
    OperatorBoolean FALSE = (a, b) -> {
        return false;
    };
    OperatorBoolean NOT_OR = (a, b) -> {
        return !a && !b;
    };
    OperatorBoolean ONLY_SECOND = (a, b) -> {
        return b && !a;
    };
    OperatorBoolean NOT_FIRST = (a, b) -> {
        return !a;
    };
    OperatorBoolean ONLY_FIRST = (a, b) -> {
        return a && !b;
    };
    OperatorBoolean NOT_SECOND = (a, b) -> {
        return !b;
    };
    OperatorBoolean NOT_SAME = (a, b) -> {
        return a != b;
    };
    OperatorBoolean NOT_AND = (a, b) -> {
        return !a || !b;
    };
    OperatorBoolean AND = (a, b) -> {
        return a && b;
    };
    OperatorBoolean SAME = (a, b) -> {
        return a == b;
    };
    OperatorBoolean SECOND = (a, b) -> {
        return b;
    };
    OperatorBoolean CAUSES = (a, b) -> {
        return !a || b;
    };
    OperatorBoolean FIRST = (a, b) -> {
        return a;
    };
    OperatorBoolean CAUSED_BY = (a, b) -> {
        return a || !b;
    };
    OperatorBoolean OR = (a, b) -> {
        return a || b;
    };
    OperatorBoolean TRUE = (a, b) -> {
        return true;
    };

    boolean apply(boolean a, boolean b);
}
