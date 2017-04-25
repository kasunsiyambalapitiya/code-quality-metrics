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
import org.json.JSONObject;
import org.wso2.code.quality.metrics.exceptions.CodeQualityMetricsException;
import org.wso2.code.quality.metrics.model.CommitHistoryApiResponse;
import org.wso2.code.quality.metrics.model.GraphQlResponse;
import org.wso2.code.quality.metrics.model.Graphql;
import org.wso2.code.quality.metrics.model.SearchApiResponse;
import org.wso2.code.quality.metrics.model.SingleCommitApiResponse;
import org.wso2.code.quality.metrics.model.UserDetails;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Used for getting the blame information on relevant changed line ranges on relevant files from the
 * given patch.
 *
 * @since 1.0.0
 */

public class ChangesFinder {
    private static final Logger logger = Logger.getLogger(ChangesFinder.class);

    private Map<String, String> authorNamesWithLoginNames = new HashMap<>();
    Set<String> identifiableAuthorNames = new HashSet<>();
    private Set<String> authorCommits = new HashSet<>();
    private final JSONObject jsonStructure = new JSONObject();
    private final Gson gson = new Gson();

    /**
     * Used to prevent SIC_INNER_SHOULD_BE_STATIC_ANON error that comes when building with WSO2 parent
     * pom, as suggested by the above error an static inner class is used to prevent the error.
     */
    private static class ListType extends TypeToken<List<CommitHistoryApiResponse>> {
    }

    /**
     * Used for obtaining the repositories that contain the relevant commits belongs to the given patch.
     *
     * @param gitHubToken  github token for accessing github API
     * @param commitHashes List of commits that belongs to the given patch
     * @return author commits of the bug lines which are fixed from the given patch
     */
    Set<String> obtainRepoNamesForCommitHashes(String gitHubToken, List<String> commitHashes) {
        commitHashes.forEach(commitHash -> {
            try {
                String jsonText = GithubApiCallerUtils.callSearchCommitApi(commitHash, gitHubToken);
                List<String> repoLocations = getRepoNames(jsonText);
                identifyChangedFile(repoLocations, commitHash, gitHubToken);
            } catch (CodeQualityMetricsException e) {
                logger.fatal(e.getMessage(), e.getCause());
            }
        });
        return authorCommits;
    }

    /**
     * Use to save the relevant repository names that contain the given commit hash to a List.
     *
     * @param jsonText String representation of the json response
     * @return A list of repository locations having the given commit hash
     * @throws CodeQualityMetricsException Resulted Code Quality Metrics Exception
     */
    private List<String> getRepoNames(String jsonText) throws CodeQualityMetricsException {
        List<String> repoLocation = new ArrayList<>();
        SearchApiResponse searchCommitPojo;
        try {
            searchCommitPojo = gson.fromJson(jsonText, SearchApiResponse.class);
            searchCommitPojo.getContainingRepositories()
                    .forEach(recordItem -> repoLocation.add(recordItem.getRepository().getRepositoryLocation()));
            logger.debug("Repositories having the given commit are successfully saved in an List");
        } catch (JsonSyntaxException e) {
            throw new CodeQualityMetricsException("The received json is not a valid representation for an object of " +
                    "SearchApiResponse", e.getCause());
        }
        return repoLocation;
    }

