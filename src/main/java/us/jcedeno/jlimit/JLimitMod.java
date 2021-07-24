package us.jcedeno.jlimit;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppingEvent;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

@Mod("jlimit")
public class JLimitMod {
    // Housekeeping stuff
    private static final Logger LOGGER = LogManager.getLogger();
    private static Gson gson = new GsonBuilder().setPrettyPrinting().create();
    // In ram copy of the jlimit.json file. Use this one for code.
    private HashMap<UUID, PlayerMask> playerMasks = new HashMap<UUID, PlayerMask>();
    // JsonFile containing the player masks
    private JsonFile jsonFile;
    // Thread to run the logic on.
    private static Thread saveThread;
    // Saving interval in seconds
    private int interval = 5;
    // Current tick, ignore
    private static int tick = 0;
    // Time in seconds to kick players
    private static long TIME_LIMIT = 60L; // 1 hour

    public JLimitMod() {
        // Register the events of this mod into the bus
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onStart(FMLServerStartingEvent event) {
        try {
            this.jsonFile = new JsonFile("jlimit.json");
            // Load back the data if it exists
            if (jsonFile.getJsonObject() != null) {
                playerMasks = new Gson().fromJson(jsonFile.getJsonObject(), new TypeToken<HashMap<UUID, PlayerMask>>() {
                }.getType());
            }
            saveThread = new Thread(() -> {
                while (true) {
                    tick++;

                    LOGGER.info("Thread looped");

                    // Run all our game logic
                    timeLoop();

                    // Save data every that often
                    if (tick % interval == 0) {
                        saveInfoToFlatFile();
                    }
                    // Make the thread sleep every x seconds
                    try {
                        TimeUnit.SECONDS.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
            });

            // Start the save thread.

            LOGGER.info("Thread STARTED");
            saveThread.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Events starts */

    @SubscribeEvent
    public void onStopping(FMLServerStoppingEvent e) {
        try {
            // Join the thread to halt it
            saveThread.join();
            // Save the info one last time to a flat file
            saveInfoToFlatFile();
            // Set thread to null if it's a reload or sometthing weird
            saveThread = null;
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }

    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        var player = event.getPlayer();

        var mask = playerMasks.putIfAbsent(player.getUUID(), PlayerMask.init());

        if (mask != null && mask.getCannotPlayUntil() > System.currentTimeMillis()) {
            // This is probably not the right way to do this. But it works.
            ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(player.getUUID()).connection
                    .disconnect(new StringTextComponent("You can't play until " + mask.getCannotPlayUntil()));
        }

        LOGGER.info(player.getUUID() + " " + player.getName().getString() + " joined the server");
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        var player = event.getPlayer();
        LOGGER.info(player.getUUID() + " " + player.getName().getString() + " left the server");
    }

    /** Events ends */

    /** Helper Methods starts */

    /**
     * Method that saves the time played info into the disk
     */
    public void saveInfoToFlatFile() {
        if (playerMasks == null) {
            return;
        }

        jsonFile.setJsonObject(gson.toJsonTree(playerMasks).getAsJsonObject());
        try {
            jsonFile.save();// Save all masks to file
            LOGGER.info("saved json");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Method that runs the logic of the game.
     */
    public void timeLoop() {
        LOGGER.info("Time Loop called");
        // Add time to all online masks
        ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers().stream().forEach(players -> {
            // Obtain the player's mask
            var mask = playerMasks.get(players.getUUID());
            // If null return
            if (mask == null)
                return;
            // Add a second to their time played
            mask.addSecond();
            // If the player has played for more than the time limit, kick the player.
            if (mask.getTimePlayedToday() > TIME_LIMIT) {
                // Kick them
                players.connection.disconnect(
                        new StringTextComponent("You have played for more than " + TIME_LIMIT + " seconds!"));
                // Set their cannot play until time to now + 24 hours
                mask.setCannotPlayUntil(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1));
                mask.resetTime();
            }

        });
    }

    /** Helper Methods ends */

}