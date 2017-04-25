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
import org.wso2.code.quality.metrics.exceptions.CodeQualityMetricsException;
import org.wso2.code.quality.metrics.model.Constants;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Used for all the WSO2 PMT communications.
 *
 * @since 1.0.0
 */
public class PmtApiCaller {

    /**
     * Used for calling the WSO2 PMT REST API.
     *
     * @param accessToken WSO2 PMT access token
     * @param patchId     Patch Id
     * @return String representation of the json response
     * @throws CodeQualityMetricsException results
     */
    public String callApi(String accessToken, String patchId) throws CodeQualityMetricsException {
        HttpGet httpGet;
        Properties defaultProperties = new Properties();
        try (InputStream inputStream = PmtApiCaller.class.getResourceAsStream("/code.quality.metrics.properties")) {
            defaultProperties.load(inputStream);
            String pmtApiUrl = defaultProperties.getProperty("pmt.API.url");
            String pmtUrl = pmtApiUrl + patchId;
            httpGet = new HttpGet(pmtUrl);
            httpGet.addHeader(Constants.AUTHORIZATION, Constants.BEARER + accessToken);
        } catch (IllegalArgumentException e) {
            throw new CodeQualityMetricsException("The url provided for accessing the PMT API is invalid ", e);
        } catch (IOException e) {
            throw new CodeQualityMetricsException("IO exception occurred when loading the inputstream to the " +
                    "properties object", e);
        }
        return ApiUtility.invokeApi(httpGet);
    }
}
