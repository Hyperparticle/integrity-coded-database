package main.args.option;

import cipher.CodeGen;
import convert.DBConnection;
import main.args.config.UserConfig;
import parse.ICDBQuery;
import parse.OCFQuery;
import parse.OCTQuery;
import verify.OCFQueryVerifier;
import verify.OCTQueryVerifier;
import verify.QueryVerifier;

/**
 * <p>
 *     ICDB Granularity is configured for one code per tuple (OCT) or one code per field (OCF)
 * </p>
 * Created on 5/21/2016
 *
 * @author Dan Kondratyuk
 */
public enum Granularity {
    TUPLE {
        @Override
        public ICDBQuery getQuery(String query, DBConnection icdb, CodeGen codeGen) {
            return new OCTQuery(query, icdb, codeGen);
        }

        @Override
        public QueryVerifier getVerifier(DBConnection icdb, UserConfig dbConfig) {
            return new OCTQueryVerifier(icdb, dbConfig);
        }
    },
    FIELD {
        @Override
        public ICDBQuery getQuery(String query, DBConnection icdb, CodeGen codeGen) {
            return new OCFQuery(query, icdb, codeGen);
        }

        @Override
        public QueryVerifier getVerifier(DBConnection icdb, UserConfig dbConfig) {
            return new OCFQueryVerifier(icdb, dbConfig);
        }
    };

    public abstract ICDBQuery getQuery(String query, DBConnection icdb, CodeGen codeGen);
    public abstract QueryVerifier getVerifier(DBConnection icdb, UserConfig dbConfig);
}
