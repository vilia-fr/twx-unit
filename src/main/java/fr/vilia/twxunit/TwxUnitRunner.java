package fr.vilia.twxunit;

import junit.framework.AssertionFailedError;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class TwxUnitRunner<T> extends Runner {

    private final Description suite;
    private final TwxUnitTest instance;

    public TwxUnitRunner(Class<T> clazz) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, IOException {
        if (!TwxUnitTest.class.isAssignableFrom(clazz)) {
            throw new IllegalArgumentException("TwxUnitRunner can only be applied to TwxUnitTest cases. Instead received " + clazz.getName());
        }
        this.instance = (TwxUnitTest) clazz.getConstructor().newInstance();
        this.suite = initializeSuite(instance.getUrl(), instance.getAppKey(), clazz);
    }

    public Description getDescription() {
        return this.suite;
    }

    public void run(RunNotifier notifier) {
        for (Description test: suite.getChildren()) {
            notifier.fireTestStarted(test);
            try {
                try {
                    URL url = instance.getUrl(test.getMethodName());
                    System.out.println("Executing remote test: " + url);
                } catch (Throwable t) {
                    notifier.fireTestFailure(new Failure(
                        test,
                        new AssertionFailedError("Cannot generate URL for test " + test.getMethodName())
                    ));
                }
            } finally {
                // Always call fireTestFinished() in order not to confuse the listeners
                notifier.fireTestFinished(test);
            }
        }
    }

    private List<String> getServices(URL url, String appKey) throws IOException {
        List<String> services = new ArrayList<String>();

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "text/csv");
        conn.setRequestProperty("AppKey", appKey);

        Scanner sc = null;
        try {
            // Sure, we can do something smarter here, but wanted to avoid using 3rd-party libs for such a trivial task
            sc = new Scanner(conn.getInputStream());
            while (sc.hasNextLine()) {
                String ln = sc.nextLine();
                String service = ln.substring(1, ln.indexOf('"', 1));
                if (service.startsWith("Test")) {
                    services.add(service);
                }
            }
        } finally {
            if (sc != null) {
                sc.close();
            }
        }

        return services;
    }

    private Description initializeSuite(URL url, String appKey, Class<T> clazz) throws IOException {
        Description suite = Description.createSuiteDescription(clazz.getSimpleName());
        for (String service: getServices(url, appKey)) {
            suite.addChild(Description.createTestDescription(clazz.getName(), service));
        }
        return suite;
    }

}
