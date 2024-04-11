package com.devnexus.spring;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.intellij.lang.annotations.Language;
import org.openrewrite.*;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.search.FindAnnotations;
import org.openrewrite.java.tree.J;
import org.openrewrite.maven.AddDependency;
import org.openrewrite.maven.AddPlugin;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.xml.tree.Xml;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

/**
 * Some functionality that will be common to adding new Spring features to an
 * existing Spring project.
 */
public abstract class InitSpringFeature extends ScanningRecipe<InitSpringFeature.ProjectState> {
    private final GroupArtifact featureDependency;

    protected InitSpringFeature(GroupArtifact featureDependency) {
        this.featureDependency = featureDependency;
    }

    @Override
    public InitSpringFeature.ProjectState getInitialValue(ExecutionContext ctx) {
        return new InitSpringFeature.ProjectState(false, null);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(InitSpringFeature.ProjectState state) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                Tree t = tree;
                t = new MavenIsoVisitor<ExecutionContext>() {
                    @Override
                    public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                        state.setHasFeature(getResolutionResult().findDependencies(featureDependency.getGroupId(),
                                featureDependency.getArtifactId(), null).isEmpty());
                        return document;
                    }
                }.visit(t, ctx);

                t = new JavaIsoVisitor<ExecutionContext>() {
                    @Override
                    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                        if (!FindAnnotations.find(classDecl, "@org.springframework.boot.autoconfigure.SpringBootApplication").isEmpty()) {
                            state.setApplicationMainClass(getCursor().firstEnclosingOrThrow(J.CompilationUnit.class));
                        }
                        return super.visitClassDeclaration(classDecl, ctx);
                    }
                }.visit(t, ctx);

                return t;
            }
        };
    }

    protected AddDependency addDependency(String groupId, String artifactId, String version) {
        return new AddDependency(groupId, artifactId, version, null, null, null,
                null, null, null, null, null, null);
    }

    protected AddPlugin addPlugin(String groupId, String artifactId, @Language("xml") @Nullable String configuration) {
        return new AddPlugin(groupId, artifactId, null, configuration,
                null, null, null);
    }

    protected Stream<SourceFile> generateFromResource(ProjectState state, ExecutionContext ctx,
                                                      String resource, SourceSet sourceSet) {
        return JavaParser.fromJavaVersion().classpath(JavaParser.runtimeClasspath()).build()
                .parse(ctx, StringUtils.readFully(AddWebFunctionality.class.getResourceAsStream(resource)))
                .map(sourceFile -> {
                    J.Package pkg = state.getApplicationMainClass().getPackageDeclaration();
                    Path path = Paths.get("src/" + sourceSet.name().toLowerCase() + "/java")
                            .resolve(pkg == null ? "" : pkg.getPackageName().replace('.', '/'))
                            .resolve(resource.substring(resource.lastIndexOf('/') + 1));
                    return (SourceFile) new JavaIsoVisitor<ExecutionContext>() {
                        @Override
                        public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
                            return autoFormat(cu
                                            .withPackageDeclaration(pkg)
                                            // add the style of the application main class to the generated file
                                            .withMarkers(state.getApplicationMainClass().getMarkers())
                                            .withSourcePath(path),
                                    ctx
                            );
                        }
                    }.visit(sourceFile, ctx);
                });
    }

    @Data
    @AllArgsConstructor
    public static class ProjectState {
        boolean hasFeature;
        J.CompilationUnit applicationMainClass;
    }

    public enum SourceSet {
        MAIN,
        TEST;
    }
}
