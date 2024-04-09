package com.devnexus;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

public class ChangeLiteralTo42Test implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ChangeLiteralTo42());
    }

    @Test
    void changeLiteral() {
        //language=java
        rewriteRun(
          java(
            """
              class Test {
                  void test() {
                      int n = 40;
                  }
              }
              """,
            """
              class Test {
                  void test() {
                      int n = 42;
                  }
              }
              """
          )
        );
    }
}
