package com.felipefzdz.gradle.shellcheck;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.file.RegularFile;
import org.gradle.api.internal.ConventionMapping;
import org.gradle.api.internal.IConventionAware;
import org.gradle.api.plugins.ReportingBasePlugin;
import org.gradle.api.plugins.quality.Checkstyle;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.reporting.ReportingExtension;

import java.io.File;
import java.util.concurrent.Callable;

import static org.gradle.api.internal.lambdas.SerializableLambdas.action;

public class ShellcheckPlugin implements Plugin<Project> {

    protected ShellcheckExtension extension;

    public void apply(Project project) {
        project.getPluginManager().apply(ReportingBasePlugin.class);
        extension = project.getExtensions().create("shellcheck", ShellcheckExtension.class, project);
        project.getTasks().register("shellcheck", Shellcheck.class);
        project.getTasks().withType(Shellcheck.class).configureEach(task -> configureTask(task, project));
    }

    private void configureTask(Shellcheck task, Project project) {
        configureTaskConventionMapping(task, project);
        configureReportsConventionMapping(task, project);
    }

    private void configureTaskConventionMapping(Shellcheck task, Project project) {
        ConventionMapping taskMapping = task.getConventionMapping();
        taskMapping.map("shellScripts", (Callable<File>) () -> extension.getShellScripts());
        taskMapping.map("ignoreFailures", (Callable<Boolean>) () -> extension.isIgnoreFailures());
        final ConventionMapping extensionMapping = conventionMappingOf(extension);
        extensionMapping.map("reportsDir", new Callable<File>() {
            @Override
            public File call() {
                return project.getExtensions().getByType(ReportingExtension.class).file("shellcheck");
            }
        });
    }

    private void configureReportsConventionMapping(Shellcheck task, Project project) {
        ProjectLayout layout = project.getLayout();
        ProviderFactory providers = project.getProviders();
        Provider<RegularFile> reportsDir = layout.file(providers.provider(() -> extension.getReportsDir()));
        task.getReports().all(action(report -> {
            report.getRequired().convention(true);
            report.getOutputLocation().convention(
                layout.getProjectDirectory().file(providers.provider(() -> {
                    String reportFileName = "shellcheck." + report.getName();
                    return new File(reportsDir.get().getAsFile(), reportFileName).getAbsolutePath();
                }))
            );
        }));
    }

    protected static ConventionMapping conventionMappingOf(Object object) {
        return ((IConventionAware) object).getConventionMapping();
    }
}
