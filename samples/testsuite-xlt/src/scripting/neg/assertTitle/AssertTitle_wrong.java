package scripting.neg.assertTitle;

import org.junit.Test;
import org.junit.After;

import com.xceptance.xlt.api.engine.scripting.AbstractWebDriverScriptTestCase;
import com.xceptance.xlt.api.webdriver.XltDriver;
import scripting.util.PageOpener;


/**
 * 
 */
public class AssertTitle_wrong extends AbstractWebDriverScriptTestCase
{

	/**
	 * Constructor.
	 */
	public AssertTitle_wrong()
	{
		super(new XltDriver(true), null);
	}

	@Test(expected = AssertionError.class)
	public void test() throws Throwable
	{
		PageOpener.examplePage( this );
		assertTitle( "xyz" );
    }

    @After
    public void after()
    {
        getWebDriver().quit();
    }
}