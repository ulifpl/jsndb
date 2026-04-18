package org.jsndb.annotations;

import static java.lang.annotation.ElementType.*;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import static java.lang.annotation.RetentionPolicy.*;

@Target(FIELD)
@Retention(RUNTIME)
public @interface jsndbObjectId {

}
