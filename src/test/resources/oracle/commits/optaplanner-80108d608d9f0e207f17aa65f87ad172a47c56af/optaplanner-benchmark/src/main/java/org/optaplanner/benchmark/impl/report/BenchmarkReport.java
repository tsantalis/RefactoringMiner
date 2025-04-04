/*
 * Copyright 2011 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.optaplanner.benchmark.impl.report;

import java.awt.BasicStroke;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeSet;
import javax.imageio.ImageIO;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.apache.commons.io.IOUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.ItemLabelAnchor;
import org.jfree.chart.labels.ItemLabelPosition;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.TextAnchor;
import org.optaplanner.benchmark.impl.ranking.SolverRankingWeightFactory;
import org.optaplanner.benchmark.impl.result.PlannerBenchmarkResult;
import org.optaplanner.benchmark.impl.result.ProblemBenchmarkResult;
import org.optaplanner.benchmark.impl.result.SingleBenchmarkResult;
import org.optaplanner.benchmark.impl.result.SolverBenchmarkResult;
import org.optaplanner.benchmark.impl.statistic.ProblemStatistic;
import org.optaplanner.benchmark.impl.statistic.PureSingleStatistic;
import org.optaplanner.benchmark.impl.statistic.SingleStatistic;
import org.optaplanner.benchmark.impl.statistic.common.MillisecondsSpentNumberFormat;
import org.optaplanner.core.impl.score.ScoreUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BenchmarkReport {

    protected final transient Logger logger = LoggerFactory.getLogger(getClass());

    public static final int CHARTED_SCORE_LEVEL_SIZE = 15;
    public static final int LOG_SCALE_MIN_DATASETS_COUNT = 5;

    private final PlannerBenchmarkResult plannerBenchmarkResult;

    private Locale locale = null;
    private Comparator<SolverBenchmarkResult> solverRankingComparator = null;
    private SolverRankingWeightFactory solverRankingWeightFactory = null;
    private File summaryDirectory = null;
    private List<File> bestScoreSummaryChartFileList = null;
    private List<File> bestScoreScalabilitySummaryChartFileList = null;
    private List<File> winningScoreDifferenceSummaryChartFileList = null;
    private List<File> worstScoreDifferencePercentageSummaryChartFileList = null;
    private File averageCalculateCountSummaryChartFile = null;
    private File timeSpentSummaryChartFile = null;
    private File timeSpentScalabilitySummaryChartFile = null;
    private List<File> bestScorePerTimeSpentSummaryChartFileList = null;

    private Integer defaultShownScoreLevelIndex = null;
    private List<String> warningList = null;

    private File htmlOverviewFile = null;

    public BenchmarkReport(PlannerBenchmarkResult plannerBenchmarkResult) {
        this.plannerBenchmarkResult = plannerBenchmarkResult;
    }

    public PlannerBenchmarkResult getPlannerBenchmarkResult() {
        return plannerBenchmarkResult;
    }

    public Locale getLocale() {
        return locale;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    public Comparator<SolverBenchmarkResult> getSolverRankingComparator() {
        return solverRankingComparator;
    }

    public void setSolverRankingComparator(Comparator<SolverBenchmarkResult> solverRankingComparator) {
        this.solverRankingComparator = solverRankingComparator;
    }

    public SolverRankingWeightFactory getSolverRankingWeightFactory() {
        return solverRankingWeightFactory;
    }

    public void setSolverRankingWeightFactory(SolverRankingWeightFactory solverRankingWeightFactory) {
        this.solverRankingWeightFactory = solverRankingWeightFactory;
    }

    public File getSummaryDirectory() {
        return summaryDirectory;
    }

    public List<File> getBestScoreSummaryChartFileList() {
        return bestScoreSummaryChartFileList;
    }

    public List<File> getBestScoreScalabilitySummaryChartFileList() {
        return bestScoreScalabilitySummaryChartFileList;
    }

    public List<File> getWinningScoreDifferenceSummaryChartFileList() {
        return winningScoreDifferenceSummaryChartFileList;
    }

    public List<File> getWorstScoreDifferencePercentageSummaryChartFileList() {
        return worstScoreDifferencePercentageSummaryChartFileList;
    }

    public File getAverageCalculateCountSummaryChartFile() {
        return averageCalculateCountSummaryChartFile;
    }

    public File getTimeSpentSummaryChartFile() {
        return timeSpentSummaryChartFile;
    }

    public File getTimeSpentScalabilitySummaryChartFile() {
        return timeSpentScalabilitySummaryChartFile;
    }

    public List<File> getBestScorePerTimeSpentSummaryChartFileList() {
        return bestScorePerTimeSpentSummaryChartFileList;
    }

    public Integer getDefaultShownScoreLevelIndex() {
        return defaultShownScoreLevelIndex;
    }

    public List<String> getWarningList() {
        return warningList;
    }

    public File getHtmlOverviewFile() {
        return htmlOverviewFile;
    }

    // ************************************************************************
    // Smart getters
    // ************************************************************************

    public String getRelativePathToBenchmarkReportDirectory(File file) {
        String benchmarkReportDirectoryPath = plannerBenchmarkResult.getBenchmarkReportDirectory().getAbsoluteFile().toURI().getPath();
        String filePath = file.getAbsoluteFile().toURI().getPath();
        if (!filePath.startsWith(benchmarkReportDirectoryPath)) {
            throw new IllegalArgumentException("The filePath (" + filePath
                    + ") does not start with the benchmarkReportDirectoryPath (" + benchmarkReportDirectoryPath + ").");
        }
        String relativePath = filePath.substring(benchmarkReportDirectoryPath.length());
        if (relativePath.startsWith("/")) {
            relativePath = relativePath.substring(1);
        }
        return relativePath;
    }

    public String getSolverRankingClassSimpleName() {
        Class solverRankingClass = getSolverRankingClass();
        return solverRankingClass == null ? null : solverRankingClass.getSimpleName();
    }

    public String getSolverRankingClassFullName() {
        Class solverRankingClass = getSolverRankingClass();
        return solverRankingClass == null ? null : solverRankingClass.getName();
    }

    // ************************************************************************
    // Write methods
    // ************************************************************************

    public void writeReport() {
        logger.info("Generating benchmarking report...");
        summaryDirectory = new File(plannerBenchmarkResult.getBenchmarkReportDirectory(), "summary");
        summaryDirectory.mkdir();
        plannerBenchmarkResult.accumulateResults(this);
        fillWarningList();
        writeBestScoreSummaryCharts();
        writeBestScoreScalabilitySummaryChart();
        writeWinningScoreDifferenceSummaryChart();
        writeWorstScoreDifferencePercentageSummaryChart();
        writeAverageCalculateCountPerSecondSummaryChart();
        writeTimeSpentSummaryChart();
        writeTimeSpentScalabilitySummaryChart();
        writeBestScorePerTimeSpentSummaryChart();
        for (ProblemBenchmarkResult problemBenchmarkResult : plannerBenchmarkResult.getUnifiedProblemBenchmarkResultList()) {
            if (problemBenchmarkResult.hasAnySuccess()) {
                for (ProblemStatistic problemStatistic : problemBenchmarkResult.getProblemStatisticList()) {
                    for (SingleStatistic singleStatistic : problemStatistic.getSingleStatisticList()) {
                        try {
                            singleStatistic.unhibernatePointList();
                        } catch (IllegalStateException e) {
                            if (!plannerBenchmarkResult.getAggregation()) {
                                throw new IllegalStateException("Failed to unhibernate point list of SingleStatistic ( "
                                        + singleStatistic + " ) of ProblemStatistic ( " + problemStatistic + " ).", e);
                            }
                            logger.trace("This is expected, aggregator doesn't copy CSV files. Could not read CSV file "
                                    + "( {} ) of single statistic ( {} ).", singleStatistic.getCsvFile().getAbsolutePath(), singleStatistic);
                        }
                    }
                    problemStatistic.writeGraphFiles(this);
                    for (SingleStatistic singleStatistic : problemStatistic.getSingleStatisticList()) {
                        if (plannerBenchmarkResult.getAggregation()) {
                            singleStatistic.setPointList(null);
                        } else {
                            singleStatistic.hibernatePointList();
                        }
                    }
                }
                for (SingleBenchmarkResult singleBenchmarkResult : problemBenchmarkResult.getSingleBenchmarkResultList()) {
                    if (singleBenchmarkResult.isSuccess()) {
                        for (PureSingleStatistic pureSingleStatistic : singleBenchmarkResult.getPureSingleStatisticList()) {
                            try {
                                pureSingleStatistic.unhibernatePointList();
                            } catch (IllegalStateException e) {
                                if (!plannerBenchmarkResult.getAggregation()) {
                                    throw new IllegalStateException("Failed to unhibernate point list of "
                                            + "PureSingleStatistic ( " + pureSingleStatistic + " ).", e);
                                }
                                logger.trace("This is expected, aggregator doesn't copy CSV files. Could not read CSV file "
                                        + "( {} ) of pure single statistic ( {} ).", pureSingleStatistic.getCsvFile().getAbsolutePath(), pureSingleStatistic);
                            }
                            pureSingleStatistic.writeGraphFiles(this);
                            if (plannerBenchmarkResult.getAggregation()) {
                                pureSingleStatistic.setPointList(null);
                            } else {
                                pureSingleStatistic.hibernatePointList();
                            }
                        }
                    }
                }
            }
        }
        determineDefaultShownScoreLevelIndex();
        writeHtmlOverviewFile();
    }

    protected void fillWarningList() {
        warningList = new ArrayList<String>();
        String javaVmName = System.getProperty("java.vm.name");
        if (javaVmName != null && javaVmName.contains("Client VM")) {
            warningList.add("The Java VM (" + javaVmName + ") is the Client VM."
                    + " Consider starting the java process with the argument \"-server\" to get better results.");
        }
        if (plannerBenchmarkResult.getParallelBenchmarkCount() != null
                && plannerBenchmarkResult.getAvailableProcessors() != null
                && plannerBenchmarkResult.getParallelBenchmarkCount() > plannerBenchmarkResult.getAvailableProcessors()) {
            warningList.add("The parallelBenchmarkCount (" + plannerBenchmarkResult.getParallelBenchmarkCount()
                    + ") is higher than the number of availableProcessors ("
                    + plannerBenchmarkResult.getAvailableProcessors() + ").");
        }
    }

    private void writeBestScoreSummaryCharts() {
        // Each scoreLevel has it's own dataset and chartFile
        List<DefaultCategoryDataset> datasetList = new ArrayList<DefaultCategoryDataset>(CHARTED_SCORE_LEVEL_SIZE);
        for (SolverBenchmarkResult solverBenchmarkResult : plannerBenchmarkResult.getSolverBenchmarkResultList()) {
            String solverLabel = solverBenchmarkResult.getNameWithFavoriteSuffix();
            for (SingleBenchmarkResult singleBenchmarkResult : solverBenchmarkResult.getSingleBenchmarkResultList()) {
                String planningProblemLabel = singleBenchmarkResult.getProblemBenchmarkResult().getName();
                if (singleBenchmarkResult.isSuccess()) {
                    double[] levelValues = ScoreUtils.extractLevelDoubles(singleBenchmarkResult.getAverageScore());
                    for (int i = 0; i < levelValues.length && i < CHARTED_SCORE_LEVEL_SIZE; i++) {
                        if (i >= datasetList.size()) {
                            datasetList.add(new DefaultCategoryDataset());
                        }
                        datasetList.get(i).addValue(levelValues[i], solverLabel, planningProblemLabel);
                    }
                }
            }
        }
        bestScoreSummaryChartFileList = new ArrayList<File>(datasetList.size());
        int scoreLevelIndex = 0;
        for (DefaultCategoryDataset dataset : datasetList) {
            CategoryPlot plot = createBarChartPlot(dataset,
                    "Score level " + scoreLevelIndex, NumberFormat.getInstance(locale));
            JFreeChart chart = new JFreeChart("Best score level " + scoreLevelIndex + " summary (higher is better)",
                    JFreeChart.DEFAULT_TITLE_FONT, plot, true);
            bestScoreSummaryChartFileList.add(writeChartToImageFile(chart, "bestScoreSummaryLevel" + scoreLevelIndex));
            scoreLevelIndex++;
        }
    }

    private void writeBestScoreScalabilitySummaryChart() {
        // Each scoreLevel has it's own dataset and chartFile
        List<List<XYSeries>> seriesListList = new ArrayList<List<XYSeries>>(
                CHARTED_SCORE_LEVEL_SIZE);
        int solverBenchmarkIndex = 0;
        for (SolverBenchmarkResult solverBenchmarkResult : plannerBenchmarkResult.getSolverBenchmarkResultList()) {
            String solverLabel = solverBenchmarkResult.getNameWithFavoriteSuffix();
            for (SingleBenchmarkResult singleBenchmarkResult : solverBenchmarkResult.getSingleBenchmarkResultList()) {
                if (singleBenchmarkResult.isSuccess()) {
                    long problemScale = singleBenchmarkResult.getProblemBenchmarkResult().getProblemScale();
                    double[] levelValues = ScoreUtils.extractLevelDoubles(singleBenchmarkResult.getAverageScore());
                    for (int i = 0; i < levelValues.length && i < CHARTED_SCORE_LEVEL_SIZE; i++) {
                        if (i >= seriesListList.size()) {
                            seriesListList.add(new ArrayList<XYSeries>(
                                    plannerBenchmarkResult.getSolverBenchmarkResultList().size()));
                        }
                        List<XYSeries> seriesList = seriesListList.get(i);
                        while (solverBenchmarkIndex >= seriesList.size()) {
                            seriesList.add(new XYSeries(solverLabel));
                        }
                        seriesList.get(solverBenchmarkIndex).add((double) problemScale, levelValues[i]);
                    }
                }
            }
            solverBenchmarkIndex++;
        }
        bestScoreScalabilitySummaryChartFileList = new ArrayList<File>(seriesListList.size());
        int scoreLevelIndex = 0;
        for (List<XYSeries> seriesList : seriesListList) {
            XYPlot plot = createScalabilityPlot(seriesList,
                    "Problem scale", NumberFormat.getInstance(locale),
                    "Score level " + scoreLevelIndex, NumberFormat.getInstance(locale));
            JFreeChart chart = new JFreeChart(
                    "Best score scalability level " + scoreLevelIndex + " summary (higher is better)",
                    JFreeChart.DEFAULT_TITLE_FONT, plot, true);
            bestScoreScalabilitySummaryChartFileList.add(
                    writeChartToImageFile(chart, "bestScoreScalabilitySummaryLevel" + scoreLevelIndex));
            scoreLevelIndex++;
        }
    }

    private void writeWinningScoreDifferenceSummaryChart() {
        // Each scoreLevel has it's own dataset and chartFile
        List<DefaultCategoryDataset> datasetList = new ArrayList<DefaultCategoryDataset>(CHARTED_SCORE_LEVEL_SIZE);
        for (SolverBenchmarkResult solverBenchmarkResult : plannerBenchmarkResult.getSolverBenchmarkResultList()) {
            String solverLabel = solverBenchmarkResult.getNameWithFavoriteSuffix();
            for (SingleBenchmarkResult singleBenchmarkResult : solverBenchmarkResult.getSingleBenchmarkResultList()) {
                String planningProblemLabel = singleBenchmarkResult.getProblemBenchmarkResult().getName();
                if (singleBenchmarkResult.isSuccess()) {
                    double[] levelValues = ScoreUtils.extractLevelDoubles(singleBenchmarkResult.getWinningScoreDifference());
                    for (int i = 0; i < levelValues.length && i < CHARTED_SCORE_LEVEL_SIZE; i++) {
                        if (i >= datasetList.size()) {
                            datasetList.add(new DefaultCategoryDataset());
                        }
                        datasetList.get(i).addValue(levelValues[i], solverLabel, planningProblemLabel);
                    }
                }
            }
        }
        winningScoreDifferenceSummaryChartFileList = new ArrayList<File>(datasetList.size());
        int scoreLevelIndex = 0;
        for (DefaultCategoryDataset dataset : datasetList) {
            CategoryPlot plot = createBarChartPlot(dataset,
                    "Winning score difference level " + scoreLevelIndex, NumberFormat.getInstance(locale));
            JFreeChart chart = new JFreeChart("Winning score difference level " + scoreLevelIndex
                    + " summary (higher is better)",
                    JFreeChart.DEFAULT_TITLE_FONT, plot, true);
            winningScoreDifferenceSummaryChartFileList.add(
                    writeChartToImageFile(chart, "winningScoreDifferenceSummaryLevel" + scoreLevelIndex));
            scoreLevelIndex++;
        }
    }

    private void writeWorstScoreDifferencePercentageSummaryChart() {
        // Each scoreLevel has it's own dataset and chartFile
        List<DefaultCategoryDataset> datasetList = new ArrayList<DefaultCategoryDataset>(CHARTED_SCORE_LEVEL_SIZE);
        for (SolverBenchmarkResult solverBenchmarkResult : plannerBenchmarkResult.getSolverBenchmarkResultList()) {
            String solverLabel = solverBenchmarkResult.getNameWithFavoriteSuffix();
            for (SingleBenchmarkResult singleBenchmarkResult : solverBenchmarkResult.getSingleBenchmarkResultList()) {
                String planningProblemLabel = singleBenchmarkResult.getProblemBenchmarkResult().getName();
                if (singleBenchmarkResult.isSuccess()) {
                    double[] levelValues = singleBenchmarkResult.getWorstScoreDifferencePercentage().getPercentageLevels();
                    for (int i = 0; i < levelValues.length && i < CHARTED_SCORE_LEVEL_SIZE; i++) {
                        if (i >= datasetList.size()) {
                            datasetList.add(new DefaultCategoryDataset());
                        }
                        datasetList.get(i).addValue(levelValues[i], solverLabel, planningProblemLabel);
                    }
                }
            }
        }
        worstScoreDifferencePercentageSummaryChartFileList = new ArrayList<File>(datasetList.size());
        int scoreLevelIndex = 0;
        for (DefaultCategoryDataset dataset : datasetList) {
            CategoryPlot plot = createBarChartPlot(dataset,
                    "Worst score difference percentage level " + scoreLevelIndex,
                    NumberFormat.getPercentInstance(locale));
            JFreeChart chart = new JFreeChart("Worst score difference percentage level " + scoreLevelIndex
                    + " summary (higher is better)",
                    JFreeChart.DEFAULT_TITLE_FONT, plot, true);
            worstScoreDifferencePercentageSummaryChartFileList.add(
                    writeChartToImageFile(chart, "worstScoreDifferencePercentageSummaryLevel" + scoreLevelIndex));
            scoreLevelIndex++;
        }
    }

    private void writeAverageCalculateCountPerSecondSummaryChart() {
        List<XYSeries> seriesList = new ArrayList<XYSeries>(plannerBenchmarkResult.getSolverBenchmarkResultList().size());
        for (SolverBenchmarkResult solverBenchmarkResult : plannerBenchmarkResult.getSolverBenchmarkResultList()) {
            String solverLabel = solverBenchmarkResult.getNameWithFavoriteSuffix();
            XYSeries series = new XYSeries(solverLabel);
            for (SingleBenchmarkResult singleBenchmarkResult : solverBenchmarkResult.getSingleBenchmarkResultList()) {
                if (singleBenchmarkResult.isSuccess()) {
                    long problemScale = singleBenchmarkResult.getProblemBenchmarkResult().getProblemScale();
                    long averageCalculateCountPerSecond = singleBenchmarkResult.getAverageCalculateCountPerSecond();
                    series.add((Long) problemScale, (Long) averageCalculateCountPerSecond);
                }
            }
            seriesList.add(series);
        }
        XYPlot plot = createScalabilityPlot(seriesList,
                "Problem scale", NumberFormat.getInstance(locale),
                "Average calculate count per second", NumberFormat.getInstance(locale));
        JFreeChart chart = new JFreeChart("Average calculate count summary (higher is better)",
                JFreeChart.DEFAULT_TITLE_FONT, plot, true);
        averageCalculateCountSummaryChartFile = writeChartToImageFile(chart, "averageCalculateCountSummary");
    }

    private void writeTimeSpentSummaryChart() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (SolverBenchmarkResult solverBenchmarkResult : plannerBenchmarkResult.getSolverBenchmarkResultList()) {
            String solverLabel = solverBenchmarkResult.getNameWithFavoriteSuffix();
            for (SingleBenchmarkResult singleBenchmarkResult : solverBenchmarkResult.getSingleBenchmarkResultList()) {
                String planningProblemLabel = singleBenchmarkResult.getProblemBenchmarkResult().getName();
                if (singleBenchmarkResult.isSuccess()) {
                    long timeMillisSpent = singleBenchmarkResult.getTimeMillisSpent();
                    dataset.addValue(timeMillisSpent, solverLabel, planningProblemLabel);
                }
            }
        }
        CategoryPlot plot = createBarChartPlot(dataset, "Time spent", new MillisecondsSpentNumberFormat(locale));
        JFreeChart chart = new JFreeChart("Time spent summary (lower time is better)",
                JFreeChart.DEFAULT_TITLE_FONT, plot, true);
        timeSpentSummaryChartFile = writeChartToImageFile(chart, "timeSpentSummary");
    }

    private void writeTimeSpentScalabilitySummaryChart() {
        List<XYSeries> seriesList = new ArrayList<XYSeries>(plannerBenchmarkResult.getSolverBenchmarkResultList().size());
        for (SolverBenchmarkResult solverBenchmarkResult : plannerBenchmarkResult.getSolverBenchmarkResultList()) {
            String solverLabel = solverBenchmarkResult.getNameWithFavoriteSuffix();
            XYSeries series = new XYSeries(solverLabel);
            for (SingleBenchmarkResult singleBenchmarkResult : solverBenchmarkResult.getSingleBenchmarkResultList()) {
                if (singleBenchmarkResult.isSuccess()) {
                    long problemScale = singleBenchmarkResult.getProblemBenchmarkResult().getProblemScale();
                    long timeMillisSpent = singleBenchmarkResult.getTimeMillisSpent();
                    series.add((Long) problemScale, (Long) timeMillisSpent);
                }
            }
            seriesList.add(series);
        }
        XYPlot plot = createScalabilityPlot(seriesList,
                "Problem scale", NumberFormat.getInstance(locale),
                "Time spent", new MillisecondsSpentNumberFormat(locale));
        JFreeChart chart = new JFreeChart("Time spent scalability summary (lower is better)",
                JFreeChart.DEFAULT_TITLE_FONT, plot, true);
        timeSpentScalabilitySummaryChartFile = writeChartToImageFile(chart, "timeSpentScalabilitySummary");
    }

    private void writeBestScorePerTimeSpentSummaryChart() {
        // Each scoreLevel has it's own dataset and chartFile
        List<List<XYSeries>> seriesListList = new ArrayList<List<XYSeries>>(
                CHARTED_SCORE_LEVEL_SIZE);
        int solverBenchmarkIndex = 0;
        for (SolverBenchmarkResult solverBenchmarkResult : plannerBenchmarkResult.getSolverBenchmarkResultList()) {
            String solverLabel = solverBenchmarkResult.getNameWithFavoriteSuffix();
            for (SingleBenchmarkResult singleBenchmarkResult : solverBenchmarkResult.getSingleBenchmarkResultList()) {
                if (singleBenchmarkResult.isSuccess()) {
                    long timeMillisSpent = singleBenchmarkResult.getTimeMillisSpent();
                    double[] levelValues = ScoreUtils.extractLevelDoubles(singleBenchmarkResult.getAverageScore());
                    for (int i = 0; i < levelValues.length && i < CHARTED_SCORE_LEVEL_SIZE; i++) {
                        if (i >= seriesListList.size()) {
                            seriesListList.add(new ArrayList<XYSeries>(
                                    plannerBenchmarkResult.getSolverBenchmarkResultList().size()));
                        }
                        List<XYSeries> seriesList = seriesListList.get(i);
                        while (solverBenchmarkIndex >= seriesList.size()) {
                            seriesList.add(new XYSeries(solverLabel));
                        }
                        seriesList.get(solverBenchmarkIndex).add((Long) timeMillisSpent, (Double) levelValues[i]);
                    }
                }
            }
            solverBenchmarkIndex++;
        }
        bestScorePerTimeSpentSummaryChartFileList = new ArrayList<File>(seriesListList.size());
        int scoreLevelIndex = 0;
        for (List<XYSeries> seriesList : seriesListList) {
            XYPlot plot = createScalabilityPlot(seriesList,
                    "Time spent", new MillisecondsSpentNumberFormat(locale),
                    "Score level " + scoreLevelIndex, NumberFormat.getInstance(locale));
            JFreeChart chart = new JFreeChart(
                    "Best score per time spent level " + scoreLevelIndex + " summary (higher left is better)",
                    JFreeChart.DEFAULT_TITLE_FONT, plot, true);
            bestScorePerTimeSpentSummaryChartFileList.add(
                    writeChartToImageFile(chart, "bestScorePerTimeSpentSummaryLevel" + scoreLevelIndex));
            scoreLevelIndex++;
        }
    }

    // ************************************************************************
    // Chart helper methods
    // ************************************************************************

    private CategoryPlot createBarChartPlot(DefaultCategoryDataset dataset,
            String yAxisLabel, NumberFormat yAxisNumberFormat) {
        CategoryAxis xAxis = new CategoryAxis("Data");
        xAxis.setCategoryMargin(0.40);
        NumberAxis yAxis = new NumberAxis(yAxisLabel);
        yAxis.setNumberFormatOverride(yAxisNumberFormat);
        BarRenderer renderer = createBarChartRenderer(yAxisNumberFormat);
        CategoryPlot plot = new CategoryPlot(dataset, xAxis, yAxis, renderer);
        plot.setOrientation(PlotOrientation.VERTICAL);
        return plot;
    }

    private BarRenderer createBarChartRenderer(NumberFormat numberFormat) {
        BarRenderer renderer = new BarRenderer();
        ItemLabelPosition positiveItemLabelPosition = new ItemLabelPosition(
                ItemLabelAnchor.OUTSIDE12, TextAnchor.BOTTOM_CENTER);
        renderer.setBasePositiveItemLabelPosition(positiveItemLabelPosition);
        ItemLabelPosition negativeItemLabelPosition = new ItemLabelPosition(
                ItemLabelAnchor.OUTSIDE6, TextAnchor.TOP_CENTER);
        renderer.setBaseNegativeItemLabelPosition(negativeItemLabelPosition);
        renderer.setBaseItemLabelGenerator(new StandardCategoryItemLabelGenerator(
                StandardCategoryItemLabelGenerator.DEFAULT_LABEL_FORMAT_STRING, numberFormat));
        renderer.setBaseItemLabelsVisible(true);
        return renderer;
    }

    private XYPlot createScalabilityPlot(List<XYSeries> seriesList,
            String xAxisLabel, NumberFormat xAxisNumberFormat,
            String yAxisLabel, NumberFormat yAxisNumberFormat) {
        NumberAxis xAxis;
        if (useLogarithmicProblemScale(seriesList)) {
            LogarithmicAxis logarithmicAxis = new LogarithmicAxis(xAxisLabel + " (logarithmic)");
            logarithmicAxis.setAllowNegativesFlag(true);
            xAxis = logarithmicAxis;
        } else {
            xAxis = new NumberAxis(xAxisLabel);
        }
        xAxis.setNumberFormatOverride(xAxisNumberFormat);
        NumberAxis yAxis = new NumberAxis(yAxisLabel);
        yAxis.setNumberFormatOverride(yAxisNumberFormat);
        XYPlot plot = new XYPlot(null, xAxis, yAxis, null);
        int seriesIndex = 0;
        for (XYSeries series : seriesList) {
            XYSeriesCollection seriesCollection = new XYSeriesCollection();
            seriesCollection.addSeries(series);
            plot.setDataset(seriesIndex, seriesCollection);
            XYItemRenderer renderer = createScalabilityPlotRenderer(yAxisNumberFormat);
            plot.setRenderer(seriesIndex, renderer);
            seriesIndex++;
        }
        plot.setOrientation(PlotOrientation.VERTICAL);
        return plot;
    }

    protected boolean useLogarithmicProblemScale(List<XYSeries> seriesList) {
        NavigableSet<Double> xValueSet = new TreeSet<Double>();
        int xValueListSize = 0;
        for (XYSeries series : seriesList) {
            for (XYDataItem dataItem : (List<XYDataItem>) series.getItems()) {
                xValueSet.add(dataItem.getXValue());
                xValueListSize++;
            }
        }
        if (xValueListSize < LOG_SCALE_MIN_DATASETS_COUNT) {
            return false;
        }
        // If 60% of the points are in 20% of the value space, use a logarithmic scale
        double threshold = 0.2 * (xValueSet.last() - xValueSet.first());
        int belowThresholdCount = xValueSet.headSet(threshold).size();
        return belowThresholdCount >= (0.6 * xValueSet.size());
    }

    private XYItemRenderer createScalabilityPlotRenderer(NumberFormat numberFormat) {
        XYItemRenderer renderer = new StandardXYItemRenderer(StandardXYItemRenderer.SHAPES_AND_LINES);
        // Use dashed line
        renderer.setSeriesStroke(0, new BasicStroke(
                1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f, new float[] {2.0f, 6.0f}, 0.0f
        ));
        return renderer;
    }

    private File writeChartToImageFile(JFreeChart chart, String fileNameBase) {
        BufferedImage chartImage = chart.createBufferedImage(1024, 768);
        File summaryChartFile = new File(summaryDirectory, fileNameBase + ".png");
        OutputStream out = null;
        try {
            out = new FileOutputStream(summaryChartFile);
            ImageIO.write(chartImage, "png", out);
        } catch (IOException e) {
            throw new IllegalArgumentException("Problem writing summaryChartFile: " + summaryChartFile, e);
        } finally {
            IOUtils.closeQuietly(out);
        }
        return summaryChartFile;
    }

    private void determineDefaultShownScoreLevelIndex() {
        defaultShownScoreLevelIndex = Integer.MAX_VALUE;
        for (ProblemBenchmarkResult problemBenchmarkResult : plannerBenchmarkResult.getUnifiedProblemBenchmarkResultList()) {
            if (problemBenchmarkResult.hasAnySuccess()) {
                double[] winningScoreLevels = ScoreUtils.extractLevelDoubles(
                        problemBenchmarkResult.getWinningSingleBenchmarkResult().getAverageScore());
                int[] differenceCount = new int[winningScoreLevels.length];
                for (int i = 0; i < differenceCount.length; i++) {
                    differenceCount[i] = 0;
                }
                for (SingleBenchmarkResult singleBenchmarkResult : problemBenchmarkResult.getSingleBenchmarkResultList()) {
                    if (singleBenchmarkResult.isSuccess()) {
                        double[] scoreLevels = ScoreUtils.extractLevelDoubles(singleBenchmarkResult.getAverageScore());
                        for (int i = 0; i < scoreLevels.length; i++) {
                            if (scoreLevels[i] != winningScoreLevels[i]) {
                                differenceCount[i] = differenceCount[i] + 1;
                            }
                        }
                    }
                }
                int firstInterestingLevel = differenceCount.length - 1;
                for (int i = 0; i < differenceCount.length; i++) {
                    if (differenceCount[i] > 0) {
                        firstInterestingLevel = i;
                        break;
                    }
                }
                if (defaultShownScoreLevelIndex > firstInterestingLevel) {
                    defaultShownScoreLevelIndex = firstInterestingLevel;
                }
            }
        }
    }

    private void writeHtmlOverviewFile() {
        File benchmarkReportDirectory = plannerBenchmarkResult.getBenchmarkReportDirectory();
        WebsiteResourceUtils.copyResourcesTo(benchmarkReportDirectory);

        htmlOverviewFile = new File(benchmarkReportDirectory, "index.html");
        Configuration freemarkerCfg = new Configuration();
        freemarkerCfg.setDefaultEncoding("UTF-8");
        freemarkerCfg.setLocale(locale);
        freemarkerCfg.setClassForTemplateLoading(BenchmarkReport.class, "");

        String templateFilename = "benchmarkReport.html.ftl";
        Map<String, Object> model = new HashMap<String, Object>();
        model.put("benchmarkReport", this);
        model.put("reportHelper", new ReportHelper());

        Writer writer = null;
        try {
            Template template = freemarkerCfg.getTemplate(templateFilename);
            writer = new OutputStreamWriter(new FileOutputStream(htmlOverviewFile), "UTF-8");
            template.process(model, writer);
        } catch (IOException e) {
            throw new IllegalArgumentException("Can not read templateFilename (" + templateFilename
                    + ") or write htmlOverviewFile (" + htmlOverviewFile + ").", e);
        } catch (TemplateException e) {
            throw new IllegalArgumentException("Can not process Freemarker templateFilename (" + templateFilename
                    + ") to htmlOverviewFile (" + htmlOverviewFile + ").", e);
        } finally {
            IOUtils.closeQuietly(writer);
        }
    }

    private Class getSolverRankingClass() {
        if (solverRankingComparator != null) {
            return solverRankingComparator.getClass();
        } else if (solverRankingWeightFactory != null) {
            return solverRankingWeightFactory.getClass();
        } else {
            return null;
        }
    }

}
