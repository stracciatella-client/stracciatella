package net.stracciatella.module;

import static net.stracciatella.module.Module.LifeCycle.*;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import net.stracciatella.Stracciatella;
import org.slf4j.Logger;

public interface Module {

    default ModuleEntry entry() {
        return Stracciatella.instance().service(ModuleManager.class).module(this);
    }

    default Logger logger() {
        return entry().logger();
    }

    default LifeCycle lifeCycle() {
        return entry().lifeCycle();
    }

    default ModuleConfiguration configuration() {
        return entry().moduleConfiguration();
    }

    default String name() {
        return configuration().name();
    }

    default String id() {
        return configuration().id();
    }

    /**
     * <p>You need to make sure there are no classloading problems caused by code in the module.</p>
     * <p>For instance net.minecraft.client.Minecraft gets loaded before all mixins are applied.</p>
     */
    enum LifeCycle {
        /**
         * <p>Before the object instance is created.</p>
         * <p>This should never be of any use to modules</p>
         * <p>Before {@link #INITIALIZED}</p>
         * <p>This is even before fabric mixins are loaded.</p>
         * <p>Before any minecraft code.</p>
         */
        REGISTERED,
        /**
         * <p>After creating object instance.</p>
         * <p>After {@link #REGISTERED}</p>
         * <p>Before {@link #MIXINS}.</p>
         * <p>The module instance was just created.</p>
         * <p>This is even before fabric mixins are loaded.</p>
         * <p>Before any minecraft code.</p>
         */
        INITIALIZED,
        /**
         * <p>After {@link #INITIALIZED}.</p>
         * <p>Before {@link #PRE_LAUNCH}.</p>
         * <p>When fabric mixins are initialized.</p>
         * <p>Before any minecraft code.</p>
         */
        MIXINS,
        /**
         * <p>After {@link #MIXINS}.</p>
         * <p>Before {@link #STARTED}.</p>
         * <p>When fabric pre-launch code is executed.</p>
         * <p>Before any minecraft code.</p>
         */
        PRE_LAUNCH,
        /**
         * <p>After {@link #MIXINS}.</p>
         * <p>Before {@link #STOPPING}.</p>
         * <p>When the fabric mod initializer is called.</p>
         * <p>Put any minecraft code here!</p>
         */
        STARTED,
        /**
         * <p>After {@link #STARTED}.</p>
         * <p>After {@link #INITIALIZED}.</p>
         * <p>After {@link #MIXINS}.</p>
         * <p>After {@link #PRE_LAUNCH}.</p>
         * <p>Before {@link #STOPPED}.</p>
         * <p>This is called when the module is being stopped.</p>
         * <p><b>Free any resources here.</b></p>
         * <p>This might happen because the module is being reloaded.</p>
         */
        STOPPING,
        /**
         * <p>After {@link #STOPPING}.</p>
         * <p>After {@link #REGISTERED}.</p>
         * <p>This is the final stage of a module.</p>
         * <p><b>This {@link LifeCycle} can't be used in a {@link Task}!</b></p>
         */
        STOPPED;

        static {
            REGISTERED.next(INITIALIZED, STOPPED);
            INITIALIZED.next(MIXINS, STOPPING);
            MIXINS.next(PRE_LAUNCH, STOPPING);
            PRE_LAUNCH.next(STARTED, STOPPING);
            STARTED.next(STOPPING);
            STOPPING.next(STOPPED);
        }

        private LifeCycle[] next = new LifeCycle[0];

        private void next(LifeCycle... next) {
            this.next = next;
        }

        public boolean canChangeTo(LifeCycle lifeCycle) {
            for (var l : next)
                if (l == lifeCycle) return true;
            return false;
        }
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Task {
        LifeCycle lifeCycle();

        /**
         * <p>This is a way to specify which {@link LifeCycle}s may come before this task.</p>
         * <p>Defaults to all LifeCycles, but should be changed for scenarios where the module is {@link LifeCycle#STOPPING stopping}</p>
         *
         * @return all the possible {@link LifeCycle}s before this task.
         */
        LifeCycle[] lifeCycleFrom() default {REGISTERED, INITIALIZED, MIXINS, PRE_LAUNCH, STARTED, STOPPING, STOPPED};

        /**
         * The lower the order, the earlier the task is executed. Negative values are allowed
         */
        int order() default 0;
    }
}
