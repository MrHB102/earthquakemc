package dev.earthquakemc;

import io.papermc.paper.event.connection.configuration.PlayerConnectionInitialConfigureEvent;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

/**
 * Horizon's server-side extras on Paper/Spigot: {@code horizon:quiet_commands}, and a warning when the server's
 * Minecraft version is not Horizon's and nothing translates between them.
 */
public final class EarthquakePlugin extends JavaPlugin implements PluginMessageListener, Listener {
    /** Protocol of Minecraft 26.3, the version Horizon speaks (horizon-core's PROTOCOL_VERSION). */
    private static final int HORIZON_PROTOCOL = 777;
    private static final int PENDING = 4096, PER_TICK = 64;
    // Horizon's client refuses these too: they carry a chat message, which needs the player's signature.
    private static final Set<String> SIGNED = Set.of("say", "msg", "tell", "w", "me", "teammsg", "tm");

    private final Map<UUID, ArrayDeque<String>> queues = new HashMap<>();

    @Override
    public void onEnable() {
        getServer().getMessenger().registerIncomingPluginChannel(this, QuietBatch.CHANNEL, this);
        getServer().getMessenger().registerOutgoingPluginChannel(this, QuietBatch.CHANNEL);
        getServer().getScheduler().runTaskTimer(this, this::runQueued, 1, 1);
        try {
            Class.forName("io.papermc.paper.event.connection.configuration.PlayerConnectionInitialConfigureEvent");
            getServer().getPluginManager().registerEvents(this, this);
        } catch (ClassNotFoundException e) {
            // shortcut: on Spigot the announcement can't be sent in the configuration phase; Horizon then sends chat commands.
            getLogger().info("Not Paper: quiet commands are not announced, Horizon falls back to chat commands.");
        }
        int protocol = Bukkit.getUnsafe().getProtocolVersion();
        if (protocol != HORIZON_PROTOCOL && Bukkit.getPluginManager().getPlugin("ViaVersion") == null)
            getLogger().warning("Server protocol " + protocol + " is not Horizon's " + HORIZON_PROTOCOL
                    + " and ViaVersion is missing: Horizon cannot join. Install ViaVersion (newer client) or ViaBackwards (older).");
    }

    /** The announcement: an empty payload in the configuration phase, which is how Horizon learns to send batches. */
    @EventHandler
    public void onConfigure(PlayerConnectionInitialConfigureEvent event) {
        event.getConnection().sendPluginMessage(this, QuietBatch.CHANNEL, new byte[0]);
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        QuietBatch batch;
        try {
            batch = QuietBatch.decode(message);
        } catch (RuntimeException e) {
            getLogger().warning(player.getName() + " sent quiet commands that were refused: " + e.getMessage());
            return;
        }
        ArrayDeque<String> queue = queues.computeIfAbsent(player.getUniqueId(), id -> new ArrayDeque<>());
        int room = Math.max(0, PENDING - queue.size());
        batch.lines().stream().limit(room).forEach(queue::add);
        getLogger().info(player.getName() + " queued " + Math.min(room, batch.lines().size()) + " quiet command(s) from mod "
                + batch.modId().replaceAll("\\p{Cntrl}", "?") + (room < batch.lines().size() ? ", rest dropped" : ""));
    }

    /** PER_TICK lines a player a tick, so a mod can't flood the server. */
    private void runQueued() {
        for (Iterator<Map.Entry<UUID, ArrayDeque<String>>> it = queues.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, ArrayDeque<String>> entry = it.next();
            Player player = Bukkit.getPlayer(entry.getKey());
            ArrayDeque<String> queue = entry.getValue();
            if (player == null || queue.isEmpty()) {
                it.remove();
                continue;
            }
            for (int i = 0; i < PER_TICK && !queue.isEmpty(); i++) {
                String line = queue.poll().stripLeading().replaceFirst("^/+", "");
                if (SIGNED.contains(line.split(" ", 2)[0].toLowerCase())) continue;
                try {
                    // shortcut: runs as the player at their own permissions, but Bukkit has no way to mute the feedback.
                    player.performCommand(line);
                } catch (RuntimeException e) {
                    getLogger().log(Level.WARNING, "Quiet command failed for " + player.getName(), e);
                }
            }
        }
    }
}
