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

import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Properties;

/**
 * Contains the entry point to this application. The patch id should be passed as a command line argument for finding
 * authors, approved reviewers, commented  reviewers and mergers of bug lines of code which are been fixed from the
 * given patch.
 *
 * @since 1.0.0
 */
public class Application {
    private static final Logger logger = Logger.getLogger(Application.class);

    public static void main(String[] args) {
        if (args.length == 1) {
            String patchId = args[0];
            Properties defaultProperties = new Properties();
            try (InputStream inputStream = Application.class.getResourceAsStream
                    ("/code.quality.metrics.properties")) {
                defaultProperties.load(inputStream);
                byte[] pmtTokenInBytes = Base64.getDecoder().decode(defaultProperties.getProperty("pmt.token"));
                String pmtToken = new String(pmtTokenInBytes, StandardCharsets.UTF_8);
                byte[] githubTokenInBytes = Base64.getDecoder().decode(defaultProperties.getProperty("github.token"));
                String githubToken = new String(githubTokenInBytes, StandardCharsets.UTF_8);
                CodeQualityMetricsExecutor codeQualityMetricsExecutor = new CodeQualityMetricsExecutor(pmtToken,
                        patchId, githubToken);
                codeQualityMetricsExecutor.execute();
            } catch (IOException e) {
                logger.fatal("IO exception occurred when loading the inputstream to the " +
                        "properties object", e);
            }
        } else {
            logger.fatal("Incorrect command line arguments were given");
            logger.debug("Please enter only the required patch id as the command line argument to initiate the " +
                    "program");
        }
    }
}
