package com.songoda.epicrpg.tasks;

import com.songoda.core.compatibility.CompatibleParticleHandler;
import com.songoda.core.compatibility.CompatibleSound;
import com.songoda.core.utils.LocationUtils;
import com.songoda.core.utils.TextUtils;
import com.songoda.epicrpg.EpicRPG;
import com.songoda.epicrpg.Region.Region;
import com.songoda.epicrpg.settings.Settings;
import com.songoda.epicrpg.story.contender.ContendentManager;
import com.songoda.epicrpg.story.contender.StoryContender;
import com.songoda.epicrpg.story.contender.StoryParty;
import com.songoda.epicrpg.story.contender.StoryPlayer;
import com.songoda.epicrpg.story.quest.ActiveQuest;
import com.songoda.epicrpg.story.quest.Objective;
import com.songoda.epicrpg.story.quest.Quest;
import com.songoda.epicrpg.story.quest.RemainingObjective;
import com.songoda.epicrpg.story.quest.action.ActiveAction;
import com.songoda.epicrpg.story.quest.action.actions.RightClickBlock;
import com.songoda.epicrpg.story.quest.action.actions.RightClickCitizen;
import net.citizensnpcs.api.CitizensAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.stream.Collectors;

public class QuestTask extends BukkitRunnable {

    private static QuestTask instance;
    private static EpicRPG plugin;
    private static ContendentManager contendentManager;

    private Map<UUID, BossBar> activeBossBars = new HashMap<>();


    public QuestTask(EpicRPG plug) {
        plugin = plug;
    }

    public static QuestTask startTask(EpicRPG plug) {
        plugin = plug;
        contendentManager = plugin.getContendentManager();
        if (instance == null) {
            instance = new QuestTask(plugin);
            instance.runTaskTimerAsynchronously(plugin, 0, 5);
        }

        return instance;
    }

