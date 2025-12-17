package me.josscoder.jbridge.waterdogpe;

import dev.waterdog.proxy.command.CommandMap;
import dev.waterdog.proxy.event.EventManager;
import dev.waterdog.proxy.event.defaults.PreTransferEvent;
import dev.waterdog.proxy.event.defaults.ProxyPingEvent;
import dev.waterdog.proxy.event.defaults.ProxyQueryEvent;
import dev.waterdog.proxy.logger.Color;
import dev.waterdog.proxy.network.serverinfo.ServerInfo;
import dev.waterdog.proxy.player.ProxiedPlayer;
import dev.waterdog.proxy.plugin.Plugin;
import dev.waterdog.proxy.utils.config.Configuration;
import lombok.Getter;
import me.josscoder.jbridge.JBridgeCore;
import me.josscoder.jbridge.service.ServiceInfo;
import me.josscoder.jbridge.waterdogpe.command.ServerListCommand;
import me.josscoder.jbridge.waterdogpe.command.WhereAmICommand;
import me.josscoder.jbridge.waterdogpe.task.ServicePongTask;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class JBridgeWaterdogPE extends Plugin {

    @Getter
    private static JBridgeWaterdogPE instance;

    @Override
    public void onEnable() {
        instance = this;

        loadConfig();
        Configuration config = getConfig();

        JBridgeCore jBridgeCore = new JBridgeCore();
        jBridgeCore.boot(
                config.getString("redis.hostname"),
                config.getInt("redis.port"),
                config.getString("redis.password"),
                config.getBoolean("debug"),
                new WaterdogPELogger()
        );

        if (!config.exists("service.id")) {
            config.set("service.id", UUID.randomUUID().toString().substring(0, 8));
            config.save();
        }

        ServiceInfo serviceInfo = new ServiceInfo(
                config.getString("service.id"),
                "",
                config.getString("service.group", "proxy"),
                config.getString("service.region", "us"),
                config.getString("service.branch", "dev"),
                -1
        );

        jBridgeCore.setCurrentServiceInfo(serviceInfo);

        registerCommands();
        registerEvents();

        int interval = config.getInt("service.handling-interval", 5);

        // WaterdogPE v2 scheduler
        getProxy().getScheduler().scheduleRepeating(
                this,
                new ServicePongTask(),
                interval,
                interval,
                TimeUnit.SECONDS
        );
    }

    private void registerCommands() {
        CommandMap map = getProxy().getCommandMap();
        map.unregisterCommand("wdlist");
        map.registerCommand(new WhereAmICommand());
        map.registerCommand(new ServerListCommand());
    }

    private void registerEvents() {
        EventManager manager = getProxy().getEventManager();
        manager.subscribe(ProxyPingEvent.class, this::onPing);
        manager.subscribe(ProxyQueryEvent.class, this::onQuery);
        manager.subscribe(PreTransferEvent.class, this::onTransfer);
    }

    private void onPing(ProxyPingEvent event) {
        event.setMaximumPlayerCount(
                JBridgeCore.getInstance()
                        .getServiceHandler()
                        .getMaxPlayers()
        );
    }

    private void onQuery(ProxyQueryEvent event) {
        event.setMaximumPlayerCount(
                JBridgeCore.getInstance()
                        .getServiceHandler()
                        .getMaxPlayers()
        );
    }

    private void onTransfer(PreTransferEvent event) {
        ProxiedPlayer player = event.getPlayer();
        ServerInfo targetServer = event.getTargetServer();

        if (player.getServerInfo() == null ||
                targetServer == null ||
                player.getServerInfo().getServerName()
                        .equalsIgnoreCase(targetServer.getServerName())
        ) {
            return;
        }

        player.sendMessage(
                Color.GRAY + "Connecting you to " + targetServer.getServerName()
        );
    }

    @Override
    public void onDisable() {
        JBridgeCore.getInstance().shutdown();
    }
}
