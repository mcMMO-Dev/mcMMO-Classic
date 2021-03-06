package com.gmail.nossr50.skills.mining;

import com.gmail.nossr50.config.experience.ExperienceConfig;
import com.gmail.nossr50.datatypes.skills.SkillType;
import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.util.Misc;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.ItemStack;

public class Mining {

    /**
     * Calculate XP gain for Mining.
     *
     * @param blockState The {@link BlockState} to check ability activation for
     */
    public static int getBlockXp(BlockState blockState) {
        int xp = ExperienceConfig.getInstance().getXp(SkillType.MINING, blockState.getType());

        if (xp == 0 && mcMMO.getModManager().isCustomMiningBlock(blockState)) {
            xp = mcMMO.getModManager().getBlock(blockState).getXpGain();
        }

        return xp;
    }
}
