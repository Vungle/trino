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
import io.trino.spi.Plugin;
import io.trino.spi.function.ScalarFunction;
import io.trino.spi.function.SqlType;
import io.trino.spi.type.StandardTypes;
import org.apache.datasketches.hll.HllSketch;
import org.apache.datasketches.hll.TgtHllType;
import org.apache.datasketches.memory.Memory;

import java.util.Set;

public class DataSketchesHllPlugin
        implements Plugin
{
    @Override
    public Set<Class<?>> getFunctions()
    {
        return Set.of(DataSketchesHllFunctions.class);
    }

    public static class DataSketchesHllFunctions
    {
        private static final int DEFAULT_LOG_K = 12; // Default precision parameter

        @ScalarFunction(value = "hll_create", deterministic = true)
        @SqlType(StandardTypes.VARBINARY)
        public static Slice hllCreate()
        {
            HllSketch sketch = new HllSketch(DEFAULT_LOG_K, TgtHllType.HLL_4);
            return Slices.wrappedBuffer(sketch.toCompactByteArray());
        }

        @ScalarFunction(value = "hll_create", deterministic = true)
        @SqlType(StandardTypes.VARBINARY)
        public static Slice hllCreate(@SqlType(StandardTypes.INTEGER) long logK)
        {
            HllSketch sketch = new HllSketch((int) logK, TgtHllType.HLL_4);
            return Slices.wrappedBuffer(sketch.toCompactByteArray());
        }

        @ScalarFunction("hll_add")
        @SqlType(StandardTypes.VARBINARY)
        public static Slice hllAdd(@SqlType(StandardTypes.VARBINARY) Slice sketch, @SqlType(StandardTypes.VARCHAR) Slice value)
        {
            if (sketch == null) {
                return hllCreate();
            }
            try {
                if (sketch.length() < 8) {
                    throw new IllegalArgumentException("Invalid HLL sketch: too small");
                }
                HllSketch hllSketch = HllSketch.heapify(Memory.wrap(sketch.getBytes()));
                if (value != null) {
                    hllSketch.update(value.toStringUtf8());
                }
                return Slices.wrappedBuffer(hllSketch.toCompactByteArray());
            }
            catch (Exception e) {
                throw new IllegalArgumentException("Invalid HLL sketch: " + e.getMessage());
            }
        }

        @ScalarFunction("hll_add")
        @SqlType(StandardTypes.VARBINARY)
        public static Slice hllAdd(@SqlType(StandardTypes.VARBINARY) Slice sketch, @SqlType(StandardTypes.BIGINT) long value)
        {
            if (sketch == null) {
                return hllCreate();
            }
            try {
                if (sketch.length() < 8) {
                    throw new IllegalArgumentException("Invalid HLL sketch: too small");
                }
                HllSketch hllSketch = HllSketch.heapify(Memory.wrap(sketch.getBytes()));
                hllSketch.update(value);
                return Slices.wrappedBuffer(hllSketch.toCompactByteArray());
            }
            catch (Exception e) {
                throw new IllegalArgumentException("Invalid HLL sketch: " + e.getMessage());
            }
        }

        @ScalarFunction("hll_add")
        @SqlType(StandardTypes.VARBINARY)
        public static Slice hllAdd(@SqlType(StandardTypes.VARBINARY) Slice sketch, @SqlType(StandardTypes.DOUBLE) double value)
        {
            if (sketch == null) {
                return hllCreate();
            }
            try {
                if (sketch.length() < 8) {
                    throw new IllegalArgumentException("Invalid HLL sketch: too small");
                }
                HllSketch hllSketch = HllSketch.heapify(Memory.wrap(sketch.getBytes()));
                hllSketch.update(value);
                return Slices.wrappedBuffer(hllSketch.toCompactByteArray());
            }
            catch (Exception e) {
                throw new IllegalArgumentException("Invalid HLL sketch: " + e.getMessage());
            }
        }

        @ScalarFunction("hll_estimate")
        @SqlType(StandardTypes.DOUBLE)
        public static double hllEstimate(@SqlType(StandardTypes.VARBINARY) Slice sketch)
        {
            HllSketch hllSketch = HllSketch.heapify(Memory.wrap(sketch.getBytes()));
            return hllSketch.getEstimate();
        }

        @ScalarFunction("hll_union")
        @SqlType(StandardTypes.VARBINARY)
        public static Slice hllUnion(@SqlType(StandardTypes.VARBINARY) Slice sketch1, @SqlType(StandardTypes.VARBINARY) Slice sketch2)
        {
            HllSketch union = HllSketch.heapify(Memory.wrap(sketch1.getBytes()));
            HllSketch other = HllSketch.heapify(Memory.wrap(sketch2.getBytes()));
            union.update(other.toCompactByteArray());
            return Slices.wrappedBuffer(union.toCompactByteArray());
        }

        @ScalarFunction("hll_union_agg")
        @SqlType(StandardTypes.VARBINARY)
        public static Slice hllUnionAgg(@SqlType(StandardTypes.VARBINARY) Slice sketch)
        {
            try {
                if (sketch.length() < 8) {
                    throw new IllegalArgumentException("Invalid HLL sketch: too small");
                }
                HllSketch hllSketch = HllSketch.heapify(Memory.wrap(sketch.getBytes()));
                // Create a new sketch to hold the union
                HllSketch union = new HllSketch(DEFAULT_LOG_K, TgtHllType.HLL_4);
                // Update the union with the input sketch
                union.update(hllSketch.toCompactByteArray());
                return Slices.wrappedBuffer(union.toCompactByteArray());
            }
            catch (Exception e) {
                throw new IllegalArgumentException("Invalid HLL sketch: " + e.getMessage());
            }
        }

        @ScalarFunction("hll_std_error")
        @SqlType(StandardTypes.DOUBLE)
        public static double hllStdError(@SqlType(StandardTypes.VARBINARY) Slice sketch)
        {
            HllSketch hllSketch = HllSketch.heapify(Memory.wrap(sketch.getBytes()));
            return hllSketch.getRelErr(true, true, 0, 0);
        }

        @ScalarFunction("hll_upper_bound")
        @SqlType(StandardTypes.DOUBLE)
        public static double hllUpperBound(@SqlType(StandardTypes.VARBINARY) Slice sketch, @SqlType(StandardTypes.DOUBLE) double numStdDev)
        {
            HllSketch hllSketch = HllSketch.heapify(Memory.wrap(sketch.getBytes()));
            return hllSketch.getUpperBound((int) numStdDev);
        }

        @ScalarFunction("hll_lower_bound")
        @SqlType(StandardTypes.DOUBLE)
        public static double hllLowerBound(@SqlType(StandardTypes.VARBINARY) Slice sketch, @SqlType(StandardTypes.DOUBLE) double numStdDev)
        {
            HllSketch hllSketch = HllSketch.heapify(Memory.wrap(sketch.getBytes()));
            return hllSketch.getLowerBound((int) numStdDev);
        }

        @ScalarFunction("hll_is_empty")
        @SqlType(StandardTypes.BOOLEAN)
        public static boolean hllIsEmpty(@SqlType(StandardTypes.VARBINARY) Slice sketch)
        {
            HllSketch hllSketch = HllSketch.heapify(Memory.wrap(sketch.getBytes()));
            return hllSketch.isEmpty();
        }

        @ScalarFunction("hll_get_log_k")
        @SqlType(StandardTypes.INTEGER)
        public static long hllGetLogK(@SqlType(StandardTypes.VARBINARY) Slice sketch)
        {
            HllSketch hllSketch = HllSketch.heapify(Memory.wrap(sketch.getBytes()));
            return hllSketch.getLgConfigK();
        }

        // New functions for working with Apache DataSketches format

        @ScalarFunction("hll_from_string")
        @SqlType(StandardTypes.VARBINARY)
        public static Slice hllFromString(@SqlType(StandardTypes.VARCHAR) Slice base64Slice)
        {
            try {
                String base64String = base64Slice.toStringUtf8();
                byte[] bytes = java.util.Base64.getDecoder().decode(base64String);
                HllSketch sketch = HllSketch.heapify(Memory.wrap(bytes));
                return Slices.wrappedBuffer(sketch.toCompactByteArray());
            }
            catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid HLL sketch format: " + e.getMessage());
            }
        }

        @ScalarFunction("hll_to_string")
        @SqlType(StandardTypes.VARCHAR)
        public static Slice hllToString(@SqlType(StandardTypes.VARBINARY) Slice sketch)
        {
            String base64String = java.util.Base64.getEncoder().encodeToString(sketch.getBytes());
            return Slices.utf8Slice(base64String);
        }

        @ScalarFunction("hll_validate")
        @SqlType(StandardTypes.BOOLEAN)
        public static boolean hllValidate(@SqlType(StandardTypes.VARBINARY) Slice sketch)
        {
            try {
                if (sketch.length() < 8) {
                    return false;
                }
                HllSketch.heapify(Memory.wrap(sketch.getBytes()));
                return true;
            }
            catch (Exception e) {
                return false;
            }
        }

        @ScalarFunction("hll_get_serialization_bytes")
        @SqlType(StandardTypes.INTEGER)
        public static long hllGetSerializationBytes(@SqlType(StandardTypes.VARBINARY) Slice sketch)
        {
            HllSketch hllSketch = HllSketch.heapify(Memory.wrap(sketch.getBytes()));
            return hllSketch.getUpdatableSerializationBytes();
        }

        @ScalarFunction("hll_get_compact_bytes")
        @SqlType(StandardTypes.INTEGER)
        public static long hllGetCompactBytes(@SqlType(StandardTypes.VARBINARY) Slice sketch)
        {
            HllSketch hllSketch = HllSketch.heapify(Memory.wrap(sketch.getBytes()));
            return hllSketch.getCompactSerializationBytes();
        }

        @ScalarFunction("hll_get_updatable_bytes")
        @SqlType(StandardTypes.INTEGER)
        public static long hllGetUpdatableBytes(@SqlType(StandardTypes.VARBINARY) Slice sketch)
        {
            HllSketch hllSketch = HllSketch.heapify(Memory.wrap(sketch.getBytes()));
            return hllSketch.getUpdatableSerializationBytes();
        }

        @ScalarFunction("hll_get_serialization_version")
        @SqlType(StandardTypes.INTEGER)
        public static long hllGetSerializationVersion(@SqlType(StandardTypes.VARBINARY) Slice sketch)
        {
            HllSketch hllSketch = HllSketch.heapify(Memory.wrap(sketch.getBytes()));
            return hllSketch.getSerializationVersion();
        }
    }
}
