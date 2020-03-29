package fr.vilia.twxunit;

import com.thingworx.entities.utils.ThingUtilities;
import com.thingworx.entities.utils.UserUtilities;
import com.thingworx.metadata.ServiceDefinition;
import com.thingworx.security.context.SecurityContext;
import com.thingworx.security.users.User;
import com.thingworx.things.Thing;
import com.thingworx.types.collections.ValueCollection;
import com.thingworx.types.primitives.DatetimePrimitive;
import com.thingworx.types.primitives.IntegerPrimitive;
import com.thingworx.types.primitives.StringPrimitive;
import com.thingworx.webservices.context.ThreadLocalContext;
import org.joda.time.DateTime;

/**
 * All methods are synchronized.
 */
public class TestExecution {

    private final String id;
    private final String testSuite;
    private final String testCase;
    private final String description;
    private final String runAs;

    private String result = null;
    private ExecutionState state = ExecutionState.Scheduled;
    private DateTime start = null;
    private DateTime end = null;

    public TestExecution(String id, String testSuite, String testCase, String description, String runAs) {
        this.id = id;
        this.testSuite = testSuite;
        this.testCase = testCase;
        this.description = description;
        this.runAs = runAs;
    }

    public synchronized void start() {
        if (state.equals(ExecutionState.Scheduled)) {
            start = new DateTime();
            state = ExecutionState.Executing;
        } else {
            throw new IllegalStateException("Trying to start test case '" + id + "' from state '" + state + "'");
        }
    }

    public synchronized void complete(boolean success, String result) {
        if (state.equals(ExecutionState.Executing)) {
            end = new DateTime();
            state = success ? ExecutionState.Success : ExecutionState.Failure;
            this.result = result;
        } else {
            throw new IllegalStateException("Trying to complete test case '" + id + "' from state '" + state + "'");
        }
    }

    public synchronized void abort(boolean timeout, String result) {
        if (state.equals(ExecutionState.Scheduled) || state.equals(ExecutionState.Executing)) {
            end = new DateTime();
            state = timeout ? ExecutionState.Timeout : ExecutionState.Aborted;
            this.result = result;
        } else {
            throw new IllegalStateException("Trying to abort test case '" + id + "' from state '" + state + "'");
        }
    }

    public synchronized ValueCollection toValueCollection() {
        ValueCollection vc = new ValueCollection();
        try {
            if (id != null) vc.setValue("id", new StringPrimitive(id));
            if (testSuite != null) vc.setValue("testSuite", new StringPrimitive(testSuite));
            if (testCase != null) vc.setValue("testCase", new StringPrimitive(testCase));
            if (description != null) vc.setValue("description", new StringPrimitive(description));
            if (result != null) vc.setValue("result", new StringPrimitive(result));
            if (state != null) vc.setValue("state", new StringPrimitive(state.toString()));
            if (runAs != null) vc.setValue("runAs", new StringPrimitive(runAs.toString()));
            if (start != null) vc.setValue("start", new DatetimePrimitive(start));
            if (end != null) vc.setValue("end", new DatetimePrimitive(end));
            if (start != null && end != null) {
                vc.setValue("duration", new IntegerPrimitive((Number) Long.valueOf(end.getMillis() - start.getMillis())));
            }
        } catch (Exception e) {
            // We won't ever be here
            e.printStackTrace();
        }
        return vc;
    }

    public void execute() {
        Thing t = ThingUtilities.findThing(testSuite);
        if (t == null) {
            abort(false, "Test suite " + testSuite + " for execution " + id + " does not exist");
            return;
        }

        ServiceDefinition s = t.getEffectiveServiceDefinition(testCase);
        if (s == null) {
            abort(false, "Test case " + testCase + " for execution " + id + " does not exist");
            return;
        }

        SecurityContext previousContext = ThreadLocalContext.getSecurityContext();
        try {
            if (runAs != null && !runAs.isEmpty()) {
                setSecurityContext();
            }
            start();
            try {
                t.processAPIServiceRequest(testCase, new ValueCollection());
                complete(true, "");
            } catch (Exception e) {
                e.printStackTrace();
                complete(false, e.getMessage());
            } finally {
                ThreadLocalContext.setSecurityContext(previousContext);
            }
        } catch (TestingException e) {
            e.printStackTrace();
            abort(false, e.getMessage());
            ThreadLocalContext.setSecurityContext(previousContext);
        }
    }

    private void setSecurityContext() throws TestingException {
        User user = UserUtilities.findUser(runAs);
        if (user == null) {
            throw new TestingException("User " + runAs + " not found while trying to execute test " + id);
        }
        SecurityContext context = SecurityContext.createUserContext(user);
        ThreadLocalContext.setSecurityContext(context);
    }

    public ExecutionState getState() {
        return state;
    }

    public String getTestSuite() {
        return testSuite;
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return "TestExecution{" +
                "id='" + id + '\'' +
                ", testSuite='" + testSuite + '\'' +
                ", testCase='" + testCase + '\'' +
                ", description='" + description + '\'' +
                ", runAs='" + runAs + '\'' +
                ", result='" + result + '\'' +
                ", state=" + state +
                ", start=" + start +
                ", end=" + end +
                '}';
    }
}
