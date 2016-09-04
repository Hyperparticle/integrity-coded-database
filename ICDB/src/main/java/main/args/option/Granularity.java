package main.args.option;

import crypto.CodeGen;
import io.DBConnection;
import io.source.DataSource;
import main.args.config.UserConfig;
import parse.ICDBQuery;
import parse.OCFQuery;
import parse.OCTQuery;
import stats.RunStatistics;
import stats.Statistics;
import stats.StatisticsMetadata;
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
        public ICDBQuery getQuery(String query, DBConnection icdb, CodeGen codeGen, RunStatistics statistics) {
            return new OCTQuery(query, icdb, codeGen, statistics);
        }

        @Override
        public QueryVerifier getVerifier(DBConnection icdb, UserConfig dbConfig, int threads, DataSource.Fetch fetch, RunStatistics statistics) {
            return new OCTQueryVerifier(icdb, dbConfig, threads, fetch, statistics);
        }
    },
    FIELD {
        @Override
        public ICDBQuery getQuery(String query, DBConnection icdb, CodeGen codeGen, RunStatistics statistics) {
            return new OCFQuery(query, icdb, codeGen, statistics);
        }

        @Override
        public QueryVerifier getVerifier(DBConnection icdb, UserConfig dbConfig, int threads, DataSource.Fetch fetch, RunStatistics statistics) {
            return new OCFQueryVerifier(icdb, dbConfig, threads, fetch, statistics);
        }
    };

    public abstract ICDBQuery getQuery(String query, DBConnection icdb, CodeGen codeGen, RunStatistics statistics);
    public abstract QueryVerifier getVerifier(DBConnection icdb, UserConfig dbConfig, int threads, DataSource.Fetch fetch, RunStatistics statistics);
}
