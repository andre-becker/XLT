package action.modules.MultiSelection_byValue_actions;

import org.junit.Assert;

import com.xceptance.xlt.api.actions.AbstractHtmlPageAction;
import com.xceptance.xlt.api.engine.scripting.AbstractHtmlUnitScriptAction;
import com.gargoylesoftware.htmlunit.html.HtmlPage;


/**
 * TODO: Add class description
 */
public class label_whitespace0 extends AbstractHtmlUnitScriptAction
{

    /**
     * Constructor.
     * @param prevAction The previous action.
     */
    public label_whitespace0(final AbstractHtmlPageAction prevAction)
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
        page = addSelection("id=select_18", "value=");
        assertText("id=cc_change", "change (select_18) empty");
        page = addSelection("id=select_18", "value= ");
        assertText("id=cc_change", "change (select_18) empty, 1 space");
        page = addSelection("id=select_18", "value=  ");
        assertText("id=cc_change", "change (select_18) empty, 1 space, 2 spaces");
        page = removeSelection("id=select_18", "value=");
        assertText("id=cc_change", "change (select_18) 1 space, 2 spaces");
        page = removeSelection("id=select_18", "value= ");
        assertText("id=cc_change", "change (select_18) 2 spaces");
        page = removeSelection("id=select_18", "value=  ");

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

        assertText("id=cc_change", "change (select_18)");

    }
}