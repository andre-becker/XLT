package action.testcases;

import org.junit.Test;
import com.xceptance.xlt.api.actions.AbstractHtmlPageAction;
import com.xceptance.xlt.api.engine.scripting.AbstractHtmlUnitScriptTestCase;

import action.modules.Open_ExamplePage;
import action.testcases.storeValue_actions.storeValueAction;

/**
 * TODO: Add class description
 */
public class storeValue extends AbstractHtmlUnitScriptTestCase
{

    /**
     * Constructor.
     */
    public storeValue()
    {
        super("http://localhost:8080/");
    }

    @Test
    public void test() throws Throwable
    {
        AbstractHtmlPageAction lastAction = null;

        final Open_ExamplePage open_ExamplePage = new Open_ExamplePage();
        lastAction = open_ExamplePage.run(lastAction);

        lastAction = new storeValueAction(lastAction);
        lastAction.run();


    }
}