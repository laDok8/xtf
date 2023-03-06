package cz.xtf.junit5.annotations;

import cz.xtf.junit5.extensions.KnownIssueHandler;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.extension.ExtendWith;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@ExtendWith(KnownIssueHandler.class)
public @interface KnownIssue {
  String value();
}
