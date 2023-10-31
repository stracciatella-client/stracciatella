package net.stracciatella.event;

import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Type.*;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import net.fabricmc.fabric.api.event.Event;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class EventRegistry {
    private static final ListenerClassLoader LOADER = new ListenerClassLoader();
    private static final ConcurrentHashMap<Event<?>, ListenerEntry<?>> map = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public static <T> void register(Event<T> event, T listener) {
        var entry = (ListenerEntry<T>) map.computeIfAbsent(event, e -> EventRegistry.generateListener(event));
        entry.register(listener);
    }

    @SuppressWarnings("unchecked")
    public static <T> void unregister(Event<T> event, T listener) {
        var entry = (ListenerEntry<T>) map.get(event);
        if (entry == null) throw new IllegalArgumentException("Listener not registered");
        entry.unregister(listener);
    }

    @SuppressWarnings("unchecked")
    private static <T> @NotNull ListenerEntry<T> generateListener(Event<T> event) {
        try {
            var handlersField = event.getClass().getDeclaredField("handlers");
            handlersField.setAccessible(true);
            var handlers = (T[]) handlersField.get(event);

            var invokerFactoryField = event.getClass().getDeclaredField("invokerFactory");
            invokerFactoryField.setAccessible(true);
            var invokerFactory = (Function<T[], T>) invokerFactoryField.get(event);

            Class<?> handlersClass = handlers.getClass();
            Class<T> componentType = (Class<T>) handlersClass.componentType();
            T listener = generateListener(invokerFactory, event, componentType);
            event.register(listener);
            return new ListenerEntry<>(componentType, (Regeneratable<T>) listener);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T generateListener(Function<T[], T> invokerFactory, Event<T> event, Class<T> cls) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        if (!cls.isInterface()) throw new IllegalArgumentException("Wrong class: " + cls.getName());
        var declaredMethods = cls.getDeclaredMethods();
        if (declaredMethods.length != 1) throw new IllegalArgumentException("Wrong methods: " + Arrays.stream(declaredMethods).map(Method::getName).toList());

        Method method = declaredMethods[0];
        var node = new ClassNode();

        var className = EventRegistry.class.getPackage().getName().replace('.', '/') + "/GeneratedListener_" + Integer.toHexString(event.hashCode());
        final var callbackName = "handle";
        final var callbackType = getType(cls);
        final var callbackDescriptor = callbackType.getDescriptor();
        final var factoryName = "factory";
        final var factoryType = getType(Function.class);
        final var factoryDescriptor = factoryType.getDescriptor();

        node.visit(V21, ACC_PUBLIC | ACC_FINAL, className, null, getType(Object.class).getInternalName(), new String[]{getType(Regeneratable.class).getInternalName(), callbackType.getInternalName()});
        node.visitSource("<dynamic>", null);

        {
            node.visitField(ACC_PRIVATE | ACC_FINAL, factoryName, factoryDescriptor, null, null);
            node.visitField(ACC_PRIVATE, callbackName, callbackDescriptor, null, null);
        }

        {
            MethodNode constructor = new MethodNode(ACC_PUBLIC, "<init>", getMethodDescriptor(VOID_TYPE, getType(Function.class)), null, null);
            constructor.visitVarInsn(ALOAD, 0);
            constructor.visitMethodInsn(INVOKESPECIAL, getType(Object.class).getInternalName(), "<init>", getMethodDescriptor(VOID_TYPE), false);
            constructor.visitVarInsn(ALOAD, 0);
            constructor.visitVarInsn(ALOAD, 1);
            constructor.visitFieldInsn(PUTFIELD, className, factoryName, factoryDescriptor);
            constructor.visitInsn(RETURN);

            node.methods.add(constructor);
        }

        {
            var methodName = method.getName();
            var methodDescriptor = getMethodDescriptor(method);
            var methodNode = new MethodNode(ACC_PUBLIC, methodName, methodDescriptor, null, null);
            methodNode.visitVarInsn(ALOAD, 0);
            methodNode.visitFieldInsn(GETFIELD, className, callbackName, callbackDescriptor);
            var parameters = method.getParameters();
            for (var i = 0; i < parameters.length; i++) {
                methodNode.visitVarInsn(loadOpcode(parameters[i].getType()), i + 1);
            }
            methodNode.visitMethodInsn(INVOKEINTERFACE, callbackType.getInternalName(), methodName, methodDescriptor, true);

            methodNode.visitInsn(returnOpcode(method.getReturnType()));

            node.methods.add(methodNode);
        }

        {
            // The main regenerate method
            var methodName = "regenerate";
            var methodDescriptor = getMethodDescriptor(VOID_TYPE, getType(cls.arrayType()));
            var methodNode = new MethodNode(ACC_PUBLIC, methodName, methodDescriptor, null, null);
            methodNode.visitVarInsn(ALOAD, 0);
            methodNode.visitVarInsn(ALOAD, 0);
            methodNode.visitFieldInsn(GETFIELD, className, factoryName, factoryDescriptor);
            methodNode.visitVarInsn(ALOAD, 1);
            methodNode.visitMethodInsn(INVOKEINTERFACE, factoryType.getInternalName(), "apply", getMethodDescriptor(getType(Object.class), getType(Object.class)), true);
            methodNode.visitTypeInsn(CHECKCAST, callbackType.getInternalName());
            methodNode.visitFieldInsn(PUTFIELD, className, callbackName, callbackDescriptor);
            methodNode.visitInsn(RETURN);

            node.methods.add(methodNode);

            // The synthetic regenerate method because of generics
            var syntheticDescriptor = getMethodDescriptor(VOID_TYPE, getType(Object[].class));
            methodNode = new MethodNode(ACC_PUBLIC | ACC_SYNTHETIC | ACC_BRIDGE, methodName, syntheticDescriptor, null, null);
            methodNode.visitVarInsn(ALOAD, 0);
            methodNode.visitVarInsn(ALOAD, 1);
            methodNode.visitTypeInsn(CHECKCAST, getType(cls.arrayType()).getInternalName());
            methodNode.visitMethodInsn(INVOKEVIRTUAL, className, methodName, methodDescriptor, false);
            methodNode.visitInsn(RETURN);

            node.methods.add(methodNode);
        }

        var cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        node.accept(cw);

        // var traceClassVisitor = new TraceClassVisitor(null, new Textifier(), new PrintWriter(System.out));
        // new ClassReader(cw.toByteArray()).accept(traceClassVisitor, 0);

        var byteBuffer = ByteBuffer.wrap(cw.toByteArray());
        var generatedClass = LOADER.define(className.replace('/', '.'), byteBuffer);
        var constructor = generatedClass.getConstructor(Function.class);
        for (int i = 0; i < 10; i++) {
        }
        return (T) constructor.newInstance(invokerFactory);
    }

    private static int returnOpcode(Class<?> type) {
        return switch (type) {
            case Class<?> cls when cls == void.class -> RETURN;
            case Class<?> cls when cls == boolean.class -> IRETURN;
            case Class<?> cls when cls == byte.class -> IRETURN;
            case Class<?> cls when cls == short.class -> IRETURN;
            case Class<?> cls when cls == char.class -> IRETURN;
            case Class<?> cls when cls == int.class -> IRETURN;
            case Class<?> cls when cls == long.class -> LRETURN;
            case Class<?> cls when cls == float.class -> FRETURN;
            case Class<?> cls when cls == double.class -> DRETURN;
            default -> ARETURN;
        };
    }

    private static int loadOpcode(Class<?> type) {
        return switch (type) {
            case Class<?> cls when cls == boolean.class -> ILOAD;
            case Class<?> cls when cls == byte.class -> ILOAD;
            case Class<?> cls when cls == short.class -> ILOAD;
            case Class<?> cls when cls == char.class -> ILOAD;
            case Class<?> cls when cls == int.class -> ILOAD;
            case Class<?> cls when cls == long.class -> LLOAD;
            case Class<?> cls when cls == float.class -> FLOAD;
            case Class<?> cls when cls == double.class -> DLOAD;
            default -> ALOAD;
        };
    }

    public interface Regeneratable<T> {
        void regenerate(T[] listeners);
    }

    private static class ListenerEntry<T> {
        private final Object lock = new Object();
        private final Class<?> componentType;
        private final Regeneratable<T> regeneratable;
        private volatile T[] array;

        @SuppressWarnings("unchecked")
        public ListenerEntry(Class<?> componentType, Regeneratable<T> regeneratable) {
            this.componentType = componentType;
            this.regeneratable = regeneratable;
            this.array = (T[]) Array.newInstance(componentType, 0);
        }

        @SuppressWarnings("unchecked")
        public void register(T listener) {
            synchronized (lock) {
                var newArray = (T[]) Array.newInstance(componentType, array.length + 1);
                System.arraycopy(array, 0, newArray, 0, array.length);
                newArray[array.length] = listener;
                array = newArray;
                regeneratable.regenerate(array);
            }
        }

        @SuppressWarnings("unchecked")
        public void unregister(T listener) {
            synchronized (lock) {
                var index = -1;
                for (int i = 0; i < array.length; i++) {
                    if (array[i] == listener) {
                        index = i;
                        break;
                    }
                }
                if (index == -1) throw new IllegalArgumentException("Listener not registered");
                var newArray = (T[]) Array.newInstance(componentType, array.length - 1);
                if (index != 0) System.arraycopy(array, 0, newArray, 0, index);
                var remaining = newArray.length - index;
                System.arraycopy(array, index + 1, newArray, index, remaining);
                array = newArray;
                regeneratable.regenerate(array);
            }
        }
    }

    private static class ListenerClassLoader extends ClassLoader {
        static {
            ClassLoader.registerAsParallelCapable();
        }

        public ListenerClassLoader() {
            super(ListenerClassLoader.class.getClassLoader());
        }

        public Class<?> define(String name, ByteBuffer byteBuffer) {
            return defineClass(name, byteBuffer, getClass().getProtectionDomain());
        }
    }
}

