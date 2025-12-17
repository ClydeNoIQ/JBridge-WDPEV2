package me.josscoder.jbridge.waterdogpe.task;

import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.network.serverinfo.BedrockServerInfo;
import dev.waterdog.waterdogpe.network.serverinfo.ServerInfo;
import me.josscoder.jbridge.JBridgeCore;
import me.josscoder.jbridge.service.ServiceInfo;
import me.josscoder.jbridge.waterdogpe.JBridgeWaterdogPE;
import org.apache.logging.log4j.Logger;

import java.net.InetSocketAddress;
import java.util.Map;

public class ServicePongTask implements Runnable {

    @Override
    public void run() {
        ProxyServer proxy = ProxyServer.getInstance();
        Logger logger = JBridgeWaterdogPE.getInstance().getLogger();

        Map<String, ServiceInfo> cacheServers = JBridgeCore.getInstance()
                .getServiceHandler()
                .getServiceInfoMapCache();

        // Clone to avoid concurrent modification issues
        proxy.getServers().values().forEach(bedrockServer -> {

            String serverName = bedrockServer.getServerName();

            if (cacheServers.containsKey(serverName)) {
                ServiceInfo service = cacheServers.get(serverName);

                String currentAddress =
                        bedrockServer.getAddress().getHostString() + ":" +
                        bedrockServer.getAddress().getPort();

                if (!service.getAddress().equalsIgnoreCase(currentAddress)) {
                    bedrockServer.getPlayers().forEach(player ->
                            player.disconnect("Server IP changed while online, duplicate server?")
                    );

                    proxy.removeServerInfo(serverName);
                    registerService(proxy, logger, service, AddType.UPDATE_ADDRESS);
                }
            } else {
                bedrockServer.getPlayers().forEach(player ->
                        player.disconnect("Server timed out, broken connection?")
                );

                proxy.removeServerInfo(serverName);
                logger.info("Removed {} due to timeout...", serverName);
            }
        });

        // Register missing services
        cacheServers.values().stream()
                .filter(service -> proxy.getServerInfo(service.getShortId()) == null)
                .forEach(service ->
                        registerService(proxy, logger, service, AddType.NORMAL)
                );
    }

    private enum AddType {
        NORMAL,
        UPDATE_ADDRESS
    }

    private void registerService(
            ProxyServer proxy,
            Logger logger,
            ServiceInfo service,
            AddType addType
    ) {
        String[] addressSplit = service.getAddress().split(":");
        InetSocketAddress socketAddress = new InetSocketAddress(
                addressSplit[0],
                Integer.parseInt(addressSplit[1])
        );

        ServerInfo serverInfo = new BedrockServerInfo(
                service.getShortId(),
                socketAddress,
                socketAddress
        );

        boolean registered = proxy.registerServerInfo(serverInfo);

        if (registered) {
            if (addType == AddType.NORMAL) {
                logger.info(
                        "Added {} ({}:{})",
                        service.getRegionGroupAndShortId(),
                        addressSplit[0],
                        addressSplit[1]
                );
            } else {
                logger.warn(
                        "Server IP for \"{}\" updated!",
                        service.getRegionGroupAndShortId()
                );
            }
        } else {
            logger.warn(
                    "Could not add server {} because it already exists",
                    service.getRegionGroupAndShortId()
            );
        }
    }
}
