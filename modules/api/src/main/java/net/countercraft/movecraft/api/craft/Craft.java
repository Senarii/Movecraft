/*
 * This file is part of Movecraft.
 *
 *     Movecraft is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Movecraft is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Movecraft.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.countercraft.movecraft.api.craft;

import net.countercraft.movecraft.api.MovecraftLocation;
import net.countercraft.movecraft.api.Rotation;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/*import net.countercraft.movecraft.async.AsyncManager;
import net.countercraft.movecraft.async.rotation.RotationTask;
import net.countercraft.movecraft.async.translation.TranslationTask;
import net.countercraft.movecraft.async.translation.TranslationTaskData;*/

public abstract class Craft {
    protected final CraftType type;
    protected int[][][] hitBox;
    protected MovecraftLocation[] blockList;
    protected World w;
    private AtomicBoolean processing = new AtomicBoolean();
    protected int minX;
    protected int minZ;
    private int maxHeightLimit;
    private boolean cruising;
    private boolean sinking;
    private boolean disabled;
    private byte cruiseDirection;
    private long lastCruiseUpdate;
    private long lastBlockCheck;
    private long lastRotateTime=0;
    private long origPilotTime;
    private int lastDX, lastDY, lastDZ;
    private double burningFuel;
    private boolean pilotLocked;
    private double pilotLockedX;
    private double pilotLockedY;
    private int origBlockCount;
    private double pilotLockedZ;
    private Player notificationPlayer;
    private Player cannonDirector;
    private Player AADirector;
    private HashMap<Player, Long> movedPlayers = new HashMap<>();
    //private int blockUpdates;
    private float meanMoveTime;
    private int numMoves;
    public Craft(CraftType type, World world) {
        this.type = type;
        this.w = world;
        this.blockList = new MovecraftLocation[1];
        if (type.getMaxHeightLimit() > w.getMaxHeight() - 1) {
            this.maxHeightLimit = w.getMaxHeight() - 1;
        } else {
            this.maxHeightLimit = type.getMaxHeightLimit();
        }
        this.pilotLocked = false;
        this.pilotLockedX = 0.0;
        this.pilotLockedY = 0.0;
        this.pilotLockedZ = 0.0;
        this.cannonDirector = null;
        this.AADirector = null;
        this.lastCruiseUpdate = System.currentTimeMillis() - 10000;
        this.cruising = false;
        this.sinking = false;
        this.disabled = false;
        this.origPilotTime = System.currentTimeMillis();
        numMoves = 0;
    }

    public boolean isNotProcessing() {
        return !processing.get();
    }

    public void setProcessing(boolean processing) {
        this.processing.set(processing);
    }

    public MovecraftLocation[] getBlockList() {
        synchronized (blockList) {
            return blockList.clone();
        }
    }

    public void setBlockList(MovecraftLocation[] blockList) {
        synchronized (this.blockList) {
            this.blockList = blockList;
        }
    }

    public CraftType getType() {
        return type;
    }

    public World getW() {
        return w;
    }

    public int[][][] getHitBox() {
        return hitBox;
    }

    public void setHitBox(int[][][] hitBox) {
        this.hitBox = hitBox;
    }

    public abstract void detect(Player player, Player notificationPlayer, MovecraftLocation startPoint);

    public abstract void translate(int dx, int dy, int dz);

    @Deprecated
    public void resetSigns(boolean resetCruise, boolean resetAscend, boolean resetDescend) {
        for (MovecraftLocation aBlockList : blockList) {
            int blockID = w.getBlockAt(aBlockList.getX(), aBlockList.getY(), aBlockList.getZ()).getTypeId();
            if (blockID == 63 || blockID == 68) {
                Sign s = (Sign) w.getBlockAt(aBlockList.getX(), aBlockList.getY(), aBlockList.getZ()).getState();
                if (resetCruise)
                    if (ChatColor.stripColor(s.getLine(0)).equals("Cruise: ON")) {
                        s.setLine(0, "Cruise: OFF");
                        s.update(true);
                    }
                if (resetAscend)
                    if (ChatColor.stripColor(s.getLine(0)).equals("Ascend: ON")) {
                        s.setLine(0, "Ascend: OFF");
                        s.update(true);
                    }
                if (resetDescend)
                    if (ChatColor.stripColor(s.getLine(0)).equals("Descend: ON")) {
                        s.setLine(0, "Descend: OFF");
                        s.update(true);
                    }
            }
        }
    }

    public abstract void rotate(Rotation rotation, MovecraftLocation originPoint);

    public abstract void rotate(Rotation rotation, MovecraftLocation originPoint, boolean isSubCraft);

