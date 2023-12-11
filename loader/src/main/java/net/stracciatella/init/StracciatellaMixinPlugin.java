package net.stracciatella.init;

import java.util.List;
import java.util.Set;

import net.stracciatella.Stracciatella;
import net.stracciatella.module.Module;
import net.stracciatella.module.ModuleManager;
import net.stracciatella.module.SimpleModuleManager;
import net.stracciatella.module.StracciatellaThrowables;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public class StracciatellaMixinPlugin implements IMixinConfigPlugin {

    @Override
    public void onLoad(String mixinPackage) {
        try {
            var stracciatella = Stracciatella.instance();
            stracciatella.register("mixin_plugin", StracciatellaMixinPlugin.class, this);
            var moduleManager = (SimpleModuleManager) stracciatella.service(ModuleManager.class);
            moduleManager.changeLifeCycle(Module.LifeCycle.MIXINS);
        } catch (Throwable e) {
            throw StracciatellaThrowables.propagate(e, AssertionError.class, AssertionError::new);
        }
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
