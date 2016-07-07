package main.args.config;

import cipher.mac.AlgorithmType;
import main.args.option.Granularity;

/**
 * <p>
 *     Represents a configuration file
 * </p>
 * Created on 6/8/2016
 *
 * @author Dan Kondratyuk
 */
public class Config {
    public String ip;
    public int port;
    public String user;
    public String password;
    public String schema;
    public String icdbSchema;
    public String key;
    public AlgorithmType algorithm;
    public Granularity granularity;
}
