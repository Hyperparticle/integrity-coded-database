package main.args.option;

import crypto.CodeGen;
import io.DBConnection;
import io.source.DataSource;
import main.args.config.UserConfig;
import parse.ICDBQuery;
import parse.OCFQuery;
import parse.OCTQuery;
import verify.OCFQueryVerifier;
import verify.OCTQueryVerifier;
import verify.QueryVerifier;

/**
 * ICDB Granularity is configured for one code per tuple (OCT) or one code per field (OCF)
 *
 * Created on 5/21/2016
 * @author Dan Kondratyuk
 */
public enum Granularity {
    TUPLE {
        @Override
        public ICDBQuery getQuery(String query, DBConnection icdb, CodeGen codeGen) {
            return new OCTQuery(query, icdb, codeGen);
        }

        @Override
        public QueryVerifier getVerifier(DBConnection icdb, UserConfig dbConfig, int threads, DataSource.Fetch fetch) {
            return new OCTQueryVerifier(icdb, dbConfig, threads, fetch);
        }
    },
    FIELD {
        @Override
        public ICDBQuery getQuery(String query, DBConnection icdb, CodeGen codeGen) {
            return new OCFQuery(query, icdb, codeGen);
        }

        @Override
        public QueryVerifier getVerifier(DBConnection icdb, UserConfig dbConfig, int threads, DataSource.Fetch fetch) {
            return new OCFQueryVerifier(icdb, dbConfig, threads, fetch);
        }
    };

    public abstract ICDBQuery getQuery(String query, DBConnection icdb, CodeGen codeGen);
    public abstract QueryVerifier getVerifier(DBConnection icdb, UserConfig dbConfig, int threads, DataSource.Fetch fetch);
}
