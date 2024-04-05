package net.stracciatella.fullscreen.mixin;

import com.mojang.blaze3d.platform.Monitor;
import com.mojang.blaze3d.platform.ScreenManager;
import com.mojang.blaze3d.platform.VideoMode;
import com.mojang.blaze3d.platform.Window;
import net.stracciatella.fullscreen.config.ConfigHandler;
import net.stracciatella.fullscreen.util.DimensionsResolver;
import net.stracciatella.fullscreen.util.WindowHooks;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Window.class)
public abstract class WindowMixin implements WindowHooks {
    @Shadow
    private int x;
    @Shadow
    private int y;
    @Shadow
    private int width;
    @Shadow
    private int height;

    @Shadow
    private int windowedX;
    @Shadow
    private int windowedY;
    @Shadow
    private int windowedWidth;
    @Shadow
    private int windowedHeight;

    @Shadow
    private boolean fullscreen;

    @Shadow
    @Final
    private long window;

    @Shadow
    private boolean dirty;

    @Shadow
    @Final
    private ScreenManager screenManager;

    @Shadow
    protected abstract void setMode();

    @Shadow
    @Nullable
    public abstract Monitor findBestMonitor();

    // Determines if the window *was* in borderless fullscreen (hence the windowed coordinates should not be trusted)
    @Unique
    private boolean wasEnabled = false;
    @Unique
    private int oldWindowedX = 0;
    @Unique
    private int oldWindowedY = 0;
    @Unique
    private int oldWindowedWidth = 0;
    @Unique
    private int oldWindowedHeight = 0;

    // Update the window to use borderless fullscreen, when the video/fullscreen mode is changed
    @SuppressWarnings("UnreachableCode")
    @Inject(method = "setMode", at = @At("HEAD"), cancellable = true)
    private void beforeUpdateWindowRegion(CallbackInfo ci) {
        boolean currFullscreen = GLFW.glfwGetWindowMonitor(this.window) != 0L;
        if (ConfigHandler.getInstance().isEnabled() && fullscreen) {
            if (!currFullscreen && !wasEnabled) {
                // Currently in windowed mode; save old coordinates
                windowedX = x;
                windowedY = y;
                windowedWidth = width;
                windowedHeight = height;
            }
            GLFW.glfwSetWindowAttrib(window, GLFW.GLFW_DECORATED, GLFW.GLFW_FALSE);

            // Monitor monitor = findBestMonitor();
            DimensionsResolver res = new DimensionsResolver();
            if (res.resolve((Window) (Object) this, screenManager)) {

                // if (monitor != null) {
                // Note: x/y/width/height can change between any GLFW call
                x = res.x;
                y = res.y;
                width = res.width;
                height = res.height;
                // VideoMode mode = monitor.getCurrentMode();
                // x = monitor.getX();
                // y = monitor.getY();
                // width = mode.getWidth();
                // height = mode.getHeight();
                // Set dimensions
                GLFW.glfwSetWindowMonitor(window, 0L, x, y, width, height, GLFW.GLFW_DONT_CARE);

                wasEnabled = true;
                ci.cancel();
            } else {
                // Reset decorated flag
                GLFW.glfwSetWindowAttrib(window, GLFW.GLFW_DECORATED, GLFW.GLFW_TRUE);
            }
        } else {
            // Reset decorated flag
            GLFW.glfwSetWindowAttrib(window, GLFW.GLFW_DECORATED, GLFW.GLFW_TRUE);
        }

        // The rest of this function will reset the windowed coordinates; if going borderless -> fullscreen, need to
        // make sure the old windowed coordinates are preserved
        oldWindowedX = windowedX;
        oldWindowedY = windowedY;
        oldWindowedWidth = windowedWidth;
        oldWindowedHeight = windowedHeight;
    }

    @Inject(method = "setMode", at = @At("RETURN"))
    private void afterUpdateWindowRegion(CallbackInfo ci) {
        if (wasEnabled) {
            wasEnabled = false;

            // See above (preserves old windowed coordinates; ignores those from borderless)
            windowedX = oldWindowedX;
            windowedY = oldWindowedY;
            windowedWidth = oldWindowedWidth;
            windowedHeight = oldWindowedHeight;
        }
    }

    // Pretend to the constructor code (that creates the window) that it is not fullscreen
    @Redirect(method = "<init>", at = @At(value = "FIELD", target = "Lcom/mojang/blaze3d/platform/Window;fullscreen:Z", opcode = Opcodes.GETFIELD),
            // currentFullscreen still needs to be set correctly
            slice = @Slice(from = @At(value = "FIELD", target = "Lcom/mojang/blaze3d/platform/Window;actuallyFullscreen:Z", opcode = Opcodes.PUTFIELD)))
    public boolean constructorIsFullscreen(Window window) {
        if (ConfigHandler.getInstance().isEnabled()) {
            return false;
        }
        return fullscreen;
    }

    // Save config and update video mode if fullscreen is toggled
    @Inject(method = "toggleFullScreen", at = @At("HEAD"))
    public void onToggleFullscreen(CallbackInfo info) {
        ConfigHandler.getInstance().saveIfDirty();
    }

    // Save config and update video mode if video mode is applied
    @Inject(method = "changeFullscreenVideoMode", at = @At("HEAD"))
    private void onApplyVideoMode(CallbackInfo info) {
        ConfigHandler.getInstance().saveIfDirty();
    }

    @Unique
    @Override
    public void stracciatella$apply() {
        dirty = true;
        setMode();
    }
}