package io.fntlv.bluematrix.core.module.registration;

import io.fntlv.bluematrix.core.module.Module;
import io.fntlv.bluematrix.core.module.ModuleInfo;
import io.fntlv.bluematrix.core.module.registration.issue.ModuleRegistrationIssueType;
import io.fntlv.bluematrix.core.module.registration.issue.issues.DuplicateModuleIdIssue;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModuleCandidateAdmissionTest {

    private final ModuleCandidateAdmission admission = new ModuleCandidateAdmission();

    @Test
    void emptyCandidatesReturnEmptyResult() {
        ModuleRegistrationStageResult<ModuleCandidate> result = admission.admit(Collections.emptyList());

        assertTrue(result.passed().isEmpty());
        assertTrue(result.issues().isEmpty());
    }

    @Test
    void uniqueCandidatesPassInOriginalOrder() {
        ModuleCandidate first = candidate(FirstModule.class);
        ModuleCandidate second = candidate(SecondModule.class);

        ModuleRegistrationStageResult<ModuleCandidate> result = admission.admit(Arrays.asList(first, second));

        assertEquals(2, result.passed().size());
        assertSame(first, result.passed().get(0));
        assertSame(second, result.passed().get(1));
        assertTrue(result.issues().isEmpty());
    }

    @Test
    void duplicateCandidatesBecomeIssues() {
        ModuleCandidate first = candidate(FirstDuplicateModule.class);
        ModuleCandidate second = candidate(SecondDuplicateModule.class);

        ModuleRegistrationStageResult<ModuleCandidate> result = admission.admit(Arrays.asList(first, second));

        assertTrue(result.passed().isEmpty());
        assertEquals(2, result.issues().size());
        assertDuplicateIssue(result.issues().all().get(0), "duplicate");
        assertDuplicateIssue(result.issues().all().get(1), "duplicate");
    }

    @Test
    void duplicateCandidatesDoNotRemoveUniqueCandidates() {
        ModuleCandidate first = candidate(FirstModule.class);
        ModuleCandidate firstDuplicate = candidate(FirstDuplicateModule.class);
        ModuleCandidate second = candidate(SecondModule.class);
        ModuleCandidate secondDuplicate = candidate(SecondDuplicateModule.class);

        ModuleRegistrationStageResult<ModuleCandidate> result = admission.admit(Arrays.asList(
                first,
                firstDuplicate,
                second,
                secondDuplicate
        ));

        assertEquals(2, result.passed().size());
        assertSame(first, result.passed().get(0));
        assertSame(second, result.passed().get(1));
        assertEquals(2, result.issues().size());
    }

    @Test
    void multipleDuplicateGroupsAreRejected() {
        ModuleCandidate firstDuplicate = candidate(FirstDuplicateModule.class);
        ModuleCandidate secondDuplicate = candidate(SecondDuplicateModule.class);
        ModuleCandidate firstOtherDuplicate = candidate(FirstOtherDuplicateModule.class);
        ModuleCandidate secondOtherDuplicate = candidate(SecondOtherDuplicateModule.class);

        ModuleRegistrationStageResult<ModuleCandidate> result = admission.admit(Arrays.asList(
                firstDuplicate,
                firstOtherDuplicate,
                secondDuplicate,
                secondOtherDuplicate
        ));

        assertTrue(result.passed().isEmpty());
        assertEquals(4, result.issues().size());
        assertDuplicateIssue(result.issues().all().get(0), "duplicate");
        assertDuplicateIssue(result.issues().all().get(1), "other-duplicate");
        assertDuplicateIssue(result.issues().all().get(2), "duplicate");
        assertDuplicateIssue(result.issues().all().get(3), "other-duplicate");
    }

    private static ModuleCandidate candidate(Class<? extends Module> moduleClass) {
        return new ModuleCandidate(moduleClass, moduleClass.getAnnotation(ModuleInfo.class));
    }

    private static void assertDuplicateIssue(Object issue, String moduleId) {
        DuplicateModuleIdIssue duplicateIssue = assertInstanceOf(DuplicateModuleIdIssue.class, issue);
        assertEquals(ModuleRegistrationIssueType.DUPLICATE_MODULE_ID, duplicateIssue.type());
        assertEquals(moduleId, duplicateIssue.moduleId());
        assertEquals(moduleId, duplicateIssue.duplicatedModuleId());
        assertTrue(duplicateIssue.moduleClassName().contains("DuplicateModule"));
    }

    @ModuleInfo(id = "first", name = "First")
    private static class FirstModule implements Module {
        @Override
        public void onLoad() {
        }

        @Override
        public void onEnable() {
        }

        @Override
        public void onDisable() {
        }
    }

    @ModuleInfo(id = "second", name = "Second")
    private static class SecondModule implements Module {
        @Override
        public void onLoad() {
        }

        @Override
        public void onEnable() {
        }

        @Override
        public void onDisable() {
        }
    }

    @ModuleInfo(id = "duplicate", name = "First Duplicate")
    private static class FirstDuplicateModule implements Module {
        @Override
        public void onLoad() {
        }

        @Override
        public void onEnable() {
        }

        @Override
        public void onDisable() {
        }
    }

    @ModuleInfo(id = "duplicate", name = "Second Duplicate")
    private static class SecondDuplicateModule implements Module {
        @Override
        public void onLoad() {
        }

        @Override
        public void onEnable() {
        }

        @Override
        public void onDisable() {
        }
    }

    @ModuleInfo(id = "other-duplicate", name = "First Other Duplicate")
    private static class FirstOtherDuplicateModule implements Module {
        @Override
        public void onLoad() {
        }

        @Override
        public void onEnable() {
        }

        @Override
        public void onDisable() {
        }
    }

    @ModuleInfo(id = "other-duplicate", name = "Second Other Duplicate")
    private static class SecondOtherDuplicateModule implements Module {
        @Override
        public void onLoad() {
        }

        @Override
        public void onEnable() {
        }

        @Override
        public void onDisable() {
        }
    }
}