    public int getMaxX() {
        return minX + hitBox.length;
    }

    public int getMaxZ() {
        return minZ + hitBox[0].length;
    }

    public int getMinY() {
        int minY = 65535;
        int maxY = -65535;
        for (int[][] i1 : hitBox) {
            if (i1 != null)
                for (int[] i2 : i1) {
                    if (i2 != null) {
                        if (i2[0] < minY) {
                            minY = i2[0];
                        }
                        if (i2[1] > maxY) {
                            maxY = i2[1];
                        }
                    }
                }
        }
        return minY;
    }

    public int getMaxY() {
        int minY = 65535;
        int maxY = -65535;
        for (int[][] i1 : hitBox) {
            if (i1 != null)
                for (int[] i2 : i1) {
                    if (i2 != null) {
                        if (i2[0] < minY) {
                            minY = i2[0];
                        }
                        if (i2[1] > maxY) {
                            maxY = i2[1];
                        }
                    }
                }
        }
        return maxY;
    }

    public int getMinZ() {
        return minZ;
    }

    public void setMinZ(int minZ) {
        this.minZ = minZ;
    }

    public int getMinX() {
        return minX;
    }

    public void setMinX(int minX) {
        this.minX = minX;
    }

    public boolean getCruising() {
        return cruising;
    }

    public void setCruising(boolean cruising) {
        this.cruising = cruising;
    }

    public boolean getSinking() {
        return sinking;
    }

    public void setSinking(boolean sinking) {
        this.sinking = sinking;
    }

    public boolean getDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public byte getCruiseDirection() {
        return cruiseDirection;
    }

    public void setCruiseDirection(byte cruiseDirection) {
        this.cruiseDirection = cruiseDirection;
    }

    public void setLastCruisUpdate(long update) {
        this.lastCruiseUpdate = update;
    }

    public long getLastCruiseUpdate() {
        return lastCruiseUpdate;
    }

    public long getLastBlockCheck() {
        return lastBlockCheck;
    }

    public void setLastBlockCheck(long update) {
        this.lastBlockCheck = update;
    }

    public int getLastDX() {
        return lastDX;
    }

    public void setLastDX(int dX) {
        this.lastDX = dX;
    }

    public int getLastDY() {
        return lastDY;
    }

    public void setLastDY(int dY) {
        this.lastDY = dY;
    }

    public int getLastDZ() {
        return lastDZ;
    }

    public void setLastDZ(int dZ) {
        this.lastDZ = dZ;
    }

    public boolean getPilotLocked() {
        return pilotLocked;
    }

    public void setPilotLocked(boolean pilotLocked) {
        this.pilotLocked = pilotLocked;
    }

    public HashMap<Player, Long> getMovedPlayers() {
        return movedPlayers;
    }

    public double getPilotLockedX() {
        return pilotLockedX;
    }

    public void setPilotLockedX(double pilotLockedX) {
        this.pilotLockedX = pilotLockedX;
    }

    public double getPilotLockedY() {
        return pilotLockedY;
    }

    public void setPilotLockedY(double pilotLockedY) {
        this.pilotLockedY = pilotLockedY;
    }

    public double getPilotLockedZ() {
        return pilotLockedZ;
    }

    public void setPilotLockedZ(double pilotLockedZ) {
        this.pilotLockedZ = pilotLockedZ;
    }

    public double getBurningFuel() {
        return burningFuel;
    }

    public void setBurningFuel(double burningFuel) {
        this.burningFuel = burningFuel;
    }

    public int getOrigBlockCount() {
        return origBlockCount;
    }

    public void setOrigBlockCount(int origBlockCount) {
        this.origBlockCount = origBlockCount;
    }

    public Player getNotificationPlayer() {
        return notificationPlayer;
    }

    public void setNotificationPlayer(Player notificationPlayer) {
        this.notificationPlayer = notificationPlayer;
    }

    public Player getCannonDirector() {
        return cannonDirector;
    }

    public void setCannonDirector(Player cannonDirector) {
        this.cannonDirector = cannonDirector;
    }

    public Player getAADirector() {
        return AADirector;
    }

    public void setAADirector(Player AADirector) {
        this.AADirector = AADirector;
    }

    public long getOrigPilotTime() {
        return origPilotTime;
    }

