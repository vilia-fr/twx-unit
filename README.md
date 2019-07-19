# TwxUnit

[![Build Status](https://travis-ci.org/vilia-fr/twxunit.svg?branch=master)](https://travis-ci.org/vilia-fr/twxunit)

TwxUnit is an Apache 2.0-licensed unit testing framework for PTC ThingWorx platform, 
compatible with JUnit semantics and existing tooling for JUnit 2+. It is the first
such solution that we are aware of, and as such it is actively developed.

Lack of static typing in ThingWorx provides great productivity benefits for small
projects, but becomes somewhat of an issue as soon as the code base grows beyond some
limit. Unit testing seems like a suitable answer to this problem. Unfortunately PTC
hasn't included any unit testing facility into the core platform (yet). This project
is here to try filling this gap. 

It is originally developed and actively maintained by [Vilia](http://vilia.fr) as part 
of our effort towards open industrialization of ThingWorx development.

## Design goals

1. Simplify learning curve by replicating JUnit 4 semantics and overall JUnit naming 
convention
2. Allow executing tests from within ThingWorx
3. (Re-)use existing JUnit tooling for executing tests from outside of ThingWorx 
   (e.g. from a CI pipeline)
4. Avoid complex trickery, be reasonably lightweight, predictable and simple for an 
average ThingWorx developer to understand inner workings
5. Allow executing specific tests in the context of arbitrary ThingWorx users
6. Trivial to install, use and remove, no external dependencies

## System requirements

TwxUnit was tested against ThingWorx 7.4 and 8.4, although it should theoretically 
work with older versions just as well. It can be installed by importing entities, so
there is no need to install 

On the Java side it requires Java 1.5 and JUnit 2, 3, 4 or 5 with junit-vintage-engine 
on the classpath. We compile and test TwxUnit against JUnit 4.12, and there are no 
external runtime dependencies.

## Installation

TwxUnit can be executed either directly from ThingWorx, or as a JUnit4 test suite. 
In the latter case the custom runner will do all necessary remote introspection for 
you.

### Installation on ThingWorx (mandatory)

Check out this project or simply download the XML file with entities definitions (see
[src/main/twx/TwxUnit.xml]())

In ThingWorx Composer go to Import/Export > IMPORT > From File, choose the XML file
and click Import. That's all, you can start using TwxUnit now (see examples below).

Here's the complete list of all entities in XML file:

- `TwxUnit`: A Project marker for all entities related to TwxUnit;
- `HasTests`: A Thing Shape, which is a mix-in to mark test suites;
- `TestDefinition`: A Data Shape representing test suites / cases tree;
- `TestRunResults`: A Data Shape describing test run results (both synchronous and async);
- `TestRunner`: A Thing representing the test execution framework. This is where most of the
"business logic" resides.
- `TestRuns`: A Stream keeping results for asynchronous test runs;
- `TestManagerMashup`: A Mashup, which alows to execute and monitor test runs; 
- `Testers`: A User Group to define who is allowed to run tests;

### Installation on Java (optional)

Include the following dependency in your Maven project:

```xml
<dependency>
    <groupId>fr.vilia.twxunit</groupId>
    <artifactId>twxunit</artifactId>
    <version>1.0</version>
    <scope>test</scope>
</dependency>
```

Or download a single JAR file from our CI system.

## Removing TwxUnit from ThingWorx

To uninstall TwxUnit one needs to delete all test suite things and then delete all
entities from `TwxUnit` project, or simply execute service `TestRunner.Uninstall` with
user having sufficient permissions -- it will do the same, except for deleting test suites.

## Examples

### Hello, World!

In ThingWorx Composer create a GenericThing called `HelloWorldTest`, implementing 
ThingShape `HasTests`. Add a service `TestSimpleAssertion`, keeping Inputs/Outputs 
section empty (default): 

```javascript
me.AssertStringEquals({
  expected: "Hello",
  actual: "World"
});
``` 

Save `HelloWorldTest` and execute service `TestRunner.RunAll`. Expected results:

| testSuite      | testName            | timestamp           | duration | state | message |
| -------------- | ------------------- | ------------------- | -------- | ----- | ------- |
| HelloWorldTest | TestSimpleAssertion | 11/07/2019 16:15:22 | 15       | fail  | AssertEquals: Expected "Hello", actual "World" |

### Assertions

TwxUnit supports most of [classic JUnit assertion methods](http://junit.sourceforge.net/javadoc/org/junit/Assert.html), 
adding type postfixes to compensate for ThingWorx lack of services overloading:  

```javascript
me.AssertStringEquals({ expected: "Hello", actual: "hello" });  // Will fail
me.AssertStringEquals({ expected: null, actual: null });        // Ok

me.AssertArrayEquals({ expected: [2, 1], actual: [1, 2] });     // Will fail
me.AssertArrayEquals({ expected: [1, 2], actual: [1, 2] });     // OK

me.AssertNumberEquals({ expected: 3, actual: 3.0 });            // OK
me.AssertNumberEquals({ expected: 3, actual: 3.1 });            // Will fali

me.AssertJsonEquals({                                           // OK 
    expected: { a: 3, b: '3' }, 
    actual: { b: '3', a: 3.0 } 
});
me.AssertJsonEquals({                                           // Will fail 
    expected: { a: 3, b: [] }, 
    actual: { a: 3, b: {} } 
});

var t1 = Resources["InfoTableFunctions"].CreateInfoTableFromDataShape({ 
    infoTableName : "t1", dataShapeName : "IntegerValueStream" 
});
t1.AddRow({ id: 'id1', value: 3 });
var t2 = Resources["InfoTableFunctions"].CreateInfoTableFromDataShape({ 
    infoTableName : "t2", dataShapeName : "IntegerValueStream" 
});
t2.AddRow({ id: 'id2', value: 3 });
me.AssertInfotableEquals({ expected: t1, actual: t2 });         // Will fail
t1.rows[0].id = 'id1';
me.AssertInfotableEquals({ expected: t1, actual: t2 });         // OK
t2.AddRow({ id: 'id3', value: 5 });
me.AssertInfotableEquals({ expected: t1, actual: t2 });         // Will fail

me.AssertStringNotEmpty({ string: "  " });                      // OK
me.AssertStringNotEmpty({ string: null });                      // Will fail

me.AssertTrue({ condition: 5 == 5.0 });                         // OK
me.AssertTrue({ condition: "5" == 5.0 });                       // Will fail

me.Fail();                                                      // Will fail
me.Fail({ message: "Condition failed" });                       // Will fail
``` 

### Test suites

### Context

### Before and After

### Using web UI

### Running tests under specific user accounts

### Running time-consuming tests

### Running tests as part of CI

### Mixing with other tests

## Some best practices

- Naming convention for test suites and test cases

## Security

- Use PASSWORD properties to encrypt credentials when executing tests
- All services prefixed with "Private" are allowed to be executed by the _System_ user
- TwxUnit tests can be executed by non-Admin user, as soon as he is in the _Testers_ group

## API reference

### ThingWorx

#### Assertions

#### Test runner

Tests are ran by executing services on the `TestRunner` Thing. A number of services
are avilable for your convenience:

1. **RunOne**: Takes Thing name `testSuite` as a parameter and executes it as a test 
suite, following standard JUnit lifecycle:
    
   - `BeforeAll`
   - `BeforeTest(testName)`
   - `TestAbc` -- In lexicographical order, following JUnit 1.x naming convention
   - `AfterTest(testName)`
   - `BeforeTest(testName)`
   - `TestDef`
   - `AfterTest(testName)`
   - ...
   - `AfterAll`

   Optional parameters:

   - `appKey`: An AppKey to authenticate this request (cannot be used together with
   `user` and `password` parameters);
   - `user`: HTTP Basic Auth user to authenticate this request (has to be used 
   together with `password` parameter);
   - `password`: HTTP Basic Auth password to authenticate this request (has to be used 
   together with `password` parameter);
   - `timeout`: Test execution timeout in milliseconds (_including_ local HTTP 
   connection overhead);
   - `ignoreSslErrors`: Ignores SSL errors;
   - `baseUrl`: _Local_ ThingWorx base URL, e.g. "https://localhost:8443/Thingworx"
   (required if any of the above parameters are specified);
   
   If any of those optional parameters are provided, then the test will be executed via 
   an HTTP call. Be careful, in certain situations it may cause unexpected side effects.

2. **RunAll**: Queries all Things implementing `HasTests` Thing Shape, and then does 
the same as `RunOne`, aggregating all results in the same infotable.

3. **RunOneAsync**: An asynchronous logged version of `RunOne`. It takes `testRunId` 
as a mandatory parameter and executes tests in the same way as `RunOne`. Each test 
result is immediately persisted in `TestRuns` Stream. This allows monitoring test run
progress via a couple of services `GetRunState` and `QueryRunResults`, both taking
`testRunId` as a parameter. For external HTTP polling there's a convenience service 
`IsRunFinished`, which returns a simple BOOLEAN value.

    If another test run with the same ID exist, the service will fail with "Duplicate 
    test run ID" error message.  

4. **RunAllAsync**: Similar to `RunOneAsync`, but applies to all found test suites.

Test runners return results in `TestRunResult` DataShape:

- `testRunId`: ID of the test run, if specified;
- `testSuite`: Name of the test suite Thing;
- `testName`: Name of the test case / service;
- `timestamp`: When the test started execution;
- `duration`: Test duration in milliseconds (_excluding_ local HTTP connection time,
i.e. pure test case time regardless parameter values);
- `state`: One of the following values: `queued`, `executing`, `success`, `fail`, `timeout`;
- `message`: Assertion failure message, if any;

The first three fields form a primary key.

When you execute tests externally from our JUnit test runner, it uses the service 
`ExternalRunAs`. The latter just wraps `RunAs` and is provided to allow you to 
hook into tests execution logic if necessary.  

### Java

TwxUnit only adds a couple of annotations to configure connection to ThingWorx and 
substitute standard JUnit's `Test` annotation.

## Effect on ScriptLog

ThingWorx doesn't allow to hide exceptions, always appending them to the Script log
no matter what.

## Building and extending TwxUnit

## Testing TwxUnit

As you would expect, we use TwxUnit to test itself. The corresponding test suite can be 
executed after importing [src/test/twx/TwxUnitTests.xml](). We encourage you to check it
out, since it contains a pretty comprehensive list of TwxUnit's usage examples.

Java part is tested via a JUnit4 test suite in [src/test/java](). Among other things it 
executes `TwxUnitTests` suite, so it requires the latter to be installed in ThingWorx 
running on http://localhost:8080/Thingworx first (thus it is more of an integration 
test, but then all this test runner does is executing ThingWorx services remotely, so 
it barely makes sense to test it separately).

To summarize, in order to run complete test suite you need to:

1. Install ThingWorx (a trial version will do just fine)
2. Check out this Git repository
3. Modify [src/test/resources/twx-connection.properties]() to provide ThingWorx URL and 
access credentials
4. Install TwxUnit on ThingWorx as described above 
5. In ThingWorx Composer import [src/test/twx/TwxUnitTests.xml]() file
6. Run JUnit test suite from project root by executing `mvn test`

The run should take anywhere between 5 and 10 minutes on a typical laptop. To clean up 
just uninstall TwxUnit as described above.

## Internal design

Internally TwxUnit is very simple, just like JUnit. In fact we tried following JUnit 
source code as careful as possible in ThingWorx context, to ensure consistent semantics 
between two frameworks. 

TestRunner finds and executes test cases, asserts throw exceptions.

## Future plans

In no specific order:

- Localizable failure messages
- Assert descriptions