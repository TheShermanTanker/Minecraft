package net.minecraft.network.chat;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public class ChatClickable {
    private final ChatClickable.EnumClickAction action;
    private final String value;

    public ChatClickable(ChatClickable.EnumClickAction action, String value) {
        this.action = action;
        this.value = value;
    }

    public ChatClickable.EnumClickAction getAction() {
        return this.action;
    }

    public String getValue() {
        return this.value;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (object != null && this.getClass() == object.getClass()) {
            ChatClickable clickEvent = (ChatClickable)object;
            if (this.action != clickEvent.action) {
                return false;
            } else {
                if (this.value != null) {
                    if (!this.value.equals(clickEvent.value)) {
                        return false;
                    }
                } else if (clickEvent.value != null) {
                    return false;
                }

                return true;
            }
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return "ClickEvent{action=" + this.action + ", value='" + this.value + "'}";
    }

    @Override
    public int hashCode() {
        int i = this.action.hashCode();
        return 31 * i + (this.value != null ? this.value.hashCode() : 0);
    }

    public static enum EnumClickAction {
        OPEN_URL("open_url", true),
        OPEN_FILE("open_file", false),
        RUN_COMMAND("run_command", true),
        SUGGEST_COMMAND("suggest_command", true),
        CHANGE_PAGE("change_page", true),
        COPY_TO_CLIPBOARD("copy_to_clipboard", true);

        private static final Map<String, ChatClickable.EnumClickAction> LOOKUP = Arrays.stream(values()).collect(Collectors.toMap(ChatClickable.EnumClickAction::getName, (a) -> {
            return a;
        }));
        private final boolean allowFromServer;
        private final String name;

        private EnumClickAction(String name, boolean userDefinable) {
            this.name = name;
            this.allowFromServer = userDefinable;
        }

        public boolean isAllowedFromServer() {
            return this.allowFromServer;
        }

        public String getName() {
            return this.name;
        }

        public static ChatClickable.EnumClickAction getByName(String name) {
            return LOOKUP.get(name);
        }
    }
}
