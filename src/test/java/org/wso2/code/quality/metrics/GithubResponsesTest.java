/*
 *  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.code.quality.metrics;

import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;
import org.wso2.code.quality.metrics.exceptions.CodeQualityMetricsException;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;

/**
 * A class used to test saveRepoNames,savePrNumberAndRepoName and saveReviewers methods of the GithubResponses class
 *
 * @since 1.0.0
 */
public class GithubResponsesTest {
    private static final Logger logger = Logger.getLogger(GithubResponsesTest.class);
    private static String githubToken;
    private static Class<?> changesFinderClass;
    private static Class<?> reviewAnalyserClass;

    @BeforeClass
    public static void setupTheEnvironment() throws CodeQualityMetricsException {
        githubToken = new Token().getGithubToken();
        try {
            changesFinderClass = Class.forName("org.wso2.code.quality.metrics.ChangesFinder");
            reviewAnalyserClass = Class.forName("org.wso2.code.quality.metrics.ReviewAnalyser");
        } catch (ClassNotFoundException e) {
            logger.error(e.getMessage(), e.getCause());
        }
    }

    @Test
    public void testObtainRepoNamesForCommitHashes() throws CodeQualityMetricsException {
        List<String> commitHash = new ArrayList<>();
        commitHash.add("ad0debb15f1abac020b8ba69066ae4ebec782bdc");
        Set<String> expectedAuthorCommits = new HashSet<>();
        expectedAuthorCommits.add("90fec04e4ac05281612de8d445c5767c26433b0d");
        try {
            Method obtainRepoNamesForCommitHashesMethod = changesFinderClass.getDeclaredMethod
                    ("obtainRepoNamesForCommitHashes", String.class, List.class);
            obtainRepoNamesForCommitHashesMethod.setAccessible(true);
            Object changesFinder = changesFinderClass.newInstance();
            @SuppressWarnings("unchecked")
            Set<String> actualAuthorCommits = (Set<String>) obtainRepoNamesForCommitHashesMethod.invoke
                    (changesFinder, githubToken, commitHash);
            assertThat(actualAuthorCommits.size(), is(expectedAuthorCommits.size()));
            assertThat(actualAuthorCommits, is(expectedAuthorCommits));
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException |
                InvocationTargetException e) {
            logger.error(e.getMessage(), e.getCause());
        }
    }

    @Test
    public void testSaveRepoNames() {
        Map<String, List<String>> commitHashWithRepoNames = new HashMap<>();
        commitHashWithRepoNames.put("eaa45529cbabc5f30a2ffaa4781821ad0a5223ab",
                Collections.singletonList("wso2/carbon-apimgt"));
        commitHashWithRepoNames.put("2b1d973d089ebc3af3b9e7b893f48cf905758cf4",
                Collections.singletonList("wso2/carbon-apimgt"));
        commitHashWithRepoNames.put("e3c3457149b109178d510aac965d5a85cc465aa0",
                Collections.singletonList("wso2/wso2-axis2-transports"));
        commitHashWithRepoNames.forEach((commitHash, repository) -> {
            try {
                String jsonText = GithubApiCallerUtils.callSearchCommitApi(commitHash, githubToken);
                Method saveRepoNamesMethod = changesFinderClass.getDeclaredMethod("getRepoNames", String.class);
                saveRepoNamesMethod.setAccessible(true);
                Object changesFinder = changesFinderClass.newInstance();
                @SuppressWarnings("unchecked")
                List<String> repoLocations = (List<String>) saveRepoNamesMethod.invoke(changesFinder, jsonText);
                assertEquals("List of RepoLocations obtained must be same", repository, repoLocations);
            } catch (CodeQualityMetricsException | InstantiationException | IllegalAccessException |
                    NoSuchMethodException |
                    InvocationTargetException e) {
                logger.error(e.getMessage(), e.getCause());
            }
        });
    }

    @Test
    public void testSavePrNumberAndRepoName() throws CodeQualityMetricsException {
        String jsonText = GithubApiCallerUtils.callSearchIssueApi
                ("0015c02145c8ec6d3bba433f2fb5e850e1d25846", githubToken);
        Map<String, Set<Integer>> expectedPrNoWithRepoName = new HashMap<>();
        Set<Integer> expectedPrSet = new HashSet<>();
        expectedPrSet.add(656);
        expectedPrSet.add(657);
        expectedPrNoWithRepoName.put("wso2/carbon-apimgt", expectedPrSet);
        try {
            Method savePrNumberAndRepoNameMethod = reviewAnalyserClass.getDeclaredMethod
                    ("savePrNumberAndRepoName", String.class);
            savePrNumberAndRepoNameMethod.setAccessible(true);
            Object reviewAnalyser = reviewAnalyserClass.newInstance();
            @SuppressWarnings("unchecked")
            Map<String, Set<Integer>> actualPrNoWithRepoName2 = (Map<String, Set<Integer>>)
                    savePrNumberAndRepoNameMethod.invoke(reviewAnalyser, jsonText);
            assertThat(actualPrNoWithRepoName2, is(expectedPrNoWithRepoName));
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException |
                NoSuchMethodException e) {
            logger.error(e.getMessage(), e.getCause());
        }
    }

