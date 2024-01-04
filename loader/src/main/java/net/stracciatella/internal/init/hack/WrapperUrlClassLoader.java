package net.stracciatella.internal.init.hack;

import java.net.URL;
import java.net.URLClassLoader;

class WrapperUrlClassLoader extends URLClassLoader {
    public WrapperUrlClassLoader() {
        super(new URL[0]);
    }

    @Override
    public void addURL(URL url) {
        super.addURL(url);
    }
}
