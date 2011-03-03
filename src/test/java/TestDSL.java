import com.nirima.jenkins.dsl.JenkinsDSL;
import junit.framework.TestCase;

/**
 * Created by IntelliJ IDEA.
 * User: magnayn
 * Date: 12/12/2010
 * Time: 15:14
 * To change this template use File | Settings | File Templates.
 */
public class TestDSL extends TestCase
{
    public void testDSL() throws Exception
    {
        JenkinsDSL dsl = new JenkinsDSL(null);
        dsl.initScript("jenkins { println '1=' + this.woot; inTransaction { println \"2=yay ${woot}\";  println '3=' + this.woot;  } }"
                );
    }
}