    @Test
    public void testObtainRepoNamesForCommitHashesForAuthorNames() throws CodeQualityMetricsException {
        List<String> commitHash1 = new ArrayList<>();
        commitHash1.add("bba8ce79cd3373445e21dd12deffae1a7b48dca9");
        commitHash1.add("1c0e28ca181a08398efbc8ba8e984d8800e23c95");
        commitHash1.add("a8ddc56575ede78c6a1882df20789bb2cc04022c");
        Set<String> expectedAuthorName1 = new HashSet<>();
        expectedAuthorName1.add("ruchiraw");
        obtainRepoNamesForCommitHashes(commitHash1, expectedAuthorName1);

        List<String> commitHash2 = new ArrayList<>();
        commitHash2.add("2b1d973d089ebc3af3b9e7b893f48cf905758cf4");
        commitHash2.add("eaa45529cbabc5f30a2ffaa4781821ad0a5223ab");
        Set<String> expectedAuthorName2 = new HashSet<>();
        expectedAuthorName2.add("Chamila");
        expectedAuthorName2.add("lalaji");
        expectedAuthorName2.add("Amila De Silva");
        expectedAuthorName2.add("Lakmali");
        obtainRepoNamesForCommitHashes(commitHash2, expectedAuthorName2);
    }

    private void obtainRepoNamesForCommitHashes(List<String> commitHash, Set<String> expectedAuthorName) {
        try {
            Field authorNamesField = changesFinderClass.getDeclaredField("authorNamesWithLoginNames");
            authorNamesField.setAccessible(true);
            Method obtainRepoNamesForCommitHashesMethod = changesFinderClass.getDeclaredMethod
                    ("obtainRepoNamesForCommitHashes", String.class, List.class);
            Object changesFinder = changesFinderClass.newInstance();
            obtainRepoNamesForCommitHashesMethod.setAccessible(true);
            obtainRepoNamesForCommitHashesMethod.invoke(changesFinder, githubToken, commitHash);
            @SuppressWarnings("unchecked")
            Map<String, String> authorNamesWithLoginNames = (Map<String, String>) authorNamesField.get(changesFinder);
            Set<String> actualAuthorNames = authorNamesWithLoginNames.keySet();
            assertThat(actualAuthorNames.size(), is(expectedAuthorName.size()));
            assertThat(actualAuthorNames, is(expectedAuthorName));
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | NoSuchFieldException |
                InvocationTargetException e) {
            logger.error(e.getMessage(), e.getCause());
        }
    }

    @Test
    public void testSaveReviewers() {
        Map<String, Set<Integer>> prNoWithRepoName1 = new HashMap<>();
        Set<Integer> prNumberSet1 = new HashSet<>();
        prNumberSet1.add(656);
        prNumberSet1.add(657);
        prNoWithRepoName1.put("wso2/carbon-apimgt", prNumberSet1);
        // since no reviewers for pull requests 656,657
        List<String> expectedApprovedReviewers1 = Collections.emptyList();
        List<String> expectedCommentedReviewers1 = Collections.emptyList();
        testSaveReviewersForPrs(prNoWithRepoName1, expectedApprovedReviewers1,
                expectedCommentedReviewers1);
        Map<String, Set<Integer>> prNoWithRepoName2 = new HashMap<>();
        Set<Integer> prNumberSet2 = new HashSet<>();
        prNumberSet2.add(885);
        prNoWithRepoName2.put("wso2/product-is", prNumberSet2);
        List<String> expectedApprovedReviewers2 = Collections.singletonList("darshanasbg");
        List<String> expectedCommentedReviewers2 = Collections.singletonList("isharak");
        testSaveReviewersForPrs(prNoWithRepoName2, expectedApprovedReviewers2,
                expectedCommentedReviewers2);
    }

    private void testSaveReviewersForPrs(Map<String, Set<Integer>> prNoWithRepoName1, List<String>
            expectedApprovedReviewers1, List<String> expectedCommentedReviewers1) {
        try {
            Field approvedReviewersField = reviewAnalyserClass.getDeclaredField("approvedReviewers");
            Field commentedReviewersField = reviewAnalyserClass.getDeclaredField("commentedReviewers");
            approvedReviewersField.setAccessible(true);
            commentedReviewersField.setAccessible(true);
            Method saveReviewersMethod = reviewAnalyserClass.getDeclaredMethod("saveReviewers", Map.class,
                    String.class);
            saveReviewersMethod.setAccessible(true);
            Object reviewAnalyser = reviewAnalyserClass.newInstance();
            saveReviewersMethod.invoke(reviewAnalyser, prNoWithRepoName1, githubToken);
            @SuppressWarnings("unchecked")
            Map<String, Integer> actualApprovedReviewersWithIds = (Map<String, Integer>) approvedReviewersField.get
                    (reviewAnalyser);
            Set<String> actualApprovedReviewers = actualApprovedReviewersWithIds.keySet();
            @SuppressWarnings("unchecked")
            Map<String, Integer> actualCommentedReviewersWithIds = (Map<String, Integer>) commentedReviewersField.get
                    (reviewAnalyser);
            Set<String> actualCommentedReviewers = actualCommentedReviewersWithIds.keySet();
            assertThat("List of approved users ", actualApprovedReviewers, containsInAnyOrder
                    (expectedApprovedReviewers1.toArray()));
            assertThat("List of commented users ", actualCommentedReviewers, containsInAnyOrder
                    (expectedCommentedReviewers1.toArray()));
        } catch (IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException |
                NoSuchFieldException e) {
            logger.error(e.getMessage(), e.getCause());
        }
    }
}
