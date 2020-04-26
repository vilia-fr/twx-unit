package fr.vilia.twxunit;

import com.thingworx.metadata.annotations.ThingworxPropertyDefinition;
import com.thingworx.metadata.annotations.ThingworxPropertyDefinitions;

@ThingworxPropertyDefinitions(properties = {
    @ThingworxPropertyDefinition(
        name = "remoteThings",
        baseType = "INFOTABLE",
        aspects = {"dataShape:EmulatedRemoteThing", "isPersistent:true"}
    )
})
public class HasRemoteThings {

}
