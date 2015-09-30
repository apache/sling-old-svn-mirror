package org.apache.sling.ide.eclipse.ui.wizards.np;

import static org.junit.Assert.assertThat;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

public class ChooseArchetypeWizardPageTest {

    @Test
    public void comparatorForDifferentVersions() {
        
        String key1 = "org.apache.sling : template1 : 1.0.0";
        String key2 = "org.apache.sling : template1 : 1.0.2";
        
        int res = ChooseArchetypeWizardPage.ARTIFACT_KEY_COMPARATOR.compare(key1, key2);
        
        assertThat(res, CoreMatchers.equalTo(1));
    }
}
