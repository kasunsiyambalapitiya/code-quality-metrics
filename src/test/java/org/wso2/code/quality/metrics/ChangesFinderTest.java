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

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * A class used to test identifyChangedFile method of the ChangeFinder class
 *
 * @since 1.0.0
 */
public class ChangesFinderTest {
    private static final Logger logger = Logger.getLogger(ChangesFinderTest.class);

    @Test
    public void testIdentifyDeletedLines() {
        String patchString;
        Set<Integer> actualDeltedLinesForPatchString;
        Set<Integer> expectedDeltedLinesForPatchString1 = new HashSet<>();
        expectedDeltedLinesForPatchString1.add(822);
        expectedDeltedLinesForPatchString1.add(823);
        expectedDeltedLinesForPatchString1.add(824);
        expectedDeltedLinesForPatchString1.add(907);
        expectedDeltedLinesForPatchString1.add(908);
        expectedDeltedLinesForPatchString1.add(909);
        expectedDeltedLinesForPatchString1.add(1081);
        expectedDeltedLinesForPatchString1.add(1082);
        expectedDeltedLinesForPatchString1.add(1083);
        expectedDeltedLinesForPatchString1.add(1087);
        expectedDeltedLinesForPatchString1.add(1094);
        expectedDeltedLinesForPatchString1.add(1143);
        expectedDeltedLinesForPatchString1.add(1144);
        expectedDeltedLinesForPatchString1.add(1145);
        expectedDeltedLinesForPatchString1.add(1146);
        expectedDeltedLinesForPatchString1.add(1147);
        expectedDeltedLinesForPatchString1.add(1148);
        expectedDeltedLinesForPatchString1.add(1149);
        expectedDeltedLinesForPatchString1.add(1150);
        expectedDeltedLinesForPatchString1.add(1151);
        expectedDeltedLinesForPatchString1.add(1152);
        expectedDeltedLinesForPatchString1.add(1153);
        expectedDeltedLinesForPatchString1.add(1165);
        try {
            Class<?> changeFinderClass = Class.forName("org.wso2.code.quality.metrics.ChangesFinder");
            Object changeFinder = changeFinderClass.newInstance();
            Method identifyDeletedLinesMethod = changeFinderClass.getDeclaredMethod("identifyDeletedLines",
                    String.class);
            identifyDeletedLinesMethod.setAccessible(true);
            patchString = readFile("patchStrings/patchString1.txt");
            actualDeltedLinesForPatchString = (Set<Integer>) identifyDeletedLinesMethod.invoke(changeFinder,
                    patchString);
            assertThat(actualDeltedLinesForPatchString.size(), is(expectedDeltedLinesForPatchString1.size()));
            assertThat(actualDeltedLinesForPatchString, is(expectedDeltedLinesForPatchString1));
            patchString = readFile("patchStrings/patchString2.txt");
            actualDeltedLinesForPatchString = (Set<Integer>) identifyDeletedLinesMethod.invoke(changeFinder,
                    patchString);
            assertTrue(actualDeltedLinesForPatchString.isEmpty());
        } catch (IllegalAccessException | ClassNotFoundException | InstantiationException | NoSuchMethodException
                | InvocationTargetException e) {
            logger.error(e.getMessage(), e.getCause());
        }
    }

    /**
     * This is used to get the contents of the txt file as a single String
     *
     * @param path location path to the relevant txt file
     * @return single String containing all the contents of the given file
     */
    private String readFile(String path) {
        String result = "";
        try {
            result = IOUtils.toString(getClass().getResourceAsStream("/" + path));
        } catch (IOException e) {
            logger.error(e.getMessage(), e.getCause());
        }
        return result;
    }
}
