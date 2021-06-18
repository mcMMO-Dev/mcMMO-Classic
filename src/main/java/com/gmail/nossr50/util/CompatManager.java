package com.gmail.nossr50.util;

import org.bukkit.World;

public final class CompatManager {
    private static boolean methodExists(String clazz, String method)
    {
        try {
            Class.forName(clazz).getMethod(method);
            return true;
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            return false;
        }
    }

    private static final boolean hasWorldMin = methodExists("org.bukkit.World", "getMinHeight");
    public static int getWorldMinCompat(World world) {
        if (hasWorldMin)
            return world.getMinHeight();
        return 0;
    }
}
