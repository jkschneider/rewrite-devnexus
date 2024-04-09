package com.devnexus;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

public class LoggingExceptionConcatenationTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new LoggingExceptionConcatenationRecipe())
          .parser(JavaParser.fromJavaVersion().classpath("slf4j-api"));
    }

    @Test
    void loggingException() {
        rewriteRun(
          //language=java
          java(
            """
              import org.slf4j.Logger;
                            
              class Test {
                  void test(Logger l) {
                      try {
                      } catch(Exception e) {
                          l.error(("Log me " +
                                  "another ") + e);
                      }
                  }
              }
              """,
            """
              import org.slf4j.Logger;
                            
              class Test {
                  void test(Logger l) {
                      try {
                      } catch(Exception e) {
                          l.error("Log me " +
                                  "another ", e);
                      }
                  }
              }
              """
          )
        );
    }
}
