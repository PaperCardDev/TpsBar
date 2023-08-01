package cn.paper_card.tps_bar;

import com.destroystokyo.paper.event.server.ServerTickEndEvent;
import com.destroystokyo.paper.event.server.ServerTickStartEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("unused")
public final class TpsBar extends JavaPlugin {

    private double mspt = 0;

    private final @NotNull HashMap<UUID, BossBar> playerTpsBar = new HashMap<>();


    @Override
    public void onEnable() {

        this.getServer().getAsyncScheduler().runAtFixedRate(this, task -> {

            final double tps = this.mspt > 50 ? (1000 / this.mspt) : 20;

            synchronized (this.playerTpsBar) {
                for (final UUID uuid : this.playerTpsBar.keySet()) {
                    final BossBar bar = this.playerTpsBar.get(uuid);
                    if (bar == null) continue;

                    final Player player = this.getServer().getPlayer(uuid);
                    if (player == null || !player.isOnline()) continue;

                    bar.setTitle("MSPT: %.2f    TPS: %.2f   Ping: %dms".formatted(this.mspt, tps, player.getPing()));
                    bar.setProgress(this.mspt / 50);
                }
            }

        }, 1, 1, TimeUnit.SECONDS);


        this.getServer().getPluginManager().registerEvents(new Listener() {

            private long startTime = -1;
            private int ticks = 0;
            private long time = 0;

            @EventHandler
            public void on1(@NotNull ServerTickStartEvent event) {
                this.startTime = System.currentTimeMillis();
            }

            @EventHandler
            public void on2(@NotNull ServerTickEndEvent event) {
                final long time = System.currentTimeMillis() - this.startTime;
                this.time += time;
                ++this.ticks;
                if (this.ticks == 20) {
                    TpsBar.this.mspt = (double) this.time / this.ticks;
                    this.ticks = 0;
                    this.time = 0;
                }
            }

        }, this);

        this.getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void on1(@NotNull PlayerJoinEvent event) {
                synchronized (playerTpsBar) {
                    final BossBar bossBar = playerTpsBar.get(event.getPlayer().getUniqueId());
                    if (bossBar != null) {
                        bossBar.addPlayer(event.getPlayer());
                    }
                }
            }

        }, this);

        final PluginCommand command = this.getCommand("tps-bar");
        assert command != null;
        command.setExecutor((commandSender, command1, s, strings) -> {
            if (!(commandSender instanceof final Player player)) {
                commandSender.sendMessage(Component.text("该命令只能由玩家来执行"));
                return true;
            }

            final UUID id = player.getUniqueId();

            synchronized (this.playerTpsBar) {
                BossBar bossBar = this.playerTpsBar.get(id);
                if (bossBar == null) {
                    bossBar = getServer().createBossBar(null, BarColor.GREEN, BarStyle.SEGMENTED_20);
                    bossBar.addPlayer(player);
                    this.playerTpsBar.put(id, bossBar);

                    commandSender.sendMessage(Component.text("已开启你的TpsBar"));

                } else {
                    bossBar.removeAll();
                    this.playerTpsBar.remove(id);

                    commandSender.sendMessage(Component.text("已关闭你的TpsBar"));
                }
            }

            return true;
        });
    }

}
