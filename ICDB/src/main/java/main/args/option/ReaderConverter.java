package main.args.option;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

/**
 * Maps the supplied filename String to a Reader
 *
 * Created on 6/8/2016
 * @author Dan Kondratyuk
 */
public class ReaderConverter implements IStringConverter<Reader> {

    @Override
    public Reader convert(String value) {
        try {
            return new FileReader(value);
        } catch (IOException e) {
            throw new ParameterException("Unable to open file " + value);
        }
    }

}
