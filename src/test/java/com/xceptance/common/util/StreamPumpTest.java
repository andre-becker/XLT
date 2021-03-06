package com.xceptance.common.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests the implementation of {@link StreamPump}.
 * 
 * @author Hartmut Arlt (Xceptance Software Technologies GmbH)
 */
public class StreamPumpTest
{

    /**
     * Tests the implementation of {@link StreamPump#StreamPump(InputStream, String)} by passing an invalid file name.
     */
    @Test(expected = FileNotFoundException.class)
    public void testInit_InvalidOutputFileName() throws Throwable
    {
        new StreamPump(null, System.getProperty("java.io.tmpdir"));
    }

    /**
     * Tests the implementation of {@link StreamPump#StreamPump(InputStream, File)} by passing an invalid file object.
     */
    @Test(expected = FileNotFoundException.class)
    public void testInit_InvalidOutputFile() throws Throwable
    {
        new StreamPump(null, new File(System.getProperty("java.io.tmpdir")));
    }

    /**
     * Tests the implementation of {@link StreamPump#run()} by passing null references to
     * {@link StreamPump#StreamPump(InputStream, OutputStream)}.
     */
    @Test(expected = NullPointerException.class)
    public void testRun_NPE() throws Throwable
    {
        new StreamPump(null, (OutputStream) null).run();
    }

    /**
     * Tests the implementation of {@link StreamPump#run()}.
     */
    @Test
    public void testRun() throws Throwable
    {
        final String testString = "Test STriNg";
        final InputStream in = new ByteArrayInputStream(testString.getBytes());
        final OutputStream out = new ByteArrayOutputStream();

        new StreamPump(in, out).run();

        Assert.assertEquals(testString, ((ByteArrayOutputStream) out).toString());
    }

}
