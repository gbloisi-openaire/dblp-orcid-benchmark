package eu.openaire.dblp_benchmark;

import com.google.common.base.Joiner;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JaroWinklerOrderedToken {
    static public final Pattern SPLIT_REGEX = Pattern.compile("[\\s\\p{Punct}\\p{Pd}]+");

    static private List<String> tokenize(String s) {
        return Stream
                .of(SPLIT_REGEX.split(StringUtils.stripAccents(s).toLowerCase(Locale.ROOT)))
                .filter(x -> !x.isEmpty())
                .sorted()
                .collect(Collectors.toList());
    }

    public static double compare(String a1, String a2) {
        if (a1 == null || a2 == null) {
            return 0.0;
        }

        String ca = Joiner.on(' ').join(tokenize(a1));
        String cb = Joiner.on(' ').join(tokenize(a2));

        return new JaroWinklerSimilarity().apply(ca, cb);
    }
}
