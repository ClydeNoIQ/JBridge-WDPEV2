package me.josscoder.jbridge.waterdogpe.command;

import dev.waterdog.waterdogpe.command.Command;
import dev.waterdog.waterdogpe.command.CommandSender;
import dev.waterdog.waterdogpe.command.CommandSettings;
import dev.waterdog.waterdogpe.logger.Color;
import dev.waterdog.waterdogpe.network.serverinfo.ServerInfo;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import me.josscoder.jbridge.JBridgeCore;
import me.josscoder.jbridge.service.ServiceInfo;

public class WhereAmICommand extends Command {

    public WhereAmICommand() {
        super("whereami", CommandSettings.builder()
                .setDescription("Provide information about the proxy you are on")
                .setAliases(new String[]{"connection"})
                .build()
        );
    }

    @Override
    public boolean onExecute(CommandSender sender, String alias, String[] args) {
        String proxyId = JBridgeCore.getInstance().getCurrentServiceInfo().getGroupAndId();

        if (!sender.isPlayer()) {
            sender.sendMessage(Color.GOLD + "You are connected to proxy " + proxyId);
            return true;
        }

        ProxiedPlayer player = (ProxiedPlayer) sender;

        String serverId = "?";
        ServerInfo serverInfo = player.getServerInfo();
        if (serverInfo != null) {
            serverId = serverInfo.getServerName();
        }

        ServiceInfo serviceInfo = JBridgeCore.getInstance()
                .getServiceHandler()
                .getService(serverId);

        String serviceId = serviceInfo != null ? serviceInfo.getGroupAndId() : serverId;

        sender.sendMessage(Color.GOLD + "You are connected to proxy " + proxyId +
                "\n" +
                "You are connected to server " + serviceId
        );
        return true;
    }
}
