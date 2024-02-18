package net.stracciatella.injected;

import java.io.InputStream;
import java.net.URL;

public class ClassLoaderWrapper {

    public static ClassLoaderAccessor accessor;

    @SuppressWarnings("unused") // Used in ClassLoaderTransformer
    public static InputStream getResourceAsStream(String name) {
        // System.out.println("Get as stream " + name + " " + accessor);
        if (accessor == null) return null;
        // System.out.println("RET: " + ret);
        return accessor.accessorGetResourceAsStream(name);
    }

    @SuppressWarnings("unused") // Used in ClassLoaderTransformer
    public static URL getResource(String name) {
        if (accessor == null) return null;
        return accessor.accessorGetResource(name);
    }

    @SuppressWarnings("unused") // Used in ClassLoaderTransformer
    public static URL findResource(String name) {
        if (accessor == null) return null;
        return accessor.accessorFindResource(name);
    }
}
