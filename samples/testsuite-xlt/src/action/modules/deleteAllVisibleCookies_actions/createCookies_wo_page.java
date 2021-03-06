package action.modules.deleteAllVisibleCookies_actions;

import org.junit.Assert;

import com.xceptance.xlt.api.actions.AbstractHtmlPageAction;
import com.xceptance.xlt.api.engine.scripting.AbstractHtmlUnitScriptAction;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import action.modules.AssertCookie;

/**
 * TODO: Add class description
 */
public class createCookies_wo_page extends AbstractHtmlUnitScriptAction
{

    /**
     * Constructor.
     * @param prevAction The previous action.
     */
    public createCookies_wo_page(final AbstractHtmlPageAction prevAction)
    {
        super(prevAction);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void preValidate() throws Exception
    {
        final HtmlPage page = getPreviousAction().getHtmlPage();
        Assert.assertNotNull("Failed to get page from previous action", page);

    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void execute() throws Exception
    {
        HtmlPage page = getPreviousAction().getHtmlPage();
        createCookie("testsuite-xlt_1=xlt-testsuite_1");
        createCookie("testsuite-xlt_2=xlt-testsuite_2");
        createCookie("testsuite-xlt_3=xlt-testsuite_3");
        final AssertCookie assertCookie = new AssertCookie("testsuite-xlt_1", "xlt-testsuite_1");
        page = assertCookie.run(page);

        final AssertCookie assertCookie0 = new AssertCookie("testsuite-xlt_2", "xlt-testsuite_2");
        page = assertCookie0.run(page);

        final AssertCookie assertCookie1 = new AssertCookie("testsuite-xlt_3", "xlt-testsuite_3");
        page = assertCookie1.run(page);

        close();
        deleteAllVisibleCookies();

        setHtmlPage(page);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void postValidate() throws Exception
    {
        final HtmlPage page = getHtmlPage();
        Assert.assertNotNull("Failed to load page", page);


    }
}