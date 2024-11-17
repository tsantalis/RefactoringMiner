/**
 * Copyright (C) 2015 BonitaSoft S.A.
 * BonitaSoft, 32 rue Gustave Eiffel - 38000 Grenoble
 * This library is free software; you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation
 * version 2.1 of the License.
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth
 * Floor, Boston, MA 02110-1301, USA.
 */
package org.bonitasoft.engine.core.form.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.util.List;

import org.bonitasoft.engine.bpm.CommonBPMServicesTest;
import org.bonitasoft.engine.commons.exceptions.SObjectNotFoundException;
import org.bonitasoft.engine.core.form.FormMappingService;
import org.bonitasoft.engine.core.form.SFormMapping;
import org.bonitasoft.engine.core.process.definition.ProcessDefinitionService;
import org.bonitasoft.engine.core.process.definition.model.SProcessDefinition;
import org.bonitasoft.engine.core.process.definition.model.impl.SProcessDefinitionImpl;
import org.bonitasoft.engine.form.FormMappingTarget;
import org.bonitasoft.engine.form.FormMappingType;
import org.bonitasoft.engine.page.PageService;
import org.bonitasoft.engine.page.SPage;
import org.bonitasoft.engine.page.URLAdapterConstants;
import org.bonitasoft.engine.test.CommonTestUtil;
import org.bonitasoft.engine.transaction.TransactionService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Baptiste Mesta
 */
public class FormMappingServiceIT extends CommonBPMServicesTest {

    private PageService pageService;
    public FormMappingService formMappingService;

    private TransactionService transactionService;
    public static final String PAGE_NAME = "custompage_coucou";
    private SPage page;
    private ProcessDefinitionService processDefinitionService;
    private SProcessDefinition p1;
    private SProcessDefinition p2;

    @Before
    public void setup() throws Exception {
        processDefinitionService = getTenantAccessor().getProcessDefinitionService();
        transactionService = getTransactionService();
        formMappingService = getTenantAccessor().getFormMappingService();
        pageService = getTenantAccessor().getPageService();
        transactionService.begin();
        p1 = processDefinitionService.store(new SProcessDefinitionImpl("P1", "1.0"), "display", "display");
        p2 = processDefinitionService.store(new SProcessDefinitionImpl("P2", "1.0"), "display", "display");
        page = pageService.addPage(
                CommonTestUtil.createTestPageContent(PAGE_NAME, "coucou depuis la page", "C'était juste pour dire coucou"), "mySuperPage.zip",
                54L);
        transactionService.complete();
    }

    @After
    public void tearDown() throws Exception {
        clearFormMapping();
    }

    protected void clearFormMapping() throws Exception {
        transactionService.begin();
        for (SFormMapping sFormMapping : formMappingService.list(0, 1000)) {
            formMappingService.delete(sFormMapping);
        }
        pageService.deletePage(page.getId());
        processDefinitionService.delete(p1.getId());
        processDefinitionService.delete(p2.getId());
        transactionService.complete();
    }

    @Test
    public void createAndListFormMapping() throws Exception {
        //given
        transactionService.begin();

        formMappingService.create(p1.getId(), "step1", FormMappingType.TASK.getId(), "INTERNAL", PAGE_NAME);
        formMappingService.create(p1.getId(), null, FormMappingType.PROCESS_START.getId(), "URL", "http://bit.coin");
        formMappingService.create(p1.getId(), null, FormMappingType.PROCESS_OVERVIEW.getId(), "LEGACY", null);
        formMappingService.create(p2.getId(), null, FormMappingType.PROCESS_OVERVIEW.getId(), "UNDEFINED", null);
        transactionService.complete();

        transactionService.begin();
        List<SFormMapping> list = formMappingService.list(p1.getId(), 0, 10);
        List<SFormMapping> listAll = formMappingService.list(0, 10);

        transactionService.complete();
        assertThat(list).extracting("type").containsExactly(FormMappingType.TASK.getId(), FormMappingType.PROCESS_START.getId(),
                FormMappingType.PROCESS_OVERVIEW.getId());
        assertThat(list).extracting("task").containsExactly("step1", null, null);
        assertThat(list).extracting("pageMapping.url").containsExactly(null, "http://bit.coin", null);
        assertThat(list).extracting("pageMapping.urlAdapter").containsExactly(null, URLAdapterConstants.EXTERNAL_URL_ADAPTER,
                URLAdapterConstants.LEGACY_URL_ADAPTER);
        assertThat(list).extracting("pageMapping.pageId").containsExactly(page.getId(), null, null);
        //        assertThat(list).extracting("pageMapping.key").containsExactly();
        assertThat(listAll).extracting("type").containsExactly(FormMappingType.TASK.getId(), FormMappingType.PROCESS_START.getId(),
                FormMappingType.PROCESS_OVERVIEW.getId(), FormMappingType.PROCESS_OVERVIEW.getId());
        assertThat(listAll).extracting("processDefinitionId").containsExactly(p1.getId(), p1.getId(), p1.getId(), p2.getId());
    }

