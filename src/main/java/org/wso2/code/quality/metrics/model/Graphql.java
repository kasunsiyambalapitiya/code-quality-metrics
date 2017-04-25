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

package org.wso2.code.quality.metrics.model;

import java.io.Serializable;

/**
 * This is a bean class used to set the json input structures for calling the github graphql API.
 *
 * @since 1.0.0
 */
public class Graphql implements Serializable {
    private String graphqlInputWithoutHistory;
    private static final long serialVersionUID = 42L;

    public Graphql() {
    }

    public String getGraphqlInputWithoutHistory() {
        return graphqlInputWithoutHistory;
    }

    public void setGraphqlInputWithoutHistory(String owner, String repositoryName, String
            parentCommitHashForCallingGraphQl, String fileName) {
        this.graphqlInputWithoutHistory = "{repository(owner:\"" + owner + "\",name:\"" + repositoryName + "\")" +
                "{object(expression:\"" + parentCommitHashForCallingGraphQl + "\"){ ... on Commit{blame(path:\"" +
                fileName + "\"){ranges{startingLine endingLine age commit{ url author { name email user {login}} } } " +
                "} } } } }";
    }
}
