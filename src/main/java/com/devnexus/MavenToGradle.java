package com.devnexus;

import org.openrewrite.*;
import org.openrewrite.gradle.GradleParser;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.maven.tree.Scope;
import org.openrewrite.xml.tree.Xml;

import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class MavenToGradle extends ScanningRecipe<List<String>> {

    @Override
    public String getDisplayName() {
        return "Convert Maven to Gradle";
    }

    @Override
    public String getDescription() {
        return "A surefire way to start a fight with your Maven friends. Yeah, you get it...";
    }

    @Override
    public List<String> getInitialValue(ExecutionContext ctx) {
        return new ArrayList<>();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(List<String> acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree preVisit(Tree tree, ExecutionContext ctx) {
                MavenResolutionResult mrr = tree.getMarkers().findFirst(MavenResolutionResult.class).orElse(null);
                if (mrr != null) {
                    String dependencies = collectDependencies(mrr);

                    String buildGradle = String.format(
                            "plugins {\n" +
                            "    id 'java'\n" +
                            "}\n" +
                            "\n" +
                            "repositories {\n" +
                            "    mavenCentral()\n" +
                            "}\n" +
                            "\n" +
                            "dependencies {\n" +
                            "%s\n" +
                            "}\n", dependencies);

                    acc.add(buildGradle);
                }

                return tree;
            }

            private String collectDependencies(MavenResolutionResult mrr) {
                Map<String, Set<Scope>> depsByScope = new LinkedHashMap<>();

                for (Map.Entry<Scope, List<ResolvedDependency>> scopeDependencies : mrr.getDependencies().entrySet()) {
                    for (ResolvedDependency dependency : scopeDependencies.getValue()) {
                        if (dependency.getDepth() != 0) {
                            continue;
                        }

                        depsByScope.computeIfAbsent(dependency.getGav().toString(), s -> new LinkedHashSet<>())
                                .add(scopeDependencies.getKey());
                    }
                }

                StringJoiner dependencies = new StringJoiner("\n");
                for (Map.Entry<String, Set<Scope>> dep : depsByScope.entrySet()) {
                    String conf = "implementation";
                    if (dep.getValue().contains(Scope.Compile)) {
                        conf = dep.getValue().contains(Scope.Runtime) ? "implementation" : "compileOnly";
                    } else if (dep.getValue().contains(Scope.Runtime)) {
                        conf = "runtimeOnly";
                    } else if (dep.getValue().contains(Scope.Test)) {
                        conf = "testImplementation";
                    }
                    dependencies.add("    " + conf + " '" + dep.getKey() + "'");
                }

                return dependencies.toString();
            }
        };
    }

    @Override
    public Collection<? extends SourceFile> generate(List<String> acc, ExecutionContext ctx) {
        return GradleParser.builder().build().parse(acc.toArray(new String[0]))
                .map(gradle -> (SourceFile) gradle.withSourcePath(Paths.get("build.gradle")))
                .collect(Collectors.toList());
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(List<String> acc) {
        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                //noinspection DataFlowIssue
                return null;
            }
        };
    }
}
