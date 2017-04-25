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

import com.google.gson.annotations.SerializedName;

/**
 * Pojo class used for parsing JSON response received from github graphql API.
 *
 * @since 1.0.0
 */
public class GraphqlRange {
    @SerializedName("startingLine")
    private int startingLine;

    @SerializedName("endingLine")
    private int endingLine;

    @SerializedName("age")
    private int age;

    @SerializedName("commit")
    private GraphqlCommit commit;

    public int getStartingLine() {
        return startingLine;
    }

    public void setStartingLine(int startingLine) {
        this.startingLine = startingLine;
    }

    public int getEndingLine() {
        return endingLine;
    }

    public void setEndingLine(int endingLine) {
        this.endingLine = endingLine;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public GraphqlCommit getCommit() {
        return commit;
    }

    public void setCommit(GraphqlCommit commit) {
        this.commit = commit;
    }
}
