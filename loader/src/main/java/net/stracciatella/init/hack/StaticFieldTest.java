package net.stracciatella.init.hack;

import java.util.HashMap;
import java.util.Map;

class StaticFieldTest {
    public static volatile Map<String, String> m1 = new HashMap<>();
}
