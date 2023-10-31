package net.stracciatella.init;

import java.util.HashMap;
import java.util.Map;

import net.fabricmc.accesswidener.AccessWidener;
import net.fabricmc.accesswidener.AccessWidenerReader;
import net.fabricmc.accesswidener.AccessWidenerVisitor;
import net.fabricmc.loader.impl.FabricLoaderImpl;

public class AccessWidenerConfig implements AccessWidenerVisitor {

    private final Map<String, Access> classAccesses = new HashMap<>();
    private final Map<Triple, Access> methodAccesses = new HashMap<>();
    private final Map<Triple, Access> fieldAccesses = new HashMap<>();
    private boolean mutable = true;

    public void freeze() {
        mutable = false;

        var accessWidener = FabricLoaderImpl.INSTANCE.getAccessWidener();
        for (var entry : classAccesses.entrySet()) {
            entry.getValue().apply(accessWidener, entry.getKey());
        }
        for (var entry : methodAccesses.entrySet()) {
            entry.getValue().apply(accessWidener, entry.getKey());
        }
        for (var entry : fieldAccesses.entrySet()) {
            entry.getValue().apply(accessWidener, entry.getKey());
        }
    }

    private void checkMutable() {
        if (!mutable) throw new IllegalStateException("Not mutable");
    }

    @Override
    public void visitClass(String name, AccessWidenerReader.AccessType access, boolean transitive) {
        var a = classAccesses.getOrDefault(name, ClassAccess.DEFAULT);
        if (a.is(access)) return;
        checkMutable();
        classAccesses.put(name, a.apply(access));
    }

    @Override
    public void visitField(String owner, String name, String descriptor, AccessWidenerReader.AccessType access, boolean transitive) {
        var key = new Triple(owner, name, descriptor);
        var a = fieldAccesses.getOrDefault(key, FieldAccess.DEFAULT);
        if (a.is(access)) return;
        checkMutable();
        if (access != AccessWidenerReader.AccessType.MUTABLE) visitClass(owner, access, transitive);
        fieldAccesses.put(key, a.apply(access));
    }

    @Override
    public void visitMethod(String owner, String name, String descriptor, AccessWidenerReader.AccessType access, boolean transitive) {
        var key = new Triple(owner, name, descriptor);
        var a = methodAccesses.getOrDefault(key, MethodAccess.DEFAULT);
        if (a.is(access)) return;
        checkMutable();
        visitClass(owner, access, transitive);
        methodAccesses.put(key, a.apply(access));
    }

    public enum ClassAccess implements Access {
        DEFAULT() {
            @Override
            public Access makeAccessible() {
                return ACCESSIBLE;
            }

            @Override
            public Access makeExtendable() {
                return EXTENDABLE;
            }
        },
        ACCESSIBLE() {
            @Override
            public Access makeAccessible() {
                return this;
            }

            @Override
            public Access makeExtendable() {
                return ACCESSIBLE_EXTENDABLE;
            }

            @Override
            public void apply(AccessWidener accessWidener, String className) {
                accessWidener.visitClass(className, AccessWidenerReader.AccessType.ACCESSIBLE, false);
            }
        },
        EXTENDABLE() {
            @Override
            public Access makeAccessible() {
                return ACCESSIBLE_EXTENDABLE;
            }

            @Override
            public Access makeExtendable() {
                return this;
            }

            @Override
            public void apply(AccessWidener accessWidener, String className) {
                accessWidener.visitClass(className, AccessWidenerReader.AccessType.EXTENDABLE, false);
            }
        },
        ACCESSIBLE_EXTENDABLE() {
            @Override
            public Access makeAccessible() {
                return this;
            }

            @Override
            public Access makeExtendable() {
                return this;
            }

            @Override
            public void apply(AccessWidener accessWidener, String className) {
                ACCESSIBLE.apply(accessWidener, className);
                EXTENDABLE.apply(accessWidener, className);
            }
        };

        @Override
        public boolean is(AccessWidenerReader.AccessType type) {
            return switch (type) {
                case ACCESSIBLE -> this == ACCESSIBLE || this == ACCESSIBLE_EXTENDABLE;
                case EXTENDABLE -> this == EXTENDABLE || this == ACCESSIBLE_EXTENDABLE;
                default -> false;
            };
        }
    }

