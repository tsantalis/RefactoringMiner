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

import net.sf.antcontrib.BuildFileTestBase;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * BIG NOTE
 * <p>Always expect specific exceptions.  Most of these test cases when
 * first submitted were not and therefore were not testing what they said
 * they were testing.  Exceptions were being caused by other things and the
 * tests were still passing.  Now all tests expect a specific exception
 * so if any other is thrown we will fail the test case.</p>
 * <p>Testcase for &lt;propertycopy&gt;.</p>
 */
public class CompileWithWallsTest extends BuildFileTestBase {

    /**
     * Field baseDir.
     */
    private static String baseDir = "";

    /**
     * Method setUp.
     */
    @Before
    public void setUp() {
        configureProject("walls/compilewithwalls.xml");
        baseDir = getProject().getBaseDir().getAbsolutePath();
    }

    /**
     * Method tearDown.
     */
    @After
    public void tearDown() {
        executeTarget("cleanup");

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
        expectSpecificBuildException("testTooManyNestedWallElements",
                "TooManyNestedWallElements",
                "compilewithwalls task only supports one nested walls element or one walls attribute");
    }

    /**
     * Method testFakeTest.
     */
    @Test
    public void testFakeTest() {
        // this is being deprecated, tests no longer really needed.
    }

    /**
     * Method testTooManyNestedJavacElements.
     */
    @Ignore
    public void testTooManyNestedJavacElements() {
        expectSpecificBuildException("testTooManyNestedJavacElements",
                "TooManyNestedJavacElements",
                "compilewithwalls task only supports one nested javac element");
    }

    /**
     * Method testNoWallElement.
     */
    @Ignore
    public void testNoWallElement() {
        expectSpecificBuildException("testNoWallElement",
                "NoWallElement",
                "There must be a nested walls element");
    }

    /**
     * Method testNoJavacElement.
     */
    @Ignore
    public void testNoJavacElement() {
        expectSpecificBuildException("testNoJavacElement",
                "NoJavacElement",
                "There must be a nested javac element");
    }

    /**
     * Method testMoreThanOneSrcDirInJavac.
     */
    @Ignore
    public void testMoreThanOneSrcDirInJavac() {
        executeTarget("testMoreThanOneSrcDirInJavac");
    }

    /**
     * Method testNoSrcDirInJavac.
     */
    @Ignore
    public void testNoSrcDirInJavac() {
        expectSpecificBuildException("testNoSrcDirInJavac",
                "NoSrcDirInJavac",
                "Javac inside compilewithwalls must have a srcdir specified");
    }

    /**
     * Method testIntermediaryDirAndDestDirSame.
     */
    @Ignore
    public void testIntermediaryDirAndDestDirSame() {
        expectSpecificBuildException("testIntermediaryDirAndDestDirSame",
                "IntermediaryDirAndDestDirSame",
                "intermediaryBuildDir attribute cannot be specified\n"
                        + "to be the same as destdir or inside desdir of the javac task.\n"
                        + "This is an intermediary build directory only used by the\n"
                        + "compilewithwalls task, not the class file output directory.\n"
                        + "The class file output directory is specified in javac's destdir attribute");
    }

    /**
     * Method testIntermediaryDirInsideDestDir.
     */
    @Ignore
    public void testIntermediaryDirInsideDestDir() {
        expectSpecificBuildException("testIntermediaryDirInsideDestDir",
                "IntermediaryDirInsideDestDir",
                "intermediaryBuildDir attribute cannot be specified\n"
                        + "to be the same as destdir or inside desdir of the javac task.\n"
                        + "This is an intermediary build directory only used by the\n"
                        + "compilewithwalls task, not the class file output directory.\n"
                        + "The class file output directory is specified in javac's destdir attribute");
    }

    /**
     * Method testPackageDoesntEndWithStar.
     */
    @Ignore
    public void testPackageDoesntEndWithStar() {
        expectSpecificBuildException("testPackageDoesntEndWithStar",
                "PackageDoesntEndWithStar",
                "The package='biz.xsoftware' must end with "
                        + ".* or .** such as biz.xsoftware.* or "
                        + "biz.xsoftware.**");
    }

    /**
     * Method testPackageDoesntHaveSlash.
     */
    @Ignore
    public void testPackageDoesntHaveSlash() {
        expectSpecificBuildException("testPackageDoesntHaveSlash",
                "PackageDoesntHaveSlash",
                "A package name cannot contain '\\' or '/' like package="
                        + "biz/xsoftware.*\nIt must look like biz.xsoftware.* for example");
    }