    public MovecraftLocation getMidPoint() {
        int maxDX=0;
        int maxDZ=0;
        int maxY=0;
        int minY=32767;
        for(int[][] i1 : hitBox) {
            maxDX++;
            if (i1!=null) {
                int indexZ=0;
                for(int[] i2 : i1) {
                    indexZ++;
                    if(i2!=null) {
                        if(i2[0]<minY) {
                            minY=i2[0];
                        }
                    }
                    if (i2!=null) {
                        if(i2[1]<maxY) {
                            maxY=i2[1];
                        }
                    }
                }
                if (indexZ>maxDZ) {
                    maxDZ=indexZ;
                }

            }
        }
        int midX=minX+(maxDX/2);
        int midY=(minY+maxY)/2;
        int midZ=minZ+(maxDZ/2);
        return new MovecraftLocation(midX, midY, midZ);
       /* return new MovecraftLocation(
                (getMaxX()+getMinX())/2,
                (getMaxY()+getMinY())/2,
                (getMaxZ()+getMinZ())/2);*/
    }

    public float getMeanMoveTime() {
        return meanMoveTime;
    }

    public void addMoveTime(float moveTime){
        meanMoveTime = (meanMoveTime*numMoves + moveTime)/(++numMoves);
    }

    public int getTickCooldown() {
        if(sinking)
            return type.getSinkRateTicks();
        double chestPenalty = 0;
        for(MovecraftLocation location : blockList){
            if(location.toBukkit(w).getBlock().getType()==Material.CHEST)
                chestPenalty++;
        }
        chestPenalty*=type.getChestPenalty();
        if(meanMoveTime==0)
            return type.getCruiseTickCooldown()+(int)chestPenalty;
        if(!cruising)
            return type.getTickCooldown()+(int)chestPenalty;
        if(type.getDynamicFlyBlockSpeedFactor()!=0){
            double count = 0;
            Material flyBlockMaterial = Material.getMaterial(type.getDynamicFlyBlock());
            for(MovecraftLocation location : blockList){
                if(location.toBukkit(w).getBlock().getType()==flyBlockMaterial)
                    count++;
            }
            return  Math.max((int)(type.getCruiseTickCooldown()* (1 - count /blockList.length) +chestPenalty),1);
        }

        if(type.getDynamicLagSpeedFactor()==0)
            return type.getCruiseTickCooldown()+(int)chestPenalty;
        //TODO: modify skip blocks by an equal proportion to this, than add another modifier based on dynamic speed factor
        return Math.max((int)(type.getCruiseTickCooldown()*meanMoveTime*20/type.getDynamicLagSpeedFactor() +chestPenalty),1);
    }

    /**
     * gets the speed of a craft in blocks per second.
     * @return the speed of the craft
     */
    public double getSpeed(){
        return 20*type.getCruiseSkipBlocks()/(double)getTickCooldown();
    }

    public long getLastRotateTime() {
        return lastRotateTime;
    }

    public void setLastRotateTime(long lastRotateTime) {
        this.lastRotateTime = lastRotateTime;
    }

    public int getWaterLine(){
        //TODO: Remove this temporary system in favor of passthrough blocks
        // Find the waterline from the surrounding terrain or from the static level in the craft type
        int waterLine = 0;
        int maxY = getMaxY();
        int minY = getMinY();
        int maxZ = getMaxZ();
        int minZ = getMinZ();
        int maxX = getMaxX();
        int minX = getMinX();
        if (type.getStaticWaterLevel() != 0) {
            return type.getStaticWaterLevel();
        }
        // figure out the water level by examining blocks next to the outer boundaries of the craft
        for (int posY = maxY + 1; posY >= minY - 1; posY--) {
            int numWater = 0;
            int numAir = 0;
            int posX;
            int posZ;
            posZ = minZ - 1;
            for (posX = minX - 1; posX <= maxX + 1; posX++) {
                int typeID = w.getBlockAt(posX, posY, posZ).getTypeId();
                if (typeID == 9)
                    numWater++;
                if (typeID == 0)
                    numAir++;
            }
            posZ = maxZ + 1;
            for (posX = minX - 1; posX <= maxX + 1; posX++) {
                int typeID = w.getBlockAt(posX, posY, posZ).getTypeId();
                if (typeID == 9)
                    numWater++;
                if (typeID == 0)
                    numAir++;
            }
            posX = minX - 1;
            for (posZ = minZ; posZ <= maxZ; posZ++) {
                int typeID = w.getBlockAt(posX, posY, posZ).getTypeId();
                if (typeID == 9)
                    numWater++;
                if (typeID == 0)
                    numAir++;
            }
            posX = maxX + 1;
            for (posZ = minZ; posZ <= maxZ; posZ++) {
                int typeID = w.getBlockAt(posX, posY, posZ).getTypeId();
                if (typeID == 9)
                    numWater++;
                if (typeID == 0)
                    numAir++;
            }
            if (numWater > numAir) {
                return posY;
            }
        }
        return waterLine;
    }
}