package net.runelite.client.plugins.microbot.vzn.protithefarm.models;

import lombok.Getter;
import lombok.Setter;
import net.runelite.api.ObjectID;
import net.runelite.api.TileObject;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.tithefarm.TitheFarmPlantState;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;

import static net.runelite.api.coords.WorldPoint.fromRegion;

public class TitheFarmPlant {

    private static final Duration PLANT_TIME = Duration.ofMinutes(1);

    @Getter @Setter private int index;
    @Getter @Setter private Instant planted;
    @Getter private final TitheFarmPlantState state;

    public int regionX;
    public int regionY;

    public TitheFarmPlant(int regionX, int regionY, int index) {
        this.planted = Instant.now();
        this.state = TitheFarmPlantState.UNWATERED;
        this.regionX = regionX;
        this.regionY = regionY;
        this.index = index;
    }

    public int[] expectedPatchGameObject() {
        return new int[]{ObjectID.BOLOGANO_SEEDLING, ObjectID.LOGAVANO_SEEDLING, ObjectID.GOLOVANOVA_SEEDLING};
    }

    public int[] expectedWateredObject() {
        return new int[]{
                ObjectID.BOLOGANO_SEEDLING, ObjectID.BOLOGANO_PLANT, ObjectID.BOLOGANO_PLANT_27401,
                ObjectID.LOGAVANO_SEEDLING, ObjectID.LOGAVANO_PLANT, ObjectID.LOGAVANO_PLANT_27412,
                ObjectID.GOLOVANOVA_SEEDLING, ObjectID.GOLOVANOVA_PLANT, ObjectID.GOLOVANOVA_PLANT_27390
        };
    }

    public int nextWateredObject(int currentId) {
        if (currentId == ObjectID.BOLOGANO_SEEDLING) {
            return ObjectID.BOLOGANO_PLANT;
        } else if (currentId == ObjectID.BOLOGANO_PLANT) {
            return ObjectID.BOLOGANO_PLANT_27401;
        } else if (currentId == ObjectID.LOGAVANO_SEEDLING) {
            return ObjectID.LOGAVANO_PLANT;
        } else if (currentId == ObjectID.LOGAVANO_PLANT) {
            return ObjectID.LOGAVANO_PLANT_27412;
        } else if (currentId == ObjectID.GOLOVANOVA_SEEDLING) {
            return ObjectID.GOLOVANOVA_PLANT;
        } else if (currentId == ObjectID.GOLOVANOVA_PLANT) {
            return ObjectID.GOLOVANOVA_PLANT_27390;
        } else {
            return -1;
        }
    }

    public int[] expectedHarvestObject() {
        return new int[]{
                ObjectID.BOLOGANO_PLANT_27404,
                ObjectID.LOGAVANO_PLANT_27415,
                ObjectID.GOLOVANOVA_PLANT_27393
        };
    }

    public TileObject getGameObject() {
        return Rs2GameObject.findGameObjectByLocation(fromRegion(Microbot.getClient().getLocalPlayer().getWorldLocation().getRegionID(), regionX, regionY, 0));
    }

    public boolean isEmptyPatch() {
        return getGameObject().getId() == ObjectID.TITHE_PATCH;
    }

    public boolean isEmptyPatchOrSeedling() {
        return Arrays.stream(expectedPatchGameObject()).anyMatch(id -> id == getGameObject().getId());
    }

    public boolean isValidToWater() {
        return Arrays.stream(expectedWateredObject()).anyMatch(id -> id == getGameObject().getId()) || isStage1() || isStage2();
    }

    public boolean isValidToHarvest() {
        return Arrays.stream(expectedHarvestObject()).anyMatch(id -> id == getGameObject().getId());
    }

    public boolean isStage1() {
        return getGameObject().getId() == ObjectID.LOGAVANO_PLANT
                || getGameObject().getId() == ObjectID.GOLOVANOVA_PLANT
                || getGameObject().getId() == ObjectID.BOLOGANO_PLANT;
    }

    public boolean isStage2() {
        return getGameObject().getId() == ObjectID.LOGAVANO_PLANT_27412
                || getGameObject().getId() == ObjectID.BOLOGANO_PLANT_27401
                || getGameObject().getId() == ObjectID.GOLOVANOVA_PLANT_27390;
    }

    public double getPlantTimeRelative() {
        Duration duration = Duration.between(planted, Instant.now());
        return duration.compareTo(PLANT_TIME) < 0 ? (double) duration.toMillis() / PLANT_TIME.toMillis() : 2;
    }
}
