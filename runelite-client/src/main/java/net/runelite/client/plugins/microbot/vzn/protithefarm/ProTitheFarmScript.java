package net.runelite.client.plugins.microbot.vzn.protithefarm;

import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Skill;
import net.runelite.api.TileObject;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.microbot.util.antiban.Rs2AntibanSettings;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;
import net.runelite.client.plugins.microbot.vzn.protithefarm.enums.TitheFarmMaterial;
import net.runelite.client.plugins.microbot.vzn.protithefarm.enums.TitheFarmState;
import net.runelite.client.plugins.microbot.vzn.protithefarm.models.TitheFarmPlant;
import net.runelite.client.plugins.microbot.vzn.util.TimeUtil;
import org.apache.commons.lang3.ObjectUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static net.runelite.client.plugins.microbot.util.antiban.enums.ActivityIntensity.EXTREME;

public class ProTitheFarmScript extends Script {

    public static final double VERSION = 1.0;

    private static final int DISTANCE_THRESHOLD_MINIMAP_WALK = 8;

    private ProTitheFarmPlugin plugin;
    private ProTitheFarmConfig config;

    @Getter private Instant startTime = null;
    @Getter @Setter private int startXp = -1;
    public boolean error = false;

    @Getter private List<TitheFarmPlant> plants = new ArrayList<>();
    @Getter private TitheFarmState state = TitheFarmState.FILLING_CANS;
    @Getter private int stage = 0;
    @Getter private int fruitsFarmed = 0;
    @Getter @Setter private int gricollerCanCharges = -1;

    @Override
    public void shutdown() {
        super.shutdown();

        if (mainScheduledFuture != null && !mainScheduledFuture.isCancelled()) {
            mainScheduledFuture.cancel(true);
        }

        Microbot.log("Shutting down script");
    }

    public boolean run(ProTitheFarmConfig config) {
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

        this.plugin = ProTitheFarmPlugin.instance;
        this.config = config;
        this.startTime = Instant.now();
        this.error = false;
        this.state = TitheFarmState.FILLING_CANS;
        this.stage = 0;

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
            startXp = Microbot.getClient().getSkillExperience(Skill.FARMING);
        }

        if (!isInMinigame()) {
            return;
        }

        if (plants.isEmpty()) {
            populatePlantsList();
        }

        TitheFarmMaterial material = TitheFarmMaterial.getSeedForLevel();

        if (!Rs2Inventory.hasItem(material.getName())) {
            return;
        }

        evaluateRunEnergy();

