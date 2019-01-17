package com.gmail.nossr50.skills.acrobatics;

import com.gmail.nossr50.config.AdvancedConfig;
import com.gmail.nossr50.config.Config;
import com.gmail.nossr50.config.experience.ExperienceConfig;
import com.gmail.nossr50.config.experience.XPConfig;

public final class Acrobatics {
    public static double rollThreshold         = AdvancedConfig.getInstance().getRollDamageThreshold();
    public static double gracefulRollThreshold = AdvancedConfig.getInstance().getGracefulRollDamageThreshold();
    public static double dodgeDamageModifier   = AdvancedConfig.getInstance().getDodgeDamageModifier();

    public static int dodgeXpModifier = XPConfig.getInstance().getDodgeXPModifier();
    public static int rollXpModifier  = XPConfig.getInstance().getRollXPModifier();
    public static int fallXpModifier  = XPConfig.getInstance().getFallXPModifier();

    public static double featherFallXPModifier = XPConfig.getInstance().getFeatherFallXPModifier();

    public static boolean dodgeLightningDisabled = Config.getInstance().getDodgeLightningDisabled();

    private Acrobatics() {};

    protected static double calculateModifiedDodgeDamage(double damage, double damageModifier) {
        return Math.max(damage / damageModifier, 1.0);
    }

    protected static double calculateModifiedRollDamage(double damage, double damageThreshold) {
        return Math.max(damage - damageThreshold, 0.0);
    }
}
