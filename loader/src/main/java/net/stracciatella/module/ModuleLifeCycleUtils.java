package net.stracciatella.module;

import static net.stracciatella.module.Module.LifeCycle.*;
import static net.stracciatella.module.SimpleModuleManager.*;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import net.stracciatella.util.Pair;
import org.jetbrains.annotations.NotNull;

class ModuleLifeCycleUtils {

    private void throwIf(SimpleModuleEntry entry, Module.LifeCycle lifeCycle) {
        if (entry.lifeCycle() == lifeCycle) throw new IllegalStateException("[" + entry.moduleConfiguration().name() + "] state is " + entry.lifeCycle());
    }

    private void changeToInitialized(SimpleModuleEntry entry) {
        for (var dependency : entry.dependencies()) {
            throwIf(dependency, STOPPING);
            throwIf(dependency, STOPPED);
            if (dependency.lifeCycle() != REGISTERED) continue;
            changeToInitialized(dependency);
        }
        var fromLifeCycle = entry.lifeCycle(INITIALIZED);
        runLifeCycleTasks(entry, fromLifeCycle);
    }

    private void changeToMixins(SimpleModuleEntry entry) {
        for (var dependency : entry.dependencies()) {
            throwIf(dependency, STOPPING);
            throwIf(dependency, STOPPED);
            throwIf(dependency, REGISTERED);
            var lifeCycle = dependency.lifeCycle();
            if (lifeCycle == MIXINS) continue;
            if (lifeCycle == PRE_LAUNCH) continue;
            if (lifeCycle == STARTED) continue;
            changeToMixins(dependency);
        }
        var fromLifeCycle = entry.lifeCycle(MIXINS);
        runLifeCycleTasks(entry, fromLifeCycle);
    }

    private void changeToPreLaunch(SimpleModuleEntry entry) {
        for (var dependency : entry.dependencies()) {
            throwIf(dependency, STOPPING);
            throwIf(dependency, STOPPED);
            throwIf(dependency, REGISTERED);
            throwIf(dependency, INITIALIZED);
            var lifeCycle = dependency.lifeCycle();
            if (lifeCycle == PRE_LAUNCH) continue;
            if (lifeCycle == STARTED) continue;
            changeToPreLaunch(dependency);
        }
        var fromLifeCycle = entry.lifeCycle(PRE_LAUNCH);
        runLifeCycleTasks(entry, fromLifeCycle);
    }

    private void changeToStarted(SimpleModuleEntry entry) {
        for (var dependency : entry.dependencies()) {
            throwIf(dependency, STOPPING);
            throwIf(dependency, STOPPED);
            throwIf(dependency, REGISTERED);
            throwIf(dependency, INITIALIZED);
            throwIf(dependency, MIXINS);
            var lifeCycle = dependency.lifeCycle();
            if (lifeCycle == STARTED) continue;
            changeToStarted(dependency);
        }
        var fromLifeCycle = entry.lifeCycle(STARTED);
        runLifeCycleTasks(entry, fromLifeCycle);
    }

    private void changeToStopping(SimpleModuleEntry entry) {
        for (var dependant : entry.dependants()) {
            var lifeCycle = dependant.lifeCycle();
            if (lifeCycle == REGISTERED) {
                changeToStopped(dependant);
                continue;
            }
            if (lifeCycle == STOPPED) continue;
            if (lifeCycle == STOPPING) continue;
            changeToStopping(dependant);
        }
        var fromLifeCycle = entry.lifeCycle(STOPPING);
        runLifeCycleTasks(entry, fromLifeCycle);
    }

    private void changeToStopped(SimpleModuleEntry entry) {
        for (var dependant : entry.dependants()) {
            var lifeCycle = dependant.lifeCycle();
            if (lifeCycle == STOPPED) continue;
            throwIf(dependant, INITIALIZED);
            throwIf(dependant, MIXINS);
            throwIf(dependant, PRE_LAUNCH);
            throwIf(dependant, STARTED);
            changeToStopped(dependant);
        }
        entry.lifeCycle(STOPPED);
    }

    void changeLifeCycle(@NotNull SimpleModuleEntry entry, @NotNull Module.LifeCycle lifeCycle) {
        var currentLifeCycle = entry.lifeCycle();
        if (!currentLifeCycle.canChangeTo(lifeCycle)) {
            throw new IllegalStateException("Can't change LifeCycle from " + currentLifeCycle + " to  " + lifeCycle);
        }
        switch (lifeCycle) {
            case INITIALIZED -> changeToInitialized(entry);
            case MIXINS -> changeToMixins(entry);
            case PRE_LAUNCH -> changeToPreLaunch(entry);
            case STARTED -> changeToStarted(entry);
            case STOPPING -> changeToStopping(entry);
            case STOPPED -> changeToStopped(entry);
            default -> throw new IllegalStateException("Unexpected value: " + lifeCycle);
        }
    }

    private void runLifeCycleTasks(SimpleModuleEntry entry, Module.LifeCycle fromLifeCycle) {
        var module = entry.module();
        var cls = module.getClass();
        var methods = findLifeCycleTasks(cls, entry.lifeCycle(), fromLifeCycle);
        for (var method : methods) {
            try {
                method.invoke(module);
            } catch (Throwable e) {
                LOGGER.error("Failed to execute module task {}", method.getName(), e);
            }
        }
    }

    private List<Method> findLifeCycleTasks(Class<?> cls, Module.LifeCycle lifeCycle, Module.LifeCycle fromLifeCycle) {
        var methods = new ArrayList<Pair<Method, Module.Task>>();
        collectLifeCycleTasks(cls, lifeCycle, fromLifeCycle, methods);
        return methods.stream().sorted(Comparator.comparingInt(o -> o.right().order())).map(Pair::left).toList();
    }

    private void collectLifeCycleTasks(Class<?> cls, Module.LifeCycle lifeCycle, Module.LifeCycle fromLifeCycle, List<Pair<Method, Module.Task>> methods) {
        var superClass = cls.getSuperclass();
        if (superClass != null) collectLifeCycleTasks(superClass, lifeCycle, fromLifeCycle, methods);
        var declaredMethods = cls.getDeclaredMethods();
        for (var method : declaredMethods) {
            if (!method.isAnnotationPresent(Module.Task.class)) continue;
            var task = method.getAnnotation(Module.Task.class);
            if (task.lifeCycle() != lifeCycle) continue;
            if (!Set.of(task.lifeCycleFrom()).contains(fromLifeCycle)) continue;
            if (method.getParameterCount() != 0) {
                LOGGER.error("Module task must not have any parameters!");
                LOGGER.error("Parameters: {}", String.join(", ", Arrays.stream(method.getParameters()).map(p -> p.getType().getSimpleName()).toList()));
                LOGGER.error("In class {}", cls.getName());
                continue;
            }
            method.setAccessible(true);
            methods.add(Pair.of(method, task));
        }
    }
}
