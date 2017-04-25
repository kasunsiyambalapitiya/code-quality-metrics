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

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.wso2.code.quality.metrics.exceptions.CodeQualityMetricsException;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

import static org.wso2.code.quality.metrics.model.Constants.ACCEPT;
import static org.wso2.code.quality.metrics.model.Constants.AUTHORIZATION;
import static org.wso2.code.quality.metrics.model.Constants.BEARER;

/**
 * Utility class used for all github communications.
 *
 * @since 1.0.0
 */
public final class GithubApiCallerUtils {
    private static final Logger logger = Logger.getLogger(GithubApiCallerUtils.class);
    private static final Properties defaultProperties = new Properties();

    /**
     * To prevent instantiation from other classes
     */
    private GithubApiCallerUtils() {
    }

    //for loading the properties file
    static {
        try (InputStream inputStream = GithubApiCallerUtils.class.getResourceAsStream("/code.quality.metrics" +
                ".properties")) {
            defaultProperties.load(inputStream);
        } catch (IOException e) {
            logger.fatal("code.quality.metrics.properties file was not loaded to defaultProperties successfully");
        }
    }

    /**
     * Used for calling the github search REST API.
     *
     * @param commitHash        commit hash to be searched
     * @param githubAccessToken Github access token for accessing github API
     * @return String representation of the json response
     * @throws CodeQualityMetricsException results
     */
    public static String callSearchCommitApi(String commitHash, String githubAccessToken) throws
            CodeQualityMetricsException {
        HttpGet httpGet;
        try {
            String url = defaultProperties.getProperty("search.commit.API.url") + commitHash;
            httpGet = new HttpGet(url);
            httpGet.addHeader(AUTHORIZATION, BEARER + githubAccessToken);
            //as the accept header is needed for accessing commit search API which is still in preview mode
            httpGet.addHeader(ACCEPT, defaultProperties.getProperty("search.commit.API.header"));
        } catch (IllegalArgumentException e) {
            throw new CodeQualityMetricsException("The url provided for accessing the Github Search Commit API is " +
                    "invalid ", e);
        }
        return ApiUtility.invokeApi(httpGet);
    }

    /**
     * Used to call github commit REST API.
     *
     * @param repoLocation      repository location
     * @param filePath          location of the file
     * @param githubAccessToken Github access token for accessing github API
     * @return String representation of the json response
     * @throws CodeQualityMetricsException Resulted Code Quality Metrics Exception
     */
    public static String callCommitHistoryApi(String repoLocation, String filePath, String githubAccessToken)
            throws CodeQualityMetricsException {
        HttpGet httpGet;
        try {
            String tempUrl = defaultProperties.getProperty("commit.history.API.url");
            String url = tempUrl.replaceFirst("REPO_LOCATION", repoLocation).replaceFirst("FILE_NAME", filePath);
            httpGet = new HttpGet(url);
            httpGet.addHeader(AUTHORIZATION, BEARER + githubAccessToken);
        } catch (IllegalArgumentException e) {
            throw new CodeQualityMetricsException("The url provided for accessing the Github Search Commit API is " +
                    "invalid ", e);
        }
        return ApiUtility.invokeApi(httpGet);
    }

    /**
     * Used to call github single commit REST API.
     *
     * @param repoLocation      repository location
     * @param commitHash        relevant commit hash to find details of
     * @param githubAccessToken Github access token for accessing github API
     * @return String representation of the json response
     */
    public static String callSingleCommitApi(String repoLocation, String commitHash, String githubAccessToken)
            throws CodeQualityMetricsException {
        HttpGet httpGet;
        try {
            String tempUrl = defaultProperties.getProperty("single.commit.API.url");
            String url = tempUrl.replaceFirst("REPO_LOCATION", repoLocation).replaceFirst("COMMIT_HASH", commitHash);
            httpGet = new HttpGet(url);
            httpGet.addHeader(AUTHORIZATION, BEARER + githubAccessToken);
        } catch (IllegalArgumentException e) {
            throw new CodeQualityMetricsException("The url provided for accessing the Github Search Commit API is " +
                    "invalid ", e);
        }
        return ApiUtility.invokeApi(httpGet);

    }

