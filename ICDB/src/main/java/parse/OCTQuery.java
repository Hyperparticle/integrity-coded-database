package parse;

import crypto.CodeGen;
import crypto.Convert;
import com.google.common.base.Charsets;
import io.DBConnection;
import io.DataConverter;
import io.Format;
import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.ItemsList;
import net.sf.jsqlparser.expression.operators.relational.MultiExpressionList;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.util.SelectUtils;
import org.jooq.tools.StringUtils;
import org.jooq.util.mysql.information_schema.tables.Statistics;
import stats.RunStatistics;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Parses Tuple Queries
 *
 * Created on 7/18/2016
 * @author Dan Kondratyuk
 */
public class OCTQuery extends ICDBQuery {

    public OCTQuery(String query, DBConnection icdb, CodeGen codeGen, RunStatistics statistics) {
        super(query, icdb, codeGen, statistics);
    }

    ////////////
    // SELECT //
    ////////////

    @Override
    protected Statement parseConvertedQuery(Select select) {
        return select; // Return the original query. // TODO: convert SELECT * to return all non-icdb columns
    }

    /**
     * SELECT conversion. This effectively turns any SELECT query into a SELECT * query
     */
    @Override
    protected Statement parseVerifyQuery(Select select) {
        PlainSelect plainSelect = (PlainSelect) select.getSelectBody();
        List<SelectItem> selectItems = plainSelect.getSelectItems();

        // Check for the aggregate query
        List<String> aggregateFxnColumn = new ArrayList<>();

        for (SelectItem item : selectItems) {
            if (!(item instanceof  SelectExpressionItem)) { continue; }

            if (((SelectExpressionItem) item).getExpression() instanceof Function){
                Function function = (Function) ((SelectExpressionItem) item).getExpression();
                if (function!=null ){
                    if (function.getParameters()!=null){
                        isAggregateQuery=true;
                        String columnname=function.getParameters().toString();

                        if(!aggregateFxnColumn.contains(columnname.substring(1, columnname.length()-1)))
                            aggregateFxnColumn.add(columnname.substring(1, columnname.length()-1));
                        //map column name with operation for verification of aggregate function result
                        columnOperation.put(function.getName()+columnname,function.getName());
                    }else
                        return select;
                }
            }
        }

        List<SelectItem> selectList = new ArrayList<>();
        selectList.add(new AllColumns());

        // Convert query to a SELECT * to obtain all tuples
        plainSelect.setSelectItems(selectList);


        // Join
        // Sum, Count, Average, Min, Max

        return select;
    }

    ////////////
    // INSERT //
    ////////////

    @Override
    protected Statement parseConvertedQuery(Insert insert) {
        // Get expression list from query
        ItemsList itemsList = insert.getItemsList();

        if (itemsList instanceof MultiExpressionList) {
            ((MultiExpressionList) itemsList).getExprList().stream()
                    .map(ExpressionList::getExpressions)
                    .forEach(this::convertExpressionList);
        } else {
            List<Expression> expressions = ((ExpressionList) itemsList).getExpressions();
            convertExpressionList(expressions);
        }

        return insert;
    }

    @Override
    protected Statement parseVerifyQuery(Insert insert) {
        return null; // Verifying an insert statement is not necessary
    }

    ////////////
    // DELETE //
    ////////////

    @Override
    protected Statement parseConvertedQuery(Delete delete) {
        return delete; // Delete does not require any conversion
    }

    @Override
    protected Statement parseVerifyQuery(Delete delete) {
        // We verify delete so that we can revoke all deleted serial numbers
        Table table = delete.getTable();

        Select select = SelectUtils.buildSelectFromTableAndSelectItems(table, new AllColumns());

        // Apply the where clause to the SELECT
        PlainSelect plainSelect = (PlainSelect) select.getSelectBody();
        plainSelect.setWhere(delete.getWhere());

        plainSelect.setLimit(delete.getLimit());
        plainSelect.setOrderByElements(delete.getOrderByElements());

        return select;
    }

    ////////////
    // UPDATE //
    ////////////

    @Override
    protected Statement parseConvertedQuery(Update update) {
        List<Column> allColumns = icdb.getFields(update.getTables().get(0).toString())
                .stream().map(Column::new)
                .collect(Collectors.toList());
        update.setColumns(allColumns);

        List<Expression> expressions = updateSelectResults
            .map(record -> {
                List<Expression> values = new ArrayList<>();
                for (int i = 0; i < record.size() - 2; i++) {
                    final Object value = record.get(i);

                    if (value instanceof String) {
                        values.add(new StringValue("'" + value + "'"));
                    } else {
                        values.add(new HexValue(value.toString()));
                    }
                }

                // Add this serial to be revoked upon successful execution
                final long serial = (long) record.get(Format.SERIAL_COLUMN);
                serialsToBeRevoked.add(serial);

                return values;
            })
            .stream()
            .findFirst() // TODO: get all results
            .orElseThrow(() -> new RuntimeException("Failed to parse query"));

        convertExpressionList(expressions);
        update.setExpressions(expressions);

        return update;
    }

    @Override
    protected Statement parseVerifyQuery(Update update) {
        // TODO: one select query per table
        List<Table> tables = update.getTables();

        Select select = SelectUtils.buildSelectFromTableAndSelectItems(tables.get(0), new AllColumns());

        // Apply the where clause to the SELECT
        PlainSelect plainSelect = (PlainSelect) select.getSelectBody();
        plainSelect.setWhere(update.getWhere());

        return select;
    }

    /**
     * Generates a serial number and signature, and adds them to the list of expressions
     */
    private void convertExpressionList(List<Expression> expressions) {
        // Obtain the data bytes
        final List<String> data = expressions.stream()
                .map(expression -> {
                    // Get rid of those pesky quotes
                    if (expression instanceof StringValue) {
                        return ((StringValue) expression).getValue();
                    }

                    return expression.toString();
                })
                .collect(Collectors.toList());
        final String dataString = StringUtils.join(data.toArray());
        final byte[] dataBytes = dataString.getBytes(Charsets.UTF_8);

        DataConverter converter = new DataConverter(dataBytes, codeGen, icrl);

        // Add base64 representation of signature to store it in the query properly
        final String signatureString = Convert.toBase64(converter.getSignature());
        expressions.add(new HexValue("from_base64('" + signatureString + "')"));

        // Add serial number to expression list
        Long serial = converter.getSerial();
        expressions.add(new DoubleValue(serial.toString()));
    }

}