    /**
     * Method testDependsOnNonExistPackage.
     */
    @Ignore
    public void testDependsOnNonExistPackage() {
        expectSpecificBuildException("testDependsOnNonExistPackage",
                "DependsOnNonExistPackage",
                "package name=modA did not have modB"
                        + " listed before it and cannot compile without it");
    }

    /**
     * Method testDependsOnPackageAfter.
     */
    @Ignore
    public void testDependsOnPackageAfter() {
        expectSpecificBuildException("testDependsOnPackageAfter",
                "DependsOnPackageAfter",
                "package name=modA did not have modB"
                        + " listed before it and cannot compile without it");
    }

    /**
     * Method testPackageABreakingWhenAIsCompiledFirst.
     */
    @Ignore
    public void testPackageABreakingWhenAIsCompiledFirst() {
        expectSpecificBuildException("testPackageABreakingWhenAIsCompiledFirst",
                "PackageABreakingWhenAIsCompiledFirst",
                "Compile failed; see the compiler error output for details.");
    }

    /**
     * This test case tests when modB depends on modA but it was
     * not specified in the walls so modA is not in modB's path.
     * The build should then fail until they change the build.xml
     * so modB depends on modA in the walls element.
     */
    @Ignore
    public void testPackageBBreakingWhenAIsCompiledFirst() {
        expectSpecificBuildException("testPackageBBreakingWhenAIsCompiledFirst",
                "PackageBBreakingWhenAIsCompiledFirst",
                "Compile failed; see the compiler error output for details.");

        //modA should have been compiled successfully, it is only modB that
        //fails.  It is very important we make sure A got compiled otherwise
        //we are not testing the correct behavior and the test would be wrong.
        ensureClassFileExists("testB/mod/modA/ModuleA.class", true);
        ensureClassFileExists("testB/mod/modB/ModuleB.class", false);
    }

    /**
     * Method testCompileOfAllUsingDepends.
     */
    @Ignore
    public void testCompileOfAllUsingDepends() {
        ensureClassFileExists("testC/mod/Module.class", false);
        //make sure we are testing the correct thing and Module.java exists!
        ensureJavaFileExists("testC/mod/Module.java", true);

        executeTarget("testCompileOfAllUsingDepends");

        //must test class files were actually created afterwards.
        //The build might pass with no class files if the task is
        //messed up.
        ensureClassFileExists("testC/mod/Module.class", true);

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
    @Ignore
    public void testDependsOnPackageAfterExternalWalls() {
        expectSpecificBuildException("testDependsOnPackageAfterExternalWalls",
                "DependsOnPackageAfterExternalWalls",
                "package name=modA did not have modB"
                        + " listed before it and cannot compile without it");
    }

    /**
     * This test case tests when modB depends on modA but it was
     * not specified in the walls so modA is not in modB's path.
     * The build should then fail until they change the build.xml
     * so modB depends on modA in the walls element.
     */
    @Ignore
    public void testPackageBBreakingWhenAIsCompiledFirstExternalWalls() {
        ensureClassFileExists("testB/mod/modA/ModuleA.class", false);
        ensureJavaFileExists("testB/mod/modB/ModuleB.java", true);

        expectSpecificBuildException("testPackageBBreakingWhenAIsCompiledFirst",
                "PackageBBreakingWhenAIsCompiledFirst",
                "Compile failed; see the compiler error output for details.");

        //modA should have been compiled successfully, it is only modB that
        //fails.  It is very important we make sure A got compiled otherwise
        //we are not testing the correct behavior and the test would be wrong.
        ensureClassFileExists("testB/mod/modA/ModuleA.class", true);
        ensureClassFileExists("testB/mod/modB/ModuleB.class", false);
    }

    /**
     * Method testCompileOfAllUsingDependsExternalWalls.
     */
    @Ignore
    public void testCompileOfAllUsingDependsExternalWalls() {
        ensureClassFileExists("testC/mod/Module.class", false);
        ensureJavaFileExists("testC/mod/Module.java", true);
        executeTarget("testCompileOfAllUsingDependsExternalWalls");
        //must test class files were actually created afterwards.
        //The build might pass with no class files if the task is
        //messed up.
        ensureClassFileExists("testC/mod/Module.class", true);
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
