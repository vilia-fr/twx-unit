package fr.vilia.twxunit;

import ch.qos.logback.classic.Logger;
import com.thingworx.entities.utils.GroupUtilities;
import com.thingworx.entities.utils.UserUtilities;
import com.thingworx.logging.LogUtilities;
import com.thingworx.security.context.SecurityContext;
import com.thingworx.security.groups.Group;
import com.thingworx.security.users.User;
import com.thingworx.webservices.context.ThreadLocalContext;
import org.mozilla.javascript.*;

public class AssertScriptLibrary {

    private static final Logger LOG = LogUtilities.getInstance().getScriptLogger(AssertScriptLibrary.class);

	public static void assertTrue(Context cx, Scriptable me, Object[] args, Function func) throws Exception {
        if (args.length < 1 || args.length > 2) {
            throw new Exception("Invalid number of arguments in assertTrue");
        }
        if (args[0] instanceof Boolean) {
            if (!(Boolean) args[0]) {
                throw new AssertionFailedError(
                        args.length == 1 ? "Assertion failed, value should be true" : (String) args[1]
                    );
            }
        } else {
            throw new AssertionFailedError("assertTrue parameter should be explicitly Boolean");
        }
	}

	public static void assertFalse(Context cx, Scriptable me, Object[] args, Function func) throws Exception {
        if (args.length < 1 || args.length > 2) {
            throw new Exception("Invalid number of arguments in assertFalse");
        }
        if (args[0] instanceof Boolean) {
            if ((Boolean) args[0]) {
                throw new AssertionFailedError(
                        args.length == 1 ? "Assertion failed, value should be false" : (String) args[1]
                    );
            }
        } else {
            throw new AssertionFailedError("assertFalse parameter should be explicitly Boolean");
        }
	}

	public static void assertEquals(Context cx, Scriptable me, Object[] args, Function func) throws Exception {
        if (args.length < 2 || args.length > 3) {
            throw new Exception("Invalid number of arguments in assertEquals");
        }

        Object op1, op2;
        if (args[0] instanceof Double && args[1] instanceof Integer) {
            op1 = args[0];
            op2 = Double.valueOf((Integer) args[1]);
        } else if (args[0] instanceof Integer && args[1] instanceof Double) {
            op1 = Double.valueOf((Integer) args[0]);
            op2 = args[1];
        } else {
            op1 = args[0];
            op2 = args[1];
        }

        if((op1 == null && op2 != null) || (op1 != null && !op1.equals(op2))) {
            throw new AssertionFailedError(
                    args.length == 2 ? "Assertion failed, values should be equal: " + op1 + " != " + op2 : (String) args[2]
                );
        }
	}

	public static void assertNotEquals(Context cx, Scriptable me, Object[] args, Function func) throws Exception {
        if (args.length < 2 || args.length > 3) {
            throw new Exception("Invalid number of arguments in assertNotEquals");
        }

        Object op1, op2;
        if (args[0] instanceof Double && args[1] instanceof Integer) {
            op1 = args[0];
            op2 = Double.valueOf((Integer) args[1]);
        } else if (args[0] instanceof Integer && args[1] instanceof Double) {
            op1 = Double.valueOf((Integer) args[0]);
            op2 = args[1];
        } else {
            op1 = args[0];
            op2 = args[1];
        }

        if((op1 == null && op2 == null) || (op1 != null && op1.equals(op2))) {
            throw new AssertionFailedError(
                    args.length == 2 ? "Assertion failed, values should not be equal: " + op1 + " == " + op2 : (String) args[2]
                );
        }
	}

	public static void assertNull(Context cx, Scriptable me, Object[] args, Function func) throws Exception {
        if (args.length < 1 || args.length > 2) {
            throw new Exception("Invalid number of arguments in assertNull");
        }
        if (args[0] != null) {
            throw new AssertionFailedError(
                    args.length == 1 ? "Assertion failed, value should be null: " + args[0] : (String) args[1]
                );
        }
	}

	public static void assertNotNull(Context cx, Scriptable me, Object[] args, Function func) throws Exception {
        if (args.length < 1 || args.length > 2) {
            throw new Exception("Invalid number of arguments in assertNotNull");
        }
        if (args[0] == null) {
            throw new AssertionFailedError(
                    args.length == 1 ? "Assertion failed, value should not be null" : (String) args[1]
                );
        }
	}

    private static void setSecurityContext(String principal) throws TestingException {
        Group group = GroupUtilities.findGroup(principal);
        if (group == null) {
            User user = UserUtilities.findUser(principal);
            if (user == null) {
                throw new TestingException("User or group '" + principal + "' not found for checking permissions");
            }
            SecurityContext sc = SecurityContext.createUserContext(user);
            ThreadLocalContext.setSecurityContext(sc);
        } else {
            ThreadLocalContext.setSecurityContext(SecurityContext.createGroupContext(group));
        }
    }

    public static Object assertHasPermissions(Context cx, Scriptable me, Object[] args, Function func) throws Throwable {
        if (args.length != 2 && args.length != 3) {
            throw new Exception("Invalid number of arguments in assertHasPermissions");
        }

        if (!(args[0] instanceof String)) {
            throw new Exception("The first assertHasPermissions argument must be a string with username");
        }

        if (!(args[1] instanceof NativeFunction)) {
            throw new Exception("The second assertHasPermissions argument must be a function");
        }

        SecurityContext previousContext = ThreadLocalContext.getSecurityContext();
        try {
            setSecurityContext((String)args[0]);
            return ((NativeFunction)args[1]).call(cx, func.getParentScope(), me, new Object[]{});
        } catch (WrappedException we) {
            Throwable t = we.getWrappedException();
            if (t.getMessage() != null && t.getMessage().startsWith("Not authorized for")) {
                throw new AssertionFailedError(
                        args.length == 2 ? "Assertion failed, principal " + args[0] + " should have permissions: " + t.getMessage() : (String) args[2]
                    );
            } else {
                throw t;
            }
        } finally {
            ThreadLocalContext.setSecurityContext(previousContext);
        }
    }

    public static void assertHasNoPermissions(Context cx, Scriptable me, Object[] args, Function func) throws Throwable {
        if (args.length != 2 && args.length != 3) {
            throw new Exception("Invalid number of arguments in assertNoPermissions");
        }

        if (!(args[0] instanceof String)) {
            throw new Exception("The first assertNoPermissions argument must be a string with username");
        }

        if (!(args[1] instanceof NativeFunction)) {
            throw new Exception("The second assertNoPermissions argument must be a function");
        }

        SecurityContext previousContext = ThreadLocalContext.getSecurityContext();

        boolean success = false;
        try {
            setSecurityContext((String)args[0]);
            ((NativeFunction)args[1]).call(cx, func.getParentScope(), me, new Object[]{});
            success = true;
        } catch (WrappedException we) {
            Throwable t = we.getWrappedException();
            if (t.getMessage() == null || !t.getMessage().startsWith("Not authorized for")) {
                throw t;
            }
        } finally {
            ThreadLocalContext.setSecurityContext(previousContext);
        }

        if (success) {
            throw new AssertionFailedError(
                    args.length == 2 ? "Assertion failed, principal " + args[0] + " should not have permissions" : (String) args[2]
                );
        }
    }
}
