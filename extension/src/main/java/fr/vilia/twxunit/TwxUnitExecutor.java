package fr.vilia.twxunit;

import ch.qos.logback.classic.Logger;
import com.thingworx.data.util.InfoTableInstanceFactory;
import com.thingworx.entities.utils.EntityUtilities;
import com.thingworx.entities.utils.ThingUtilities;
import com.thingworx.logging.LogUtilities;
import com.thingworx.metadata.annotations.*;
import com.thingworx.persistence.TransactionFactory;
import com.thingworx.relationships.RelationshipTypes;
import com.thingworx.security.applicationkeys.ApplicationKey;
import com.thingworx.security.context.SecurityContext;
import com.thingworx.things.Thing;
import com.thingworx.things.connected.RemoteThing;
import com.thingworx.types.InfoTable;
import com.thingworx.types.collections.ValueCollection;
import com.thingworx.types.primitives.BooleanPrimitive;
import com.thingworx.types.primitives.InfoTablePrimitive;
import com.thingworx.types.primitives.IntegerPrimitive;
import com.thingworx.webservices.context.ThreadLocalContext;

import java.io.IOException;
import java.util.*;

@ThingworxConfigurationTableDefinitions(
        tables = {@ThingworxConfigurationTableDefinition(
                name = "LocalClientConfiguration",
                dataShape = @ThingworxDataShapeDefinition(fields = {
                        @ThingworxFieldDefinition(name = "url", baseType = "STRING"),
                        @ThingworxFieldDefinition(name = "appKeyName", baseType = "STRING")
                })
        )}
)
@ThingworxPropertyDefinitions(properties = {
    @ThingworxPropertyDefinition(name = "isExecuting", baseType = "BOOLEAN"),
    @ThingworxPropertyDefinition(name = "countRemaining", baseType = "INTEGER"),
    @ThingworxPropertyDefinition(name = "countExecuted", baseType = "INTEGER"),
    @ThingworxPropertyDefinition(name = "countTotal", baseType = "INTEGER"),
    @ThingworxPropertyDefinition(name = "execution", baseType = "INFOTABLE", aspects = {"dataShape:TestExecution"})
})
public class TwxUnitExecutor extends RemoteThing {

    private static final Logger LOG = LogUtilities.getInstance().getApplicationLogger(TwxUnitExecutor.class);
    private LinkedList<TestExecution> executing = new LinkedList<TestExecution>();
    private boolean isAborted = false;

    private InfoTable launchExecution(String testSuite, String defaultRunAs, boolean async) throws Exception {
        executing = new LinkedList<TestExecution>(getFlatExecution(testSuite, defaultRunAs));
        updateProperties();

        if (async) {
            executeAsync();
        } else {
            executeSync();
        }

        InfoTable result = InfoTableInstanceFactory.createInfoTableFromDataShape("TestExecution");
        for (TestExecution t: executing) {
            result.addRow(t.toValueCollection());
        }
        return result;
    }

    private void updateProperties() {
        try {
            int countRemaining = 0;
            InfoTable it = InfoTableInstanceFactory.createInfoTableFromDataShape("TestExecution");
            synchronized (executing) {
                for (TestExecution te : executing) {
                    if (te.getState().equals(ExecutionState.Scheduled) || te.getState().equals(ExecutionState.Executing)) {
                        countRemaining++;
                    }
                    it.addRow(te.toValueCollection());
                }
                setPropertyValue("isExecuting", new BooleanPrimitive(countRemaining > 0));
                setPropertyValue("countRemaining", new IntegerPrimitive((Number) countRemaining));
                setPropertyValue("countExecuted", new IntegerPrimitive((Number) (executing.size() - countRemaining)));
                setPropertyValue("countTotal", new IntegerPrimitive((Number) executing.size()));
                setPropertyValue("execution", new InfoTablePrimitive(it));
            }
        } catch (Throwable t) {
            t.printStackTrace();
            LOG.error("Error while updating status properties: " + t.getMessage());
        }
    }

    private void executeAsync() {
        final SecurityContext parentContext = ThreadLocalContext.getSecurityContext();
        new Thread(new Runnable() {
            @Override
            public void run() {
                ThreadLocalContext.setSecurityContext(parentContext);   // Inherit security context from parent thread
                executeSync();
            }
        }).start();
    }

