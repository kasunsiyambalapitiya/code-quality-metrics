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


import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.wso2.code.quality.metrics.exceptions.CodeQualityMetricsException;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * A class used to test findCommitHashesInPatch method of the CodeQualityMetricsExecutor class
 *
 * @since 1.0.0
 */
public class CodeQualityMetricsExecutorTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void testFindCommitHashesInPatch() throws CodeQualityMetricsException {
        String pmtToken = new Token().getPmtToken();
        String githubToken = new Token().getGithubToken();
        Map<String, List<String>> patchesAndCommits = new HashMap<>();
        patchesAndCommits.put("WSO2-CARBON-PATCH-4.4.0-0680", Arrays.asList("eaa45529cbabc5f30a2ffaa4781821ad0a5223ab"
                , "2b1d973d089ebc3af3b9e7b893f48cf905758cf4"));
        patchesAndCommits.put("WSO2-CARBON-PATCH-4.4.0-0682",
                Collections.singletonList("e3c3457149b109178d510aac965d5a85cc465aa0"));
        patchesAndCommits.put("WSO2-CARBON-PATCH-4.4.0-0692",
                Collections.singletonList("67a60e081c8e0fe01d087f60cd9b629bcea172ae"));
        for (Map.Entry<String, List<String>> map : patchesAndCommits.entrySet()) {
            CodeQualityMetricsExecutor codeQualityMetricsExecutor = new CodeQualityMetricsExecutor(pmtToken,
                    map.getKey(), githubToken);
            List commitHashes = codeQualityMetricsExecutor.findCommitHashesInPatch();
            assertEquals("Must match with the relevant commit list", map.getValue(), commitHashes);
        }
    }

    @Test
    public void testFindCommitHashesInPatchForException() throws CodeQualityMetricsException {
        CodeQualityMetricsExecutor codeQualityMetricsExecutor = new CodeQualityMetricsExecutor("pmt.token",
                "patch1", "github.token");
        exception.expect(CodeQualityMetricsException.class);
        exception.expectMessage("Error occurred while calling PMT API");
        codeQualityMetricsExecutor.findCommitHashesInPatch();
    }
}
