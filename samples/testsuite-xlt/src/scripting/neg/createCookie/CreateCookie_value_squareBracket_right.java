package scripting.neg.createCookie;

import org.junit.After;
import org.junit.Test;

import scripting.util.AbstractCreateCookie;
import scripting.util.PageOpener;

import com.xceptance.xlt.api.util.XltException;
import com.xceptance.xlt.api.webdriver.XltDriver;


/**
 * 
 */
public class CreateCookie_value_squareBracket_right extends AbstractCreateCookie
{
	/**
	 * Constructor.
	 */
	public CreateCookie_value_squareBracket_right()
	{
		super(new XltDriver(true), null);
	}

	@Test(expected = XltException.class)
	public void test() throws Throwable
	{
		PageOpener.examplePage( this );
		create( CreateCookie_value_squareBracket_right.class.getSimpleName(), "foo]bar" );
    }

    @After
    public void after()
    {
        getWebDriver().quit();
    }
}