package scripting.neg.addSelection;

import org.junit.After;
import org.junit.Test;
import org.openqa.selenium.InvalidSelectorException;

import scripting.util.PageOpener;

import com.xceptance.xlt.api.engine.scripting.AbstractWebDriverScriptTestCase;
import com.xceptance.xlt.api.webdriver.XltDriver;


/**
 * 
 */
public class AddSelection_locator_index extends AbstractWebDriverScriptTestCase
{

	/**
	 * Constructor.
	 */
	public AddSelection_locator_index()
	{
		super(new XltDriver(true), null);
	}

	@Test(expected = InvalidSelectorException.class)
	public void test() throws Throwable
	{
		PageOpener.examplePage( this );
		addSelection( "index=0", "index=0" );
	}
	
	@After
	public void after()
	{
		getWebDriver().quit();
	}
}