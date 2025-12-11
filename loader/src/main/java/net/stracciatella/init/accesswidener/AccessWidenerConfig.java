package net.stracciatella.init.accesswidener;

import java.util.HashMap;
import java.util.Map;

import net.fabricmc.loader.impl.FabricLoaderImpl;
import net.fabricmc.loader.impl.lib.classtweaker.api.ClassTweaker;
import net.fabricmc.loader.impl.lib.classtweaker.api.visitor.AccessWidenerVisitor;
import net.fabricmc.loader.impl.lib.classtweaker.api.visitor.AccessWidenerVisitor.AccessType;
import net.fabricmc.loader.impl.lib.classtweaker.api.visitor.ClassTweakerVisitor;
import org.jetbrains.annotations.Nullable;

public class AccessWidenerConfig implements ClassTweakerVisitor {

    private final Map<String, Visitor> visitorMap = new HashMap<>();
    private boolean mutable = true;

    public void freeze() {
        mutable = false;

        var ct = FabricLoaderImpl.INSTANCE.getClassTweaker();
        for (var visitor : visitorMap.values()) {
            visitor.apply(ct);
        }
    }

    private void checkMutable() {
        if (!mutable) throw new IllegalStateException("Not mutable");
    }

    private class Visitor implements AccessWidenerVisitor {
        private final String owner;
        private final Map<Descriptor, Access> methodAccesses = new HashMap<>();
        private final Map<Descriptor, Access> fieldAccesses = new HashMap<>();
        private Access classAccess = ClassAccess.DEFAULT;

        public Visitor(String owner) {
            this.owner = owner;
        }

        private void apply(ClassTweaker tweaker) {
            var aw = tweaker.visitAccessWidener(owner);
            classAccess.apply(aw);
            methodAccesses.forEach((descriptor, access) -> access.apply(aw, descriptor));
            fieldAccesses.forEach((descriptor, access) -> access.apply(aw, descriptor));
        }

        @Override
        public void visitClass(AccessType access, boolean transitive) {
            if (classAccess.is(access)) return;
            checkMutable();
            classAccess = classAccess.apply(access);
        }

        @Override
        public void visitField(String name, String descriptor, AccessType access, boolean transitive) {
            var key = new Descriptor(name, descriptor);
            var a = fieldAccesses.getOrDefault(key, FieldAccess.DEFAULT);
            if (a.is(access)) return;
            checkMutable();
            if (access != AccessType.MUTABLE) visitClass(access, transitive);
            fieldAccesses.put(key, a.apply(access));
        }

        @Override
        public void visitMethod(String name, String descriptor, AccessType access, boolean transitive) {
            var key = new Descriptor(name, descriptor);
            var a = methodAccesses.getOrDefault(key, MethodAccess.DEFAULT);
            if (a.is(access)) return;
            checkMutable();
            visitClass(access, transitive);
            methodAccesses.put(key, a.apply(access));
        }
    }

    @Override
    public @Nullable AccessWidenerVisitor visitAccessWidener(String classOwner) {
        return visitorMap.computeIfAbsent(classOwner, Visitor::new);
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
            public void apply(AccessWidenerVisitor accessWidener) {
                accessWidener.visitClass(AccessType.ACCESSIBLE, false);
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
            public void apply(AccessWidenerVisitor accessWidener) {
                accessWidener.visitClass(AccessType.EXTENDABLE, false);
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
            public void apply(AccessWidenerVisitor accessWidener) {
                ACCESSIBLE.apply(accessWidener);
                EXTENDABLE.apply(accessWidener);
            }
        };

        @Override
        public boolean is(AccessType type) {
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
            public void apply(AccessWidenerVisitor accessWidener, Descriptor descriptor) {
                accessWidener.visitMethod(descriptor.name(), descriptor.descriptor(), AccessType.ACCESSIBLE, false);
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
            public void apply(AccessWidenerVisitor accessWidener, Descriptor descriptor) {
                accessWidener.visitMethod(descriptor.name(), descriptor.descriptor(), AccessType.EXTENDABLE, false);
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
            public void apply(AccessWidenerVisitor accessWidener, Descriptor descriptor) {
                ACCESSIBLE.apply(accessWidener, descriptor);
                EXTENDABLE.apply(accessWidener, descriptor);
            }
        };

        @Override
        public boolean is(AccessType type) {
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
            public void apply(AccessWidenerVisitor accessWidener, Descriptor descriptor) {
                accessWidener.visitField(descriptor.name(), descriptor.descriptor(), AccessType.ACCESSIBLE, false);
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
            public void apply(AccessWidenerVisitor accessWidener, Descriptor descriptor) {
                accessWidener.visitField(descriptor.name(), descriptor.descriptor(), AccessType.MUTABLE, false);
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
            public void apply(AccessWidenerVisitor accessWidener, Descriptor descriptor) {
                ACCESSIBLE.apply(accessWidener, descriptor);
                MUTABLE.apply(accessWidener, descriptor);
            }
        };

        @Override
        public boolean is(AccessType type) {
            return switch (type) {
                case ACCESSIBLE -> this == ACCESSIBLE || this == ACCESSIBLE_MUTABLE;
                case MUTABLE -> this == MUTABLE || this == ACCESSIBLE_MUTABLE;
                default -> false;
            };
        }
    }

    public interface Access {

        boolean is(AccessType type);

        default Access apply(AccessType type) {
            return switch (type) {
                case ACCESSIBLE -> makeAccessible();
                case EXTENDABLE -> makeExtendable();
                case MUTABLE -> makeMutable();
            };
        }

        default void apply(AccessWidenerVisitor accessWidener) {
            throw new UnsupportedOperationException();
        }

        default void apply(AccessWidenerVisitor accessWidener, Descriptor descriptor) {
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

    public record Descriptor(String name, String descriptor) {
    }
}
