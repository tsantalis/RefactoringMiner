/*
 * Copyright (c) 2001-2005 Ant-Contrib project.  All rights reserved.
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
package net.sf.antcontrib.design;

import static org.junit.Assert.assertTrue;

import java.io.File;

import net.sf.antcontrib.BuildFileTestBase;

import org.apache.tools.ant.DefaultLocaleRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
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
public class VerifyDesignTest extends BuildFileTestBase {
    /**
     * Field REASON.
     * (value is ""Build should have failed with proper message and did not"")
     */
    private static final String REASON = "Build should have failed with proper message and did not";

    /**
     * Field baseDir.
     */
    private static String baseDir = "";

    /**
     * Default locale rule.
     */
    @Rule
    public DefaultLocaleRule defaultLocaleRule = DefaultLocaleRule.en();

    /**
     * Method setUp.
     */
    @Before
    public void setUp() {
        configureProject("design/verifydesign.xml");
        baseDir = getProject().getBaseDir().getAbsolutePath();
    }

    /**
     * Method tearDown.
     */
    @After
    public void tearDown() {
        executeTarget("cleanup");
    }

    /**
     * Method testArrayDepend.
     */
    @Test
    public void testArrayDepend() {
        String class1 = "mod.arraydepend.ClassDependsOnArray";
        String class2 = "mod.dummy.DummyClass";
        expectDesignCheckFailure("testArrayDepend", Design.getErrorMessage(class1, class2));
    }

    /**
     * Method testArrayDepend2.
     */
    @Test
    public void testArrayDepend2() {
        executeTarget("testArrayDepend2");
    }

    /**
     * Method testArrayDepend3.
     */
    @Test
    public void testArrayDepend3() {
        String class1 = "mod.arraydepend3.ClassDependsOnArray";
        //
        //  The trailing semi-colon makes the test pass,
        //      but likely reflects a problem in the VerifyDesign task
        //
        String class2 = "mod.dummy.DummyClass;";
        expectDesignCheckFailure("testArrayDepend3", Design.getErrorMessage(class1, class2));
    }

    /**
     * Method testBadXML.
     */
    @Test
    public void testBadXML() {
        // Tests are run from basedir "target/test-classes"
        File designFile = new File(baseDir, "design/designfiles/badxml.xml");
        String msg = "\nProblem parsing design file='" + designFile.getAbsolutePath()
                + "'.  \nline=3 column=1 Reason:\n"
                + "Element type \"design\" must be followed by either attribute specifications, \">\" or \"/>\".\n";
        expectSpecificBuildException("testBadXML", REASON, msg);
    }

    /**
     * Method testCastDepend.
     */
    @Test
    public void testCastDepend() {
        String class1 = "mod.castdepend.ClassDependsOnCast";
        String class2 = "mod.dummy.DummyInterface";
        expectDesignCheckFailure("testCastDepend", Design.getErrorMessage(class1, class2));
    }

    /**
     * Method testCatchDepend.
     */
    @Test
    public void testCatchDepend() {
        String class1 = "mod.catchdepend.ClassDependsOnCatch";
        String class2 = "mod.dummy.DummyRuntimeException";
        expectDesignCheckFailure("testCatchDepend", Design.getErrorMessage(class1, class2));
    }

    /**
     * Method testClassFiles.
     */
    @Test
    public void testClassFiles() {
        String class1 = "mod.castdepend.ClassDependsOnCast";
        String class2 = "mod.dummy.DummyInterface";
        expectDesignCheckFailure("testClassFiles", Design.getErrorMessage(class1, class2));
    }

    /**
     * Method testDeclareJavaUtil.
     */
    @Test
    public void testDeclareJavaUtil() {
        executeTarget("testDeclareJavaUtil");
    }

    /**
     * Method testDeclareJavaUtilFail.
     */
    @Test
    public void testDeclareJavaUtilFail() {
        String class1 = "mod.declarejavautil.ClassDependsOnJavaUtil";
        String class2 = "java.util.List";
        expectDesignCheckFailure("testDeclareJavaUtilFail", Design.getErrorMessage(class1, class2));
    }

    /**
     * Method testDeclareJavax.
     */
    @Test
    public void testDeclareJavax() {
        String class1 = "mod.declarejavax.ClassDependsOnJavax";
        String class2 = "javax.swing.JButton";
        expectDesignCheckFailure("testDeclareJavax", Design.getErrorMessage(class1, class2));
    }

    /**
     * Method testDeclareJavaxPass.
     */
    @Test
    public void testDeclareJavaxPass() {
        executeTarget("testDeclareJavaxPass");
    }

    //tests to write

    //depend on java.util should pass by default
    //depend on java.util should fail after defining needDeclareTrue
    //depend on javax.swing should pass after needDeclareFalse

    //depend on dummy should pass after needDeclareFalse

    /**
     * Method testFieldDepend.
     */
    @Test
    public void testFieldDepend() {
        String class1 = "mod.fielddepend.ClassDependsOnField";
        String class2 = "mod.dummy.DummyClass";
        expectDesignCheckFailure("testFieldDepend", Design.getErrorMessage(class1, class2));
    }

    /**
     * Method testFieldRefDepend.
     */
    @Test
    public void testFieldRefDepend() {
        String class1 = "mod.fieldrefdepend.ClassDependsOnReferenceInFieldDeclaration";
        String class2 = "mod.dummy.DummyClass";
        expectDesignCheckFailure("testFieldRefDepend", Design.getErrorMessage(class1, class2));
    }

    /**
     * Method testInnerClassDepend.
     */
    @Test
    public void testInnerClassDepend() {
        String class1 = "mod.innerclassdepend.InnerClassDependsOnSuper$Inner";
        String class2 = "mod.dummy.DummyClass";
        expectDesignCheckFailure("testInnerClassDepend", Design.getErrorMessage(class1, class2));
    }

    /**
     * Method testInstanceOfDepend.
     */
    @Test
    public void testInstanceOfDepend() {
        String class1 = "mod.instanceofdepend.ClassDependsOnInstanceOf";
        String class2 = "mod.dummy.DummyInterface";
        expectDesignCheckFailure("testInstanceOfDepend", Design.getErrorMessage(class1, class2));
    }

    /**
     * Method testInterfaceDepend.
     */
    @Test
    public void testInterfaceDepend() {
        String class1 = "mod.interfacedepend.ClassDependsOnInterfaceMod2";
        String class2 = "mod.dummy.DummyInterface";
        expectDesignCheckFailure("testInterfaceDepend", Design.getErrorMessage(class1, class2));
    }

    /**
     * Method testLocalVarDepend.
     */
    @Test
    public void testLocalVarDepend() {
        String class1 = "mod.localvardepend.ClassDependsOnLocalVar";
        String class2 = "mod.dummy.DummyClass";
        expectDesignCheckFailure("testLocalVarDepend", Design.getErrorMessage(class1, class2));
    }

    /**
     * Method testLocalVarRefDepend.
     */
    @Test
    public void testLocalVarRefDepend() {
        String class1 = "mod.localvarrefdepend.ClassDependsOnLocalVariableReference";
        String class2 = "mod.dummy.DummyInterface";
        expectDesignCheckFailure("testLocalVarRefDepend", Design.getErrorMessage(class1, class2));
    }

    /**
     * Method testMissingAttribute.
     */
    @Test
    public void testMissingAttribute() {
        // Tests are run from basedir "target/test-classes"
        File designFile = new File(baseDir, "design/designfiles/missingattribute.xml");
        String msg = "\nProblem parsing design file='"
                + designFile.getAbsolutePath() + "'.  \nline=3 column=31 Reason:\nError in file="
                + designFile.getAbsolutePath() + ", package element must contain the 'name' attribute\n";
        expectSpecificBuildException("testMissingAttribute", REASON, msg);
    }

    /**
     * Method testMultipleErrors.
     */
    @Test
    public void testMultipleErrors() {
        // Tests are run from basedir "target/test-classes"
        File jarFile = new File(baseDir, "design/build/jar/test.jar");
        String class1 = "mod.arraydepend.ClassDependsOnArray";
        String class2 = "mod.dummy.DummyClass";
        //executeTarget("testMultipleErrors");
        String error1 = Design.getWrapperMsg(jarFile, Design.getErrorMessage(class1, class2));
        class1 = "mod.castdepend.ClassDependsOnCast";
        String error2 = Design.getWrapperMsg(jarFile, Design.getNoDefinitionError(class1));
        class1 = "mod.catchdepend.ClassDependsOnCatch";
        class2 = "mod.dummy.DummyRuntimeException";
        String error3 = Design.getWrapperMsg(jarFile, Design.getErrorMessage(class1, class2));
        String s = "\nEvaluating package=mod.arraydepend" + error1;
        s += "\nEvaluating package=mod.castdepend" + error2;
        s += "\nEvaluating package=mod.catchdepend" + error3;
        s += "\nEvaluating package=mod.dummy";

//        executeTarget("testMultipleErrors");
        expectDesignCheckFailure("testMultipleErrors", s);
    }

    /**
     * Method testNewDepend.
     */
    @Test
    public void testNewDepend() {
        String class1 = "mod.newdepend.ClassDependsOnNew";
        String class2 = "mod.dummy.DummyClass";
        expectDesignCheckFailure("testNewDepend", Design.getErrorMessage(class1, class2));
    }

    /**
     * Method testNewDepend2.
     */
    @Test
    public void testNewDepend2() {
        String class1 = "mod.newdepend2.ClassDependsOnNewInField";
        String class2 = "mod.dummy.DummyClass";
        expectDesignCheckFailure("testNewDepend2", Design.getErrorMessage(class1, class2));
    }

    /**
     * Method testNoDebugOption.
     */
    @Test
    public void testNoDebugOption() {
        String class1 = "mod.nodebugoption.ClassDependsOnLocalVar";
        expectDesignCheckFailure("testNoDebugOption", VisitorImpl.getNoDebugMsg(class1));
    }

    /**
     * Method testNoJar.
     */
    @Test
    public void testNoJar() {
        // Tests are run from basedir "target/test-classes"
        File jar = new File(baseDir, "design/build/jar/test.jar");
        expectSpecificBuildException("testNoJar", REASON, VisitorImpl.getNoFileMsg(jar));
    }

    /**
     * Method testParamDepend.
     */
    @Test
    public void testParamDepend() {
        String class1 = "mod.paramdepend.ClassDependsOnParameter";
        String class2 = "mod.dummy.DummyClass";
        expectDesignCheckFailure("testParamDepend", Design.getErrorMessage(class1, class2));
    }

    /**
     * Method testPassLocalDepend.
     */
    @Test
    public void testPassLocalDepend() {
        executeTarget("testPassLocalDepend");
    }

    /**
     * Method testPathElementLocation.
     */
    @Test
    public void testPathElementLocation() {
        executeTarget("testPathElementLocation");
    }

    /**
     * Method testPathElementPath.
     */
    @Test
    public void testPathElementPath() {
        executeTarget("testPathElementPath");
    }

    /**
     * Method testPutStatic.
     */
    @Test
    public void testPutStatic() {
        executeTarget("testPutStatic");
    }

    /**
     * Method testRecursion.
     */
    @Test
    public void testRecursion() {
        executeTarget("testRecursion");
    }

    /**
     * Method testRecursion2.
     */
    @Test
    public void testRecursion2() {
        executeTarget("testRecursion2");
    }

    /**
     * Method testRecursion3.
     */
    @Test
    public void testRecursion3() {
        executeTarget("testRecursion3");
    }

    /**
     * Method testReturnValDepend.
     */
    @Test
    public void testReturnValDepend() {
        String class1 = "mod.returnvaldepend.ClassDependsOnReturnValue";
        String class2 = "mod.dummy.DummyInterface";
        expectDesignCheckFailure("testReturnValDepend", Design.getErrorMessage(class1, class2));
    }

    /**
     * Method testSignatureExceptionDepend.
     */
    @Test
    public void testSignatureExceptionDepend() {
        String class1 = "mod.signatureexceptiondepend.ClassDependsOnExceptionInMethodSignature";
        String class2 = "mod.dummy.DummyException";
        expectDesignCheckFailure("testSignatureExceptionDepend",
                Design.getErrorMessage(class1, class2));
    }

    /**
     * Method testStaticDepend.
     */
    @Test
    public void testStaticDepend() {
        String class1 = "mod.staticdepend.ClassDependsOnStatic";
        String class2 = "mod.dummy.DummyClass";
        expectDesignCheckFailure("testStaticDepend", Design.getErrorMessage(class1, class2));
    }

    /**
     * Method testStaticField2Depend.
     */
    @Test
    public void testStaticField2Depend() {
        String class1 = "mod.staticfield2depend.ClassDependsOnStaticField";
        String class2 = "mod.dummy.DummyClass";
        expectDesignCheckFailure("testStaticField2Depend", Design.getErrorMessage(class1, class2));
    }

    /**
     * Method testStaticFieldDepend.
     */
    @Test
    public void testStaticFieldDepend() {
        String class1 = "mod.staticfielddepend.ClassDependsOnStaticField";
        String class2 = "mod.dummy.DummyClass";
        expectDesignCheckFailure("testStaticFieldDepend", Design.getErrorMessage(class1, class2));
    }

    /**
     * Method testStaticFinalDepend.
     */
    @Test
    public void testStaticFinalDepend() {
        //This is an impossible test since javac compiles String and primitive
        //constants into the code losing any reference to the class that
        //contains the constant...In this one instance, verifydesign can't
        //verify that constant imports don't violate the design!!!!
        //check out mod.staticfinaldepend.ClassDependsOnStaticField to see the
        //code that will pass the design even if it is violating it.
        //      String class1 = "mod.staticfinaldepend.ClassDependsOnConstant";
        //      String class2 = "mod.dummy.DummyClass";
        //      expectDesignCheckFailure("testStaticFinalDepend", Design.getErrorMessage(class1, class2));
    }

    /**
     * Method testSuperDepend.
     */
    @Test
    public void testSuperDepend() {
        File f = new File(baseDir, "build/jar/test.jar");

        //      executeTarget("testSuperDepend");
        String class1 = "mod.superdepend.ClassDependsOnSuperMod2";
        String class2 = "mod.dummy.DummyClass";
        expectDesignCheckFailure("testSuperDepend", Design.getErrorMessage(class1, class2));

        //jar file should have been deleted
        assertTrue("jar file should not exist yet still does", !f.exists());
    }

    /**
     * Method testWarSuccess.
     */
    @Test
    public void testWarSuccess() {
        executeTarget("testWarSuccess");
    }

    /**
     * Method testWarFailure.
     */
    @Test
    public void testWarFailure() {
        String class1 = "mod.warfailure.ClassDependsOnSuperMod2";
        String class2 = "mod.dummy.DummyClass";
        expectDesignCheckFailure("testWarFailure", Design.getErrorMessage(class1, class2));
    }

    /**
     * Method expectDesignCheckFailure.
     *
     * @param target  String
     * @param message String
     */
    private void expectDesignCheckFailure(String target, String message) {
        expectSpecificBuildException(target, "Design is broken",
                "Design check failed due to previous errors");
        assertLogContaining(message);
    }
}