    @Override
    public void run() {
        if (plugin.getStoryManager().getQuests().stream().noneMatch(Quest::isActive)) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            StoryContender contender = contendentManager.getContender(player);
            if (!(contender instanceof StoryParty))
                plugin.getContendentManager().discoverQuests(contender);
            boolean updatedBossBar = false;

            List<ActiveQuest> active = contender.getActiveQuests();
            if (active.isEmpty() && contender instanceof StoryParty) {
                BossBar bossBar = activeBossBars.computeIfAbsent(contender.getUniqueId(),
                        b -> Bukkit.createBossBar("title",
                                BarColor.valueOf(Settings.BOSS_BAR_COLOR.getString()),
                                BarStyle.SOLID));
                bossBar.setTitle(plugin.getLocale().getMessage("general.bossbar.idle").getMessage());
                bossBar.setProgress(1);
                bossBar.addPlayer(player);
                BossBar playerBar = activeBossBars.get(player.getUniqueId());
                if (playerBar != null)
                    playerBar.removeAll();
            }
            for (ActiveQuest activeQuest : new HashSet<>(active).stream()
                    .sorted(Comparator.comparing(q -> !q.isFocused()))
                    .sorted(Comparator.comparing(q -> {
                        if (q.getActiveQuest() == null)
                            return false;
                        Region region = plugin.getStoryManager()
                                .getEnabledQuest(q.getActiveQuest()).getRegion();
                        return region == null
                                || !LocationUtils.isInArea(player.getLocation(), region.getPos1(), region.getPos2());
                    }))
                    .collect(Collectors.toCollection(LinkedHashSet::new))) {
                if (activeQuest == null) continue;
                Quest quest = plugin.getStoryManager().getEnabledQuest(activeQuest.getActiveQuest());

                if (quest == null || quest.getObjectives() == null || quest.getObjectives().size() == 0)
                    continue;

                if (activeQuest.getRemainingObjectives().isEmpty()) {
                    CompatibleSound.UI_TOAST_CHALLENGE_COMPLETE.play(player);
                    contender.completeQuest(quest);
                    quest.giveRewards(player);
                    continue;
                }
                if (updatedBossBar) continue;
                updatedBossBar = true;

                Map.Entry<UUID, RemainingObjective> entry = activeQuest.getRemainingObjectives().entrySet().iterator().next();
                RemainingObjective remainingObjective = entry.getValue();
                Objective currentObjective = quest.getObjective(remainingObjective.getUniqueId());

                List<ActiveAction> activeActions = plugin.getActionManager().getActiveActionsByObjective(currentObjective);

                if (activeActions.isEmpty()) continue;

                double current = activeActions.get(0).getAmount() == 1 ? activeActions.size() -
                        remainingObjective.getRemainingActions().size() : activeActions.get(0).getAmount() -
                        remainingObjective.getAmount(activeActions.get(0));
                int goal = activeActions.get(0).getAmount() == 1 ? activeActions.size() : activeActions.get(0).getAmount();

                ActiveAction action = activeActions.get(0);
                // Effects
                if (action.getAction() instanceof RightClickCitizen) {
                    Location location = CitizensAPI.getNPCRegistry()
                            .getById(((RightClickCitizen.RightClickCitizenDataStore) action
                                    .getActionDataStore()).getCitizenId()).getStoredLocation();
                    if (player.getWorld() == location.getWorld())
                        CompatibleParticleHandler.redstoneParticles(location
                                        .add(0, 2.5, 0),
                                255, 85, 255, 1, 5, .1f, player);
                } else if (action.getAction() instanceof RightClickBlock) {
                    Location location = (((RightClickBlock.RightClickBlockDataStore) action
                            .getActionDataStore()).getLocation());
                    if (player.getWorld() == location.getWorld())
                        CompatibleParticleHandler.redstoneParticles(location.add(.5, .5, .5),
                                255, 85, 255, 1, 10, .5f, player);
                }

                String title = TextUtils.formatText(plugin.getLocale().getMessage("general.bossbar.objective").getMessage()
                        + currentObjective.getTitle());
                BossBar bossBar = activeBossBars.computeIfAbsent(player.getUniqueId(),
                        b -> Bukkit.createBossBar(title,
                                BarColor.valueOf(Settings.BOSS_BAR_COLOR.getString()),
                                BarStyle.SOLID));

                boolean silent = false;
                if (contender instanceof StoryPlayer && ((StoryPlayer) contender).isSilent())
                    silent = true;

                if (currentObjective.isVisible() && !silent)
                    bossBar.addPlayer(player);
                else {
                    bossBar.removePlayer(player);
                    activeBossBars.remove(player.getUniqueId());
                    bossBar.removeAll();
                }
                if (contender instanceof StoryParty) {
                    BossBar partyBar = activeBossBars.get(contender.getUniqueId());
                    if (partyBar != null)
                        partyBar.removeAll();
                }
                if (goal == 1 || goal == 0) {
                    bossBar.setTitle(title);
                    bossBar.setProgress(1);
                } else {
                    bossBar.setTitle(plugin.getLocale().getMessage("general.bossbar.amount")
                            .processPlaceholder("title", title)
                            .processPlaceholder("current", current)
                            .processPlaceholder("goal", goal).getMessage());
                    bossBar.setProgress(current / goal);
                }
            }

            if (!updatedBossBar && activeBossBars.containsKey(player.getUniqueId())) {
                BossBar bossBar = activeBossBars.remove(player.getUniqueId());
                bossBar.removeAll();
            }
        }
    }

    public void flush() {
        for (BossBar bossBar : activeBossBars.values())
            bossBar.removeAll();
    }

    public void remove(StoryParty storyParty, Player player) {
        BossBar bossBar = activeBossBars.get(storyParty.getUniqueId());
        if (bossBar == null) return;
        bossBar.removePlayer(player);
    }

    public void removeAll(StoryParty storyParty) {
        BossBar bossBar = activeBossBars.get(storyParty.getUniqueId());
        if (bossBar == null) return;
        bossBar.removeAll();
    }
}
