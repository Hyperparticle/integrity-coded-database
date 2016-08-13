package main.args

import main.args.config.UserConfig

/**
 * A command-line command that specifies how it should be executed
 *
 * Created on 8/12/2016
 * @author Dan Kondratyuk
 */
interface ConfigCommand {

    /**
     * Execute this command with given user input
     */
    fun execute(userConfig: UserConfig)
}