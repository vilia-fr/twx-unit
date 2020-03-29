package fr.vilia.twxunit;

import com.thingworx.entities.utils.ThingUtilities;
import com.thingworx.metadata.ServiceDefinition;
import com.thingworx.things.Thing;
import com.thingworx.types.collections.ValueCollection;
import com.thingworx.types.primitives.StringPrimitive;

public class TestDefinition {

    private final String testSuite;
    private final String testCase;
    private final String runAs;
    private final String description;

    public TestDefinition(String testSuite, String testCase, String runAs, String description) {
        this.testSuite = testSuite;
        this.testCase = testCase;
        this.runAs = runAs;
        this.description = description;
    }

    public TestDefinition(String testSuite, String testCase, String runAs) throws TestingException {
        this.testSuite = testSuite;
        this.testCase = testCase;
        this.runAs = runAs;
        this.description = parseDescription(testSuite, testCase);
    }

    private static String parseDescription(String testSuite, String testCase) throws TestingException {
        Thing t = ThingUtilities.findThing(testSuite);
        if (t == null) {
            throw new TestingException("Test suite " + testSuite + " does not exist");
        }

        ServiceDefinition s = t.getEffectiveServiceDefinition(testCase);
        if (s == null) {
            throw new TestingException("Test case " + testCase + " does not exist on test suite " + testSuite);
        }

        return s.getDescription();
    }

    public synchronized ValueCollection toValueCollection() {
        ValueCollection vc = new ValueCollection();
        try {
            vc.setValue("testSuite", new StringPrimitive(testSuite));
            vc.setValue("testCase", new StringPrimitive(testCase));
            vc.setValue("runAs", new StringPrimitive(runAs));
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
}
