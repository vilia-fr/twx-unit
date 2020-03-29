package fr.vilia.twxunit;

import com.thingworx.communications.client.ClientConfigurator;
import com.thingworx.communications.client.ConnectedThingClient;

import java.io.Closeable;
import java.io.IOException;

public class TwxClient extends ConnectedThingClient implements Closeable, AutoCloseable {

    public TwxClient(String wsUrl, String appKey, boolean ignoreSslErrors) throws Exception {
        super(getConfig(wsUrl, appKey, ignoreSslErrors));
        start();
        waitForConnection(30000);
    }

    private static ClientConfigurator getConfig(String wsUrl, String appKey, boolean ignoreSslErrors) {
        ClientConfigurator conf = new ClientConfigurator();
        conf.setUri(wsUrl);
        conf.setAppKey(appKey);
        conf.ignoreSSLErrors(ignoreSslErrors);
        return conf;
    }

    @Override
    public void close() throws IOException {
        try {
            shutdown();
        } catch (Exception e) {
            throw new IOException("Unable to shutdown ThingWorx client", e);
        }
    }
}