        switch (state) {
            case FILLING_CANS:
                if (hasWateringCanToBeFilled()) {
                    fillWateringCans();
                }

                if (hasGricollersCan()) {
                    checkGricollersCan();

                    if (gricollerCanCharges < 100) {
                        fillGricollersCan();
                    }
                }

                state = TitheFarmState.PLANTING;
                break;
            case PLANTING:
                walkToStartPoint();

                for (TitheFarmPlant plant : plants) {
                    plantSeed(material, plant);
                }

                state = TitheFarmState.WATERING;
                break;
            case WATERING:
                for (int i = 0; i < 3; i++) {
                    walkToStartPoint();

                    for (TitheFarmPlant plant : plants) {
                        waterPlant(plant);
                    }
                }

                state = TitheFarmState.HARVESTING;
                break;
            case HARVESTING:
                walkToStartPoint();

                for (TitheFarmPlant plant : plants) {
                    harvestPlant(plant);
                }

                if (Rs2Inventory.itemQuantity(ItemID.HOSIDIUS_TITHE_FRUIT_A) >= 100
                        || Rs2Inventory.itemQuantity(ItemID.HOSIDIUS_TITHE_FRUIT_B) >= 100
                        || Rs2Inventory.itemQuantity(ItemID.HOSIDIUS_TITHE_FRUIT_C) >= 100) {
                    TileObject object = ObjectUtils.firstNonNull(
                            Rs2GameObject.findObjectById(ObjectID.TITHE_SACK_OF_FRUIT_EMPTY),
                            Rs2GameObject.findObjectById(ObjectID.TITHE_SACK_OF_FRUIT)
                    );

                    if (object != null) {
                        Rs2Walker.walkMiniMap(object.getWorldLocation(), 2);
                        Rs2GameObject.interact(object, "Deposit");
                        Rs2Inventory.waitForInventoryChanges(5000);
                    }
                }

                state = TitheFarmState.FILLING_CANS;
                break;
        }
    }

    public String getTimeRunning() {
        return startTime != null ? TimeUtil.formatIntoAbbreviatedString((int) Duration.between(startTime, Instant.now()).toSeconds()) : "";
    }

    public int getXpGained() {
        return Microbot.getClient().getSkillExperience(Skill.FARMING) - startXp;
    }

    private boolean isInMinigame() {
        return Rs2Widget.getWidget(15794178) != null;
    }

    private WorldPoint getStartPoint() {
        return WorldPoint.fromRegion(
                Microbot.getClient().getLocalPlayer().getWorldLocation().getRegionID(),
                config.getStartPointX(),
                config.getStartPointY(),
                Microbot.getClient().getLocalPlayer().getWorldView().getPlane()
        );
    }

    private void evaluateRunEnergy() {
        if (Rs2Player.getRunEnergy() <= 60 && !Rs2Player.hasStaminaActive() && Rs2Inventory.hasItem("Stamina potion")) {
            Rs2Inventory.interact("Stamina potion", "Drink");
        }

        if (!Rs2Player.isRunEnabled()) {
            Rs2Player.toggleRunEnergy(true);
        }
    }

    private void walkToStartPoint() {
        Rs2Walker.walkMiniMap(getStartPoint(), 2);
        sleepUntil(() -> Rs2Player.getWorldLocation().distanceTo2D(getStartPoint()) < DISTANCE_THRESHOLD_MINIMAP_WALK);
    }

    private TileObject walkToBarrel() {
        final TileObject gameObject = Rs2GameObject.findObjectById(ObjectID.WATER_BARREL1);

        if (gameObject == null) {
            return null;
        }

        if (gameObject.getWorldLocation().distanceTo2D(Microbot.getClient().getLocalPlayer().getWorldLocation()) > DISTANCE_THRESHOLD_MINIMAP_WALK) {
            Rs2Walker.walkMiniMap(gameObject.getWorldLocation(), 2);
            sleepUntil(Rs2Player::isMoving);
        }

        sleepUntil(() -> gameObject.getWorldLocation().distanceTo2D(Microbot.getClient().getLocalPlayer().getWorldLocation()) < DISTANCE_THRESHOLD_MINIMAP_WALK);

        return gameObject;
    }

    public boolean hasWateringCanToBeFilled() {
        return Rs2Inventory.hasItem(ItemID.WATERING_CAN_7) || Rs2Inventory.hasItem(ItemID.WATERING_CAN_6)
                || Rs2Inventory.hasItem(ItemID.WATERING_CAN_5) || Rs2Inventory.hasItem(ItemID.WATERING_CAN_4)
                || Rs2Inventory.hasItem(ItemID.WATERING_CAN_3) || Rs2Inventory.hasItem(ItemID.WATERING_CAN_2)
                || Rs2Inventory.hasItem(ItemID.WATERING_CAN_1) || Rs2Inventory.hasItem(ItemID.WATERING_CAN_0);
    }

    public boolean hasGricollersCan() {
        return Rs2Inventory.hasItem(ItemID.ZEAH_WATERINGCAN);
    }

    public int getWateringCanToBeFilled() {
        return Stream.of(
                ItemID.WATERING_CAN_7, ItemID.WATERING_CAN_6, ItemID.WATERING_CAN_5,
                ItemID.WATERING_CAN_4, ItemID.WATERING_CAN_3, ItemID.WATERING_CAN_2,
                ItemID.WATERING_CAN_1, ItemID.WATERING_CAN_0
        ).filter(Rs2Inventory::hasItem).findFirst().orElse(-1);
    }

    private boolean isNextToBarrel(TileObject barrelObject) {
        return barrelObject.getWorldLocation().distanceTo2D(Microbot.getClient().getLocalPlayer().getWorldLocation()) <= 1;
    }

    private void fillWateringCans() {
        int wateringCanId = getWateringCanToBeFilled();

        while (wateringCanId != -1) {
            TileObject barrel = walkToBarrel();
            Rs2Inventory.interact(getWateringCanToBeFilled(), "Use");
            sleepUntil(() -> Rs2Inventory.getSelectedItemId() == getWateringCanToBeFilled(), 500);
            Rs2GameObject.interact(barrel, "Use");
            sleep(500);
            wateringCanId = getWateringCanToBeFilled();
        }
    }

    private void fillGricollersCan() {
        WorldPoint startLocation = Rs2Player.getWorldLocation();

        boolean filled = false;
        while (!filled) {
            // Step 1: Click patch
            TileObject barrel = walkToBarrel();

            if (barrel == null) {
                return;
            }

            Rs2Inventory.interact(ItemID.ZEAH_WATERINGCAN, "Use");
            sleepUntil(() -> Rs2Inventory.getSelectedItemId() == ItemID.ZEAH_WATERINGCAN, 500);
            Rs2GameObject.interact(barrel, "Use");

            // Step 3: Retry click if player didn't move
            sleep(150); // small delay for move to register

            if (!isNextToBarrel(barrel) && startLocation.equals(Rs2Player.getWorldLocation())) {
                continue;
            }

            // Step 4: Wait for refill
            filled = sleepUntil(() -> gricollerCanCharges >= 100, 1500);
        }
    }

    private void checkGricollersCan() {
        gricollerCanCharges = -1;
        Rs2Inventory.interact(ItemID.ZEAH_WATERINGCAN, "Check");
        sleep(1000);
    }

    private void dropFertiliser() {
        if (Rs2Inventory.hasItem("Gricoller's fertiliser")) {
            Rs2Inventory.drop("Gricoller's fertiliser");
        }
    }

    private static void clickPatch(TitheFarmPlant plant) {
        WorldView worldView = Microbot.getClient().getWorldView(Rs2Player.getLocalLocation().getWorldView());

        if (worldView == null) {
            return;
        }

        WorldPoint worldPoint = WorldPoint.fromRegion(
                Microbot.getClient().getLocalPlayer().getWorldLocation().getRegionID(),
                plant.regionX,
                plant.regionY,
                worldView.getPlane()
        );

        Rs2GameObject.interact(worldPoint);
    }

    private void clickPatch(TitheFarmPlant plant, String action) {
        WorldView worldView = Microbot.getClient().getWorldView(Rs2Player.getLocalLocation().getWorldView());

        if (worldView == null) {
            return;
        }

        WorldPoint worldPoint = WorldPoint.fromRegion(
                Microbot.getClient().getLocalPlayer().getWorldLocation().getRegionID(),
                plant.regionX,
                plant.regionY,
                worldView.getPlane()
        );

        Rs2GameObject.interact(worldPoint, action);
    }

    private boolean isNextToPatch(TitheFarmPlant plant) {
        return plant.getGameObject().getWorldLocation().distanceTo2D(Microbot.getClient().getLocalPlayer().getWorldLocation()) <= 2;
    }

    private void plantSeed(TitheFarmMaterial material, TitheFarmPlant plant) {
        WorldPoint startLocation = Rs2Player.getWorldLocation();

        boolean planted = false;
        while (!planted) {
            // Step 1: Use the seed
            Rs2Inventory.interact(material.getSeedId(), "Use");
            sleepUntil(() -> Rs2Inventory.getSelectedItemId() == material.getSeedId(), 500);

            // Step 2: Click patch
            clickPatch(plant);

            // Step 3: Retry click if player didn't move
            sleep(150); // small delay for move to register

            if (!isNextToPatch(plant) && startLocation.equals(Rs2Player.getWorldLocation())) {
                continue;
            }

            if (Arrays.stream(plant.expectedPatchGameObject()).anyMatch(id -> id == plant.getGameObject().getId())) {
                break;
            }

            // Step 4: Wait for proximity + animation
            planted = sleepUntil(() -> {
                var player = Microbot.getClient().getLocalPlayer();
                return player.getAnimation() == 2291 && isNextToPatch(plant);
            }, 1500);
        }
    }

    private void waterPlant(TitheFarmPlant plant) {
        WorldPoint startLocation = Rs2Player.getWorldLocation();
        int nextObjectId = plant.nextWateredObject(plant.getGameObject().getId());

        boolean watered = false;
        while (!watered) {
            // Step 1: Click patch
            clickPatch(plant, "Water");

            // Step 3: Retry click if player didn't move
            sleep(150); // small delay for move to register

            if (!isNextToPatch(plant) && startLocation.equals(Rs2Player.getWorldLocation())) {
                continue;
            }

            if (plant.getGameObject().getId() == nextObjectId) {
                break;
            }

            // Step 4: Wait for proximity + animation
            watered = sleepUntil(() -> {
                var player = Microbot.getClient().getLocalPlayer();
                return player.getAnimation() == 2293 && isNextToPatch(plant);
            }, 1500);
        }
    }

    private void harvestPlant(TitheFarmPlant plant) {
        WorldPoint startLocation = Rs2Player.getWorldLocation();

        boolean harvested = false;
        while (!harvested) {
            // Step 1: Click patch
            clickPatch(plant, "Harvest");

            // Step 3: Retry click if player didn't move
            sleep(150); // small delay for move to register

            if (!isNextToPatch(plant) && startLocation.equals(Rs2Player.getWorldLocation())) {
                continue;
            }

            if (Arrays.stream(plant.expectedPatchGameObject()).anyMatch(id -> id == plant.getGameObject().getId())) {
                break;
            }

            // Step 4: Wait for proximity + animation
            harvested = sleepUntil(() -> {
                var player = Microbot.getClient().getLocalPlayer();
                return player.getAnimation() == 830 && isNextToPatch(plant);
            }, 1500);
        }
    }

    private void populatePlantsList() {
        plants = new ArrayList<>(Arrays.asList(
                new TitheFarmPlant(35, 25, 1),
                new TitheFarmPlant(40, 25, 2),
                new TitheFarmPlant(35, 28, 3),
                new TitheFarmPlant(40, 28, 4),
                new TitheFarmPlant(35, 31, 5),
                new TitheFarmPlant(40, 31, 6),
                new TitheFarmPlant(35, 34, 7),
                new TitheFarmPlant(40, 34, 8),
                new TitheFarmPlant(35, 40, 9),
                new TitheFarmPlant(40, 40, 10),
                new TitheFarmPlant(35, 43, 11),
                new TitheFarmPlant(40, 43, 12),
                new TitheFarmPlant(35, 46, 13),
                new TitheFarmPlant(40, 46, 14),
                new TitheFarmPlant(35, 49, 15),
                new TitheFarmPlant(40, 49, 16)
        ));
    }

}
