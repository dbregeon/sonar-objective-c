/*
 * Sonar Objective-C Plugin
 * Copyright (C) 2012 François Helg, Cyril Picat and OCTO Technology
 * dev@sonar.codehaus.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.objectivec;

import java.util.Collection;
import java.util.Locale;

import org.slf4j.LoggerFactory;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.checks.AnnotationCheckFactory;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.PersistenceMode;
import org.sonar.api.measures.RangeDistributionBuilder;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.File;
import org.sonar.api.resources.InputFileUtils;
import org.sonar.api.resources.Project;
import org.sonar.api.rules.Violation;
import org.sonar.objectivec.ObjectiveCAstScanner;
import org.sonar.objectivec.ObjectiveCConfiguration;
import org.sonar.objectivec.api.ObjectiveCGrammar;
import org.sonar.objectivec.api.ObjectiveCMetric;
import org.sonar.objectivec.checks.CheckList;
import org.sonar.plugins.objectivec.core.ObjectiveC;
import org.sonar.squid.api.CheckMessage;
import org.sonar.squid.api.SourceCode;
import org.sonar.squid.api.SourceFile;
import org.sonar.squid.api.SourceFunction;
import org.sonar.squid.indexer.QueryByParent;
import org.sonar.squid.indexer.QueryByType;

import com.sonar.sslr.squid.AstScanner;
import com.sonar.sslr.squid.checks.SquidCheck;

public class ObjectiveCSquidSensor implements Sensor {

    private final Number[] FUNCTIONS_DISTRIB_BOTTOM_LIMITS = { 1, 2, 4, 6, 8,
            10, 12, 20, 30 };
    private final Number[] FILES_DISTRIB_BOTTOM_LIMITS = { 0, 5, 10, 20, 30,
            60, 90 };

    private final AnnotationCheckFactory annotationCheckFactory;

    private Project project;
    private SensorContext context;
    private AstScanner<ObjectiveCGrammar> scanner;

    public ObjectiveCSquidSensor(final RulesProfile profile) {
        this.annotationCheckFactory = AnnotationCheckFactory.create(profile,
                CheckList.REPOSITORY_KEY, CheckList.getChecks());
    }

    public boolean shouldExecuteOnProject(final Project project) {
        return ObjectiveC.KEY.equals(project.getLanguageKey());
    }

    public void analyse(final Project project, final SensorContext context) {
        this.project = project;
        this.context = context;

        final Collection<SquidCheck> squidChecks = annotationCheckFactory.getChecks();
        this.scanner = ObjectiveCAstScanner.create(
                createConfiguration(project),
                squidChecks.toArray(new SquidCheck[squidChecks.size()]));
        scanner.scanFiles(InputFileUtils.toFiles(project.getFileSystem()
                .mainFiles(ObjectiveC.KEY)));

        final Collection<SourceCode> squidSourceFiles = scanner.getIndex().search(
                new QueryByType(SourceFile.class));
        save(squidSourceFiles);
    }

    private ObjectiveCConfiguration createConfiguration(final Project project) {
        return new ObjectiveCConfiguration(project.getFileSystem()
                .getSourceCharset());
    }

    private void save(final Collection<SourceCode> squidSourceFiles) {
        for (final SourceCode squidSourceFile : squidSourceFiles) {
            final SourceFile squidFile = (SourceFile) squidSourceFile;

            final File sonarFile = File.fromIOFile(
                    new java.io.File(squidFile.getKey()), project);

            saveFilesComplexityDistribution(sonarFile, squidFile);
            saveFunctionsComplexityDistribution(sonarFile, squidFile);
            saveMeasures(sonarFile, squidFile);
            saveViolations(sonarFile, squidFile);
        }
    }

    private void saveMeasures(final File sonarFile, final SourceFile squidFile) {
        context.saveMeasure(sonarFile, CoreMetrics.FILES,
                squidFile.getDouble(ObjectiveCMetric.FILES));
        context.saveMeasure(sonarFile, CoreMetrics.LINES,
                squidFile.getDouble(ObjectiveCMetric.LINES));
        context.saveMeasure(sonarFile, CoreMetrics.NCLOC,
                squidFile.getDouble(ObjectiveCMetric.LINES_OF_CODE));
        context.saveMeasure(sonarFile, CoreMetrics.FUNCTIONS,
                squidFile.getDouble(ObjectiveCMetric.FUNCTIONS));
        context.saveMeasure(sonarFile, CoreMetrics.STATEMENTS,
                squidFile.getDouble(ObjectiveCMetric.STATEMENTS));
        context.saveMeasure(sonarFile, CoreMetrics.CLASSES,
                squidFile.getDouble(ObjectiveCMetric.CLASSES));
        context.saveMeasure(sonarFile, CoreMetrics.COMPLEXITY,
                squidFile.getDouble(ObjectiveCMetric.COMPLEXITY));
        context.saveMeasure(sonarFile, CoreMetrics.COMMENT_BLANK_LINES,
                squidFile.getDouble(ObjectiveCMetric.COMMENT_BLANK_LINES));
        context.saveMeasure(sonarFile, CoreMetrics.COMMENT_LINES,
                squidFile.getDouble(ObjectiveCMetric.COMMENT_LINES));
    }

    private void saveFunctionsComplexityDistribution(final File sonarFile,
            final SourceFile squidFile) {
        final Collection<SourceCode> squidFunctionsInFile = scanner.getIndex()
                .search(new QueryByParent(squidFile),
                        new QueryByType(SourceFunction.class));
        final RangeDistributionBuilder complexityDistribution = new RangeDistributionBuilder(
                CoreMetrics.FUNCTION_COMPLEXITY_DISTRIBUTION,
                FUNCTIONS_DISTRIB_BOTTOM_LIMITS);
        LoggerFactory.getLogger(getClass()).info("For file " + sonarFile + ":");
        for (final SourceCode squidFunction : squidFunctionsInFile) {
            LoggerFactory.getLogger(getClass()).info("Complexity of " + squidFunction.getName() + " is " + squidFunction
                    .getDouble(ObjectiveCMetric.COMPLEXITY));
            complexityDistribution.add(squidFunction
                    .getDouble(ObjectiveCMetric.COMPLEXITY));
        }
        context.saveMeasure(sonarFile, complexityDistribution.build()
                .setPersistenceMode(PersistenceMode.MEMORY));
    }

    private void saveFilesComplexityDistribution(final File sonarFile,
            final SourceFile squidFile) {
        final RangeDistributionBuilder complexityDistribution = new RangeDistributionBuilder(
                CoreMetrics.FILE_COMPLEXITY_DISTRIBUTION,
                FILES_DISTRIB_BOTTOM_LIMITS);
        complexityDistribution.add(squidFile
                .getDouble(ObjectiveCMetric.COMPLEXITY));
        context.saveMeasure(sonarFile, complexityDistribution.build()
                .setPersistenceMode(PersistenceMode.MEMORY));
    }

    private void saveViolations(final File sonarFile, final SourceFile squidFile) {
        final Collection<CheckMessage> messages = squidFile.getCheckMessages();
        if (messages != null) {
            for (final CheckMessage message : messages) {
                final Violation violation = Violation
                        .create(annotationCheckFactory.getActiveRule(message
                                .getChecker()), sonarFile)
                        .setLineId(message.getLine())
                        .setMessage(message.getText(Locale.ENGLISH));
                context.saveViolation(violation);
            }
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

}
