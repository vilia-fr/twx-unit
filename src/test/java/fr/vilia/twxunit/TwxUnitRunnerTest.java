package fr.vilia.twxunit;

@TwxSuite(
    url = "http://localhost:8084/Thingworx",
    introspectionAppKey = "0d854d92-3e50-4a7a-bcc4-bce9d64f2939"
)
public class TwxUnitRunnerTest extends TwxUnitTest {

    @TwxTest( appKey = "bb30a13e-a322-4fe5-854c-6c506f9e82e4" )
    public void TestOne() { }

}
