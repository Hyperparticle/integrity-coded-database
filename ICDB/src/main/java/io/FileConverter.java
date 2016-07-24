package io;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import io.destination.FileDestination;
import main.ICDBTool;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jooq.tools.StringUtils;
import org.supercsv.io.CsvListReader;
import org.supercsv.io.CsvListWriter;
import org.supercsv.prefs.CsvPreference;

import com.google.common.base.Charsets;
import com.google.common.base.Stopwatch;

import crypto.CodeGen;
import crypto.Convert;
import main.args.option.Granularity;
import io.source.FileSource;
import verify.ICRL;

/**
 * A FileConverter takes an input DB data file and generates a converted ICDB
 * data file. This class only supports MySQL for now.
 *
 * Created 5/8/2016
 * @author Dan Kondratyuk
 */
public class FileConverter {

	private final CodeGen codeGen;
	private final Granularity granularity;

    private final ICRL icrl;

	private static final Logger logger = LogManager.getLogger();

	public FileConverter(CodeGen codeGen, Granularity granularity) {
		this.codeGen = codeGen;
		this.granularity = granularity;
        this.icrl = ICRL.init();
	}

	public void convertFile(final File input, final File output) {
		Stopwatch convertTime = Stopwatch.createStarted();

		try (
            final Writer writer = new FileWriter(output)
        ) {
			// Parse the csv
			final Stream<List<String>> csvInput = FileSource.stream(input);
            final FileDestination csvOutput = new FileDestination(output);

			switch (granularity) {
			case TUPLE:
				csvOutput.write(convertLineOCT(csvInput));
				break;
			case FIELD:
                csvOutput.write(convertLineOCF(csvInput));
				break;
			}

			writer.close();
		} catch (IOException e) {
			logger.error("Unable to convert file {}: {}", input.getName(), e.getMessage());
		}

		logger.debug("Converted table {} in {}", input.getName(), convertTime.elapsed(ICDBTool.TIME_UNIT));
	}

	private Stream<List<String>> convertLineOCT(Stream<List<String>> csvInput) throws IOException {
        return csvInput.map(line -> {
            // Combine the list into a string
            final String data = StringUtils.join(line.toArray());
            final byte[] dataBytes = data.getBytes(Charsets.UTF_8);
            convertLine(line, dataBytes, codeGen, icrl);

            return line;
        });
	}

	private Stream<List<String>> convertLineOCF(Stream<List<String>> csvInput) throws IOException {
        List<String> collector = new ArrayList<>();

	    return csvInput.map(line -> {
            collector.clear();
            collector.addAll(line);
            for (String field : line) {
                final byte[] dataBytes = field.getBytes(Charsets.UTF_8);
                convertLine(collector, dataBytes, codeGen, icrl);
            }

            return line;
        });
	}

	/**
	 * Given some data, this method generates codes (svc + serial) from it and
	 * adds them to the end of the supplied list
	 * 
	 * @param collector the list to collect the codes
	 */
	private static void convertLine(final List<String> collector, byte[] data, CodeGen codeGen, ICRL icrl) {
        final long serial = icrl.getNext();
        final String serialString = Long.toString(serial);

		final byte[] serialBytes = ByteBuffer.allocate(8).putLong(serial).array();
		final byte[] allData = ArrayUtils.addAll(data, serialBytes);

		// Generate the signature
		final byte[] signature = codeGen.generateSignature(allData);
		final String signatureString = Convert.toBase64(signature);

		// Write the line
		collector.add(signatureString);
		collector.add(serialString);
	}

}
