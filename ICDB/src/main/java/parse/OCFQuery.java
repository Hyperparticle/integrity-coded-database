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
import net.sf.jsqlparser.util.TablesNamesFinder;
import org.jooq.Field;
import stats.RunStatistics;
import stats.Statistics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Parses Field Queries
 *
 * Created on 7/18/2016
 * @author Dan Kondratyuk
 */
public class OCFQuery extends ICDBQuery {

    public OCFQuery(String query, DBConnection icdb, CodeGen codeGen, RunStatistics statistics) {
        super(query, icdb, codeGen, statistics);
    }

    ////////////
    // SELECT //
    ////////////

    @Override
    protected Statement parseConvertedQuery(Select select) {
        return select; // Return the original query. // TODO: convert SELECT * to return all non-icdb columns
    }

    @Override
    protected Statement parseVerifyQuery(Select select) {
        TablesNamesFinder tableNamesFinder = new TablesNamesFinder();
        List<String> tables = tableNamesFinder.getTableList(select);

        PlainSelect plainSelect = (PlainSelect) select.getSelectBody();
        List<SelectItem> selectItems = plainSelect.getSelectItems();

        // If SELECT *, the verify query is the same, and if join with *, rearrange fields of both tables combined
        if (selectItems.get(0) instanceof AllColumns ) {
            if (plainSelect.getJoins()!=null){
                selectItems.clear();

                for (String table:tables) {
                    List<String> Fields=icdb.getFields(table);
                    int total=Fields.size();
                    for (int i=0; i<total/3;i++) {
                        selectItems.add(new SelectExpressionItem(new HexValue(Fields.get(i))));
                    }
                }
            }
            else
            return select;
        }else {
            //check if aggregate function in select items and replace with respective column name
            List<SelectItem> aggregateFxnItem=new ArrayList<>();
            List<String> aggregateFxnColumn=new ArrayList<>();
            for (SelectItem item:selectItems) {
                if (((SelectExpressionItem) item).getExpression() instanceof Function){
                    Function function=(Function) ((SelectExpressionItem) item).getExpression();
                    if (function!=null ){
                        if (function.getParameters()!=null){
                            isAggregateQuery=true;
                        String columnname=function.getParameters().toString();

                            if(!aggregateFxnColumn.contains(columnname.substring(1, columnname.length()-1)))
                        aggregateFxnColumn.add(columnname.substring(1, columnname.length()-1));
                            //map column name with operation for verification of aggregate function result
                            columnOperation.put(function.getName()+columnname,function.getName());
                        aggregateFxnItem.add(item);
                        }else
                            return select;
                    }
                }
            }
            addAggregateFunctionColumn(selectItems,aggregateFxnItem,aggregateFxnColumn);

        }


        addWhereColumn(selectItems, plainSelect.getWhere());
        tables.forEach(table -> addPrimaryKeyColumn(selectItems, table));

        List<SelectItem> signatureItems = getICSelectItems(selectItems, Format.IC_SUFFIX, Format.SERIAL_SUFFIX);
        selectItems.addAll(signatureItems);

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

        // Get all columns, because we are deleting the entire row
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
        return update; // TODO
    }

    @Override
    protected Statement parseVerifyQuery(Update update) {
        return null;
    }

    private static List<SelectItem> getICSelectItems(List<SelectItem> items, String... suffixes) {
        return items.stream()
            .flatMap(item -> Arrays.stream(suffixes)
                .map(suffix -> new SelectExpressionItem(new HexValue(item.toString() + suffix)))
            )
            .collect(Collectors.toList());
    }

    private static void addWhereColumn(List<SelectItem> items, Expression where) {
        if (!(where instanceof BinaryExpression)) {
            return;
        }

        Expression leftExpression = ((BinaryExpression) where).getLeftExpression();

        if (!(leftExpression instanceof Column)) {
            return;
        }

        // Check for duplicate columns
        boolean hasColumn = items.stream()
                .anyMatch(item -> item.toString().equals(leftExpression.toString()));

        if (hasColumn) {
            return;
        }

        items.add(new SelectExpressionItem(leftExpression));
    }

    private void addPrimaryKeyColumn(List<SelectItem> items, String table) {
        icdb.getPrimaryKeys(table)
            .forEach(key -> {
                boolean hasColumn = items.stream()
                    .anyMatch(item -> item.toString().equals(key));

                //add key (along with tablename to remove ambiguity)
                if (!hasColumn) {
                    items.add(new SelectExpressionItem(new HexValue(table+"."+key)));
                }else {
                    List<SelectItem> existingKeyItems=new ArrayList<SelectItem>();
                    for (SelectItem item:items) {
                        if (item.toString().equals(key)){
                            existingKeyItems.add(item);
                        }
                    }

                    items.removeAll(existingKeyItems);
                    items.add(0,new SelectExpressionItem(new HexValue(table+"."+key)));
                }
            });

    }

    private void addAggregateFunctionColumn(List<SelectItem> items,List<SelectItem> aggregateFxnItem,List<String> aggregateFxnColumn) {
            items.removeAll(aggregateFxnItem);
        for (String column:aggregateFxnColumn) {
            items.add(new SelectExpressionItem(new HexValue(column)));
        }


    }

    /**
     * Generates a serial number and signature for each expression, and adds them to the list of expressions
     */
    private void convertExpressionList(List<Expression> expressions) {
        new ArrayList<>(expressions).stream()
            .map(expression -> {
                // Get rid of those pesky quotes
                if (expression instanceof StringValue) {
                    return ((StringValue) expression).getValue();
                }

                return expression.toString();
            })
            .forEach(dataString -> {
                final byte[] dataBytes = dataString.getBytes(Charsets.UTF_8);

                DataConverter converter = new DataConverter(dataBytes, codeGen, icrl);

                // Add base64 representation of signature to store it in the query properly
                final String signatureString = Convert.toBase64(converter.getSignature());
                expressions.add(new HexValue("from_base64('" + signatureString + "')"));

                // Add serial number to expression list
                Long serial = converter.getSerial();
                expressions.add(new DoubleValue(serial.toString()));
            });

    }

}
