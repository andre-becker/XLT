package com.xceptance.xlt.report.external;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.xceptance.common.util.ParameterCheckUtils;
import com.xceptance.xlt.api.report.external.AbstractLineParser;
import com.xceptance.xlt.api.report.external.ValueSet;
import com.xceptance.xlt.engine.util.TimerUtils;
import com.xceptance.xlt.report.AbstractReader;
import com.xceptance.xlt.report.external.converter.AbstractDataConverter;

/**
 * Simple file reader that defines basic functionality for parsing a given file.
 * 
 * @author Matthias Ullrich (Xceptance Software Technologies GmbH)
 */
public class Reader extends AbstractReader<ValueSet>
{
    private static final Log LOG = LogFactory.getLog(Reader.class);

    /**
     * File name
     */
    private final String fileName;

    /**
     * Encoding type (see chapter '<code>Standard charsets</code>' in {@link java.nio.charset.Charset})
     */
    private final String encoding;

    private final AbstractLineParser parser;

    private AbstractDataConverter converter;

    /**
     * Create a reader instance.
     * 
     * @param fileName
     *            file name
     * @param encoding
     *            encoding type (see chapter '<code>Standard charsets</code>' in {@link java.nio.charset.Charset})
     * @param parser
     *            the external data parser to be used for the given file
     */
    public Reader(final String fileName, final String encoding, final AbstractLineParser parser)
    {
        super("ExternalDataReader");

        ParameterCheckUtils.isNonEmptyString(fileName, "fileName");
        ParameterCheckUtils.isNonEmptyString(encoding, "encoding");

        this.fileName = fileName;
        this.encoding = encoding;
        this.parser = parser;
    }

    /**
     * @param converter
     */
    public void setConverter(final AbstractDataConverter converter)
    {
        if (this.converter == null)
        {
            this.converter = converter;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ValueSet processLine(final String line)
    {
        try
        {
            return parser.parse(line);
        }
        catch (final Exception ex)
        {
            if (LOG.isErrorEnabled())
            {
                LOG.error(String.format("Failed to read file '%s': %s\n", getFileName(), ex.getMessage()));
            }

            System.out.println("FAILED: " + ex.getMessage());
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void processLineResult(final ValueSet point)
    {
        final long time = point.getTime();
        try
        {
            converter.parse(time, point.getValues());
        }
        catch (final Exception e)
        {
            LOG.warn("Failed to parse external data", e);
            System.err.println("\nFailed to parse external data: " + e);
        }
    }

    /**
     * Reads the external data from the configured file.
     */
    public void readData()
    {
        setOverallStartTime(TimerUtils.getTime());

        try (final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(getFileName()),
                                                                                            getEncoding())))
        {
            // System.out.printf("Reading file '%s' ...\n", VFS.getManager().resolveFile(getFileName()));

            read(bufferedReader);

            // wait for the data processor thread to empty the queue
            waitForDataRecordProcessingToComplete();
        }
        catch (final Exception e)
        {
            System.out.println("Failed to read data records: " + e.getMessage());
            LOG.error("Failed to read data records", e);
        }
        finally
        {
            cleanUp();
        }
    }

    /**
     * Get the file name.
     * 
     * @return file name
     */
    public String getFileName()
    {
        return fileName;
    }

    /**
     * Get the file encoding.
     * 
     * @return file encoding
     */
    public String getEncoding()
    {
        return encoding;
    }
}
