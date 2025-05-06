package net.minecraft.network.chat;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import javax.annotation.Nullable;

public class LastSeenMessagesValidator {
    private final int lastSeenCount;
    private final ObjectList<LastSeenTrackedEntry> trackedMessages = new ObjectArrayList<>();
    @Nullable
    private MessageSignature lastPendingMessage;

    public LastSeenMessagesValidator(int pLastSeenCount) {
        this.lastSeenCount = pLastSeenCount;

        for (int i = 0; i < pLastSeenCount; i++) {
            this.trackedMessages.add(null);
        }
    }

    public void addPending(MessageSignature pSignature) {
        if (!pSignature.equals(this.lastPendingMessage)) {
            this.trackedMessages.add(new LastSeenTrackedEntry(pSignature, true));
            this.lastPendingMessage = pSignature;
        }
    }

    public int trackedMessagesCount() {
        return this.trackedMessages.size();
    }

    public void applyOffset(int pOffset) throws LastSeenMessagesValidator.ValidationException {
        int i = this.trackedMessages.size() - this.lastSeenCount;
        if (pOffset >= 0 && pOffset <= i) {
            this.trackedMessages.removeElements(0, pOffset);
        } else {
            throw new LastSeenMessagesValidator.ValidationException("Advanced last seen window by " + pOffset + " messages, but expected at most " + i);
        }
    }

    public LastSeenMessages applyUpdate(LastSeenMessages.Update pUpdate) throws LastSeenMessagesValidator.ValidationException {
        this.applyOffset(pUpdate.offset());
        ObjectList<MessageSignature> objectlist = new ObjectArrayList<>(pUpdate.acknowledged().cardinality());
        if (pUpdate.acknowledged().length() > this.lastSeenCount) {
            throw new LastSeenMessagesValidator.ValidationException(
                "Last seen update contained " + pUpdate.acknowledged().length() + " messages, but maximum window size is " + this.lastSeenCount
            );
        } else {
            for (int i = 0; i < this.lastSeenCount; i++) {
                boolean flag = pUpdate.acknowledged().get(i);
                LastSeenTrackedEntry lastseentrackedentry = this.trackedMessages.get(i);
                if (flag) {
                    if (lastseentrackedentry == null) {
                        throw new LastSeenMessagesValidator.ValidationException(
                            "Last seen update acknowledged unknown or previously ignored message at index " + i
                        );
                    }

                    this.trackedMessages.set(i, lastseentrackedentry.acknowledge());
                    objectlist.add(lastseentrackedentry.signature());
                } else {
                    if (lastseentrackedentry != null && !lastseentrackedentry.pending()) {
                        throw new LastSeenMessagesValidator.ValidationException(
                            "Last seen update ignored previously acknowledged message at index " + i + " and signature " + lastseentrackedentry.signature()
                        );
                    }

                    this.trackedMessages.set(i, null);
                }
            }

            LastSeenMessages lastseenmessages = new LastSeenMessages(objectlist);
            if (!pUpdate.verifyChecksum(lastseenmessages)) {
                throw new LastSeenMessagesValidator.ValidationException("Checksum mismatch on last seen update: the client and server must have desynced");
            } else {
                return lastseenmessages;
            }
        }
    }

    public static class ValidationException extends Exception {
        public ValidationException(String pMessage) {
            super(pMessage);
        }
    }
}