    @Test
    public void create_and_get_FormMapping() throws Exception {
        transactionService.begin();
        SFormMapping taskForm = formMappingService.create(p1.getId(), "step1", FormMappingType.TASK.getId(), "URL", "http://bit.coin");
        transactionService.complete();

        transactionService.begin();
        SFormMapping sFormMapping = formMappingService.get(taskForm.getId());
        SFormMapping sFormMappingByProperties = formMappingService.get(p1.getId(), FormMappingType.TASK.getId(), "step1");
        transactionService.complete();
        assertThat(sFormMapping).isEqualTo(taskForm).isEqualTo(sFormMappingByProperties);
    }

    @Test
    public void create_and_get_by_key_FormMapping() throws Exception {
        transactionService.begin();
        SFormMapping taskForm = formMappingService.create(p1.getId(), "step1", FormMappingType.TASK.getId(), "URL", "http://bit.coin");
        transactionService.complete();

        transactionService.begin();
        SFormMapping sFormMapping = formMappingService.get(taskForm.getPageMapping().getKey());
        SFormMapping sFormMappingByProperties = formMappingService.get(p1.getId(), FormMappingType.TASK.getId(), "step1");
        transactionService.complete();
        assertThat(sFormMapping).isEqualTo(taskForm).isEqualTo(sFormMappingByProperties);
    }

    @Test
    public void create_and_get_FormMapping_with_no_task() throws Exception {
        transactionService.begin();
        SFormMapping taskForm = formMappingService.create(p1.getId(), "task", FormMappingType.TASK.getId(), "URL", "http://bit.coin");
        transactionService.complete();

        transactionService.begin();
        SFormMapping sFormMapping = formMappingService.get(taskForm.getId());
        SFormMapping sFormMappingByProperties = formMappingService.get(p1.getId(), FormMappingType.TASK.getId());
        transactionService.complete();
        assertThat(sFormMapping).isEqualTo(taskForm);
        assertThat(sFormMappingByProperties).isEqualTo(sFormMapping);
    }

    @Test
    public void delete_FormMapping() throws Exception {
        transactionService.begin();
        SFormMapping taskForm = formMappingService.create(p1.getId(), "step1", FormMappingType.TASK.getId(), "URL", "http://bit.coin");
        transactionService.complete();

        transactionService.begin();
        formMappingService.delete(formMappingService.get(taskForm.getId()));
        transactionService.complete();

        transactionService.begin();
        try {
            formMappingService.get(taskForm.getId());
            fail("should have thrown a not found Exception");
        } catch (SObjectNotFoundException e) {
            //ok
        }
        transactionService.complete();
    }

    @Test
    public void update_FormMapping() throws Exception {
        transactionService.begin();
        SFormMapping taskForm = formMappingService.create(p1.getId(), "step1", FormMappingType.TASK.getId(), "URL", "http://bit.coin");
        transactionService.complete();

        transactionService.begin();
        SFormMapping sFormMapping = formMappingService.get(taskForm.getId());
        formMappingService.update(sFormMapping, "newFormName", null);
        transactionService.complete();

        transactionService.begin();
        SFormMapping updatedInDatabase = formMappingService.get(taskForm.getId());
        transactionService.complete();

        assertThat(sFormMapping).isEqualTo(updatedInDatabase);
        assertThat(updatedInDatabase.getPageMapping().getUrl()).isEqualTo("newFormName");
        assertThat(updatedInDatabase.getTarget()).isEqualTo(FormMappingTarget.URL.name());
        assertThat(updatedInDatabase.getLastUpdateDate()).isGreaterThan(taskForm.getLastUpdateDate());

        Thread.sleep(10);

        transactionService.begin();
        SFormMapping reupdated = formMappingService.get(taskForm.getId());
        formMappingService.update(reupdated, null, page.getId());
        transactionService.complete();

        assertThat(reupdated.getPageMapping().getUrl()).isNull();
        assertThat(reupdated.getPageMapping().getPageId()).isEqualTo(page.getId());
        assertThat(reupdated.getTarget()).isEqualTo(FormMappingTarget.INTERNAL.name());
        assertThat(reupdated.getLastUpdateDate()).isGreaterThan(updatedInDatabase.getLastUpdateDate());

    }
}
