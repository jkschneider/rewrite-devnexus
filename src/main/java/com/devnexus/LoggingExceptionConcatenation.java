package com.devnexus;

import com.google.errorprone.refaster.annotation.AfterTemplate;
import com.google.errorprone.refaster.annotation.BeforeTemplate;
import org.openrewrite.java.template.RecipeDescriptor;
import org.slf4j.Logger;

@RecipeDescriptor(name = "Exceptions should be arguments to logging calls",
        description = "Don't concatenate exceptions to logging strings.")
public class LoggingExceptionConcatenation {

    @BeforeTemplate
    void before(Logger logger, String msg, Exception e) {
        logger.error(msg + e);
    }

    @AfterTemplate
    void after(Logger logger, String msg, Exception e) {
        logger.error(msg, e);
    }
}
