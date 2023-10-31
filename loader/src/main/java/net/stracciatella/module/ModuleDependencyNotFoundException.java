package net.stracciatella.module;

public class ModuleDependencyNotFoundException extends RuntimeException {
    public ModuleDependencyNotFoundException(String message) {
        super(message);
    }
}
