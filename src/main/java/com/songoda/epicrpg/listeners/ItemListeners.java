package com.songoda.epicrpg.listeners;

import com.songoda.epicrpg.EpicRPG;
import com.songoda.epicrpg.story.quest.action.ActiveAction;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;

public class ItemListeners implements Listener {

    private final EpicRPG plugin;

    public ItemListeners(EpicRPG plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemDrop(PlayerDropItemEvent event) {

        for (ActiveAction action : plugin.getActionManager().getActiveActions())
            action.getAction().onDrop(event, action);
    }

    // We're putting this on normal so it works with UltimateStacker
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onItemPickup(PlayerPickupItemEvent event) {

        for (ActiveAction action : plugin.getActionManager().getActiveActions())
            action.getAction().onPickup(event, action);
    }

}
