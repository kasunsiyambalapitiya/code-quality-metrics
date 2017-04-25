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

import org.hamcrest.collection.IsMapContaining;
import org.junit.BeforeClass;
import org.junit.Test;
import org.wso2.code.quality.metrics.exceptions.CodeQualityMetricsException;

import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;


/**
 * A class used to getFilesChanged method of the GithubSdkClient class
 *
 * @since 1.0.0
 */
public class GithubSDKClientTest {
    private static GitHubSDKClient gitHubSDKClient;

    @BeforeClass
    public static void setupTheEnvironment() throws CodeQualityMetricsException {
        String githubToken = new Token().getGithubToken();
        gitHubSDKClient = new GitHubSDKClient(githubToken);
    }

    @Test
    public void testGetFilesChanged() throws CodeQualityMetricsException {
        Map<String, String> actualFileNamesAndPatches = gitHubSDKClient.getFilesChanged
                ("wso2/carbon-apimgt", "eaa45529cbabc5f30a2ffaa4781821ad0a5223ab");
        assertThat(actualFileNamesAndPatches.size(), is(4));
        assertThat(actualFileNamesAndPatches, IsMapContaining.hasKey("components/sso-hostobject/org.wso2.carbon" +
                ".hostobjects.sso/src/main/java/org/wso2/carbon/hostobjects/sso/SAMLSSORelyingPartyObject.java"));
        assertThat(actualFileNamesAndPatches, IsMapContaining.hasKey("components/sso-hostobject/org.wso2.carbon." +
                "hostobjects.sso/src/main/java/org/wso2/carbon/hostobjects/sso/internal/SessionInfo.java"));
        assertThat(actualFileNamesAndPatches, IsMapContaining.hasKey("features/apimgt/org.wso2.carbon.apimgt." +
                "publisher.feature/src/main/resources/publisher/site/themes/wso2/templates/sso/logout/template.jag"));
        assertThat(actualFileNamesAndPatches, IsMapContaining.hasKey("features/apimgt/org.wso2.carbon.apimgt." +
                "store.feature/src/main/resources/store/site/themes/wso2/templates/sso/logout/template.jag"));
    }
}
