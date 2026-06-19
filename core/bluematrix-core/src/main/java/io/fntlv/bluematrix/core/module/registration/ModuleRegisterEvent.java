package io.fntlv.bluematrix.core.module.registration;

import io.fntlv.bluematrix.core.event.ModuleEvent;
import io.fntlv.bluematrix.core.module.ModuleContext;
import io.fntlv.bluematrix.core.module.registration.ModuleCandidate;
import lombok.Getter;

@Getter
public abstract class ModuleRegisterEvent implements ModuleEvent {

    private ModuleRegisterEvent() {
    }

    @Getter
    public static final class Pre extends ModuleRegisterEvent {
        private final ModuleCandidate candidate;

        public Pre(ModuleCandidate candidate) {
            this.candidate = candidate;
        }
    }

    @Getter
    public static final class Post extends ModuleRegisterEvent {
        private final ModuleContext context;

        public Post(ModuleContext context) {
            this.context = context;
        }
    }
}
