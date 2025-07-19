package net.stracciatella.core.gui.api;

public class MenuItem {
    int boxheight = 20;
    String label;
    Runnable action;
    
    public MenuItem(String label, Runnable action) {
        this.label = label;
        this.action = action;
    }

    public int getBoxheight() {
        return boxheight;
    }

    public String getLabel() {
        return label;
    }

    public void execute() {
        action.run();
    }
}
