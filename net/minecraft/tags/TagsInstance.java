package net.minecraft.tags;

public class TagsInstance {
    private static volatile ITagRegistry instance = TagStatic.createCollection();

    public static ITagRegistry getInstance() {
        return instance;
    }

    public static void bind(ITagRegistry tagManager) {
        instance = tagManager;
    }
}
