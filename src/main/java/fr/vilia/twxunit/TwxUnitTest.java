package fr.vilia.twxunit;

import org.junit.runner.RunWith;

import java.net.MalformedURLException;
import java.net.URL;

@RunWith(TwxUnitRunner.class)
public abstract class TwxUnitTest {

    private final TwxSuite config;
    private final String thingName;

    protected TwxUnitTest() {
        Class<? extends TwxUnitTest> clazz = getClass();
        if (clazz.isAnnotationPresent(TwxSuite.class)) {
            config = clazz.getAnnotation(TwxSuite.class);
            thingName = getClass().getSimpleName();
        } else {
            throw new IllegalArgumentException("You have to use TwxSuite annotation to configure TwxUnit test suite");
        }
    }

    URL getUrl(String service) throws MalformedURLException {
        return new URL(config.url() + "/Things/" + thingName + "/Services/" + service);
    }

    URL getUrl() throws MalformedURLException {
        return new URL(config.url() + "/Things/" + thingName + "/ServiceDefinitions");
    }

    String getAppKey() {
        return config.introspectionAppKey();
    }

}
