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
package io.prestosql.testing.datatype;

import io.prestosql.testing.sql.PrestoSqlExecutor;
import io.prestosql.testing.sql.SqlExecutor;
import io.prestosql.testing.sql.TestTable;

import java.util.List;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;

public class CreateAndPrestoInsertDataSetup
        implements DataSetup
{
    private final SqlExecutor sqlExecutor;
    private final PrestoSqlExecutor prestoSqlExecutor;
    private final String tableNamePrefix;

    public CreateAndPrestoInsertDataSetup(SqlExecutor sqlExecutor, PrestoSqlExecutor prestoSqlExecutor, String tableNamePrefix)
    {
        this.sqlExecutor = sqlExecutor;
        this.prestoSqlExecutor = prestoSqlExecutor;
        this.tableNamePrefix = tableNamePrefix;
    }

    @Override
    public TestTable setupTestTable(List<ColumnSetup> inputs)
    {
        TestTable testTable = createTestTable(inputs);
        try {
            insertRows(testTable, inputs);
        }
        catch (Exception e) {
            testTable.close();
            throw e;
        }
        return testTable;
    }

    private void insertRows(TestTable testTable, List<ColumnSetup> inputs)
    {
        String valueLiterals = inputs.stream()
                .map(ColumnSetup::getInputLiteral)
                .collect(joining(", "));
        prestoSqlExecutor.execute(format("INSERT INTO %s VALUES(%s)", testTable.getName(), valueLiterals));
    }

    private TestTable createTestTable(List<ColumnSetup> inputs)
    {
        return new TestTable(sqlExecutor, tableNamePrefix, "(" + columnDefinitions(inputs) + ")");
    }

    private String columnDefinitions(List<ColumnSetup> inputs)
    {
        List<String> columnTypeDefinitions = inputs.stream()
                .map(input -> input.getDeclaredType().orElseThrow(() -> new IllegalArgumentException("declared type not set")))
                .collect(toList());
        return range(0, columnTypeDefinitions.size())
                .mapToObj(i -> format("col_%d %s", i, columnTypeDefinitions.get(i)))
                .collect(joining(",\n"));
    }
}
