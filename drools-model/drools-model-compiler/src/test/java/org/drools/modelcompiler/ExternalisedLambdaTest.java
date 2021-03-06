/*
 * Copyright 2005 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.drools.modelcompiler;

import java.util.ArrayList;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.drools.modelcompiler.builder.RuleWriter;
import org.drools.modelcompiler.domain.Person;
import org.drools.modelcompiler.util.lambdareplace.NonExternalisedLambdaFoundException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.kie.api.KieServices;
import org.kie.api.builder.model.KieModuleModel;
import org.kie.api.runtime.KieSession;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ExternalisedLambdaTest extends BaseModelTest {

    public ExternalisedLambdaTest(RUN_TYPE testRunType) {
        super(testRunType);
    }

    @Before
    public void init() {
        RuleWriter.setCheckNonExternalisedLambda(true);
    }

    @After
    public void clear() {
        RuleWriter.setCheckNonExternalisedLambda(false);
    }

    @Test
    public void testConsequenceNoVariable() throws Exception {
        // DROOLS-4924
        String str =
                "package defaultpkg;\n" +
                     "import " + Person.class.getCanonicalName() + ";" +
                     "rule R when\n" +
                     "  $p : Person(name == \"Mario\")\n" +
                     "then\n" +
                     "  System.out.println(\"Hello\");\n" +
                     "end";

        KieModuleModel kieModuleModel = KieServices.get().newKieModuleModel();
        KieSession ksession = null;
        try {
            ksession = getKieSession(kieModuleModel, str);
        } catch (NonExternalisedLambdaFoundException e) {
            fail(e.getMessage());
        }

        Person me = new Person("Mario", 40);
        ksession.insert(me);

        assertEquals(1, ksession.fireAllRules());
    }

    @Test
    public void testExternalizeBindingVariableLambda() throws Exception {
        String str =
                "package defaultpkg;\n" +
                     "import " + Person.class.getCanonicalName() + ";" +
                     "global java.util.List list;\n" +
                     "rule R when\n" +
                     "  $p : Person($n : name == \"Mario\")\n" +
                     "then\n" +
                     "  list.add($n);\n" +
                     "end";

        KieModuleModel kieModuleModel = KieServices.get().newKieModuleModel();
        KieSession ksession = null;
        try {
            ksession = getKieSession(kieModuleModel, str);
        } catch (NonExternalisedLambdaFoundException e) {
            fail(e.getMessage());
        }

        final List<String> list = new ArrayList<>();
        ksession.setGlobal("list", list);

        Person me = new Person("Mario", 40);
        ksession.insert(me);
        ksession.fireAllRules();

        Assertions.assertThat(list).containsExactlyInAnyOrder("Mario");
    }

    @Test
    public void testExternalizeLambdaPredicate() throws Exception {
        String str =
                "package defaultpkg;\n" +
                     "import " + Person.class.getCanonicalName() + ";" +
                     "global java.util.List list;\n" +
                     "rule R when\n" +
                     "  $p : Person(name == \"Mario\")\n" +
                     "then\n" +
                     "  list.add($p.getName());\n" +
                     "end";

        KieModuleModel kieModuleModel = KieServices.get().newKieModuleModel();
        KieSession ksession = null;
        try {
            ksession = getKieSession(kieModuleModel, str);
        } catch (NonExternalisedLambdaFoundException e) {
            fail(e.getMessage());
        }

        final List<String> list = new ArrayList<>();
        ksession.setGlobal("list", list);

        Person me = new Person("Mario", 40);
        ksession.insert(me);
        ksession.fireAllRules();

        Assertions.assertThat(list).containsExactlyInAnyOrder("Mario");
    }

    @Test
    public void testExternalizeLambdaUsingVariable() throws Exception {
        String str =
                "package defaultpkg;\n" +
                     "import " + Person.class.getCanonicalName() + ";\n" +
                     "global java.util.List list;\n" +
                     "rule R when\n" +
                     "  Integer( $i : intValue )\n" +
                     "  Person( age > $i )\n" +
                     "then\n" +
                     "  list.add($i);\n" +
                     "end";

        KieModuleModel kieModuleModel = KieServices.get().newKieModuleModel();
        KieSession ksession = null;
        try {
            ksession = getKieSession(kieModuleModel, str);
        } catch (NonExternalisedLambdaFoundException e) {
            fail(e.getMessage());
        }

        final List<Integer> list = new ArrayList<>();
        ksession.setGlobal("list", list);

        ksession.insert(43);
        ksession.insert(new Person("John", 44));
        ksession.fireAllRules();

        Assertions.assertThat(list).containsExactlyInAnyOrder(43);
    }

}
