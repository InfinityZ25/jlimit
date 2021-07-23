package us.jcedeno.jlimit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;

@Mod("jlimit")
public class JLimitMod {
    private static final Logger LOGGER = LogManager.getLogger();

    public JLimitMod() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onServerStarting(FMLServerStartingEvent event) {
        // do something when the server starts
        LOGGER.info("HELLO from server starting");
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent e) {
        var player = e.getPlayer();

        LOGGER.info(player.getUUID() + " " + player.getName().getString() + " joined the server");
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedOutEvent e) {
        var player = e.getPlayer();
        LOGGER.info(player.getUUID() + " " + player.getName().getString() + " left the server");
    }

}