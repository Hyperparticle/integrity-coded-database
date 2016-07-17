package main.args.option;

import convert.DBConnection;
import main.args.config.UserConfig;
import parse.OCFQueryConverter;
import parse.OCTQueryConverter;
import parse.QueryConverter;
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
        public QueryConverter getConverter(DBConnection icdb) {
            return new OCTQueryConverter(icdb);
        }

        @Override
        public QueryVerifier getVerifier(DBConnection icdb, UserConfig dbConfig) {
            return new OCTQueryVerifier(icdb, dbConfig);
        }
    },
    FIELD {
        @Override
        public QueryConverter getConverter(DBConnection icdb) {
            return new OCFQueryConverter(icdb);
        }

        @Override
        public QueryVerifier getVerifier(DBConnection icdb, UserConfig dbConfig) {
            return new OCFQueryVerifier(icdb, dbConfig);
        }
    };

    public abstract QueryConverter getConverter(DBConnection icdb);
    public abstract QueryVerifier getVerifier(DBConnection icdb, UserConfig dbConfig);
}
