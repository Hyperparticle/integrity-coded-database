package main;

import java.io.FileNotFoundException;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import main.args.*;
import main.args.config.UserConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.base.Stopwatch;

import io.DBConnection;
import io.DBConverter;
import io.SchemaConverter;
import main.args.config.ConfigArgs;
import parse.ICDBQuery;
import verify.QueryVerifier;

/**
 * <p>
 * A tool for performing ICDB-related tasks.
 * </p>
 * Created on 5/10/2016
 *
 * @author Dan Kondratyuk
 */
public class ICDBTool {

    // The time unit for all timed log statements
    public static final TimeUnit TIME_UNIT = TimeUnit.MILLISECONDS;

	private static final Logger logger = LogManager.getLogger();

	public static void main(String[] args) {
		Stopwatch totalTime = Stopwatch.createStarted();

        try {
            // Parse the command-line arguments, and execute the command
            CommandLineArgs commandLineArgs = new CommandLineArgs(args);
            commandLineArgs.execute();
        } catch (Exception e) {
            logger.error(e.getMessage());
            logger.debug(e.getStackTrace());
        }

		logger.info("\nTotal time elapsed: {}", totalTime.elapsed(ICDBTool.TIME_UNIT));
	}

}
