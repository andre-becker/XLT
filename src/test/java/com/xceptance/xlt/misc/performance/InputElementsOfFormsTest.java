package com.xceptance.xlt.misc.performance;

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.xceptance.xlt.engine.util.TimerUtils;

/**
 * This test checks the performance of the getInputElement methods of HtmlForm, because they seem to be slow.
 * 
 * @author René Schwietzke (Xceptance Software Technologies GmbH)
 */
public class InputElementsOfFormsTest extends AbstractHtmlTest
{
    /**
     * HTML page used for test.
     */
    private HtmlPage htmlPage;

    /**
     * Test fixture setup.
     * 
     * @throws Exception
     *             thrown when setup failed
     */
    @Before
    public void setUp() throws Exception
    {
        htmlPage = setUp(this);
    }

    /**
     * Returns the first form identified by the given ID using {@link HtmlPage#getDocumentElement()} and
     * {@link HtmlElement#getElementsByAttribute(String, String, String)}.
     * 
     * @param id
     *            ID of form
     * @return first form with given ID
     */
    public HtmlForm getFirstFormByIDOld(final String id)
    {
        final List<? extends HtmlElement> forms = htmlPage.getDocumentElement().getElementsByAttribute("form", "id", id);

        return (forms.size() > 0) ? (HtmlForm) forms.get(0) : null;
    }

    /**
     * Returns the first form identified by the given ID using {@link HtmlPage#getHtmlElementById(String)}.
     * 
     * @param id
     *            ID of form
     * @return first form with given ID
     */
    public HtmlForm getFirstFormByID(final String id)
    {
        final HtmlElement form = htmlPage.getHtmlElementById(id);

        return (form != null && form instanceof HtmlForm) ? (HtmlForm) form : null;
    }

    /**
     * Returns the first form identified by the given ID using {@link HtmlPage#getForms()}.
     * 
     * @param id
     *            ID of form
     * @return first form with given ID
     */
    public HtmlForm getFirstFormByIDUsingGetForms(final String id)
    {
        final List<HtmlForm> forms = htmlPage.getForms();

        for (final HtmlForm form : forms)
        {
            if (form.getId().equals(id))
            {
                return form;
            }
        }

        return null;
    }

    /**
     * Returns the first form identified by the given ID embedded in a XPath expression
     * {@link HtmlPage#getByXPath(String)}.
     * 
     * @param xpath
     *            the XPath expression containing the ID
     * @return first form with given ID
     */
    public HtmlForm getFirstFormByIDByXPath(final String xpath) throws Exception
    {
        final List<?> forms = htmlPage.getByXPath(xpath);

        return (!forms.isEmpty()) ? (HtmlForm) forms.get(0) : null;
    }

    /**
     * The speed test.
     * 
     * @throws Exception
     *             thrown when something went wrong
     */
    @Test
    @Ignore("Performance test")
    public void speedTest() throws Exception
    {
        final String id = "addressForm";

        final int count = 10000;

        HtmlForm form1 = null;
        HtmlForm form2 = null;
        HtmlForm form3 = null;
        HtmlForm form4 = null;

        // warmup
        for (int i = 0; i < 100; i++)
        {
            getFirstFormByIDOld(id);
            getFirstFormByIDUsingGetForms(id);
            getFirstFormByID(id);
            getFirstFormByIDByXPath(id);
        }

        // measure
        final long s1 = TimerUtils.getTime();
        for (int i = 0; i < count; i++)
        {
            form1 = getFirstFormByIDOld(id);
        }
        final long e1 = TimerUtils.getTime();

        final long s2 = TimerUtils.getTime();
        for (int i = 0; i < count; i++)
        {
            form2 = getFirstFormByIDUsingGetForms(id);
        }
        final long e2 = TimerUtils.getTime();

        final long s3 = TimerUtils.getTime();
        for (int i = 0; i < count; i++)
        {
            form3 = getFirstFormByID(id);
        }
        final long e3 = TimerUtils.getTime();

        final long s4 = TimerUtils.getTime();
        final String xpath = "//form[@id='" + id + "']";
        for (int i = 0; i < count; i++)
        {
            form4 = getFirstFormByIDByXPath(xpath);
        }
        final long e4 = TimerUtils.getTime();

        System.out.println("Old     : " + (e1 - s1) + "ms");
        System.out.println("GetForms: " + (e2 - s2) + "ms");
        System.out.println("ID      : " + (e3 - s3) + "ms");
        System.out.println("XPath   : " + (e4 - s4) + "ms");

        Assert.assertNotNull(form1);
        Assert.assertNotNull(form2);
        Assert.assertNotNull(form3);
        Assert.assertNotNull(form4);

        Assert.assertEquals(form1, form2);
        Assert.assertEquals(form2, form3);
        Assert.assertEquals(form1, form3);
        Assert.assertEquals(form1, form4);
    }
}