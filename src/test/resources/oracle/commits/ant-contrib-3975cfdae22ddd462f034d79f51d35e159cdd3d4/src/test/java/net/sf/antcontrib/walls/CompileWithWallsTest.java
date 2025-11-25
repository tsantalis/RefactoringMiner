/*
 * Copyright (c) 2001-2004 Ant-Contrib project.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.sf.antcontrib.walls;

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * BIG NOTE
 * <p>Always expect specific exceptions.  Most of these test cases when
 * first submitted were not and therefore were not testing what they said
 * they were testing.  Exceptions were being caused by other things and the
 * tests were still passing.  Now all tests expect a specific exception
 * so if any other is thrown we will fail the test case.</p>
 * <p>Testcase for &lt;propertycopy&gt;.</p>
 */
public class CompileWithWallsTest {
    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /**
     * Field baseDir.
     */
    private static String baseDir = "";

    /**
     * Method setUp.
     */
    @Before
    public void setUp() {
        buildRule.configureProject("src/test/resources/walls/compilewithwalls.xml");
        baseDir = buildRule.getProject().getBaseDir().getAbsolutePath();
    }

    /**
     * Method tearDown.
     */
    @After
    public void tearDown() {
        buildRule.executeTarget("cleanup");

//        System.out.println(getFullLog());
//        System.out.println("std out. from ant build begin--------------");
//        System.out.println(getOutput());
//        System.out.println("std.out. from ant build end----------------");
//        System.out.println("std err. from ant build begin--------------");
//        System.out.println(getError());
//        System.out.println("std.err. from ant build end----------------");
    }

    /**
     * Method testTooManyNestedWallElements.
     */
    @Test
    public void testTooManyNestedWallElements() {
        String msg = "compilewithwalls task only supports one nested walls element or one walls attribute";
        thrown.expect(BuildException.class);
        thrown.expectMessage(msg);
        buildRule.executeTarget("testTooManyNestedWallElements");
    }

    /**
     * Method testTooManyNestedJavacElements.
     */
    @Test
    public void testTooManyNestedJavacElements() {
        String msg = "compilewithwalls task only supports one nested javac element";
        thrown.expect(BuildException.class);
        thrown.expectMessage(msg);
        buildRule.executeTarget("testTooManyNestedJavacElements");
    }

