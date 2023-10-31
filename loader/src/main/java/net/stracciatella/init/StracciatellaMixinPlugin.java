package net.stracciatella.init;

import java.util.List;
import java.util.Set;

import net.fabricmc.accesswidener.AccessWidenerReader;
import net.fabricmc.loader.impl.FabricLoaderImpl;
import net.stracciatella.Stracciatella;
import net.stracciatella.module.Module;
import net.stracciatella.module.ModuleManager;
import net.stracciatella.module.SimpleModuleManager;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public class StracciatellaMixinPlugin implements IMixinConfigPlugin {

    @Override
    public void onLoad(String mixinPackage) {
        try {
            var stracciatella = Stracciatella.instance();
            stracciatella.register("mixinPlugin", StracciatellaMixinPlugin.class, this);
            ((SimpleModuleManager) stracciatella.service(ModuleManager.class)).changeLifeCycle(Module.LifeCycle.MIXINS);
            var reader = new AccessWidenerReader(FabricLoaderImpl.INSTANCE.getAccessWidener());
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return false;
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