    // TODO: Verify that we don't run beyond 30 seconds (execute sync after sync)
    private void executeSync() {
        TestExecution te;
        LocalClient localClient = null;
        Set<String> testSuites = new LinkedHashSet<String>();
        while ((te = nextExecutable()) != null) {
            synchronized (executing) {
                if (isAborted) {
                    isAborted = false;
                    for (TestExecution remaining: executing) {
                        if (remaining.getState().equals(ExecutionState.Scheduled) || remaining.getState().equals(ExecutionState.Executing)) {
                            remaining.abort(false, "Abort requested");
                        }
                    }
                    if (localClient != null) {
                        try {
                            localClient.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                            te.abort(false, "Unexpected error: " + e.getMessage());
                        }
                    }
                    updateProperties();
                    return;
                }
            }
            try {
                String suite = te.getTestSuite();
                if (!testSuites.contains(suite)) {
                    // Visiting this test suite for the first time
                    try {
                        executeOther(suite, "Before");
                    } catch(Throwable t) {
                        t.printStackTrace();
                        te.abort(false, "Error while executing Before for test suite " + suite + ": " + t.getMessage());
                    } finally {
                        testSuites.add(suite);
                    }
                }

                // Do not re-execute aborted or completed test suites
                if (te.getState().equals(ExecutionState.Scheduled)) {
                    TransactionFactory.beginTransactionRequired();
                    try {
                        Set<String> remoteThings = te.getRemoteThings();
                        if (remoteThings != null && !remoteThings.isEmpty()) {
                            if (localClient == null) {
                                localClient = new LocalClient(getLocalUrl(), getLocalAppKey());
                            }
                            localClient.bind(remoteThings);
                        }
                        te.execute();
                        updateProperties();
                    } finally {
                        try {
                            TransactionFactory.endTransactionRequired();
                        } catch (Exception e) {
                            e.printStackTrace();
                            LOG.warn("Unable to commit transaction while executing " + te.getId() + ": " + e.getMessage());
                        }
                    }
                }

            } catch (Throwable t) {
                t.printStackTrace();
                te.abort(false, "Unexpected error: " + t.getMessage());
            }
        }

        // Finalize by executing After for all test suites in reverse order
        String[] testSuitesArr = testSuites.toArray(new String[]{});
        for (int i = testSuitesArr.length - 1; i >=0; --i) {
            String suite = testSuitesArr[i];
            try {
                executeOther(suite, "After");
            } catch(Throwable t) {
                t.printStackTrace();
                LOG.error("Error while executing After for test suite " + suite + ": " + t.getMessage());
            }
        }

        if (localClient != null) {
            try {
                localClient.close();
            } catch (IOException e) {
                e.printStackTrace();
                LOG.error("Error while closing local client: " + e.getMessage());
            }
        }
    }

    private void executeOther(final String testSuite, final String service) throws Exception {
        TransactionFactory.beginTransactionRequired();
        try {
            Thing t = ThingUtilities.findThing(testSuite);
            if (t == null) {
                throw new TestingException("Test suite " + testSuite + " does not exist");
            }
            SecurityContext previousContext = ThreadLocalContext.getSecurityContext();
            ThreadLocalContext.setSecurityContext(SecurityContext.createSuperUserContext());
            try {
                t.processAPIServiceRequest(service, new ValueCollection());
            } finally {
                ThreadLocalContext.setSecurityContext(previousContext);
            }
        } finally {
            try {
                TransactionFactory.endTransactionRequired();
            } catch (Exception e) {
                e.printStackTrace();
                LOG.warn("Unable to commit transaction while executing " + service + " on test suite " + testSuite + ": " + e.getMessage());
            }
        }
    }


    private void flattenPlanRecursive(TestSuite plan, List<TestDefinition> list) {
        list.addAll(plan.getTestCases());
        for (TestSuite child: plan.getSuites()) {
            flattenPlanRecursive(child, list);
        }
    }

    private void flattenExecutionRecursive(TestSuite plan, List<TestExecution> list, String prefix) {
        for (TestDefinition td: plan.getTestCases()) {
            list.add(new TestExecution(
                    prefix + td.getTestCase(),
                    td.getTestSuite(),
                    td.getTestCase(),
                    td.getDescription(),
                    td.getRunAs(),
                    td.getRemoteThings()
            ));
        }
        for (TestSuite child: plan.getSuites()) {
            flattenExecutionRecursive(child, list, prefix + child.getName() + " > ");
        }
    }

    private List<TestDefinition> getFlatExecutionPlan(String testSuite, String defaultRunAs) throws TestingException {
        TestSuite plan = new TestSuite(this, testSuite, null, defaultRunAs, new TreeSet<String>());
        List<TestDefinition> list = new ArrayList<>();
        flattenPlanRecursive(plan, list);
        return list;
    }

