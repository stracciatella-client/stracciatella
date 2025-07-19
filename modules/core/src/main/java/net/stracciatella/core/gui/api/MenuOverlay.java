package net.stracciatella.core.gui.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;


public abstract class MenuOverlay extends Screen {

    List<MenuItem> menuItems = new ArrayList<>();


    int boxX;
    int boxY;
    int boxWidth = 200;
    int boxHeight;

    public MenuOverlay(String title) {
        super(Component.literal(title));
    }


    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        // Hintergrund leicht verdunkeln
        graphics.fillGradient(0, 0, this.width, this.height,
                0x80000000, 0x80000000);

        //padding
        boxHeight = 20;

        for (MenuItem menuItem : menuItems) {
            boxHeight += menuItem.getBoxheight();
        }


        boxX = (this.width - boxWidth) / 2;
        boxY = (this.height - boxHeight) / 2;

        graphics.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 0xFF333333);

        // Menü-Items rendern
        var hoveredIndex = getHoveredIndex(mouseX, mouseY);
        for (int i = 0; i < menuItems.size(); i++) {
            int itemY = boxY + 10 + i * 20;
        //
        //     //hovered item should recolor
            int color = 0xFFFFFFFF;
            if (hoveredIndex.isPresent()) {
                color = (i == hoveredIndex.get()) ? 0xFFFFFF00 : 0xFFFFFFFF;
            } else {
                color = 0xFFFFFFFF;
            }

            graphics.drawCenteredString(this.font, menuItems.get(i).getLabel(),
                    boxX + boxWidth / 2, itemY, color);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) { // Linke Maustaste

            var clickedIndex = getHoveredIndex((int)mouseX, (int)mouseY);
            if (clickedIndex.isPresent()) {
                executeSelected(clickedIndex.get());
                return true;
            }
        }

        // Klick außerhalb der Box schließt das Menü
        if (getHoveredIndex((int)mouseX, (int)mouseY).isEmpty()) {
            this.onClose();
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private Optional<Integer> getHoveredIndex(int mouseX, int mouseY) {
        // System.out.println("mouseX: %d mouseY: %d".formatted(mouseX, mouseY));
        // System.out.println("boxX: %d boxY: %d".formatted(boxX, boxY));
        //is the mouse inside the menu
        if (mouseX >= boxX && mouseX <= boxX + boxWidth && mouseY >= boxY && mouseY <= boxY + boxHeight) {
            for (int i = 0; i < menuItems.size(); i++) {
                int itemY = boxY + 10 + i * 20;
                if (mouseY >= itemY && mouseY <= itemY + menuItems.get(i).getBoxheight()) {
                    return Optional.of(i);
                }
            }
        }
        return Optional.empty();
    }

    private void executeSelected(int clickedIndex) {
        menuItems.get(clickedIndex).execute();
        System.out.println("executed selected item " + clickedIndex);
    }

    @Override
    public boolean isPauseScreen() {
        return false; // Game läuft weiter
    }
}

