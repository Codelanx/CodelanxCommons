package com.codelanx.commons.util;

import com.codelanx.commons.config.PluginConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Rogue on 10/24/2015.
 */
public class Test {

    public static void main(String... args) throws Throwable {
        List<String> cmds = PluginConfig.DISABLED_COMMANDS.as(ArrayList.class, String.class);
    }

    public List<Test> doTest() {
        return null;
    }

    public static final class TestTwo {

        public void foo() {
            System.out.println(Reflections.accessedFrom(Test.class));
        }
    }

    /** {@inheritDoc} g-g-gee I don't know Rick, isn't that a bit dangerous? */
    protected final @SafeVarargs synchronized static strictfp boolean $validJava(boolean φ, Class<?>[]... wheee) throws Throwable {
        do do do do break; while (false); while (false); while (false); while (false);
        System.out.println(φ |= φ &= φ |= !/**/false);
        return φ |= φ &= φ |= !/**/false;
    }{}{}{}{}{}{}{}{}{}{}{}{}{}{}
}
