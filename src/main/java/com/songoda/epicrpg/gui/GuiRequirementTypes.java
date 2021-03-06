package com.songoda.epicrpg.gui;

import com.songoda.core.compatibility.CompatibleMaterial;
import com.songoda.core.gui.Gui;
import com.songoda.core.gui.GuiUtils;
import com.songoda.epicrpg.EpicRPG;
import com.songoda.epicrpg.story.quest.Objective;
import com.songoda.epicrpg.story.quest.requirement.Requirement;
import com.songoda.epicrpg.story.quest.requirement.RequirementType;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

public class GuiRequirementTypes extends Gui {

    public GuiRequirementTypes(EpicRPG plugin, Player player, Objective objective) {
        setRows(6);
        setDefaultItem(null);

        setTitle("Pick a requirement type");

        setActionForRange(0, 53, null);

        setButton(0, 8, GuiUtils.createButtonItem(CompatibleMaterial.BARRIER, "Cancel"),
                (event) -> {
                    guiManager.showGUI(player, new GuiRequirements(plugin, player, objective));
                });


        List<RequirementType> requirements = Arrays.asList(RequirementType.values());
        for (int i = 0; i < requirements.size(); i++) {
            RequirementType requirementType = requirements.get(i);
            setButton(i + 9, GuiUtils.createButtonItem(CompatibleMaterial.PAPER, requirementType.name()),
                    (event) -> {
                        player.closeInventory();
                        Requirement requirement = requirementType.init(objective);
                        objective.addRequirement(requirement);
                        requirement.setup(player);
                    });
        }
    }
}