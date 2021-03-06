package net.countercraft.movecraft.mapUpdater.update;

import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.api.MovecraftLocation;
import net.countercraft.movecraft.api.Rotation;
import net.countercraft.movecraft.api.craft.Craft;
import net.countercraft.movecraft.api.events.SignTranslateEvent;
import net.countercraft.movecraft.config.Settings;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;


public class CraftRotateCommand extends UpdateCommand{
    private Logger logger = Movecraft.getInstance().getLogger();
    @NotNull private final Craft craft;
    @NotNull private final Rotation rotation;
    @NotNull final MovecraftLocation originLocation;

    public CraftRotateCommand(@NotNull final Craft craft,@NotNull final MovecraftLocation originLocation, @NotNull final Rotation rotation){
        this.craft = craft;
        this.rotation = rotation;
        this.originLocation = originLocation;
    }

    @Override
    public void doUpdate() {
        long time = System.nanoTime();
        Movecraft.getInstance().getWorldHandler().rotateCraft(craft,originLocation,rotation);
        for(MovecraftLocation location : craft.getBlockList()){
            Block block = location.toBukkit(craft.getW()).getBlock();
            if(block.getType() == Material.WALL_SIGN || block.getType() == Material.SIGN_POST){
                Sign sign = (Sign) block.getState();
                Bukkit.getServer().getPluginManager().callEvent(new SignTranslateEvent(block, craft, sign.getLines()));
                sign.update();
            }
        }
        time = System.nanoTime() - time;
        if(Settings.Debug)
            logger.info("Total time: " + (time / 1e9) + " seconds. Rotating with cooldown of " + craft.getTickCooldown() + ". Speed of: " + String.format("%.2f", craft.getSpeed()));
        craft.addMoveTime(time/1e9f);
    }



    @NotNull
    public Craft getCraft(){
        return craft;
    }
}
