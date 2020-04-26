package fr.vilia.twxunit;

import com.thingworx.entities.utils.ThingUtilities;
import com.thingworx.things.Thing;
import com.thingworx.types.InfoTable;
import com.thingworx.types.collections.ValueCollection;
import com.thingworx.types.primitives.InfoTablePrimitive;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class TestSuite {

    private final String name;
    private final List<TestSuite> suites = new ArrayList<>();
    private final List<TestDefinition> testCases = new ArrayList<>();
    private final TwxUnitExecutor executor; // TODO: Remove

    public TestSuite(TwxUnitExecutor executor, String thing, String parent, String runAsDefault, Set<String> parsed) throws TestingException {
        this.executor = executor;
        parsed.add(thing);
        this.name = thing;
        Thing t = ThingUtilities.findThing(thing);
        if (t == null) {
            throw new TestingException("Test suite " + thing + (parent == null ? "" : (", referenced in " + parent)) + " does not exist");
        }
        if (t.implementsShape("HasTestCases")) {
            parseAuto(t, runAsDefault, parsed);
        } else if (t.implementsShape("HasTestSuite")) {
            parseManual(t, runAsDefault, parsed);
        } else {
            throw new TestingException("Thing " + thing + (parent == null ? "" : (", referenced in " + parent)) + " is not a test suite");
        }
    }

    private void parseManual(Thing thing, String runAsDefault, Set<String> parsed) throws TestingException {
        InfoTable def;
        try {
            def = ((InfoTablePrimitive) thing.getPropertyValue("testSuite")).getValue();
        } catch (Exception e) {
            e.printStackTrace();
            throw new TestingException("Unable to read property testSuite on thing " + thing.getName() + ": " + e.getMessage(), e);
        }

        for (int i = 0; i < def.getLength(); ++i) {
            ValueCollection test = def.getRow(i);
            String testSuite = test.getStringValue("testSuite");
            String testCase = test.getStringValue("testCase");
            if ((testSuite == null || testSuite.isEmpty()) && (testCase == null || testCase.isEmpty())) {
                throw new TestingException("Invalid test suite configuration: both testCase and testSuite cannot be null or empty simultaneously");
            }

            String runAs = test.getStringValue("runAs");
            if (runAs == null || runAs.isEmpty()) {
                runAs = runAsDefault;
            }
            if (testCase == null || testCase.isEmpty()) {
                // Reference to another test suite
                if (parsed.contains(testSuite)) {
                    throw new TestingException("Detected an infinite loop on test suite " + testSuite + ", path: " + parsed);
                }
                suites.add(new TestSuite(executor, testSuite, thing.getName(), runAs, parsed));
            } else {
                // Reference to test case
                if (testSuite == null || testSuite.isEmpty()) {
                    testSuite = thing.getName();
                }
                testCases.add(new TestDefinition(testSuite, testCase, runAs));
            }
        }
    }

    private void parseAuto(Thing thing, String runAsDefault, Set<String> parsed) throws TestingException {
        InfoTable services;
        try {
            services = thing.GetServiceDefinitions(null, null, null);
        } catch (Exception e) {
            e.printStackTrace();
            throw new TestingException("Unable to get service definitions on thing " + thing.getName() + ": " + e.getMessage(), e);
        }

        for (int i = 0; i < services.getLength(); ++i) {
            ValueCollection service = services.getRow(i);
            String name = service.getStringValue("name");
            if (name.startsWith("Test")) {
                // Verify that this test has no parameters and no outputs
                if (((InfoTable) service.getValue("resultType")).getRow(0).getStringValue("baseType") != "NOTHING") {
                    throw new TestingException("Test case " + thing.getName() + "." + name + " must return NOTHING");
                } else if (((InfoTable) service.getValue("parameterDefinitions")).getLength() > 0) {
                    throw new TestingException("Test case " + thing.getName() + "." + name + " must not declare any inputs");
                }
                String description = service.getStringValue("description");
                testCases.add(new TestDefinition(thing.getName(), name, runAsDefault, description));
            }
        }
    }

    public String getName() {
        return name;
    }

    public List<TestSuite> getSuites() {
        return suites;
    }

    public List<TestDefinition> getTestCases() {
        return testCases;
    }
}
