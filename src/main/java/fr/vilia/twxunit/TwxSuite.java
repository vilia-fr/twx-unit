package fr.vilia.twxunit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface TwxSuite {

    String url();

    String introspectionAppKey() default "";
    String introspectionUser() default "";
    String introspectionPassword() default "";

    String defaultAppKey() default "";
    String defaultUser() default "";
    String defaultPassword() default "";

    int defaultConnectionTimeout() default 5000;
    boolean defaultIgnoreSslErrors() default false;

}
