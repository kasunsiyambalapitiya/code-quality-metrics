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

import org.wso2.code.quality.metrics.exceptions.CodeQualityMetricsException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Properties;

/**
 * This class is used for providing github and WSO2 PMT access tokens
 *
 * @since 1.0.0
 */
public class Token {
    private String pmtToken;
    private String githubToken;

    public Token() throws CodeQualityMetricsException {
        Properties defaultProperties = new Properties();
        try (InputStream inputStream = getClass().getResourceAsStream
                ("/code.quality.metrics.properties")) {
            defaultProperties.load(inputStream);
            byte[] pmtTokenInBytes = Base64.getDecoder().decode(defaultProperties.getProperty("pmt.token"));
            pmtToken = new String(pmtTokenInBytes, StandardCharsets.UTF_8);
            byte[] githubTokenInBytes = Base64.getDecoder().decode(defaultProperties.getProperty("github.token"));
            githubToken = new String(githubTokenInBytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new CodeQualityMetricsException("IO exception occurred when loading the inputstream to the " +
                    "properties object", e);
        }
    }

    public String getPmtToken() {
        return pmtToken;
    }

    public String getGithubToken() {
        return githubToken;
    }
}
