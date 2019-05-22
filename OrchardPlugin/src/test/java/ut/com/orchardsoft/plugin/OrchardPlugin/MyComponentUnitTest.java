package ut.com.orchardsoft.plugin.OrchardPlugin;

import org.junit.Test;
import com.orchardsoft.plugin.OrchardPlugin.api.MyPluginComponent;
import com.orchardsoft.plugin.OrchardPlugin.impl.MyPluginComponentImpl;

import static org.junit.Assert.assertEquals;

public class MyComponentUnitTest
{
    @Test
    public void testMyName()
    {
        MyPluginComponent component = new MyPluginComponentImpl(null);
        assertEquals("names do not match!", "myComponent",component.getName());
    }
}