    public enum MethodAccess implements Access {
        DEFAULT() {
            @Override
            public Access makeAccessible() {
                return ACCESSIBLE;
            }

            @Override
            public Access makeExtendable() {
                return EXTENDABLE;
            }
        },
        ACCESSIBLE() {
            @Override
            public Access makeAccessible() {
                return this;
            }

            @Override
            public Access makeExtendable() {
                return ACCESSIBLE_EXTENDABLE;
            }

            @Override
            public void apply(AccessWidener accessWidener, Triple triple) {
                accessWidener.visitMethod(triple.className(), triple.name(), triple.descriptor(), AccessWidenerReader.AccessType.ACCESSIBLE, false);
            }
        },
        EXTENDABLE() {
            @Override
            public Access makeAccessible() {
                return ACCESSIBLE_EXTENDABLE;
            }

            @Override
            public Access makeExtendable() {
                return this;
            }

            @Override
            public void apply(AccessWidener accessWidener, Triple triple) {
                accessWidener.visitMethod(triple.className(), triple.name(), triple.descriptor(), AccessWidenerReader.AccessType.EXTENDABLE, false);
            }
        },
        ACCESSIBLE_EXTENDABLE() {
            @Override
            public Access makeAccessible() {
                return this;
            }

            @Override
            public Access makeExtendable() {
                return this;
            }

            @Override
            public void apply(AccessWidener accessWidener, Triple triple) {
                ACCESSIBLE.apply(accessWidener, triple);
                EXTENDABLE.apply(accessWidener, triple);
            }
        };

        @Override
        public boolean is(AccessWidenerReader.AccessType type) {
            return switch (type) {
                case ACCESSIBLE -> this == ACCESSIBLE || this == ACCESSIBLE_EXTENDABLE;
                case EXTENDABLE -> this == EXTENDABLE || this == ACCESSIBLE_EXTENDABLE;
                default -> false;
            };
        }
    }

    public enum FieldAccess implements Access {
        DEFAULT() {
            @Override
            public Access makeAccessible() {
                return ACCESSIBLE;
            }

            @Override
            public Access makeMutable() {
                return MUTABLE;
            }
        },
        ACCESSIBLE() {
            @Override
            public Access makeAccessible() {
                return this;
            }

            @Override
            public Access makeMutable() {
                return ACCESSIBLE_MUTABLE;
            }

            @Override
            public void apply(AccessWidener accessWidener, Triple triple) {
                accessWidener.visitField(triple.className(), triple.name(), triple.descriptor(), AccessWidenerReader.AccessType.ACCESSIBLE, false);
            }
        },
        MUTABLE() {
            @Override
            public Access makeAccessible() {
                return ACCESSIBLE_MUTABLE;
            }

            @Override
            public Access makeMutable() {
                return this;
            }

            @Override
            public void apply(AccessWidener accessWidener, Triple triple) {
                accessWidener.visitField(triple.className(), triple.name(), triple.descriptor(), AccessWidenerReader.AccessType.MUTABLE, false);
            }
        },
        ACCESSIBLE_MUTABLE() {
            @Override
            public Access makeAccessible() {
                return this;
            }

            @Override
            public Access makeMutable() {
                return this;
            }

            @Override
            public void apply(AccessWidener accessWidener, Triple triple) {
                ACCESSIBLE.apply(accessWidener, triple);
                MUTABLE.apply(accessWidener, triple);
            }
        };

        @Override
        public boolean is(AccessWidenerReader.AccessType type) {
            return switch (type) {
                case ACCESSIBLE -> this == ACCESSIBLE || this == ACCESSIBLE_MUTABLE;
                case MUTABLE -> this == MUTABLE || this == ACCESSIBLE_MUTABLE;
                default -> false;
            };
        }
    }

    public interface Access {

        boolean is(AccessWidenerReader.AccessType type);

        default Access apply(AccessWidenerReader.AccessType type) {
            return switch (type) {
                case ACCESSIBLE -> makeAccessible();
                case EXTENDABLE -> makeExtendable();
                case MUTABLE -> makeMutable();
            };
        }

        default void apply(AccessWidener accessWidener, String className) {
            throw new UnsupportedOperationException();
        }

        default void apply(AccessWidener accessWidener, Triple triple) {
            throw new UnsupportedOperationException();
        }

        default Access makeAccessible() {
            throw new UnsupportedOperationException();
        }

        default Access makeMutable() {
            throw new UnsupportedOperationException();
        }

        default Access makeExtendable() {
            throw new UnsupportedOperationException();
        }
    }

    public record Triple(String className, String name, String descriptor) {
    }
}