    /**
     * Method testNoWallElement.
     */
    @Test
    public void testNoWallElement() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("There must be a nested walls element");
        buildRule.executeTarget("testNoWallElement");
    }

    /**
     * Method testNoJavacElement.
     */
    @Test
    public void testNoJavacElement() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("There must be a nested javac element");
        buildRule.executeTarget("testNoJavacElement");
    }

    /**
     * Method testMoreThanOneSrcDirInJavac.
     */
    @Test
    public void testMoreThanOneSrcDirInJavac() {
        buildRule.executeTarget("testMoreThanOneSrcDirInJavac");
    }

    /**
     * Method testNoSrcDirInJavac.
     */
    @Test
    public void testNoSrcDirInJavac() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("Javac inside compilewithwalls must have a srcdir specified");
        buildRule.executeTarget("testNoSrcDirInJavac");
    }

    /**
     * Method testIntermediaryDirAndDestDirSame.
     */
    @Test
    public void testIntermediaryDirAndDestDirSame() {
        String msg = "intermediaryBuildDir attribute cannot be specified\n"
                + "to be the same as destdir or inside desdir of the javac task.\n"
                + "This is an intermediary build directory only used by the\n"
                + "compilewithwalls task, not the class file output directory.\n"
                + "The class file output directory is specified in javac's destdir attribute";
        thrown.expect(BuildException.class);
        thrown.expectMessage(msg);
        buildRule.executeTarget("testIntermediaryDirAndDestDirSame");
    }

    /**
     * Method testIntermediaryDirInsideDestDir.
     */
    @Test
    public void testIntermediaryDirInsideDestDir() {
        String msg = "intermediaryBuildDir attribute cannot be specified\n"
                + "to be the same as destdir or inside desdir of the javac task.\n"
                + "This is an intermediary build directory only used by the\n"
                + "compilewithwalls task, not the class file output directory.\n"
                + "The class file output directory is specified in javac's destdir attribute";
        thrown.expect(BuildException.class);
        thrown.expectMessage(msg);
        buildRule.executeTarget("testIntermediaryDirInsideDestDir");
    }

    /**
     * Method testPackageDoesntEndWithStar.
     */
    @Test
    public void testPackageDoesntEndWithStar() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("The package='biz.xsoftware' must end with "
                + ".* or .** such as biz.xsoftware.* or "
                + "biz.xsoftware.**");
        buildRule.executeTarget("testPackageDoesntEndWithStar");
    }

    /**
     * Method testPackageDoesntHaveSlash.
     */
    @Test
    public void testPackageDoesntHaveSlash() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("A package name cannot contain '\\' or '/' like package="
                + "biz/xsoftware.*\nIt must look like biz.xsoftware.* for example");
        buildRule.executeTarget("testPackageDoesntHaveSlash");
    }

    /**
     * Method testDependsOnNonExistPackage.
     */
    @Test
    public void testDependsOnNonExistPackage() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("package name=modA did not have modB"
                + " listed before it and cannot compile without it");
        buildRule.executeTarget("testDependsOnNonExistPackage");
    }

    /**
     * Method testDependsOnPackageAfter.
     */
    @Test
    public void testDependsOnPackageAfter() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("package name=modA did not have modB"
                + " listed before it and cannot compile without it");
        buildRule.executeTarget("testDependsOnPackageAfter");
    }

    /**
     * Method testPackageABreakingWhenAIsCompiledFirst.
     */
    @Test
    public void testPackageABreakingWhenAIsCompiledFirst() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("Compile failed; see the compiler error output for details.");
        buildRule.executeTarget("testPackageABreakingWhenAIsCompiledFirst");
    }

    /**
     * This test case tests when modB depends on modA but it was
     * not specified in the walls so modA is not in modB's path.
     * The build should then fail until they change the build.xml
     * so modB depends on modA in the walls element.
     */
    @Test
    public void testPackageBBreakingWhenAIsCompiledFirst() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("Compile failed; see the compiler error output for details.");
        try {
            buildRule.executeTarget("testPackageBBreakingWhenAIsCompiledFirst");
        } finally {
            //modA should have been compiled successfully, it is only modB that
            //fails.  It is very important we make sure A got compiled otherwise
            //we are not testing the correct behavior and the test would be wrong.
            ensureClassFileExists("testB/mod/modA/TrialBModuleA.class", true);
            ensureClassFileExists("testB/mod/modB/TrialBModuleB.class", false);
        }
    }

    /**
     * Method testCompileOfAllUsingDepends.
     */
    @Test
    public void testCompileOfAllUsingDepends() {
        ensureClassFileExists("testC/mod/TrialCModule.class", false);
        //make sure we are testing the correct thing and Module.java exists!
        ensureJavaFileExists("testC/mod/TrialCModule.java", true);

        buildRule.executeTarget("testCompileOfAllUsingDepends");

        //must test class files were actually created afterwards.
        //The build might pass with no class files if the task is
        //messed up.
        ensureClassFileExists("testC/mod/TrialCModule.class", true);
    }

    //---------------------------------------------------------
    //
    //  The following tests are all just repeats of some of the above tests
    //  except the below tests use External walls file and the above tests
    //  don't.
    //
    //---------------------------------------------------------

    /**
     * Method testDependsOnPackageAfterExternalWalls.
     */
    @Test
    public void testDependsOnPackageAfterExternalWalls() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("package name=modA did not have modB"
                + " listed before it and cannot compile without it");
        buildRule.executeTarget("testDependsOnPackageAfterExternalWalls");
    }

    /**
     * This test case tests when modB depends on modA but it was
     * not specified in the walls so modA is not in modB's path.
     * The build should then fail until they change the build.xml
     * so modB depends on modA in the walls element.
     */
    @Test
    public void testPackageBBreakingWhenAIsCompiledFirstExternalWalls() {
        ensureClassFileExists("testB/mod/modA/TrialBModuleA.class", false);
        ensureJavaFileExists("testB/mod/modB/TrialBModuleB.java", true);

        thrown.expect(BuildException.class);
        thrown.expectMessage("Compile failed; see the compiler error output for details.");
        try {
            buildRule.executeTarget("testPackageBBreakingWhenAIsCompiledFirst");
        } finally {
            //modA should have been compiled successfully, it is only modB that
            //fails.  It is very important we make sure A got compiled otherwise
            //we are not testing the correct behavior and the test would be wrong.
            ensureClassFileExists("testB/mod/modA/TrialBModuleA.class", true);
            ensureClassFileExists("testB/mod/modB/TrialBModuleB.class", false);
        }
    }

    /**
     * Method testCompileOfAllUsingDependsExternalWalls.
     */
    @Test
    public void testCompileOfAllUsingDependsExternalWalls() {
        ensureClassFileExists("testC/mod/TrialCModule.class", false);
        ensureJavaFileExists("testC/mod/TrialCModule.java", true);
        buildRule.executeTarget("testCompileOfAllUsingDependsExternalWalls");
        //must test class files were actually created afterwards.
        //The build might pass with no class files if the task is
        //messed up.
        ensureClassFileExists("testC/mod/TrialCModule.class", true);
    }

    /**
     * Method ensureJavaFileExists.
     *
     * @param file        String
     * @param shouldExist boolean
     */
    private void ensureJavaFileExists(String file, boolean shouldExist) {
        //must test that it is testing the correct directory.
        //It wasn't before.
        File f1 = new File(baseDir, file);
        if (shouldExist) {
            assertTrue("The java file=" + f1.getAbsolutePath()
                            + " didn't exist, we can't run this test.  It will pass with false results",
                    f1.exists());
        } else {
            assertTrue("The java file=" + f1.getAbsolutePath()
                            + " exists and shouldn't, we can't run this test.  It will pass with false results",
                    !f1.exists());
        }
    }

    /**
     * Method ensureClassFileExists.
     *
     * @param file        String
     * @param shouldExist boolean
     */
    private void ensureClassFileExists(String file, boolean shouldExist) {
        File f1 = new File(baseDir + "/compilewithwalls/classes", file);
        if (shouldExist) {
            assertTrue("The class file=" + f1.getAbsolutePath()
                            + " didn't get created, No build exception\nwas thrown,"
                            + " but the build failed because a class\nfile should have been created",
                    f1.exists());
        } else {
            assertTrue("The class file=" + f1.getAbsolutePath()
                            + " exists and shouldn't\n"
                            + "Test may be inaccurate if this file already exists...correct the test",
                    !f1.exists());
        }
    }

}
