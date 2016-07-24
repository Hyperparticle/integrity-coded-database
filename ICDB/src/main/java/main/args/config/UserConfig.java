package main.args.config;

import crypto.AlgorithmType;
import crypto.CodeGen;
import crypto.Key;
import main.args.option.Granularity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * <p>
 *      Global user configuration parameters
 * </p>
 * Created on 7/13/2016
 *
 * @author Dan Kondratyuk
 */
public class UserConfig {

    private static final Logger logger = LogManager.getLogger();

    private static UserConfig userConfig;
    public static UserConfig init(ConfigArgs configArgs) {
        userConfig = new UserConfig(configArgs);

        logger.info("------------------------------");
        logger.info("User Configuration");
        logger.info("------------------------------");
        logger.info("Algorithm: {}", configArgs.algorithm);
        logger.info("Granularity: {}", configArgs.granularity);
        logger.info("ICDB Schema: {}", configArgs.icdbSchema);
        logger.info("------------------------------");

        return userConfig;
    }


    public static UserConfig getInstance() {
        return userConfig;
    }

    public final String ip;
    public final int port;
    public final String user;
    public final String password;
    public final String schema;
    public final String icdbSchema;
    public final Granularity granularity;
    public final CodeGen codeGen;

    public UserConfig(ConfigArgs configArgs) {
        ip = configArgs.ip;
        port = configArgs.port;
        user = configArgs.user;
        password = configArgs.password;
        schema = configArgs.schema;
        icdbSchema = configArgs.icdbSchema;
        granularity = configArgs.granularity;

        final Key key = new Key(configArgs.macKey, configArgs.rsaKeyFile);
        final AlgorithmType algorithm = configArgs.algorithm;
        codeGen = new CodeGen(algorithm, key);
    }
}