    /**
     * Used to identify the file changed and their relevant line ranges changed in selected repository from the given
     * commit hash.
     *
     * @param repoLocation List of repository locations having the given commit hash
     * @param commitHash   commit hash to be searched for containing WSO2 repositories
     * @param githubtoken  Github access token for accessing github API
     */
    private void identifyChangedFile(List<String> repoLocation, String commitHash, String githubtoken) {
        GitHubSDKClient gitHubSDKClient = new GitHubSDKClient(githubtoken);
        repoLocation.stream()
                .filter(repositoryName -> StringUtils.contains(repositoryName, "wso2/"))
                .forEach(repositoryName -> {
                    Map<String, String> fileNamesWithPatchString;
                    tryBlock:
                    try {
                        fileNamesWithPatchString = gitHubSDKClient.getFilesChanged(repositoryName, commitHash);
                        Map<String, Set<Integer>> fileNamesWithDeletedLineNumbers = new HashMap<>();
                        Map<String, String> fileNamesWithPreviousCommitHash = new HashMap<>();
                        if (fileNamesWithPatchString.isEmpty()) {
                            break tryBlock;
                        }
                        /* looping from one file to file and saving deleted lines against the file name in another
                             map */
                        fileNamesWithPatchString.forEach((fileName, patchString) -> {
                            Set<Integer> deletedLines = identifyDeletedLines(patchString);
                            String previousCommitHashOfFile;
                            //for omitting files without having deleted lines in other words newly created files
                            if (!deletedLines.isEmpty()) {
                                fileNamesWithDeletedLineNumbers.put(fileName, deletedLines);
                                previousCommitHashOfFile = checkForOctopusMerge(repositoryName, fileName,
                                        commitHash, githubtoken);
                                if (previousCommitHashOfFile != null && !previousCommitHashOfFile.isEmpty()) {
                                    fileNamesWithPreviousCommitHash.put(fileName, previousCommitHashOfFile);
                                } else {
                                    logger.warn("The changes from " + commitHash + " on " + fileName + " file " +
                                            "may have been reversed from another commit on the same PR, so " +
                                            "commit " + commitHash + " does not appear in history of current " +
                                            "file due to git's history simplification when listing history");
                                }
                            } else {
                                logger.debug(fileName + " filename is a newly created file");
                            }
                        });
                        fetchBlameDetails(repositoryName, fileNamesWithPreviousCommitHash,
                                fileNamesWithDeletedLineNumbers, githubtoken);
                        saveIdentifiableAuthorNames(githubtoken);
                    } catch (CodeQualityMetricsException e) {
                        logger.error(e.getMessage(), e.getCause());
                    }
                });
        if (logger.isDebugEnabled()) {
            logger.debug("\n Author names :" + identifiableAuthorNames);
            logger.debug("\n Author commits :" + authorCommits);
        }
    }

    /**
     * Used to check whether the commit in the given patch is an octopuss commit or not, if it is octopuss commit an
     * exception is thrown as the current scope of the program does not support octopus commits.
     *
     * @param repositoryName   current selected Repository
     * @param filePath         current selected file name
     * @param latestCommitHash current selected recent commit contained in the given patch
     * @param githubToken      github access token for accessing github REST API
     * @return the commit hash returned from findPreviousCommitOfFile method
     */
    private String checkForOctopusMerge(String repositoryName, String filePath, String latestCommitHash, String
            githubToken) {
        String previousCommitOfFile = null;
        try {
            // need check whether the given commit has more than 2 parent commits
            String singleCommitJsonText = GithubApiCallerUtils.callSingleCommitApi(repositoryName, latestCommitHash,
                    githubToken);
            SingleCommitApiResponse singleCommitApiResponse = gson.fromJson(singleCommitJsonText,
                    SingleCommitApiResponse.class);
            if (singleCommitApiResponse.getParentCommits().size() == 1) {
                    /*if the singleCommitApiResponse.getParentCommits().size() == 1 it is fast forward merge, so no need
                 to change the latestCommitHash*/
                if (logger.isDebugEnabled()) {
                    logger.debug("The commit " + latestCommitHash + " in relevant patch is not a octopuss merge " +
                            "commit");
                }
            } else if (singleCommitApiResponse.getParentCommits().size() == 2) {
                latestCommitHash = singleCommitApiResponse.getParentCommits().get(1).getCommitHash();
                if (logger.isDebugEnabled()) {
                    logger.debug("The commit " + latestCommitHash + " in relevant patch is not a octopuss merge " +
                            "commit");
                }
            } else if (singleCommitApiResponse.getParentCommits().size() > 2) {
                throw new CodeQualityMetricsException("The commit " + latestCommitHash + " in relevant patch is " +
                        "an octopuss merge commit, octopuss merge commits are avoided in the current scope of " +
                        "the program");
            }
            previousCommitOfFile = findPreviousCommitOfFile(repositoryName, filePath, latestCommitHash, githubToken);
        } catch (CodeQualityMetricsException e) {
            logger.error(e.getMessage(), e.getCause());
        }
        return previousCommitOfFile;
    }

