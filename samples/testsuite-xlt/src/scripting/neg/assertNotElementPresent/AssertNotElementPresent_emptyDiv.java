package scripting.neg.assertNotElementPresent;

import org.junit.After;
import org.junit.Test;

import com.xceptance.xlt.api.engine.scripting.AbstractWebDriverScriptTestCase;
import com.xceptance.xlt.api.webdriver.XltDriver;
import scripting.util.PageOpener;


/**
 * 
 */
public class AssertNotElementPresent_emptyDiv extends AbstractWebDriverScriptTestCase
{

	/**
	 * Constructor.
	 */
	public AssertNotElementPresent_emptyDiv()
	{
		super(new XltDriver(true), null);
	}

	/**
	 * element exists in iframe
	 * 
	 * @throws Throwable
	 */
	@Test(expected = AssertionError.class)
	public void test() throws Throwable
	{
		PageOpener.examplePage( this );
		assertNotElementPresent( "id=invisible_empty_div" );
	}
	
	@After
	public void after()
	{
		getWebDriver().quit();
	}
}