    private List<TestExecution> getFlatExecution(String testSuite, String defaultRunAs) throws TestingException {
        TestSuite plan = new TestSuite(this, testSuite, null, defaultRunAs, new TreeSet<String>());
        List<TestExecution> list = new ArrayList<>();
        flattenExecutionRecursive(plan, list, testSuite + " > ");
        return list;
    }

    @ThingworxServiceDefinition(name = "Abort", description = "Sends a request to abort the current execution")
    @ThingworxServiceResult(name = "Result", description = "", baseType = "NOTHING")
    public void Abort() throws Exception {
        synchronized (executing) {
            if (hasExecutables()) {
                isAborted = true;
                updateProperties();
            }
        }
    }

    String getLocalUrl() {
        return this.getStringConfigurationSetting("LocalClientConfiguration", "url");
    }

    String getLocalAppKey() throws Exception {
        String appKeyName = this.getStringConfigurationSetting("LocalClientConfiguration", "appKeyName");
        ApplicationKey appKey = (ApplicationKey) EntityUtilities.findEntity(appKeyName, RelationshipTypes.ThingworxRelationshipTypes.ApplicationKey);
        if (appKey == null) {
            return null;
        } else {
            return appKey.GetKeyID();
        }
    }

    @ThingworxServiceDefinition(name = "Reset", description = "Aborts all running jobs and resets execution state to the initial point")
    @ThingworxServiceResult(name = "Result", description = "", baseType = "NOTHING")
    public void Reset() throws Exception {
        synchronized (executing) {
            if (hasExecutables()) {
                isAborted = true;
            }
            executing = new LinkedList<TestExecution>();
            updateProperties();
        }
    }

    @ThingworxServiceDefinition(name = "PreviewExecutionPlan", description = "Gets execution plan for the tests without executing them")
    @ThingworxServiceResult(name = "Result", description = "", baseType = "INFOTABLE", aspects = {"dataShape:TestDefinition"})
    public InfoTable PreviewExecutionPlan(
        @ThingworxServiceParameter(name = "testSuite", description = "Test suite thing (manual or auto)", baseType = "THINGNAME", aspects = {"isRequired:true"}) String testSuite,
        @ThingworxServiceParameter(name = "defaultRunAs", description = "Default username under which to run all tests", baseType = "USERNAME", aspects = {"isRequired:false"}) String defaultRunAs
    ) throws Exception {
        synchronized (executing) {
            InfoTable result = InfoTableInstanceFactory.createInfoTableFromDataShape("TestDefinition");
            for (TestDefinition t: getFlatExecutionPlan(testSuite, defaultRunAs)) {
                result.addRow(t.toValueCollection());
            }
            return result;
        }
    }

    private boolean hasExecutables() {
        synchronized (executing) {
            for (TestExecution te : executing) {
                if (te.getState().equals(ExecutionState.Scheduled) || te.getState().equals(ExecutionState.Executing)) {
                    return true;
                }
            }
            return false;
        }
    }

    @ThingworxServiceDefinition(name = "Run", description = "Runs all test cases in a given test suite")
    @ThingworxServiceResult(name = "Result", description = "", baseType = "INFOTABLE", aspects = {"dataShape:TestExecution"})
    public InfoTable Run(
            @ThingworxServiceParameter(name = "testSuite", description = "Test suite thing (manual or auto)", baseType = "THINGNAME", aspects = {"isRequired:true"}) String testSuite,
            @ThingworxServiceParameter(name = "defaultRunAs", description = "Default username under which to run all tests", baseType = "USERNAME", aspects = {"isRequired:false"}) String defaultRunAs,
            @ThingworxServiceParameter(name = "async", description = "Whether the tests should be executed synchronously (default) or in the background", baseType = "BOOLEAN", aspects = {"isRequired:false", "defaultValue:false"}) Boolean async
    ) throws Exception {
        synchronized (executing) {
            if (hasExecutables()) {
                throw new Exception("Another execution is in progress");
            } else {
                return launchExecution(testSuite, defaultRunAs, (async != null && async.booleanValue()) ? true : false);
            }
        }
    }

    private TestExecution nextExecutable() {
        synchronized (executing) {
            for (TestExecution te : executing) {
                if (te.getState().equals(ExecutionState.Scheduled)) {
                    return te;
                }
            }
            return null;
        }
    }
}
