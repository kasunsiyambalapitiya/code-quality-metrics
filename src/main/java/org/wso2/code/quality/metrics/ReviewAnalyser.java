/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.wso2.code.quality.metrics.exceptions.CodeQualityMetricsException;
import org.wso2.code.quality.metrics.model.Constants;
import org.wso2.code.quality.metrics.model.IssueApiResponse;
import org.wso2.code.quality.metrics.model.ReviewApiResponse;
import org.wso2.code.quality.metrics.model.UserDetails;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Used to find the reviewers of the buggy lines of code.
 *
 * @since 1.0.0
 */

public class ReviewAnalyser {
    private static final Logger logger = Logger.getLogger(ReviewAnalyser.class);

    // to store the reviewed and approved users of the pull requests
    private final Map<String, Integer> approvedReviewers = new HashMap<>();
    private final Map<String, Integer> commentedReviewers = new HashMap<>();
    private final Set<String> identifiableApprovedReviewerNames = new HashSet<>();
    private final Set<String> identifiableCommentedReviewerNames = new HashSet<>();
    private final Gson gson = new Gson();

    /**
     * Used to prevent SIC_INNER_SHOULD_BE_STATIC_ANON error that comes when building with WSO2 parent
     * pom. As suggested by the above error an static inner class is used to prevent the error.
     */
    private static class ListType extends TypeToken<List<ReviewApiResponse>> {
    }

    /**
     * Used to identify the pull requests that introduce the given commit to the code base.
     *
     * @param authorCommits Commits which the relevant pull request no must be found
     * @param githubToken   Github access token for accessing github API
     */
    void findReviewers(Set<String> authorCommits, String githubToken) {
        authorCommits.forEach(commitHash -> {
            String jsonText;
            try {
                jsonText = GithubApiCallerUtils.callSearchIssueApi(commitHash, githubToken);
                Map<String, Set<Integer>> prNoWithRepoName = savePrNumberAndRepoName(jsonText);
                if (logger.isDebugEnabled()) {
                    logger.debug("Relevant pull requests on patch " + commitHash + " with their relevant repository " +
                            "names are successfully saved in a map.");
                }
                saveReviewers(prNoWithRepoName, githubToken);
                findIdentifiableReviewerName(approvedReviewers, commentedReviewers, githubToken);
            } catch (CodeQualityMetricsException e) {
                logger.error(e.getMessage(), e.getCause());
            }
        });
    }

    /**
     * Used to save the pull requests with their relevant repository names in a map.
     *
     * @param jsonText json reponse received from the github issue API
     * @return a map of pull requests againt their repository name
     * @throws CodeQualityMetricsException results
     */
    private Map<String, Set<Integer>> savePrNumberAndRepoName(String jsonText) throws CodeQualityMetricsException {
        // map for storing the pull requests numbers against their Repository
        Map<String, Set<Integer>> prNoWithRepoName = new HashMap<>();
        try {
            IssueApiResponse issueApiResponse = gson.fromJson(jsonText, IssueApiResponse.class);
            issueApiResponse.getIssue().parallelStream()
                    .filter(searchItem -> Constants.GITHUB_REVIEW_API_CLOSED_STATE.equals(searchItem.getPrState()))
                    .filter(searchItem -> StringUtils.contains(searchItem.getRepositoryUrl(), "/wso2/"))
                    .forEach(searchItem -> {
                        String repositoryName = StringUtils.substringAfter(searchItem.getRepositoryUrl(),
                                "repos/");
                        int pullRequestNo = searchItem.getPrNumber();
                        prNoWithRepoName.putIfAbsent(repositoryName, new HashSet<>());
                        if (!prNoWithRepoName.get(repositoryName).contains(pullRequestNo)) {
                            prNoWithRepoName.get(repositoryName).add(pullRequestNo);
                        }
                    });
        } catch (JsonSyntaxException e) {
            throw new CodeQualityMetricsException(e.getMessage(), e.getCause());
        }
        return prNoWithRepoName;
    }

