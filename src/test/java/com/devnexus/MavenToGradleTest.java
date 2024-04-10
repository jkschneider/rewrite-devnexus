package com.devnexus;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.java.Assertions.mavenProject;
import static org.openrewrite.maven.Assertions.pomXml;

public class MavenToGradleTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MavenToGradle());
    }

    @Test
    void mavenToGradle() {
        rewriteRun(
          mavenProject(
            "core",
            //language=xml
            pomXml(
              """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.devnexus</groupId>
                    <artifactId>devnexus</artifactId>
                    <version>1.0-SNAPSHOT</version>
                    <dependencies>
                        <dependency>
                            <groupId>org.springframework.boot</groupId>
                            <artifactId>spring-boot-starter-web</artifactId>
                            <version>2.5.4</version>
                        </dependency>
                    </dependencies>
                </project>
                """,
              (String) null
            ),
            //language=groovy
            buildGradle(
              null,
              """
                plugins {
                    id 'java'
                }
                              
                repositories {
                    mavenCentral()
                }
                              
                dependencies {
                    implementation 'org.springframework.boot:spring-boot-starter-web:2.5.4'
                }
                """,
              spec -> spec.afterRecipe(cu -> {
                  assertThat(cu.getSourcePath()).isEqualTo(Paths.get("core/build.gradle"));
              })
            )
          )
        );
    }
}
