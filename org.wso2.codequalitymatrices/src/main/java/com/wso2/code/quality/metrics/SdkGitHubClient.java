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

package com.wso2.code.quality.metrics;

import org.apache.log4j.Logger;
import org.eclipse.egit.github.core.CommitFile;
import org.eclipse.egit.github.core.IRepositoryIdProvider;
import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.CommitService;
import org.eclipse.egit.github.core.service.RepositoryService;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is used for communicating with the github REST API from egit github API.
 *
 * @since 1.0.0
 */

public class SdkGitHubClient {
    private static final Logger logger = Logger.getLogger(SdkGitHubClient.class);

    private GitHubClient gitHubClient = null;
    private CommitService commitService = null;
    private RepositoryService repositoryService = null;

    SdkGitHubClient(String githubToken) {
        gitHubClient = new GitHubClient();
        gitHubClient.setOAuth2Token(githubToken);
        commitService = new CommitService(gitHubClient);
        repositoryService = new RepositoryService(gitHubClient);
    }

    /**
     * This method is used for saving the files changed and their relevant changed line ranges from
     * the given commit in the given Repository.
     *
     * @param repositoryName The Repository name that contain the given commit hash
     * @param commitHash     The querying commit hash
     * @return a map containg arraylist of file changed and their relevant patch
     */
    public Map<String, String> getFilesChanged(String repositoryName, String commitHash)
            throws CodeQualityMetricsException {
        Map<String, String> fileNamesAndPatches = new HashMap<>();
        try {
            IRepositoryIdProvider iRepositoryIdProvider = () -> repositoryName;
            RepositoryCommit repositoryCommit = commitService.getCommit(iRepositoryIdProvider, commitHash);
            List<CommitFile> filesChanged = repositoryCommit.getFiles();

            // this can be run parallely as patchString of a file will always be with the same file
            filesChanged.parallelStream()
                    .forEach(commitFile -> {
                        fileNamesAndPatches.put(commitFile.getFilename(),commitFile.getPatch());
                    });
            if (logger.isDebugEnabled()) {
                logger.debug("for commit hash" + commitHash + " on the " + repositoryName + " repository, files" +
                        " changed and their relevant patch strings are saved to the map successfully");
            }

        } catch (IOException e) {
            throw new CodeQualityMetricsException("IO Exception occurred when getting the commit of given SHA from " +
                    "the given Repository ", e);
        }
        return fileNamesAndPatches;
    }
}