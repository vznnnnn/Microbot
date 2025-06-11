package net.runelite.client.plugins.microbot.vzn.proconstruction;

import lombok.Getter;
import lombok.Setter;
import net.runelite.api.*;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.globval.enums.InterfaceTab;
import net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.microbot.util.antiban.Rs2AntibanSettings;
import net.runelite.client.plugins.microbot.util.dialogues.Rs2Dialogue;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.keyboard.Rs2Keyboard;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.npc.Rs2NpcModel;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.tabs.Rs2Tab;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;
import net.runelite.client.plugins.microbot.vzn.util.TimeUtil;
import org.apache.commons.lang3.ObjectUtils;

import java.awt.event.KeyEvent;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static net.runelite.client.plugins.microbot.util.antiban.enums.ActivityIntensity.EXTREME;

public class ProConstructionScript extends Script {

    public static final double VERSION = 1.0;

    private static final String DOOR_NAME = "Oak Dungeon Door";
    private static final int OAK_PLANK_ITEM_ID = ItemID.PLANK_OAK;

    private ProConstructionPlugin plugin;
    private ProConstructionConfig config;

    @Getter private Instant startTime = null;
    @Getter @Setter private int startXp = -1;
    @Getter @Setter private boolean butlerDespawned = false;
    public boolean error = false;

    @Override
    public void shutdown() {
        super.shutdown();

        if (mainScheduledFuture != null && !mainScheduledFuture.isCancelled()) {
            mainScheduledFuture.cancel(true);
        }

        Microbot.log("Shutting down script");
    }

    public boolean run(ProConstructionConfig config) {
        Microbot.enableAutoRunOn = false;
        Rs2Antiban.resetAntibanSettings();
        Rs2AntibanSettings.usePlayStyle = true;
        Rs2AntibanSettings.simulateFatigue = false;
        Rs2AntibanSettings.simulateAttentionSpan = true;
        Rs2AntibanSettings.behavioralVariability = true;
        Rs2AntibanSettings.nonLinearIntervals = true;
        Rs2AntibanSettings.dynamicActivity = true;
        Rs2AntibanSettings.profileSwitching = true;
        Rs2AntibanSettings.naturalMouse = true;
        Rs2AntibanSettings.simulateMistakes = true;
        Rs2AntibanSettings.moveMouseOffScreen = true;
        Rs2AntibanSettings.moveMouseRandomly = true;
        Rs2AntibanSettings.moveMouseRandomlyChance = 0.04;
        Rs2Antiban.setActivityIntensity(EXTREME);

        this.plugin = ProConstructionPlugin.instance;
        this.config = config;
        this.startTime = Instant.now();
        this.error = false;

        this.mainScheduledFuture = this.scheduledExecutorService.scheduleWithFixedDelay(() ->
        {
            try {
                if (!Microbot.isLoggedIn() || !super.run()) {
                    return;
                }

                handleTick();
            } catch (Exception ex) {
                Microbot.log("Error in main loop: " + ex.getMessage());
                ex.printStackTrace();
            }
        }, 0, 50, TimeUnit.MILLISECONDS);
        return true;
    }

    private void handleTick() {
        if (error) {
            Microbot.log("Error - logging out");
            Rs2Player.logout();
            sleep(1000);
            return;
        }

        if (startXp == -1) {
            startXp = Microbot.getClient().getSkillExperience(Skill.CONSTRUCTION);
        }

        handleButler();

        TileObject space = getOakDungeonDoorSpace();
        TileObject builtObject = getOakDungeonDoor();
        boolean hasRequiredPlanks = Rs2Inventory.hasItemAmount(OAK_PLANK_ITEM_ID, 10);

        if (space == null && builtObject != null) {
            removeDoor();
        } else if (space != null && builtObject == null && hasRequiredPlanks) {
            buildDoor();
        }
    }

    public String getTimeRunning() {
        return startTime != null ? TimeUtil.formatIntoAbbreviatedString((int) Duration.between(startTime, Instant.now()).toSeconds()) : "";
    }

    public int getXpGained() {
        return Microbot.getClient().getSkillExperience(Skill.CONSTRUCTION) - startXp;
    }

    public Rs2NpcModel getButler() {
        return ObjectUtils.firstNonNull(Rs2Npc.getNpc(NpcID.POH_SERVANT_DEMON), Rs2Npc.getNpc(NpcID.POH_SERVANT_MULTI_DEMON));
    }

    public TileObject getOakDungeonDoorSpace() {
        return Rs2GameObject.findObjectById(15328); // ID for oak dungeon door space
    }

    public TileObject getOakDungeonDoor() {
        return Rs2GameObject.findObjectById(13344); // ID for oak dungeon door
    }

    public boolean hasDialogueOptionToBank() {
        return Rs2Widget.findWidget("Go to the bank...", null) != null;
    }

    public boolean hasDialogueOptionToTakeBackToBank() {
        return Rs2Widget.findWidget("Take them back to the bank", null) != null;
    }

    public boolean hasDialogueOptionToUnnote() {
        return Rs2Widget.findWidget("Un-note", null) != null;
    }

    public boolean hasPayButlerDialogue() {
        return Rs2Widget.findWidget("must render unto me the 10,000 coins that are due", null) != null;
    }

    public boolean hasDialogueOptionToPay() {
        return Rs2Widget.findWidget("Okay, here's 10,000 coins.", null) != null;
    }

    public boolean hasButlerPaidDialogue() {
        return Rs2Widget.findWidget("Thou hast my unfailing service", null) != null;
    }

    public boolean hasButlerReturnedDialogue() {
        return Rs2Widget.findWidget("I have returned with what you asked", null) != null;
    }

