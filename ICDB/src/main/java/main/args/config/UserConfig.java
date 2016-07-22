package main.args.config;

import crypto.AlgorithmType;
import crypto.CodeGen;
import crypto.Key;
import main.args.option.Granularity;

/**
 * <p>
 *      Global user configuration parameters
 * </p>
 * Created on 7/13/2016
 *
 * @author Dan Kondratyuk
 */
public class UserConfig {

    private static UserConfig userConfig;
    public static UserConfig init(ConfigArgs configArgs) {
        return userConfig = new UserConfig(configArgs);
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
