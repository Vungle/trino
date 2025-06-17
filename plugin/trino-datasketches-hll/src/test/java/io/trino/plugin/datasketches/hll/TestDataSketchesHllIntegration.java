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

import io.trino.testing.AbstractTestQueryFramework;
import io.trino.testing.LocalQueryRunner;
import io.trino.testing.QueryRunner;
import org.testng.annotations.Test;

import static io.trino.testing.TestingSession.testSessionBuilder;

public class TestDataSketchesHllIntegration
        extends AbstractTestQueryFramework
{
    @Override
    protected QueryRunner createQueryRunner()
            throws Exception
    {
        LocalQueryRunner queryRunner = LocalQueryRunner.builder(testSessionBuilder().build())
                .build();
        queryRunner.installPlugin(new DataSketchesHllPlugin());
        return queryRunner;
    }

    @Test
    public void testHllCreateAndEstimate()
    {
        assertQuery(
                "SELECT hll_estimate(hll_create())",
                "SELECT 0.0");

        assertQuery(
                "SELECT hll_estimate(hll_create(14))",
                "SELECT 0.0");
    }

    @Test
    public void testHllAddAndEstimate()
    {
        assertQuery(
                "SELECT hll_estimate(hll_add(hll_create(), 'test'))",
                "SELECT 1.0");

        assertQuery(
                "SELECT hll_estimate(hll_add(hll_add(hll_create(), 'test1'), 'test2'))",
                "SELECT 2.0");
    }

    @Test
    public void testHllUnion()
    {
        assertQuery(
                "SELECT hll_estimate(hll_union(" +
                        "hll_add(hll_create(), 'test1'), " +
                        "hll_add(hll_create(), 'test2')))",
                "SELECT 2.0");
    }

    @Test
    public void testHllStringConversion()
    {
        assertQuery(
                "SELECT hll_estimate(hll_from_string(hll_to_string(hll_add(hll_create(), 'test'))))",
                "SELECT 1.0");
    }

    @Test
    public void testHllValidation()
    {
        assertQuery(
                "SELECT hll_validate(hll_create())",
                "SELECT true");

        assertQuery(
                "SELECT hll_validate(CAST(X'010203' AS VARBINARY))",
                "SELECT false");
    }

    @Test
    public void testHllMemoryUsage()
    {
        assertQuery(
                "SELECT " +
                        "hll_get_serialization_bytes(hll_create()) > 0, " +
                        "hll_get_compact_bytes(hll_create()) > 0, " +
                        "hll_get_updatable_bytes(hll_create()) > 0",
                "SELECT true, true, true");
    }

    @Test
    public void testHllErrorBounds()
    {
        assertQuery(
                "SELECT " +
                        "hll_upper_bound(hll_add(hll_create(), 'test'), 2.0) >= " +
                        "hll_estimate(hll_add(hll_create(), 'test'))",
                "SELECT true");

        assertQuery(
                "SELECT " +
                        "hll_lower_bound(hll_add(hll_create(), 'test'), 2.0) <= " +
                        "hll_estimate(hll_add(hll_create(), 'test'))",
                "SELECT true");
    }

    @Test
    public void testHllAggregation()
    {
        assertQuery(
                "SELECT hll_estimate(hll_union_agg(hll_add(hll_create(), 'test')))",
                "SELECT 1.0");
    }
}