    public boolean hasFurnitureInterfaceOpen() {
        Widget furnitureWidget = Rs2Widget.findWidget("Furniture", null);
        if (furnitureWidget != null) {
            System.out.println("Furniture interface is open.");
            return true;
        }
        System.out.println("Furniture interface is not open.");
        return false;
    }

    public boolean hasRemoveDoorInterfaceOpen() {
        return Rs2Widget.findWidget("Really remove it?", null) != null;
    }

    private void buildDoor() {
        final TileObject space = getOakDungeonDoorSpace();
        final char buildKey = '1';

        if (space == null) {
            return;
        }

        if (Rs2GameObject.interact(space, "Build")) {
            System.out.println("Interacted with build space: " + space.getId());
            sleepUntilOnClientThread(this::hasFurnitureInterfaceOpen, 2500);
            System.out.println("Pressing key: " + buildKey);
            Rs2Keyboard.keyPress(buildKey); // Ensure this is the correct key for the selected build option
            sleepUntilOnClientThread(() -> getOakDungeonDoor() != null, 2500);
            System.out.println("Built door");
        } else {
            System.out.println("Failed to interact with build space: " + space.getId());
        }
    }

    private void removeDoor() {
        final TileObject builtObject = getOakDungeonDoor();

        if (builtObject == null) {
            return;
        }

        if (Rs2GameObject.interact(builtObject, "Remove")) {
            System.out.println("Interacted with remove option: " + builtObject.getId());
            sleepUntilOnClientThread(this::hasRemoveDoorInterfaceOpen, 2500);
            Rs2Keyboard.keyPress('1');
            sleepUntilOnClientThread(() -> getOakDungeonDoorSpace() != null, 2500);
            System.out.println("Removed door");

            if (Rs2Inventory.itemQuantity(OAK_PLANK_ITEM_ID) < 10 && (getButler() == null || !Rs2Dialogue.isInDialogue())) {
                sleepUntil(() -> getButler() != null, 1500);
                handleButler();
            }
        } else {
            System.out.println("Failed to interact with remove option: " + builtObject.getId());
        }
    }

    private void handleButler() {
        Rs2NpcModel butler = getButler();

        if (!butlerDespawned && butler == null) {
            Microbot.log("Call Servant");
            Rs2Tab.switchToSettingsTab();
            sleepUntilOnClientThread(() -> Rs2Tab.getCurrentTab() == InterfaceTab.SETTINGS, 1000);
            sleep(100);

            Widget houseOptionWidget = Rs2Widget.findWidget(SpriteID.OPTIONS_HOUSE_OPTIONS, null);
            if (houseOptionWidget != null) {
                Microbot.getMouse().click(houseOptionWidget.getCanvasLocation());
            }

            sleepUntilOnClientThread(() -> Rs2Widget.hasWidget("Call Servant"), 1000);
            sleep(100);

            Widget callServantWidget = Rs2Widget.findWidget("Call Servant", null);
            if (callServantWidget != null) {
                Microbot.getMouse().click(callServantWidget.getCanvasLocation());
            }

            sleepUntil(() -> getButler() != null, 1000);
            butler = getButler();
        }

        if (butler != null) {
            Rs2Npc.interact(butler, "Talk-to");
            sleepUntil(Rs2Dialogue::isInDialogue, 1500);
            sleep(100);
        }

        handleButlerDialogue(butler);
    }

    private void handleButlerDialogue(Rs2NpcModel butler) {
        while (Rs2Dialogue.isInDialogue()) {
            if (hasPayButlerDialogue()) {
                Rs2Keyboard.keyPress(KeyEvent.VK_SPACE);
                sleepUntilOnClientThread(() -> !hasPayButlerDialogue(), 1000);
            } else if (hasDialogueOptionToPay()) {
                Rs2Keyboard.keyPress('1');
                sleepUntilOnClientThread(() -> !hasDialogueOptionToPay(), 1000);
            } else if (hasButlerPaidDialogue()) {
                Rs2Keyboard.keyPress(KeyEvent.VK_SPACE);
                sleepUntilOnClientThread(() -> !hasButlerPaidDialogue(), 1000);
            } else if (hasButlerReturnedDialogue()) {
                Rs2Keyboard.keyPress(KeyEvent.VK_SPACE);
                sleepUntilOnClientThread(() -> !hasButlerPaidDialogue(), 1000);
            } else if (hasDialogueOptionToTakeBackToBank()) {
                Rs2Keyboard.keyPress('2');
                sleepUntilOnClientThread(() -> !hasDialogueOptionToTakeBackToBank(), 1000);
                Rs2Keyboard.keyPress(KeyEvent.VK_SPACE);
                sleepUntilOnClientThread(() -> !Rs2Dialogue.isInDialogue(), 1000);
            } else if (hasDialogueOptionToBank()) {
                Rs2Inventory.useItemOnNpc(OAK_PLANK_ITEM_ID + 1, butler.getId()); // + 1 for noted item
                sleepUntilOnClientThread(() -> Rs2Widget.hasWidget("Dost thou wish me to exchange that certificate"));

                Rs2Keyboard.keyPress(KeyEvent.VK_SPACE);
                sleepUntilOnClientThread(() -> Rs2Widget.hasWidget("Select an option"));
                Rs2Keyboard.typeString("1");

                sleepUntilOnClientThread(() -> Rs2Widget.hasWidget("Enter amount:"));
                Rs2Keyboard.typeString("20");
                Rs2Keyboard.enter();
            } else if (hasDialogueOptionToUnnote()) {
                Rs2Keyboard.keyPress('1');
                sleepUntilOnClientThread(() -> !hasDialogueOptionToUnnote());
            }

            sleep(100);
        }
    }

}
