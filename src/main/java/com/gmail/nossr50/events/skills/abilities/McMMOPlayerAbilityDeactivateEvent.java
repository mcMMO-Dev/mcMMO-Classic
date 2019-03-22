package com.gmail.nossr50.events.skills.abilities;

import com.gmail.nossr50.datatypes.skills.SkillType;
import org.bukkit.entity.Player;

public class McMMOPlayerAbilityDeactivateEvent extends McMMOPlayerAbilityEvent {
    public McMMOPlayerAbilityDeactivateEvent(Player player, SkillType skill) {
        super(player, skill);
    }
}
