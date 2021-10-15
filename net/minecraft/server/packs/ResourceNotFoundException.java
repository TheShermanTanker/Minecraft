package net.minecraft.server.packs;

import java.io.File;
import java.io.FileNotFoundException;

public class ResourceNotFoundException extends FileNotFoundException {
    public ResourceNotFoundException(File packSource, String resource) {
        super(String.format("'%s' in ResourcePack '%s'", resource, packSource));
    }
}
