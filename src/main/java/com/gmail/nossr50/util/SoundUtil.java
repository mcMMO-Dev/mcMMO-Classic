package com.gmail.nossr50.util;

import com.gmail.nossr50.config.Config;

public class SoundUtil {
    // Sound Pitches & Volumes from CB
    public static final float ANVIL_USE_PITCH = 0.3F;  // Not in CB directly, I went off the place sound values
    public static final float ANVIL_USE_VOLUME = 1.0F * Config.getInstance().getMasterVolume();  // Not in CB directly, I went off the place sound values
    public static final float FIZZ_VOLUME = 0.5F * Config.getInstance().getMasterVolume();
    public static final float POP_VOLUME = 0.2F * Config.getInstance().getMasterVolume();
    public static final float BAT_VOLUME = 1.0F * Config.getInstance().getMasterVolume();
    public static final float BAT_PITCH = 0.6F;
    public static final float GHAST_VOLUME = 1.0F * Config.getInstance().getMasterVolume();
    public static final float LEVELUP_PITCH = 0.5F;  // Reduced to differentiate between vanilla level-up
    public static final float LEVELUP_VOLUME = 0.75F * Config.getInstance().getMasterVolume(); // Use max volume always

    public static float getFizzPitch() {
        return 2.6F + (Misc.getRandom().nextFloat() - Misc.getRandom().nextFloat()) * 0.8F;
    }

    public static float getPopPitch() {
        return ((Misc.getRandom().nextFloat() - Misc.getRandom().nextFloat()) * 0.7F + 1.0F) * 2.0F;
    }

    public static float getGhastPitch() {
        return (Misc.getRandom().nextFloat() - Misc.getRandom().nextFloat()) * 0.2F + 1.0F;
    }
}
