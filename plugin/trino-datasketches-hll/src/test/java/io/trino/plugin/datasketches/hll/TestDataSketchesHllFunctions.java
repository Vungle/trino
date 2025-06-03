/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.trino.plugin.datasketches.hll;

import io.airlift.slice.Slice;
import io.airlift.slice.Slices;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class TestDataSketchesHllFunctions
{
    @Test
    public void testHllCreate()
    {
        Slice sketch = DataSketchesHllPlugin.DataSketchesHllFunctions.hllCreate();
        assertTrue(DataSketchesHllPlugin.DataSketchesHllFunctions.hllValidate(sketch));
        assertEquals(DataSketchesHllPlugin.DataSketchesHllFunctions.hllGetLogK(sketch), 12L);
    }

    @Test
    public void testHllCreateWithLogK()
    {
        Slice sketch = DataSketchesHllPlugin.DataSketchesHllFunctions.hllCreate(14);
        assertTrue(DataSketchesHllPlugin.DataSketchesHllFunctions.hllValidate(sketch));
        assertEquals(DataSketchesHllPlugin.DataSketchesHllFunctions.hllGetLogK(sketch), 14L);
    }

    @Test
    public void testHllAddAndEstimate()
    {
        Slice sketch = DataSketchesHllPlugin.DataSketchesHllFunctions.hllCreate();

        // Add some values
        sketch = DataSketchesHllPlugin.DataSketchesHllFunctions.hllAdd(sketch, "value1");
        sketch = DataSketchesHllPlugin.DataSketchesHllFunctions.hllAdd(sketch, 123L);
        sketch = DataSketchesHllPlugin.DataSketchesHllFunctions.hllAdd(sketch, 123.45);

        double estimate = DataSketchesHllPlugin.DataSketchesHllFunctions.hllEstimate(sketch);
        assertTrue(estimate > 0);
        assertTrue(estimate <= 3.0); // Should be close to 3 unique values
    }

    @Test
    public void testHllUnion()
    {
        // Create two sketches
        Slice sketch1 = DataSketchesHllPlugin.DataSketchesHllFunctions.hllCreate();
        Slice sketch2 = DataSketchesHllPlugin.DataSketchesHllFunctions.hllCreate();

        // Add values to each sketch
        sketch1 = DataSketchesHllPlugin.DataSketchesHllFunctions.hllAdd(sketch1, "value1");
        sketch2 = DataSketchesHllPlugin.DataSketchesHllFunctions.hllAdd(sketch2, "value2");

        // Union the sketches
        Slice union = DataSketchesHllPlugin.DataSketchesHllFunctions.hllUnion(sketch1, sketch2);

        double estimate = DataSketchesHllPlugin.DataSketchesHllFunctions.hllEstimate(union);
        assertTrue(estimate > 0);
        assertTrue(estimate <= 2.0); // Should be close to 2 unique values
    }

    @Test
    public void testHllStringConversion()
    {
        Slice originalSketch = DataSketchesHllPlugin.DataSketchesHllFunctions.hllCreate();
        originalSketch = DataSketchesHllPlugin.DataSketchesHllFunctions.hllAdd(originalSketch, "test");

        // Convert to string and back
        String base64String = DataSketchesHllPlugin.DataSketchesHllFunctions.hllToString(originalSketch);
        Slice reconstructedSketch = DataSketchesHllPlugin.DataSketchesHllFunctions.hllFromString(base64String);

        // Compare estimates
        double originalEstimate = DataSketchesHllPlugin.DataSketchesHllFunctions.hllEstimate(originalSketch);
        double reconstructedEstimate = DataSketchesHllPlugin.DataSketchesHllFunctions.hllEstimate(reconstructedSketch);
        assertEquals(originalEstimate, reconstructedEstimate, 0.0001);
    }

    @Test
    public void testHllValidation()
    {
        Slice validSketch = DataSketchesHllPlugin.DataSketchesHllFunctions.hllCreate();
        assertTrue(DataSketchesHllPlugin.DataSketchesHllFunctions.hllValidate(validSketch));

        Slice invalidSketch = Slices.wrappedBuffer(new byte[]{1, 2, 3});
        assertFalse(DataSketchesHllPlugin.DataSketchesHllFunctions.hllValidate(invalidSketch));
    }

    @Test
    public void testHllMemoryUsage()
    {
        Slice sketch = DataSketchesHllPlugin.DataSketchesHllFunctions.hllCreate();

        long serializationBytes = DataSketchesHllPlugin.DataSketchesHllFunctions.hllGetSerializationBytes(sketch);
        long compactBytes = DataSketchesHllPlugin.DataSketchesHllFunctions.hllGetCompactBytes(sketch);
        long updatableBytes = DataSketchesHllPlugin.DataSketchesHllFunctions.hllGetUpdatableBytes(sketch);

        assertTrue(serializationBytes > 0);
        assertTrue(compactBytes > 0);
        assertTrue(updatableBytes > 0);
        assertTrue(compactBytes <= serializationBytes);
        assertTrue(updatableBytes >= serializationBytes);
    }

    @Test
    public void testHllErrorBounds()
    {
        Slice sketch = DataSketchesHllPlugin.DataSketchesHllFunctions.hllCreate();
        sketch = DataSketchesHllPlugin.DataSketchesHllFunctions.hllAdd(sketch, "test");

        double estimate = DataSketchesHllPlugin.DataSketchesHllFunctions.hllEstimate(sketch);
        double stdError = DataSketchesHllPlugin.DataSketchesHllFunctions.hllStdError(sketch);
        double upperBound = DataSketchesHllPlugin.DataSketchesHllFunctions.hllUpperBound(sketch, 2.0);
        double lowerBound = DataSketchesHllPlugin.DataSketchesHllFunctions.hllLowerBound(sketch, 2.0);

        assertTrue(upperBound >= estimate);
        assertTrue(lowerBound <= estimate);
        assertTrue(stdError > 0);
    }
}
