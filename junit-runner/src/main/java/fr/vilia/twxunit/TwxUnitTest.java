//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package fr.vilia.twxunit;

import org.junit.runner.RunWith;

import java.net.MalformedURLException;

@RunWith(TwxUnitRunner.class)
public abstract class TwxUnitTest {
    private final String wsUrl;
    private final String appKey;
    private final boolean ignoreSslErrors;
    private final String thingName;
    private final String runAs;

    protected TwxUnitTest(String wsUrl, String appKey, boolean ignoreSslErrors, String thingName, String runAs) {
        this.wsUrl = wsUrl;
        this.appKey = appKey;
        this.ignoreSslErrors = ignoreSslErrors;
        this.thingName = thingName;
        this.runAs = runAs;
    }

    public String getWsUrl() {
        return wsUrl;
    }

    public String getAppKey() {
        return appKey;
    }

    public boolean isIgnoreSslErrors() {
        return ignoreSslErrors;
    }

    public String getThingName() {
        return thingName;
    }

    public String getRunAs() {
        return runAs;
    }
}
