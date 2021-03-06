package scripting.neg.assertTextPresent;

import org.junit.Test;
import org.junit.After;

import com.xceptance.xlt.api.engine.scripting.AbstractWebDriverScriptTestCase;
import com.xceptance.xlt.api.webdriver.XltDriver;
import scripting.util.PageOpener;


/**
 * 
 */
public class AssertTextPresent_regexpWrong extends AbstractWebDriverScriptTestCase
{

	/**
	 * Constructor.
	 */
	public AssertTextPresent_regexpWrong()
	{
		super(new XltDriver(true), null);
	}

	@Test(expected = AssertionError.class)
	public void test() throws Throwable
	{
		PageOpener.examplePage( this );
		assertTextPresent( "regexp:(LOREM)|(IPSUM)" );
    }

    @After
    public void after()
    {
        getWebDriver().quit();
    }
}