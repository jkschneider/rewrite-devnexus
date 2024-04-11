package com.devnexus.spring;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.*;
import static org.openrewrite.maven.Assertions.pomXml;

public class AddWebFunctionalityTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new AddWebFunctionality())
          .parser(JavaParser.fromJavaVersion().classpath(JavaParser.runtimeClasspath()))
          .cycles(1).expectedCyclesThatMakeChanges(1);
    }

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Test
    void addWeb() {
        rewriteRun(
          //language=xml
          pomXml(
            """
              <project>
                  <parent>
                      <groupId>org.springframework.boot</groupId>
                      <artifactId>spring-boot-starter-parent</artifactId>
                      <version>3.2.4</version>
                      <relativePath/> <!-- lookup parent from repository -->
                  </parent>
                  <groupId>org.devnexus</groupId>
                  <artifactId>day-one-app</artifactId>
                  <version>0.0.1-SNAPSHOT</version>
                  <name>day-one-app</name>
                  <description>Demo project for Spring Boot</description>
                  <properties>
                      <java.version>17</java.version>
                  </properties>
                  <dependencies>
                      <dependency>
                          <groupId>org.springframework.boot</groupId>
                          <artifactId>spring-boot-starter</artifactId>
                      </dependency>
                      <dependency>
                          <groupId>org.springframework.boot</groupId>
                          <artifactId>spring-boot-starter-test</artifactId>
                          <scope>test</scope>
                      </dependency>
                  </dependencies>
                  <build>
                      <plugins>
                          <plugin>
                              <groupId>org.springframework.boot</groupId>
                              <artifactId>spring-boot-maven-plugin</artifactId>
                          </plugin>
                      </plugins>
                  </build>
              </project>
              """,
            """
              <project>
                  <parent>
                      <groupId>org.springframework.boot</groupId>
                      <artifactId>spring-boot-starter-parent</artifactId>
                      <version>3.2.4</version>
                      <relativePath/>
                      <!-- lookup parent from repository -->
                  </parent>
                  <groupId>org.devnexus</groupId>
                  <artifactId>day-one-app</artifactId>
                  <version>0.0.1-SNAPSHOT</version>
                  <name>day-one-app</name>
                  <description>Demo project for Spring Boot</description>
                  <properties>
                      <java.version>17</java.version>
                      <spring-cloud.version>2023.0.1</spring-cloud.version>
                  </properties>
                  <dependencyManagement>
                      <dependencies>
                          <dependency>
                              <groupId>org.springframework.cloud</groupId>
                              <artifactId>spring-cloud-dependencies</artifactId>
                              <version>${spring-cloud.version}</version>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
                  <dependencies>
                      <dependency>
                          <groupId>org.springframework</groupId>
                          <artifactId>spring-webmvc</artifactId>
                      </dependency>
                      <dependency>
                          <groupId>org.springframework.boot</groupId>
                          <artifactId>spring-boot-starter</artifactId>
                      </dependency>
                      <dependency>
                          <groupId>org.springframework.boot</groupId>
                          <artifactId>spring-boot-starter-web</artifactId>
                      </dependency>
                      <dependency>
                          <groupId>org.springframework.cloud</groupId>
                          <artifactId>spring-cloud-function-context</artifactId>
                          <version>4.1.1</version>
                      </dependency>
                      <dependency>
                          <groupId>org.springframework.cloud</groupId>
                          <artifactId>spring-cloud-function-web</artifactId>
                          <version>4.1.1</version>
                      </dependency>
                      <dependency>
                          <groupId>org.springframework.boot</groupId>
                          <artifactId>spring-boot-starter-test</artifactId>
                          <scope>test</scope>
                      </dependency>
                  </dependencies>
                  <build>
                      <plugins>
                          <plugin>
                              <groupId>org.springframework.boot</groupId>
                              <artifactId>spring-boot-maven-plugin</artifactId>
                          </plugin>
                      </plugins>
                  </build>
              </project>
              """
          ),
          //language=java
          srcMainJava(
            java(
              """
                package org.devnexus;

                import org.springframework.boot.SpringApplication;
                import org.springframework.boot.autoconfigure.SpringBootApplication;

                @SpringBootApplication
                public class Application {
                    public static void main(String[] args) {
                        SpringApplication.run(Application.class, args);
                    }
                }
                """
            )
          ),
          srcTestJava(
            java(
              null,
              //language=java
              """
                package org.devnexus;

                import static org.assertj.core.api.Assertions.assertThat;

                import java.net.URI;

                import org.junit.jupiter.api.Test;
                import org.springframework.beans.factory.annotation.Autowired;
                import org.springframework.boot.test.context.SpringBootTest;
                import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
                import org.springframework.boot.test.web.client.TestRestTemplate;
                import org.springframework.http.HttpStatus;
                import org.springframework.http.RequestEntity;
                import org.springframework.http.ResponseEntity;

                @SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = "spring.main.web-application-type=servlet")
                class WebFunctionTests {

                    @Autowired
                    private TestRestTemplate resetTemplate;

                    /*
                     * This test assumes you have a function of the following signature 'Function<String, String> test()'
                     * which uppercases a String. The main point of this test is that a function is invoked and tested as an HTTP endpoint
                     * using Spring Cloud Function, so please modify the test and assertion to fit your actual function.
                     */
                    @Test
                    void validateFunctionViaWeb() throws Exception {
                        ResponseEntity<String> result = this.resetTemplate.exchange(
                                RequestEntity.post(new URI("/uppercase")).body("hello devnexus"), String.class);
                        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
                        System.out.println(result.getBody());
                        assertThat(result.getBody()).isEqualTo("HELLO DEVNEXUS");
                    }
                }
                """,
              spec -> spec.path("org/devnexus/WebFunctionTests.java")
            )
          )
        );
    }
}
