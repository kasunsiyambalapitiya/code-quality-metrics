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

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;
import org.wso2.code.quality.metrics.exceptions.CodeQualityMetricsException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

/**
 * Utility class for calling APIs.
 *
 * @since 1.0.0
 */
public final class ApiUtility {
    private static final Logger logger = Logger.getLogger(ApiUtility.class);

    // to prevent instantiation
    private ApiUtility() {
    }

    /**
     * Used for calling the REST APIs.
     *
     * @param httpGet Instance of the relevant httpGet
     * @return String representation of the json response
     * @throws CodeQualityMetricsException results
     */
    public static String invokeApi(HttpGet httpGet) throws CodeQualityMetricsException {
        String jsonText;
        try (CloseableHttpClient httpClient = HttpClients.createDefault();
             CloseableHttpResponse httpResponse = httpClient.execute(httpGet)) {
            int responseCode = httpResponse.getStatusLine().getStatusCode();
            if (responseCode == 200) {
                //success
                try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(httpResponse.getEntity()
                        .getContent(), StandardCharsets.UTF_8))) {
                    StringBuilder stringBuilder = new StringBuilder();
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        stringBuilder.append(line);
                    }
                    // creating a JSON object from the response
                    jsonText = stringBuilder.toString();
                }
            } else {
                throw new CodeQualityMetricsException("Error occurred while calling the API, the response code is " +
                        responseCode);
            }
        } catch (ClientProtocolException e) {
            throw new CodeQualityMetricsException("ClientProtocolException when calling the REST API", e);
        } catch (NullPointerException e) {
            // thrown from both getStatusLine() and getEntity() method on httpResponse object
            throw new CodeQualityMetricsException(e.getMessage(), e.getCause());
        } catch (IOException e) {
            throw new CodeQualityMetricsException("IOException occurred when calling the REST API", e);
        }
        return jsonText;
    }

    /**
     * Used for calling the github graphql API.
     *
     * @param httpPost relevant instance of the httpost
     * @return String representation of the json response
     * @throws CodeQualityMetricsException results
     */
    public static String invokeGraphQlApi(HttpPost httpPost) throws CodeQualityMetricsException {
        String jsonText;
        try (CloseableHttpClient httpClient = HttpClients.createDefault();
             CloseableHttpResponse httpResponse = httpClient.execute(httpPost)) {
            int responseCode = httpResponse.getStatusLine().getStatusCode();
            if (responseCode == 200) {
                try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(httpResponse.getEntity()
                        .getContent(), StandardCharsets.UTF_8))) {
                    String line;
                    StringBuilder stringBuilder = new StringBuilder();
                    while ((line = bufferedReader.readLine()) != null) {
                        stringBuilder.append(line);
                    }
                    jsonText = stringBuilder.toString();
                    logger.debug("The response received from the Github GraphQL converted to a JSON text successfully");
                }
            } else {
                throw new CodeQualityMetricsException("Error occurred while calling the graphQL API, the response " +
                        "code is " + responseCode);
            }
        } catch (UnsupportedEncodingException e) {
            throw new CodeQualityMetricsException("Encoding error occurred before calling the github graphQL API", e);
        } catch (ClientProtocolException e) {
            throw new CodeQualityMetricsException("Client protocol exception occurred when calling the github" +
                    " graphQL API", e);
        } catch (IOException e) {
            throw new CodeQualityMetricsException("A problem or the connection was aborted while executing the" +
                    " httpPost", e);
        }
        return jsonText;
    }
}
