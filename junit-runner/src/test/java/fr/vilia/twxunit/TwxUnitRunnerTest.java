package fr.vilia.twxunit;

public class TwxUnitRunnerTest extends TwxUnitTest {
    public TwxUnitRunnerTest() {
        super(
                "ws://localhost:8080/Thingworx/WS",
                "85f86ee9-c829-43e5-bbf3-3bee659f9983",
                false,
                "TestRoot",
                null
        );
    }
}
