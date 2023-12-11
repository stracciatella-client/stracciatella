package net.stracciatella.injected;

import java.io.InputStream;
import java.net.URL;

public interface ClassLoaderAccessor {

    URL accessorGetResource(String name);

    URL accessorFindResource(String name);

    InputStream accessorGetResourceAsStream(String name);

}