    /**
     * Used to get the previous commit hash of the selected file before the current selected commit which
     * contained in the given patch.
     *
     * @param repositoryName   current selected Repository
     * @param filePath         current selected file name
     * @param latestCommitHash current selected recent commit contained in the given patch
     * @param githubToken      github access token for accessing github REST API
     * @return commits hash prior to the current selected commit hash contained in the given patch of the current
     * selected file
     */
    private String findPreviousCommitOfFile(String repositoryName, String filePath, String latestCommitHash,
                                            String githubToken) {
        String previousCommitOfFile = null;
        Map<String, String> commitWithDate = new HashMap<>();
        try {
            String commitHistoryJsonText = GithubApiCallerUtils.callCommitHistoryApi(repositoryName, filePath,
                    githubToken);
            Type listType = new ListType().getType();
            List<CommitHistoryApiResponse> commitHistoryApiResponses = gson.fromJson(commitHistoryJsonText,
                    listType);
            commitHistoryApiResponses.forEach(commitHistoryApiResponse -> {
                String commitHash = commitHistoryApiResponse.getCommitHash();
                String date = commitHistoryApiResponse.getCommit().getAuthor().getDate();
                commitWithDate.put(commitHash, date);
            });
            String latestCommitDate = commitWithDate.get(latestCommitHash);
            if (latestCommitDate != null && !latestCommitDate.isEmpty()) {
                String previousCommitDate = getPreviousCommitDate(commitWithDate, latestCommitDate);
                // looping for finding the commit Hash introduced in previous commit date
                for (Map.Entry entry : commitWithDate.entrySet()) {
                    if (entry.getValue().equals(previousCommitDate)) {
                        previousCommitOfFile = (String) entry.getKey();
                    }
                }
            } else {
                return null;
                /*the changes from given commit to the current selected file may have been reversed from another commit,
                 so the given commit does not appear in history of the current file due to git's history simplification
                  when listing history*/
            }
        } catch (CodeQualityMetricsException e) {
            logger.error(e.getMessage(), e.getCause());
        }
        return previousCommitOfFile;
    }

    /**
     * Used to get the previous commit date of the selected file before the current commit by sorting the
     * commit dates.
     *
     * @param commitWithDate   map contating all the commits with their respective date of the current selected
     *                         file
     * @param latestCommitDate latest commit date of the current selected file
     * @return previous commit date of the current selected file
     */
    private String getPreviousCommitDate(Map<String, String> commitWithDate, String latestCommitDate) {
        //creating a temporary list for sorting the date in ascending order
        List<String> sortedCommitDates = new ArrayList<>(commitWithDate.values());
        Collections.sort(sortedCommitDates);
        int indexOfLatestcommit = sortedCommitDates.indexOf(latestCommitDate);
        return sortedCommitDates.get(--indexOfLatestcommit);
    }

    /**
     * Used to obtain the blame details of files for their previous commits.
     *
     * @param repoLocation                    current selected Repository
     * @param fileNamesWithPreviousCommitHash map containing changed files with their prior commit hashes
     * @param fileNamesWithDeletedLineNumbers map containing changed files with their deleted line numbers
     * @param githubToken                     github access token for accessing github REST API
     */
    private void fetchBlameDetails(String repoLocation, Map<String, String> fileNamesWithPreviousCommitHash,
                                   Map<String, Set<Integer>> fileNamesWithDeletedLineNumbers, String githubToken) {
        // filtering the owner and the repository name from the repoLocation
        String owner = StringUtils.substringBefore(repoLocation, "/");
        String repositoryName = StringUtils.substringAfter(repoLocation, "/");
        Graphql graphqlBean = new Graphql();
        fileNamesWithPreviousCommitHash.forEach((fileName, previousCommitHash) -> {
            graphqlBean.setGraphqlInputWithoutHistory(owner, repositoryName, previousCommitHash, fileName);
            jsonStructure.put("query", graphqlBean.getGraphqlInputWithoutHistory());
            String jsonText;
            try {
                // calling the graphql API for getting blame information for the current file.
                jsonText = GithubApiCallerUtils.callGraphqlApi(jsonStructure, githubToken);
                findAuthorCommits(jsonText, fileNamesWithDeletedLineNumbers, fileName);
            } catch (CodeQualityMetricsException e) {
                logger.error(e.getMessage(), e.getCause());
            }
        });
    }

