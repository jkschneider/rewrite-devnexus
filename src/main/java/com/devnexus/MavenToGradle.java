package com.devnexus;

import org.openrewrite.*;
import org.openrewrite.gradle.GradleParser;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.maven.tree.Scope;
import org.openrewrite.xml.tree.Xml;

import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class MavenToGradle extends ScanningRecipe<List<Xml.Document>> {

    @Override
    public String getDisplayName() {
        return "Convert Maven to Gradle";
    }

    @Override
    public String getDescription() {
        return "A surefire way to start a fight with your Maven friends. Yeah, you get it...";
    }

    @Override
    public List<Xml.Document> getInitialValue(ExecutionContext ctx) {
        return new ArrayList<>();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(List<Xml.Document> acc) {
        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                acc.add(document);
                return document;
            }
        };
    }

    @Override
    public Collection<? extends SourceFile> generate(List<Xml.Document> acc, ExecutionContext ctx) {
        return acc.stream().flatMap(pom -> {
            Collection<SourceFile> buildGradles = new ArrayList<>();
            new TreeVisitor<Tree, ExecutionContext>() {
                @Override
                public Tree preVisit(Tree tree, ExecutionContext ctx) {
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

                        buildGradles.addAll(GradleParser.builder().build().parse(buildGradle)
                                .map(gradle -> (SourceFile) gradle.withSourcePath(
                                        pom.getSourcePath().resolveSibling("build.gradle")))
                                .collect(Collectors.toList()));
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
            }.visit(pom, ctx);

            return buildGradles.stream();
        }).collect(Collectors.toList());
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(List<Xml.Document> acc) {
        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                //noinspection DataFlowIssue
                return null;
            }
        };
    }
}
