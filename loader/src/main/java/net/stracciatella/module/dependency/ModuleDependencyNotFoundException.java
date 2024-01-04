package net.stracciatella.module.dependency;

public class ModuleDependencyNotFoundException extends RuntimeException {
    public ModuleDependencyNotFoundException(String message) {
        super(message);
    }
}
