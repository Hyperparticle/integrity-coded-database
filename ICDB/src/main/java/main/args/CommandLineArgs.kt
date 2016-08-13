package main.args

import java.io.FileReader
import java.io.Reader

import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import com.google.gson.Gson

import io.DBConnection
import main.args.config.ConfigArgs
import main.args.config.UserConfig
import main.args.option.ReaderConverter

/**
 * The main JCommander Command-Line argument parser class

 * Created on 5/10/2016
 * @author Dan Kondratyuk
 */
class CommandLineArgs(args: Array<String>) {
    private val jCommander: JCommander

    private val commands: Map<String, ConfigCommand>

    private val parsedCommand: String
    private val userConfig: UserConfig

    @Parameter(names = arrayOf("-c", "--config"), converter = ReaderConverter::class, description = "The path of the JSON configuration file")
    var readerConfig: Reader = FileReader("./config.json")

    init {
        commands = mapOf(
            CONVERT   to ConvertCommand(),
            EXECUTE   to ExecuteCommand(),
            BENCHMARK to BenchmarkCommand()
        )

        // Parse arguments
        jCommander = JCommander(this)
        for ((name, command) in commands) { jCommander.addCommand(command) }
        jCommander.parse(*args)

        parsedCommand = jCommander.parsedCommand

        // Parse configuration file
        val gson = Gson()
        val configArgs = gson.fromJson(readerConfig, ConfigArgs::class.java)
        userConfig = UserConfig.init(configArgs)

        // Apply configuration to database connection
        DBConnection.configure(userConfig)
    }

    /**
     * Execute the current parsed command
     */
    fun execute() = commands[parsedCommand]?.execute(userConfig) ?: usage()

    /**
     * Print the usage dialog and exit
     */
    private fun usage() {
        jCommander.usage()
        System.exit(0)
    }

    companion object {
        const val CONVERT = "convert"
        const val EXECUTE = "execute"
        const val BENCHMARK = "benchmark"

        init {
            System.setProperty("org.jooq.no-logo", "true");
        }
    }

}