    /**
     * Used to save the authors and relevant author commits of the buggy lines of code which are been fixed by
     * the given patch, to relevant lists.
     *
     * @param jsonText                        String representation of the json response received from github graphql
     *                                        API
     * @param fileNamesWithDeletedLineNumbers map containing changed files with their deleted line numbers
     * @param selectedFileName                selected file for finding the authors and author commits of its buggy
     *                                        lines
     */
    private void findAuthorCommits(String jsonText, Map<String, Set<Integer>> fileNamesWithDeletedLineNumbers, String
            selectedFileName) {
        GraphQlResponse graphQlResponse = gson.fromJson(jsonText, GraphQlResponse.class);
        Set<Integer> deletedLines = fileNamesWithDeletedLineNumbers.get(selectedFileName);
        deletedLines.forEach(deletedLineNumber ->
                graphQlResponse.getResponse().getRepository().getFile().getBlame().getRanges()
                        .stream()
                        .filter(graphqlRange -> (graphqlRange.getStartingLine() <= deletedLineNumber &&
                                graphqlRange.getEndingLine() >= deletedLineNumber))
                        .forEach(graphqlRange -> {
                            String authorName = graphqlRange.getCommit().getAuthor().getName();
                            String authorLoginName = "";
                            if (graphqlRange.getCommit().getAuthor().getUser() != null) {
                                // some blame response ranges may not have a user field
                                authorLoginName = graphqlRange.getCommit().getAuthor().getUser()
                                        .getUserLoginName();
                            }
                            authorNamesWithLoginNames.put(authorName, authorLoginName);
                            String authorcommit = StringUtils.substringAfter(graphqlRange.getCommit().getUrl(),
                                    "commit/");
                            authorCommits.add(authorcommit); // author commits are added to the set
                        }));
    }

    /**
     * Used to identify the deleted lines in the selected file from the current selected commit in given patch.
     *
     * @param patchString patch string of the selected file received from github SDK
     * @return a Set of deleted lines in the above mentioned file
     */
    private Set<Integer> identifyDeletedLines(String patchString) {
        Set<Integer> linesDeletedInSelectedFile = new HashSet<>();
        Pattern pattern = Pattern.compile("@@ -");
        String patches[] = pattern.split(patchString);
        for (String patchRange : patches) {
            Scanner scanner = new Scanner(patchRange);
            int lineNumber = 0;
            //for finding the starting line number of the modified range.
            if (scanner.hasNext()) {
                String patchLine = scanner.nextLine();
                String words[] = patchLine.split(",");
                //String at the first index of word array is the starting line number of the modified range
                lineNumber = Integer.parseInt(words[0]);
            }
            //for finding the deleted lines in the string
            while (scanner.hasNext()) {
                String patchLine = scanner.nextLine();
                String words[];
                //to differentiate completely empty lines ("") and empty lines with spaces and tabs
                if (patchLine.trim().length() != 0) {
                    words = patchLine.split("\\s");
                } else {
                    words = patchLine.trim().split("\\s");
                }
                String sign = words[0];
                // lines starting with "+" are neglected as they are newly introduced lines from the given commit
                if ("-".equals(sign) || "".equals(sign)) {
                    if ("-".equals(sign)) {
                        //lines starting with "-" are the modified lines in the old file
                        linesDeletedInSelectedFile.add(lineNumber);
                    }
                    lineNumber++;
                }
            }
        }
        return linesDeletedInSelectedFile;
    }

    /**
     * Used for obtaining identifiable author names as all the users are not using their real name as their username.
     *
     * @return Set having identifiable author names
     */
    private void saveIdentifiableAuthorNames(String githubToken) {

        authorNamesWithLoginNames.forEach((authorName, loginName) -> {
            String tempIdentifiableAuthorName = authorName;
            if (!loginName.isEmpty()) {
                try {
                    String jsonText = GithubApiCallerUtils.invokeUserDetailsAPIForLoginName(loginName, githubToken);
                    UserDetails userDetails = gson.fromJson(jsonText, UserDetails.class);
                    String fullName = userDetails.getUserName();
                    if (fullName != null && !fullName.isEmpty()) {
                        tempIdentifiableAuthorName = tempIdentifiableAuthorName + "(" + fullName + ")";
                    } else {
                        fullName = "";      //null can be returned from the API for the full name
                        tempIdentifiableAuthorName = tempIdentifiableAuthorName + "(" + fullName + ")";
                    }
                } catch (CodeQualityMetricsException e) {
                    logger.error(e.getMessage(), e.getCause());
                }
            } else {
                tempIdentifiableAuthorName = tempIdentifiableAuthorName + "()";
            }
            identifiableAuthorNames.add(tempIdentifiableAuthorName);
        });
    }
}
