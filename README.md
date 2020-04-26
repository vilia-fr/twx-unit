# TwxUnit

TwxUnit is an Apache 2.0-licensed unit testing framework for PTC ThingWorx platform, 
compatible with JUnit semantics and existing tooling for JUnit 2+. It is the first
such solution that we are aware of, and as such it is actively developed. It allows
you to do this:

```javascript
// -- QueryTestSuite.TestWithFilters()
// Tests that QueryUtility.Query() returns exactly three rows
// The rows are added in me.Before() and deleted in me.After()

var things = Things["QueryUtility"].Query({ filters: { tag: "testdata" } });
assertNotNull(things, "Query returned null");
assertEquals(3, things.rows.count, "Count is not 3");
``` 

or this:

```javascript
// -- PermissionsTestSuite.TestSystemRepository()
// Tests that SystemRepository is visible to all users, but only System can list files

assertHasNoPermissions("Users", function() {
    // Everything here is executed under an anonymous member of Users group
    assertNotNull(Things["SystemRepository"], "All users should see SystemRepository");
    Things["SystemRepository"].GetFileListingWithLinks();   // Expecting an error here
}, "Non-admin users can get the list of files");

assertHasPermissions("System", function() {
    // Everything here is executed under System user
    Things["SystemRepository"].GetFileListingWithLinks();   // Expecting NO error here
}, "System user can't get the list of files");
``` 

(see more examples below)