    /**
     * Used to save the names of the users who has approved and commented on the pull requests which
     * introduce bug lines of code to the code base. Approved users are saved in approvedReviewers list while
     * commented users are saved in commentedReviewers list.
     *
     * @param prNoWithRepoName Map containg the pull requests which introduce bug lines to the code base against the
     *                         relevant reposiory
     * @param githubToken      Github access token for accessing github API
     */
    private void saveReviewers(Map<String, Set<Integer>> prNoWithRepoName, String githubToken) {
        prNoWithRepoName.forEach((repositoryName, prNumbers) -> prNumbers.parallelStream()
                .forEach((Integer prNumber) -> {
                    try {
                        String jsonText = GithubApiCallerUtils.callReviewApi(repositoryName, prNumber, githubToken);
                        if (jsonText != null && !jsonText.isEmpty()) {
                            Type listType = new ListType().getType();
                            List<ReviewApiResponse> reviews = gson.fromJson(jsonText, listType);
                            // to filter Approved users
                            reviews.parallelStream()
                                    .filter(review -> Constants.GITHUB_REVIEW_APPROVED.equals(review.getReviewState()))
                                    .forEach(review -> approvedReviewers.put(review.getReviewer().getName(), review
                                            .getReviewer().getUserId()));
                            logger.debug("Users who approved the pull requests which introduce bug lines to the " +
                                    "code base are successfully saved to approvedReviewers list");
                            reviews.parallelStream()
                                    .filter(review -> Constants.GITHUB_REVIEW_COMMENTED.equals(review.getReviewState()))
                                    .forEach(review -> commentedReviewers.put(review.getReviewer().getName(), review
                                            .getReviewer().getUserId()));
                            logger.debug("Users who commented on the pull requests which introduce bug lines to " +
                                    "the code base are successfully saved to approvedReviewers list");
                        } else {
                            logger.warn("There are no records of reviews for pull request: " + prNumber +
                                    " on " + repositoryName + " repository");
                        }
                    } catch (CodeQualityMetricsException e) {
                        logger.error(e.getMessage(), e.getCause());
                    }
                }));
    }

    /**
     * Used for finding an identifiable user name of approved and commented users as all the users are not using their
     * real name as their username.
     *
     * @param approvedReviewers  map of Approved reviewers of bug lines
     * @param commentedReviewers map of commented reviewers of bug lines
     * @param githubToken        Github access token for accessing github API
     */
    void findIdentifiableReviewerName(Map<String, Integer> approvedReviewers, Map<String, Integer> commentedReviewers,
                                      String githubToken) {
        saveIdentifiableReviewerName(approvedReviewers, identifiableApprovedReviewerNames, githubToken);
        saveIdentifiableReviewerName(commentedReviewers, identifiableCommentedReviewerNames, githubToken);
    }

    /**
     * Used for saving identifiable user names to relevant Sets
     *
     * @param reviewers                 Map of reviewers
     * @param identifiableReviewerNames Set which identifiable user names are saved to
     * @param githubToken               Github access token for accessing github API
     */
    private void saveIdentifiableReviewerName(Map<String, Integer> reviewers, Set<String> identifiableReviewerNames,
                                              String githubToken) {
        reviewers.forEach((approvedReviewer, userId) -> {
            String fullUsername = "";
            try {
                fullUsername = getFullUserName(userId, githubToken);
            } catch (CodeQualityMetricsException e) {
                logger.error(e.getMessage(), e.getCause());
            }
            String identifiableUserName;
            if (fullUsername != null) {
                identifiableUserName = approvedReviewer + "(" + fullUsername + ")";
            } else {
                // to avoid printing null which may be returned from the User details API
                fullUsername = "";
                identifiableUserName = approvedReviewer + "(" + fullUsername + ")";
            }
            identifiableReviewerNames.add(identifiableUserName);
        });
    }

    /**
     * Used for getting the full user name of the reviewer.
     *
     * @param reviewerUserIds user Id of the user
     * @param githubToken     Github access token for accessing github API
     * @return full name of the user having the given user Id
     */
    private String getFullUserName(Integer reviewerUserIds, String githubToken) throws CodeQualityMetricsException {
        String fullUserName = null;
        try {
            String jsonText = GithubApiCallerUtils.invokeUserDetailsAPIForUserId(reviewerUserIds, githubToken);
            UserDetails userDetails = gson.fromJson(jsonText, UserDetails.class);
            fullUserName = userDetails.getUserName();
        } catch (CodeQualityMetricsException e) {
            throw new CodeQualityMetricsException(e.getMessage(), e.getCause());
        }
        return fullUserName;
    }

    /**
     * Print the list of reviewers and commented users on the pull requests which introduce bugs to the code base.
     */
    void printReviewUsers() {
        if (logger.isDebugEnabled()) {
            logger.debug("\n Reviewed and approved users of the bug lines: " + identifiableApprovedReviewerNames);
            logger.debug("\n Reviewed and commented users on bug lines: " + identifiableCommentedReviewerNames);
        }
    }
}
