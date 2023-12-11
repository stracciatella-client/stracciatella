package net.stracciatella.init.hack;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class StaticFieldTest {
    public static volatile Map<String, String> m1 = new HashMap<>();
}
