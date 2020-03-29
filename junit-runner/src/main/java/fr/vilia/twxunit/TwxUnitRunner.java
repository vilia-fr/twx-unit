//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package fr.vilia.twxunit;

import com.thingworx.relationships.RelationshipTypes;
import com.thingworx.types.InfoTable;
import com.thingworx.types.collections.ValueCollection;
import com.thingworx.types.primitives.StringPrimitive;
import junit.framework.AssertionFailedError;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TwxUnitRunner<T> extends Runner {
    private final Description suite;
    private final TwxUnitTest instance;
    private final TwxClient client;

    public TwxUnitRunner(Class<T> clazz) throws Exception {
        if (!TwxUnitTest.class.isAssignableFrom(clazz)) {
            throw new IllegalArgumentException("TwxUnitRunner can only be applied to TwxUnitTest cases. Instead received " + clazz.getName());
        } else {
            instance = (TwxUnitTest)clazz.getConstructor().newInstance();
            client = new TwxClient(instance.getWsUrl(), instance.getAppKey(), false);
            suite = initializeSuite(instance, clazz);
        }
    }

    public Description getDescription() {
        return suite;
    }

    public void run(RunNotifier notifier) {
        Iterator var2 = suite.getChildren().iterator();

        while(var2.hasNext()) {
            Description test = (Description)var2.next();
            notifier.fireTestStarted(test);

            try {
                String url = instance.getWsUrl() + " / " + test;
                System.out.println("Executing remote test: " + url);
            } catch (Throwable var8) {
                notifier.fireTestFailure(new Failure(test, new AssertionFailedError("Cannot generate URL for test " + test.getMethodName())));
            } finally {
                notifier.fireTestFinished(test);
            }
        }

    }

    private List<String> getServices(TwxUnitTest instance) throws Exception {
        ValueCollection params = new ValueCollection();
        params.put("testSuite", new StringPrimitive(instance.getThingName()));
        InfoTable plan = client.invokeService(RelationshipTypes.ThingworxEntityTypes.Things, "TwxUnit", "PreviewExecutionPlan", params, 30000);

        List<String> services = new ArrayList();
        for (int i = 0; i < plan.getLength(); ++i) {
            ValueCollection row = plan.getRow(i);
            services.add(row.getStringValue("testSuite") + " > " + row.getStringValue("testCase"));
        }
        return services;
    }

    private Description initializeSuite(TwxUnitTest instance, Class<T> clazz) throws Exception {
        Description suite = Description.createSuiteDescription(clazz.getSimpleName(), new Annotation[0]);
        Iterator var5 = getServices(instance).iterator();

        while(var5.hasNext()) {
            String service = (String)var5.next();
            suite.addChild(Description.createTestDescription(clazz.getName(), service, new Annotation[0]));
        }

        return suite;
    }
}