It is originally developed and actively maintained by [Vilia](http://vilia.fr) as part 
of our effort towards open industrialization of ThingWorx development.

## Rationale

Lack of static typing in ThingWorx provides great productivity benefits for small
projects, but becomes somewhat of an issue as soon as the code base grows beyond some
limit. Unit testing is essential to addressing this problem. Unfortunately PTC
hasn't included any unit testing facility into the core platform (yet). This project
is here to try filling this gap. 

## TwxUnit design goals

1. Simplify learning curve by replicating JUnit 4 semantics and familiar naming convention
2. Allow executing tests entirely from within ThingWorx, without doing HTTP calls, and thus
avoid storing any credentials outside of the platform.
3. (Re-)use existing JUnit tooling for executing tests from outside of ThingWorx 
   (e.g. from a CI pipeline)
4. Avoid complex trickery, be reasonably lightweight, predictable and simple for an 
average ThingWorx developer to understand inner workings
5. Allow executing specific tests in the context of arbitrary ThingWorx users
6. Small and light codebase, trivial to install and remove, no external dependencies

## System requirements

TwxUnit was tested against ThingWorx 7.4, 8.4 and 8.5, although it should work with other 
versions just as well.

On Java side (optional) it requires Java 1.6 and JUnit 2, 3, 4 or 5 with `junit-vintage-engine` 
on the classpath. We compile and test TwxUnit against JUnit 4.12, and there are no 
external runtime dependencies.

## Installation

TwxUnit can be executed either directly from ThingWorx, or as a JUnit4 test suite. 
In the latter case the custom runner will do all necessary remote introspection for 
you. Be aware that **this scenario is not fully supported yet**, so the only way to run
test suites *today* is via `TwxUnit` thing in ThingWorx Composer (see below).

### Installation on ThingWorx (mandatory)

Check out this project or simply download the ZIP file with extension package (the 
latest version is [twxunit-ext-2.3.11.zip](https://github.com/vilia-fr/twx-unit/files/4535595/twxunit-ext-2.3.11.zip))

In ThingWorx Composer go to Import/Export > IMPORT > Extension, choose the ZIP file
and click *Import*. That's all, you can start using TwxUnit now (see examples below).

Here is the complete list of all entities, which make up TwxUnit extension:

- `AssertFunctions` Script function library with `assert*` functions;
- `TwxUnit` (Project): A Project marker for all entities related to TwxUnit;
- `TwxUnit` (Thing): The default test runner;
- `TwxUnit` (Mashup): A Mashup, which alows to execute and monitor test runs; 
- `HasTestCases`: A Thing Shape to mark test simple suites;
- `HasTestSuite`: A Thing Shape for configurable test suites;
- `TestDefinition`: A Data Shape representing test suites / cases tree;
- `TestExecution`: A Data Shape describing test run results (both synchronous and async);
- `TwxUnitExecutor`: Thing Template for test runners, this is where most of the "business logic" resides.

### Installation on Java (optional)

**TODO:** Currently work in progress.

## Uninstalling TwxUnit

To uninstall TwxUnit one needs to delete all test suite things and then uninstall *twx-unit* extension.

## Upgrading TwxUnit

Just import a newer version of the extension and restart Tomcat. We follow semantic versioning to
ensure that minor-version upgrades do not break your test cases. 

## Test execution

TwxUnit is conceptually similar to JUnit in that test suites are things (/ object instances) and test cases are
services (/ methods). Test suites are executed by calling `Run({ testSuite, [defaultRunAs], [async] })` 
on a thing with `TwxUnitExecutor` base template (`TwxUnit` by default). 

Test executor parses a tree of test suites and generates a complete test plan. It is also responsible for 
executing this plan and collecting its results. You can abort the execution and reset collected statistics 
at any moment. See more details in the "TwxUnit thing and Web UI" section below.

Test cases (and test suites) go through the following lifecycle states:

```
                                      +---------+
                                  +-->| Success |
                                  |   +---------+
                                  |
+-----------+      +-----------+  |   +---------+    
| Scheduled |--+-->| Executing |--+-->| Failure |
+-----------+  |   +-----------+  |   +---------+
               |                  |
               |                  |   +---------+
               |                  +-->| Timeout |
               |                  |   +---------+
               |                  |
               |                  |   +---------+
               +------------------+-->| Aborted |
                                      +---------+
```

## Hello, World!

In ThingWorx Composer create a `GenericThing` called `HelloWorldTest`, implementing 
thing shape `HasTestCases`. Add a service `TestSimpleAssertion`, keeping its Inputs and Outputs 
empty: 

```javascript
var testedValue = ["Hello", "Earth"];
assertEquals("Hello, World", testedValue.join(', '));
``` 

Save `HelloWorldTest`, then find `TwxUnit` thing and execute service `Run`, specifying `HelloWorldTest` as `testSuite`. 
Leave default values for the remaining parameters. Expected results:

| id                                   | testSuite      | testCase            | description | result                                                                                                                            | state    | runAs | start                   | end                     | duration |
| ------------------------------------ | -------------- | ------------------- | ------------| --------------------------------------------------------------------------------------------------------------------------------- | -------- | ----- | ----------------------- | ----------------------- | -------- |
| HelloWorldTest > TestSimpleAssertion | HelloWorldTest | TestSimpleAssertion |             | Execution error in service script [TestSimpleAssertion] :: Assertion failed, values should be equal: Hello, World != Hello, Earth | Failure  |       | 2020-04-18 22:20:27.548 | 2020-04-18 22:20:27.568 | 20       |

**Note:** If you specify any inputs or outputs for your `Test*` services, TwxUnit will fail to execute the test suite.

Try to replace `"Earth"` with `"World"` and confirm successful execution.

## Assertions

TwxUnit supports most of [classic JUnit assertion methods](http://junit.sourceforge.net/javadoc/org/junit/Assert.html), 
exposed via `AssertFunctions` script function library:  

```javascript
assertTrue(testValue, [description]);
assertTrue(5 == "5");                       // OK
assertTrue(5 === "5");                      // Exception 'Assertion failed, value should be true'
assertTrue(5 === "5", '5 !== "5"');         // Exception '5 !== "5"'

assertFalse(testValue, [description]);
assertFalse(5 == "5");                      // Exception 'Assertion failed, value should be false'
assertFalse(5 === "5");                     // OK

assertEquals(expectedValue, testedValue, [description]);
assertEquals(3, 3.0);                       // OK
assertEquals(3, 3.1);                       // Exception 'Assertion failed, values should be equal: 3.0 != 3.1'

assertNotEquals(expectedValue, testedValue, [description]);
assertNotEquals(3, 3.0);                    // Exception 'Assertion failed, values should not be equal: 3 == 3'
assertNotEquals(3, 3.1);                    // OK

assertNull(testValue, [description]);
assertNull(Things["Nessie"]);               // OK
assertNull(Things["SystemRepository"]);     // Exception 'Assertion failed, value should be null: com.thingworx.things.repository.FileRepositoryThing...'

assertNotNull(testValue, [description]);
assertNotNull(Things["Nessie"]);            // Exception 'Assertion failed, value should not be null'
assertNotNull(Things["SystemRepository"]);  // OK

assertHasPermissions(principal, function, [description]);
assertHasPermissions(                       // OK
    "Administrator",                        // NB: Administrator is a user
    function() {
        Subsystems["PlatformSubsystem"].GetPlatformStats();
    }, 
    "Administrator can't get platform stats"
);
assertHasPermissions(                       // Exception 'Assertion failed, principal Users should have permissions: Not authorized for ServiceInvoke on GetPlatformStats in PlatformSubsystem'
    "Users",                                // NB: Users is a group 
    function() {
        Subsystems["PlatformSubsystem"].GetPlatformStats();
    }
);

assertHasNoPermissions(principal, function, [description]);
assertHasNoPermissions(                     // Exception 'Assertion failed, principal Administrator should not have permissions'
    "Administrator",
    function() {
        Subsystems["PlatformSubsystem"].GetPlatformStats();
    }
);
assertHasNoPermissions(                     // OK
    "Users", 
    function() {
        Subsystems["PlatformSubsystem"].GetPlatformStats();
    }
);
``` 

Functions `assertHasPermissions` / `assertHasNoPermissions` execute their "body" under the specified principal 
*and* System user, following standard ThingWorx semantics. It means that if System has access to execute some 
service or write a property, then you won't be able to check that some other user has *no* such access. For
example, the second assertion in the test below will *always* fail, regardless of how you configure permissions:

```javascript
assertHasPermissions("System", function() { me.SomeService(); });

// If we get here, then System has the right to execute SomeService(). It means 
// that this test for no permissions will always fail, because SomeService() is 
// called with "onion" security context, which includes both: Users and System.

assertHasNoPermissions("Users", function() { me.SomeService(); });
``` 

`assertHasPermissions` / `assertHasNoPermissions` also override `runAs` parameter (see below).

## Test suites

Test suites provide test grouping functionality. TwxUnit supports two types of test suites -- "simple"
and "advanced", which require using `HasTestCases` and `HasTestSuite` thing shapes, respectively. Both of 
those thing shapes define optionally overridable services `Before()` and `After()`, which wrap the execution
of a test suite (see below), while `HasTestSuite` also adds `testSuite` property, which allows to configure 
test cases precisely.

Test suite things can use any base thing template, e.g. `GenericThing`.

The services in *simple* test suites (implementing `HasTestCases`) are detected and executed automatically 
based on their names. A service is considered test case when its name begins with `Test`, it returns `NOTHING` 
and takes no parameters. All such services are executed in alphabetical order. A *simple* test suite cannot
refer to other test suites.

*Advanced* test suites allow to specify exact names of test cases / services, their order and the principal,
which should be used to execute them. Such test suites can refer to other test suites (complete or separate
services). This is configure via `testSuite` property, which is a persistable `INFOTABLE` with three fields:

| testSuite                                   | testCase                                                       | runAs                                   |
| ------------------------------------------- | -------------------------------------------------------------- | --------------------------------------- |
| Thing name (optional, `me.name` by default) | Service name (if empty, then complete `testSuite` is executed) | Principal name (current user, if empty) |

The use of those parameters is self-explanatory and rather intuitive. For example, TwxUnit will inherit
`runAs` configuration, it will execute `Before()` and `After()` in the right order and prevent circular
dependencies between test suites.

**IMPORTANT**: Make sure that `runAs` users have execution rights for the corresponding test case services to
avoid *Not authorized for ServiceInvoke* runtime errors.

Here's a comprehensive example of test suite configuration:

```
+===================================================+
| RootTestSuite: HasTestSuite                       |             +=====================================+
+===================================================+      +---|> | QueryTestSuite: HasTestCases        |
|                                                   |      |      +=====================================+
|  - testCases:                                     |      |      |                                     |
|    +================+==========+===============+  |      |      |  - Before(): { ...log "3"... }      |
|    | testSuite      | testCase | runAs         |  |      |      |  - TestSimpleQuery()                |
|    +================+==========+===============+  |      |      |  - TestComplexQuery()               |
|    | QueryTestSuite |          |               |---------+      |  - CheckResults({ data }): BOOLEAN  | 
|    +----------------+----------+---------------+  |             |  - After(): { ...log "4"...}        |
|    | AlertTestSuite |          | System        |---------+      |                                     |
|    +----------------+----------+---------------+  |      |      +-------------------------------------+
|    |                | MiscTest | Administrator |  |      |
|    +----------------+----------+---------------+  |      |      +=====================================================+
|                                                   |      +---|> | AlertTestSuite: HasTestSuite                        |
|  - Before(): { ...log "1"... }                    |             +=====================================================+
|  - MiscTest()                                     |             |                                                     |
|  - After():  { ...log "2"... }                    |             |  - testCases:                                       |
|                                                   |             |    +================+==================+=========+  |
+---------------------------------------------------+             |    | testSuite      | testCase         | runAs   |  |
                                                                  |    +================+==================+=========+  |         +==================================+
                                                                  |    | CommonTests    | AlwaysRunMe      | Bob     |------+     | CommonTests: HasTestCases        |
                                                                  |    +----------------+------------------+---------+  |   |     +==================================+
                                                                  |    |                | TestNotification |         |  |   |     |                                  |
                                                                  |    +----------------+------------------+---------+  |   |     |  - Before(): { ...log "5"... }   |
                                                                  |    |                | TestAlert        | Bob     |  |   +-------|> AlwaysRunMe()                 |
                                                                  |    +----------------+------------------+---------+  |         |  - TestSomething()               |
                                                                  |                                                     |         |                                  |
                                                                  |  - TestNotification()                               |         +----------------------------------+
                                                                  |  - TestAlert()                                      |
                                                                  |                                                     |
                                                                  +-----------------------------------------------------+
```

If you try to execute `RootTestSuite` via `TwxUnit.Run()`, specifying `Alice` as `runAs` user, it will follow 
this sequence:

1. `RootTestSuite.Before`, executed under `Alice` (and `System`, which is also the case for all other services)
2. `QueryTestSuite.Before`, executed under `Alice`
3. `QueryTestSuite.TestComplexQuery`, executed under `Alice`
4. `QueryTestSuite.TestSimpleQuery`, executed under `Alice`
5. `QueryTestSuite.After`, executed under `Alice`
6. `CommonTests.Before`, executed under `Bob`
7. `CommonTests.AlwaysRunMe`, executed under `Bob`
8. `AlertTestSuite.TestNotification`, executed under `System`
9. `AlertTestSuite.TestAlert`, executed under `Bob`
10. `RootTestSuite.MiscTest`, executed under `Administrator`
11. `RootTestSuite.After`, executed under `Alice`

**BUG:** Currently `RootTestSuite.MiscTest` (i.e. a *local* test case) is executed at point (3), which is 
incorrect.

## Before and After

See an example above. If `Before` fails with an exception, the rest of the execution is canceled. If `After` 
fails, the execution continues. `After` works like `finally`, it will be always executed if `Before` executed
successfully. Default implementations of `Before` and `After` are empty.

**BUG:** On ThingWorx 8.5 `After` is not executed if the script times out, or when the execution is aborted via
`Abort()` service.

## Timeouts and async mode

Regardless of ThingWorx version, network infrastructure, Tomcat and ThingWorx runtime all impose some timeouts 
on HTTP requests, which (by default) prevent you from receiving results of long-running services.

In addition to that, ThingWorx 8.5 introduced *service execution* timeouts (30 seconds by default), which 
will terminate tests abruptly (`After` will not be executed in such case, just like `finally` blocks in 
your services). For large or slow test suites you should strongly consider executing `TwxUnit.Run` with 
`async` flag set to `true`. Unfortunately, even this won't prevent it from failing if a single test case
takes longer than 30 seconds to execute.
 
In *async* mode TwxUnit executes all tests in a separate thread. Currently it still spawns one long-running
thread (that should be killed by the platform), and we are working on a solution to this problem, which
either uses ThingWorx `Timer` things, or just alternates between two threads spawning one enother.

**TODO**: Explain getting results in async mode

## Transactions

Each test case, `Before` and `After` is executed within its own transaction context, which ensures that you 
can expect realistic side effects from the platform.

## TwxUnit thing and Web UI

`TwxUnit` thing is a default test executor (it uses `TwxUnitExecutor` thing template). `TwxUnitExecutor` 
extends `RemoteThing` to simplify integration with other unit testing frameworks, such as JUnit (see below).
Test execution is controlled via the following

### Services

* **Run** actually executes test suites, and takes the following parameters:
  * `testSuite: THINGNAME` is the name of a thing, which implements either `HasTestCases` or `HasTestSuite` (see above);
  * `defaultRunAs: USERNAME` (optional): is the user or group name and defaults to the current user;
  * `async: BOOLEAN` (optional) defines whether the tests should run in a background thread and defaults to `false`;

  It returns an `INFOTABLE` with the summary of execution results (if `async == false`) or with the test plan,
which it has just started executing in the background (if `async == true`).
  
* **PreviewExecutionPlan**, which returns an `INFOTABLE` with an execution plan, that will be used once 
the `Run` service is executed. Parameters are the same as for `Run` service. For the above example with 
`RootTestSuite` executed under `Alice` it returns the following:

| testSuite      | testCase         | runAs         |
| -------------- | -----------------| ------------- |
| RootTestSuite  | MiscTest         | Administrator |
| QueryTestSuite | TestComplexQuery | Alice         |
| QueryTestSuite | TestSimpleQuery  | Alice         |
| CommonTests    | AlwaysRunMe      | Bob           |
| AlertTestSuite | TestNotification | System        |
| AlertTestSuite | TestAlert        | Bob           |

* **Abort** to stop an execution, regardless of whether it's *sync* or *async*. All test cases, which are
scheduled for execution after the current one will automatically complete with `Aborted` state. This service 
takes no parameters and returns `NOTHING`. It may take a while for the actual execution to complete, since
it won't abort the current test case, just cancel further executions.

  **BUG:** This service should run all necessary`After` services in the right (stack) order, but currently it 
  doesn't.

* **Reset** simply clears the last execution results log and resets the executor to its initial state. It
can be used to reset the executor if it gets stuck in "executing" state for some reason (usually due to bugs 
in TwxUnit).

### Properties

All properties work similarly in *sync* and *async* modes to simplify infrastructure implementation. Neither 
of them is logged or persisted.

* **countExecuted**, **countRemaining**, **countTotal**: Essentially represent a progress bar.

* **execution** is an `INFOTABLE<TestExecution>`, which is updated before and after each test case is 
executed. Although this property is not read-only, any changes to it will be overwritten by the executor.
This property should be monitored to get the detailed log of the execution.

* **isExecuting** is a boolean value, which should be used as a lock, preventing concurrent execution 
attempts.

### Web UI

`TwxUnit` mashup exposes TwxUnit services and properties via a simple web UI.

**TODO:** Implement and describe.

## Running tests in parallel

TwxUnit *theoretically* supports concurrent test execution, although this has never been properly tested.
To do this, you would have to create several test executors and call their `Run` services at the same time.
Each `TwxUnitExecutor` thing is self-contained and independent. 

### Running tests as part of CI

TwxUnit executors can be controlled remotely via ThingWorx Always On protocol. We're working on a JUnit
test runner, which does it transparently and thus allows to use JUnit infrastructure for running TwxUnit
tests, collecting statistics, etc.

**TODO:** Work in progress, see /junit-runner sub-project.

## Best practices

### Naming convention, projects and tags

- Test suites are things ending with "Tests", e.g. `PerformanceTests`, `UserManagementTests`, `AlertTests`;
- Test case names should be relatively short, with longer explanations in service *description*;
- Numeric prefixes can be used to control test case order, e.g. `Test01CreateThing`, `Test02ModifyThing`;
- Fill *descriptions* for test suites and test cases;
- Use the last (optional) description parameter of `assert*` functions;
- By default, one should prefer simple test suites (implementing `HasTestCases`) to advanced ones (`HasTestSuite`);
- Test suites should be tagged with some model tag, or be part of some *Tests* project to make sure they don't
accidentally become part of production codebase;
- Tag all test data generated in `Before` to simplify cleanup in `After`;

### Security

- Make sure that neither test cases nor TwxUnit extension become part of production codebase;
- Use PASSWORD properties to encrypt credentials when executing tests;
- Create some *Testers* group to make sure all TwxUnit tests are executed by non-Admin user;

**TODO:** Expand this section.

## Effect on ScriptLog

ThingWorx doesn't allow to hide exceptions, always appending them to the Script log
no matter what, therefore you will find errors in `ScriptLog` for every failed assert. 

**TODO:** Validate it for 8.5.

## Building and extending TwxUnit

TwxUnit is built using Maven. It relies on twx-maven to load ThingWorx Extension SDK to the local
Maven repository. Once you installed the SDK, building TwxUnit is trivial -- just execute 
`mvn package`.

Pull requests are welcome!

**TODO:** Link to twx-maven repo.

## Testing TwxUnit

It would be great for TwxUnit to test itself. Unfortunately, it is not yet the case, but this 
is on our priorities list. The approach we envision includes using two test executors and test
cases that verify results of one of their `Run` services.

In addition to that we need to have a JUnit test suite for the JUnit test runner. This test 
suite will assume a ThingWorx instance running side-by-side with JUnit test suite.

**TODO:** Describe the test suites.

## Future plans

Collected from misc. TODOs in no specific order:

- Add support for setting remote properties
- Add support for remote things emulation 
- Add support for testing subscriptions
- Localizable failure messages
- Fix function signatures
- Publish TwxUnit mashup
- Publish JUnit test runner
- Fix bugs
- Run unit tests in alternating threads to avoid platform timeouts
- Write-up on security
- Test suite for TwxUnit and its JUnit test runner
- Add few examples of CI pipeline configuration