package fr.vilia.twxunit;

import com.thingworx.metadata.annotations.ThingworxPropertyDefinition;
import com.thingworx.metadata.annotations.ThingworxPropertyDefinitions;
import com.thingworx.metadata.annotations.ThingworxServiceDefinition;
import com.thingworx.metadata.annotations.ThingworxServiceResult;

@ThingworxPropertyDefinitions(properties = {
    @ThingworxPropertyDefinition(
        name = "testSuite",
        baseType = "INFOTABLE",
        aspects = {"dataShape:TestDefinition", "isPersistent:true"}
    )
})
public class HasTestSuite {

    @ThingworxServiceDefinition(name = "Before", isAllowOverride = true, description = "Executes automatically before any test case in this test suite, should be overridden")
    @ThingworxServiceResult(name = "Result", description = "", baseType = "NOTHING")
    public void Before() {
    }

    @ThingworxServiceDefinition(name = "After", isAllowOverride = true, description = "Executes automatically after all test cases in this test suite, should be overridden")
    @ThingworxServiceResult(name = "Result", description = "", baseType = "NOTHING")
    public void After() {
    }

}
