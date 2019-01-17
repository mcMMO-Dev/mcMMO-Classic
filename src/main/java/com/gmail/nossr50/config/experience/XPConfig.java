package com.gmail.nossr50.config.experience;

import com.gmail.nossr50.config.VersionedConfigLoader;
import com.gmail.nossr50.datatypes.skills.MaterialType;
import com.gmail.nossr50.datatypes.skills.SkillType;
import com.gmail.nossr50.datatypes.skills.alchemy.PotionStage;
import com.gmail.nossr50.util.StringUtils;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.List;

public class XPConfig extends VersionedConfigLoader {
    private static XPConfig instance;

    private XPConfig() {
        super("xp.yml");
        validate();
    }

    public static XPConfig getInstance() {
        if (instance == null) {
            instance = new XPConfig();
        }

        return instance;
    }

    @Override
    protected void loadKeys() {}

    @Override
    protected boolean validateKeys() {
        List<String> reason = new ArrayList<String>();

        return noErrorsInConfig(reason);
    }

    /*
     * XP SETTINGS
     */

    /* Combat XP Multipliers */
    public double getCombatXP(EntityType entity) { return config.getDouble("Experience.Combat.Multiplier." + StringUtils.getPrettyEntityTypeString(entity).replace(" ", "_")); }
    public double getAnimalsXP(EntityType entity) { return config.getDouble("Experience.Combat.Multiplier." + StringUtils.getPrettyEntityTypeString(entity).replace(" ", "_"), getAnimalsXP()); }
    public double getAnimalsXP() { return config.getDouble("Experience.Combat.Multiplier.Animals", 1.0); }
    public boolean hasCombatXP(EntityType entity) {return config.contains("Experience.Combat.Multiplier." + StringUtils.getPrettyEntityTypeString(entity).replace(" ", "_")); }

    /* Materials  */
    public int getXp(SkillType skill, Material data)
    {
        String baseString = "Experience." + StringUtils.getCapitalized(skill.toString()) + ".";
        String explicitString = baseString + StringUtils.getExplicitConfigMaterialString(data);
        if (config.contains(explicitString))
            return config.getInt(explicitString);
        String friendlyString = baseString + StringUtils.getFriendlyConfigMaterialString(data);
        if (config.contains(friendlyString))
            return config.getInt(friendlyString);
        String wildcardString = baseString + StringUtils.getWildcardConfigMaterialString(data);
        if (config.contains(wildcardString))
            return config.getInt(wildcardString);
        return 0;
    }

    /* Materials  */
    public int getXp(SkillType skill, BlockData data)
    {
        String baseString = "Experience." + StringUtils.getCapitalized(skill.toString()) + ".";
        String explicitString = baseString + StringUtils.getExplicitConfigBlockDataString(data);
        if (config.contains(explicitString))
            return config.getInt(explicitString);
        String friendlyString = baseString + StringUtils.getFriendlyConfigBlockDataString(data);
        if (config.contains(friendlyString))
            return config.getInt(friendlyString);
        String wildcardString = baseString + StringUtils.getWildcardConfigBlockDataString(data);
        if (config.contains(wildcardString))
            return config.getInt(wildcardString);
        return 0;
    }

    public boolean isSkillBlock(SkillType skill, Material data)
    {
        String baseString = "Experience." + StringUtils.getCapitalized(skill.toString()) + ".";
        String explicitString = baseString + StringUtils.getExplicitConfigMaterialString(data);
        if (config.contains(explicitString))
            return true;
        String friendlyString = baseString + StringUtils.getFriendlyConfigMaterialString(data);
        if (config.contains(friendlyString))
            return true;
        String wildcardString = baseString + StringUtils.getWildcardConfigMaterialString(data);
        return config.contains(wildcardString);
    }

    public boolean isSkillBlock(SkillType skill, BlockData data)
    {
        String baseString = "Experience." + StringUtils.getCapitalized(skill.toString()) + ".";
        String explicitString = baseString + StringUtils.getExplicitConfigBlockDataString(data);
        if (config.contains(explicitString))
            return true;
        String friendlyString = baseString + StringUtils.getFriendlyConfigBlockDataString(data);
        if (config.contains(friendlyString))
            return true;
        String wildcardString = baseString + StringUtils.getWildcardConfigBlockDataString(data);
        return config.contains(wildcardString);
    }

    /* Acrobatics */
    public int getDodgeXPModifier() { return config.getInt("Experience.Acrobatics.Dodge", 120); }
    public int getRollXPModifier() { return config.getInt("Experience.Acrobatics.Roll", 80); }
    public int getFallXPModifier() { return config.getInt("Experience.Acrobatics.Fall", 120); }

    public double getFeatherFallXPModifier() { return config.getDouble("Experience.Acrobatics.FeatherFall_Multiplier", 2.0); }

    /* Alchemy */
    public double getPotionXP(PotionStage stage) { return config.getDouble("Experience.Alchemy.Potion_Stage_" + stage.toNumerical(), 10D); }

    /* Archery */
    public double getArcheryDistanceMultiplier() { return config.getDouble("Experience.Archery.Distance_Multiplier", 0.025); }

    public int getFishingShakeXP() { return config.getInt("Experience.Fishing.Shake", 50); }

    /* Repair */
    public double getRepairXPBase() { return config.getDouble("Experience.Repair.Base", 1000.0); }
    public double getRepairXP(MaterialType repairMaterialType) { return config.getDouble("Experience.Repair." + StringUtils.getCapitalized(repairMaterialType.toString())); }

    /* Taming */
    public int getTamingXP(EntityType type)
    {
        return config.getInt("Experience.Taming.Animal_Taming." + StringUtils.getPrettyEntityTypeString(type));
    }
}