    /**
     * Used to call the github review API.
     *
     * @param repoLocation      repository name
     * @param pullRequestNumber pull request number to be queried for
     * @param githubAccessToken Github access token for accessing github API
     * @return String representation of the json response
     * @throws CodeQualityMetricsException results
     */
    public static String callReviewApi(String repoLocation, int pullRequestNumber, String githubAccessToken) throws
            CodeQualityMetricsException {
        HttpGet httpGet;
        try {
            String tempUrl = defaultProperties.getProperty("review.API.url");
            String url = tempUrl.replaceFirst("REPO_LOCATION", repoLocation).replaceFirst("PULL_REQUEST_NUMBER",
                    String.valueOf(pullRequestNumber));
            httpGet = new HttpGet(url);
            httpGet.addHeader(ACCEPT, defaultProperties.getProperty("review.API.url.header"));
            httpGet.addHeader(AUTHORIZATION, BEARER + githubAccessToken);
        } catch (IllegalArgumentException e) {
            throw new CodeQualityMetricsException("The url provided for accessing the Github Review Commit API is " +
                    "invalid ", e);
        }
        return ApiUtility.invokeApi(httpGet);
    }

    /**
     * Used to call the github Issue Search API.
     *
     * @param commitHashToBeSearched commit hash to be searched for issues
     * @param githubAccessToken      Github access token for accessing github API
     * @return String representation of the json response
     * @throws CodeQualityMetricsException results
     */
    public static String callSearchIssueApi(String commitHashToBeSearched, String githubAccessToken) throws
            CodeQualityMetricsException {
        HttpGet httpGet;
        try {
            String url = defaultProperties.getProperty("search.issue.API.url") + commitHashToBeSearched;
            httpGet = new HttpGet(url);
            httpGet.addHeader(ACCEPT, defaultProperties.getProperty("search.issue.API.url.header"));
            httpGet.addHeader(AUTHORIZATION, BEARER + githubAccessToken);
        } catch (IllegalArgumentException e) {
            throw new CodeQualityMetricsException("The url provided for accessing the Github Search Issue API is " +
                    "invalid ", e);
        }
        return ApiUtility.invokeApi(httpGet);
    }

    /**
     * Used for calling the github graphql API.
     *
     * @param graphqlJsonStructure JSON input structure for calling the graphql API
     * @param githubToken          Github access token for accessing github API
     * @return String representation of the json response
     * @throws CodeQualityMetricsException results
     */
    public static String callGraphqlApi(JSONObject graphqlJsonStructure, String githubToken) throws
            CodeQualityMetricsException {
        HttpPost httpPost;
        try {
            String url = defaultProperties.getProperty("github.graphql.url");
            httpPost = new HttpPost(url);
            httpPost.addHeader(AUTHORIZATION, BEARER + githubToken);
            httpPost.addHeader(ACCEPT, defaultProperties.getProperty("github.graphql.url.header"));
            StringEntity entity = new StringEntity(graphqlJsonStructure.toString());
            httpPost.setEntity(entity);
        } catch (IllegalArgumentException e) {
            throw new CodeQualityMetricsException("The url provided for accessing the Github Graphql API is " +
                    "invalid", e);
        } catch (UnsupportedEncodingException e) {
            throw new CodeQualityMetricsException("An error occurred when creating the String entity from Json " +
                    "Structure", e);
        }
        return ApiUtility.invokeGraphQlApi(httpPost);
    }

    /**
     * Used for calling the github User details REST API.
     *
     * @param userId      Required userId
     * @param githubToken Github access token for accessing github API
     * @return String representation of the json response
     */
    public static String invokeUserDetailsAPIForUserId(Integer userId, String githubToken)
            throws CodeQualityMetricsException {
        HttpGet httpGet;
        try {
            String url = defaultProperties.getProperty("user.details.for.user.id.API.url") + userId;
            httpGet = new HttpGet(url);
            httpGet.addHeader(AUTHORIZATION, BEARER + githubToken);
        } catch (IllegalArgumentException e) {
            throw new CodeQualityMetricsException("The url provided for accessing the Github User details REST API is" +
                    " invalid", e);
        }
        return ApiUtility.invokeApi(httpGet);
    }

    public static String invokeUserDetailsAPIForLoginName(String loginName, String githubToken)
            throws CodeQualityMetricsException {
        HttpGet httpGet;
        try {
            String url = defaultProperties.getProperty("user.details.for.login.name.API.url") + loginName;
            httpGet = new HttpGet(url);
            httpGet.addHeader(AUTHORIZATION, BEARER + githubToken);
        } catch (IllegalArgumentException e) {
            throw new CodeQualityMetricsException("The url provided for accessing the Github User details REST API is" +
                    " invalid", e);
        }
        return ApiUtility.invokeApi(httpGet);
    }
}
