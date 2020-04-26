package fr.vilia.twxunit;

import com.thingworx.entities.utils.ThingUtilities;
import com.thingworx.metadata.ServiceDefinition;
import com.thingworx.things.Thing;
import com.thingworx.types.collections.ValueCollection;
import com.thingworx.types.primitives.InfoTablePrimitive;
import com.thingworx.types.primitives.StringPrimitive;

import java.util.Set;
import java.util.TreeSet;

public class TestDefinition {

    private final String testSuite;
    private final String testCase;
    private final String runAs;
    private final String description;
    private final Set<String> remoteThings;

    public TestDefinition(String testSuite, String testCase, String runAs, String description) throws TestingException {
        this.testSuite = testSuite;
        this.testCase = testCase;
        this.runAs = runAs;
        this.description = description;
        this.remoteThings = parseRemoteThings(testSuite);
    }

    public TestDefinition(String testSuite, String testCase, String runAs) throws TestingException {
        this.testSuite = testSuite;
        this.testCase = testCase;
        this.runAs = runAs;
        this.remoteThings = parseRemoteThings(testSuite);
        this.description = parseDescription(testSuite, testCase);
    }

    private static Set<String> parseRemoteThings(String testSuite) throws TestingException {
        Set<String> res = new TreeSet<>();
        Thing t = getThing(testSuite);
        if (t.implementsShape("HasRemoteThings")) {
            try {
                InfoTablePrimitive remoteThings = (InfoTablePrimitive) t.getPropertyValue("remoteThings");
                for (ValueCollection vc: remoteThings.getValue().getRows()) {
                    res.add(vc.getStringValue("thingName"));
                }
            } catch (Exception e) {
                throw new TestingException("Cannot get the list of remote things for test suite " + testSuite, e);
            }
        }
        return res;
    }

    private static String parseDescription(String testSuite, String testCase) throws TestingException {
        Thing t = getThing(testSuite);

        ServiceDefinition s = t.getEffectiveServiceDefinition(testCase);
        if (s == null) {
            throw new TestingException("Test case " + testCase + " does not exist on test suite " + testSuite);
        }

        return s.getDescription();
    }

    private static Thing getThing(String testSuite) throws TestingException {
        Thing t = ThingUtilities.findThing(testSuite);
        if (t == null) {
            throw new TestingException("Test suite " + testSuite + " does not exist");
        }
        return t;
    }

    public synchronized ValueCollection toValueCollection() {
        ValueCollection vc = new ValueCollection();
        try {
            vc.setValue("testSuite", new StringPrimitive(testSuite));
            vc.setValue("testCase", new StringPrimitive(testCase));
            vc.setValue("runAs", new StringPrimitive(runAs));
            vc.setValue("remoteThings", new StringPrimitive(remoteThings == null ? "" : remoteThings.toString()));
        } catch (Exception e) {
            // We won't ever be here
            e.printStackTrace();
        }
        return vc;
    }

    public String getTestSuite() {
        return testSuite;
    }

    public String getTestCase() {
        return testCase;
    }

    public String getRunAs() {
        return runAs;
    }

    public String getDescription() {
        return description;
    }

    public Set<String> getRemoteThings() {
        return remoteThings;
    }
}
