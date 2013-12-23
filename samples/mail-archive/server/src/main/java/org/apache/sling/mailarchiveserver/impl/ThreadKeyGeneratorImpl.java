package org.apache.sling.mailarchiveserver.impl;

import static java.lang.Character.isLetterOrDigit;
import static org.apache.sling.mailarchiveserver.impl.MessageStoreImpl.makeJcrFriendly;
import static org.apache.sling.mailarchiveserver.impl.MessageStoreImpl.removeRe;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.james.mime4j.dom.Message;
import org.apache.sling.mailarchiveserver.api.ThreadKeyGenerator;

@Component
@Service(ThreadKeyGenerator.class)
public class ThreadKeyGeneratorImpl implements ThreadKeyGenerator {

    /**
     * These constants are gotten by sampling around 1500 messages 
     * from some open source projects using SubjectLettersEntropy class.
     * 
     * Entropy of each letter position of the subject was calculated. And two letter positions 
     * with biggest entropy and occurrence in at least 90% of the messages were chosen.
     * 
     * In order to {@link #getThreadKey(Message)} works correctly, following invariant should hold: LETTER_POS_WITH_BIGGEST_ENTROPY < LETTER_POS_WITH_2ND_BIGGEST_ENTROPY
     */
    private static final int LETTER_POS_WITH_BIGGEST_ENTROPY = 9;
    private static final int LETTER_POS_WITH_2ND_BIGGEST_ENTROPY = 40;
    private static final String UNADDRESSABLE_SUBJECT = "unaddressable subject";

    public String getThreadKey(String subject) {
        String wordCharsSubj;
        String noReSubj;
        if (subject != null) {
            noReSubj = removeRe(subject);
            wordCharsSubj = noReSubj.replaceAll("\\W", "_");
            if (!isAddressable(wordCharsSubj)) {
                noReSubj = wordCharsSubj = UNADDRESSABLE_SUBJECT;
            }
        } else {
            noReSubj = wordCharsSubj = UNADDRESSABLE_SUBJECT;
        }

        char prefix1;
        char prefix2;
        prefix1 = assignPrefix(wordCharsSubj, LETTER_POS_WITH_BIGGEST_ENTROPY);
        prefix2 = assignPrefix(wordCharsSubj, LETTER_POS_WITH_2ND_BIGGEST_ENTROPY);
        return ""+prefix1+"/"+prefix1+prefix2+"/"+ makeJcrFriendly(noReSubj);
    }

    private static boolean isAddressable(String subject) {
        for (char c : subject.toCharArray()) {
            if (isLetterOrDigit(c)) {
                return true;
            }
        }
        return false;
    }

    private static char assignPrefix(String subject, int length) {
        char prefix;
        if (subject.length() > length) {

            int i = length;
            while (i > -1 && !isLetterOrDigit(subject.charAt(i))) 
                i--;
            if (i > -1) 
                prefix = subject.charAt(i);
            else {
                i = length;
                while (i<subject.length() && !isLetterOrDigit(subject.charAt(i))) 
                    i++;
                if (i<subject.length()) 
                    prefix = subject.charAt(i);
                else 
                    throw new IllegalArgumentException();
            }

        } else {

            int i = subject.length()-1;
            while (i > -1 && !isLetterOrDigit(subject.charAt(i))) 
                i--;
            if (i > -1) 
                prefix = subject.charAt(i);
            else 
                throw new IllegalArgumentException();

        }

        return prefix;
    }

}
