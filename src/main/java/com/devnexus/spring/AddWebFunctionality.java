package com.devnexus.spring;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.maven.AddManagedDependency;
import org.openrewrite.maven.AddProperty;
import org.openrewrite.maven.RemoveRedundantDependencyVersions;
import org.openrewrite.maven.UpgradeDependencyVersion;
import org.openrewrite.maven.tree.GroupArtifact;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class AddWebFunctionality extends InitSpringFeature {

    public AddWebFunctionality() {
        super(new GroupArtifact("org.springframework.boot", "spring-boot-starter-web"));
    }

    @Override
    public String getDisplayName() {
        return "Add Spring web functionality";
    }

    @Override
    public String getDescription() {
        return "Incrementally add Spring web functionality to an existing Spring project.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return Arrays.asList(
                addDependency("org.springframework.boot", "spring-boot-starter-web", "3.x"),
                addDependency("org.springframework", "spring-webmvc", "5.x"),
                new AddProperty("spring-cloud.version", "2023.0.0", true, false),
                new AddManagedDependency("org.springframework.cloud", "spring-cloud-dependencies", "${spring-cloud.version}",
                        null, null, null, null, null, null, null),
                // TODO are these not managed by the spring cloud bom?
                addDependency("org.springframework.cloud", "spring-cloud-function-context", "4.x"),
                addDependency("org.springframework.cloud", "spring-cloud-function-web", "4.x"),
                new RemoveRedundantDependencyVersions("*", "*", false, null),
                new UpgradeDependencyVersion("org.springframework.cloud", "spring-cloud-dependencies", "2023.x", null, true, null)
        );
    }

    @Override
    public Collection<? extends SourceFile> generate(InitSpringFeature.ProjectState state, ExecutionContext ctx) {
        if (!state.isHasFeature()) {
            return generateFromResource(state, ctx, "/spring/add-web/WebFunctionTests.java", SourceSet.TEST)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
