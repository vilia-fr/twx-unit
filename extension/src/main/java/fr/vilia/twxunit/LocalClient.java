package fr.vilia.twxunit;

import com.thingworx.communications.client.ClientConfigurator;
import com.thingworx.communications.client.ConnectedThingClient;
import com.thingworx.communications.client.things.VirtualThing;
import com.thingworx.types.collections.AspectCollection;

import java.io.Closeable;
import java.io.IOException;
import java.util.Set;

import static com.thingworx.types.BaseTypes.STRING;

public class LocalClient implements AutoCloseable, Closeable {

    private final ConnectedThingClient client;

    public LocalClient(String url, String appKey) throws Exception {
        client = new ConnectedThingClient(getConfiguration(url, appKey));
        client.start();
    }

    public void bind(Set<String> thingNames) throws Exception {
        for (String thingName: thingNames) {
            VirtualThing virtualThing = client.getThing(thingName);
            if (virtualThing == null) {
                virtualThing = new VirtualThing(thingName, null, client);
                virtualThing.defineProperty("rp", "", STRING, new AspectCollection());
                client.bindThing(virtualThing);
            }
        }
    }

    @Override
    public void close() throws IOException {
        try {
            client.shutdown();
        } catch (Exception e) {
            throw new IOException("Cannot shutdown LocalClient connection", e);
        }
    }

    private static ClientConfigurator getConfiguration(String url, String appKey) {
        ClientConfigurator config = new ClientConfigurator();
        config.setUri(url);
        config.setAppKey(appKey);
        config.ignoreSSLErrors(true); // Localhost connection, we don't care
        return config;
    }
}
