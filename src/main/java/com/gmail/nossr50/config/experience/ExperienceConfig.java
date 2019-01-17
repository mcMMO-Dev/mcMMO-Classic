package com.gmail.nossr50.config.experience;

import com.gmail.nossr50.config.AutoUpdateConfigLoader;
import com.gmail.nossr50.datatypes.experience.FormulaType;
import com.gmail.nossr50.datatypes.skills.MaterialType;
import com.gmail.nossr50.datatypes.skills.SkillType;
import com.gmail.nossr50.datatypes.skills.alchemy.PotionStage;
import com.gmail.nossr50.util.StringUtils;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.List;

public class ExperienceConfig extends AutoUpdateConfigLoader {
    private static ExperienceConfig instance;

    private ExperienceConfig() {
        super("experience.yml");
        validate();
    }

    public static ExperienceConfig getInstance() {
        if (instance == null) {
            instance = new ExperienceConfig();
        }

        return instance;
    }

    @Override
    protected void loadKeys() {}

    @Override
    protected boolean validateKeys() {
        List<String> reason = new ArrayList<String>();

        /*
         * FORMULA SETTINGS
         */

        /* Curve values */
        if (getMultiplier(FormulaType.EXPONENTIAL) <= 0) {
            reason.add("Experience_Formula.Exponential_Values.multiplier should be greater than 0!");
        }

        if (getMultiplier(FormulaType.LINEAR) <= 0) {
            reason.add("Experience_Formula.Linear_Values.multiplier should be greater than 0!");
        }

        if (getExponent(FormulaType.EXPONENTIAL) <= 0) {
            reason.add("Experience_Formula.Exponential_Values.exponent should be greater than 0!");
        }

        /* Global modifier */
        if (getExperienceGainsGlobalMultiplier() <= 0) {
            reason.add("Experience_Formula.Multiplier.Global should be greater than 0!");
        }

        /* PVP modifier */
        if (getPlayerVersusPlayerXP() < 0) {
            reason.add("Experience_Formula.Multiplier.PVP should be at least 0!");
        }

        /* Spawned Mob modifier */
        if (getSpawnedMobXpMultiplier() < 0) {
            reason.add("Experience_Formula.Mobspawners.Multiplier should be at least 0!");
        }

        /* Bred Mob modifier */
        if (getBredMobXpMultiplier() < 0) {
            reason.add("Experience_Formula.Breeding.Multiplier should be at least 0!");
        }

        /* Conversion */
        if (getExpModifier() <= 0) {
            reason.add("Conversion.Exp_Modifier should be greater than 0!");
        }

        return noErrorsInConfig(reason);
    }

    /*
     * FORMULA SETTINGS
     */

    /* Curve settings */
    public FormulaType getFormulaType() { return FormulaType.getFormulaType(config.getString("Experience_Formula.Curve")); }
    public boolean getCumulativeCurveEnabled() { return config.getBoolean("Experience_Formula.Cumulative_Curve", false); }

    /* Curve values */
    public double getMultiplier(FormulaType type) { return config.getDouble("Experience_Formula." + StringUtils.getCapitalized(type.toString()) + "_Values.multiplier"); }
    public int getBase(FormulaType type) { return config.getInt("Experience_Formula." + StringUtils.getCapitalized(type.toString()) + "_Values.base"); }
    public double getExponent(FormulaType type) { return config.getDouble("Experience_Formula." + StringUtils.getCapitalized(type.toString()) + "_Values.exponent"); }

    /* Global modifier */
    public double getExperienceGainsGlobalMultiplier() { return config.getDouble("Experience_Formula.Multiplier.Global", 1.0); }
    public void setExperienceGainsGlobalMultiplier(double value) { config.set("Experience_Formula.Multiplier.Global", value); }

    /* PVP modifier */
    public double getPlayerVersusPlayerXP() { return config.getDouble("Experience_Formula.Multiplier.PVP", 1.0); }

    /* Spawned Mob modifier */
    public double getSpawnedMobXpMultiplier() { return config.getDouble("Experience_Formula.Mobspawners.Multiplier", 0.0); }
    public double getBredMobXpMultiplier() { return config.getDouble("Experience_Formula.Breeding.Multiplier", 1.0); }

    /* Skill modifiers */
    public double getFormulaSkillModifier(SkillType skill) { return config.getDouble("Experience_Formula.Modifier." + StringUtils.getCapitalized(skill.toString())); }

    /* Custom XP perk */
    public double getCustomXpPerkBoost() { return config.getDouble("Experience_Formula.Custom_XP_Perk.Boost", 1.25); }

    /* Diminished Returns */
    public boolean getDiminishedReturnsEnabled() { return config.getBoolean("Diminished_Returns.Enabled", false); }
    public int getDiminishedReturnsThreshold(SkillType skill) { return config.getInt("Diminished_Returns.Threshold." + StringUtils.getCapitalized(skill.toString()), 20000); }
    public int getDiminishedReturnsTimeInterval() { return config.getInt("Diminished_Returns.Time_Interval", 10); }

    /* Conversion */
    public double getExpModifier() { return config.getDouble("Conversion.Exp_Modifier", 1); }

    /* General Settings */
    public boolean getExperienceGainsPlayerVersusPlayerEnabled() { return config.getBoolean("Experience.PVPRewards", true); }
}
