package convert;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import cipher.RNG;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jooq.tools.StringUtils;
import org.supercsv.io.CsvListReader;
import org.supercsv.io.CsvListWriter;
import org.supercsv.prefs.CsvPreference;

import com.google.common.base.Charsets;
import com.google.common.base.Stopwatch;

import cipher.CodeGen;
import cipher.Signature;
import main.args.option.Granularity;

/**
 * <p>
 * A FileConverter takes an input DB data file and generates a converted ICDB
 * data file. This class only supports MySQL for now.
 * </p>
 * Created 5/8/2016
 * 
 * @author Dan Kondratyuk
 */
public class FileConverter {

	private final CodeGen codeGen;
	private final Granularity granularity;

	private static final Logger logger = LogManager.getLogger();

	public FileConverter(CodeGen codeGen, Granularity granularity) {
		this.codeGen = codeGen;
		this.granularity = granularity;
	}

	public void convertFile(final File input, final File output) {
		Stopwatch convertTime = Stopwatch.createStarted();

		try (final Reader reader = new FileReader(input); final Writer writer = new FileWriter(output)) {
			// Parse the csv
			final CsvPreference preference = CsvPreference.STANDARD_PREFERENCE;
			final CsvListReader csvReader = new CsvListReader(reader, preference);
			final CsvListWriter csvWriter = new CsvListWriter(writer, preference);

			switch (granularity) {
			case TUPLE:
				convertLineOCT(csvReader, csvWriter);
				break;
			case FIELD:
				convertLineOCF(csvReader, csvWriter);
				break;
			}

			csvReader.close();
			csvWriter.close();
			reader.close();
			writer.close();
		} catch (IOException e) {
			logger.error("Unable to convert file {}: {}", input.getName(), e.getMessage());
		}

		logger.debug("Converted table {} in {}", input.getName(), convertTime);
	}

	private void convertLineOCT(CsvListReader csvReader, CsvListWriter csvWriter) throws IOException {
		List<String> nextLine = csvReader.read();
		while ((nextLine = csvReader.read()) != null) {
			// Combine the list into a string
			final String data = StringUtils.join(nextLine.toArray());
			final byte[] dataBytes = data.getBytes(Charsets.UTF_8);
			convertLine(nextLine, dataBytes, codeGen);

			csvWriter.write(nextLine);
		}
	}

	private void convertLineOCF(CsvListReader csvReader, CsvListWriter csvWriter) throws IOException {
		List<String> nextLine = csvReader.read();
		List<String> collector = new ArrayList<>(nextLine.size() * 3);

		while ((nextLine = csvReader.read()) != null) {
			collector.clear();
            collector.addAll(nextLine);
			for (String field : nextLine) {
				final byte[] dataBytes = field.getBytes(Charsets.UTF_8);
				convertLine(collector, dataBytes, codeGen);
			}

			csvWriter.write(collector);
		}
	}

	/**
	 * Given a String, this method generates codes (svc + serial) from it and
	 * adds them to the end of the supplied list
	 * 
	 * @param collector
	 *            the list to collect the codes
	 */
	private static void convertLine(final List<String> collector, byte[] data, CodeGen codeGen) {
        // TODO: add a serial
        final long serial = RNG.randomInt();
        final String serialString = Long.toString(serial);

		final byte[] serialBytes = ByteBuffer.allocate(8).putLong(serial).array();
		final byte[] allData = ArrayUtils.addAll(data, serialBytes);

		// Generate the signature
		final byte[] signature = codeGen.generateSignature(allData);
		final String signatureString = Signature.toBase64(signature);

		// Write the line
		collector.add(signatureString);
		collector.add(serialString);
	}

}
