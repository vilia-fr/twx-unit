package fr.vilia.twxunit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface TwxTest {

    String appKey() default "";
    String user() default "";
    String password() default "";

    int connectionTimeout() default 5000;
    boolean ignoreSslErrors() default false;

}
