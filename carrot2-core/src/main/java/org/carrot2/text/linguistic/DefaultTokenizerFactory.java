
/*
 * Carrot2 project.
 *
 * Copyright (C) 2002-2019, Dawid Weiss, Stanisław Osiński.
 * All rights reserved.
 *
 * Refer to the full license file "carrot2.LICENSE"
 * in the root folder of the repository checkout or at:
 * http://www.carrot2.org/carrot2.LICENSE
 */

package org.carrot2.text.linguistic;

import java.io.IOException;
import java.io.StringReader;
import java.util.EnumMap;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.carrot2.core.LanguageCode;
import org.carrot2.text.analysis.ExtendedWhitespaceTokenizer;
import org.carrot2.text.analysis.ITokenizer;
import org.carrot2.text.linguistic.lucene.ChineseTokenizerAdapter;
import org.carrot2.text.linguistic.lucene.ThaiTokenizerAdapter;
import org.carrot2.util.annotations.ThreadSafe;
import org.carrot2.util.attribute.Bindable;
import org.carrot2.util.factory.FallbackFactory;
import org.carrot2.util.factory.NewClassInstanceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Bindable
@ThreadSafe
public class DefaultTokenizerFactory
{
    private final static Logger logger = LoggerFactory
        .getLogger(DefaultTokenizerFactory.class);

    private final static EnumMap<LanguageCode, Supplier<ITokenizer>> tokenizerFactories;

    /**
     * Functional verification for {@link ITokenizer}.
     */
    private final static Predicate<ITokenizer> tokenizerVerifier = (tokenizer) -> {
        // Assume functional if there's no exception.
        try
        {
            tokenizer.reset(new StringReader("verify"));
            tokenizer.nextToken();
            return true;
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    };

    /**
     * Initialize factories.
     */
    static
    {
        tokenizerFactories = createDefaultTokenizers();
    }

    public ITokenizer getTokenizer(LanguageCode languageCode)
    {
        return tokenizerFactories.get(languageCode).get();
    }

    /**
     * Create default tokenizer factories.
     */
    private static EnumMap<LanguageCode, Supplier<ITokenizer>> createDefaultTokenizers()
    {
        EnumMap<LanguageCode, Supplier<ITokenizer>> map = new EnumMap<>(LanguageCode.class);

        // By default, we use our own tokenizer for all languages.
        Supplier<ITokenizer> whitespaceTokenizerFactory = new NewClassInstanceFactory<ITokenizer>(
            ExtendedWhitespaceTokenizer.class);

        for (LanguageCode lc : LanguageCode.values())
        {
            map.put(lc, whitespaceTokenizerFactory);
        }

        // Chinese and Thai are exceptions, we use adapters around tokenizers from Lucene.
        map.put(LanguageCode.CHINESE_SIMPLIFIED, 
            new NewClassInstanceFactory<ITokenizer>(ChineseTokenizerAdapter.class));

        map.put(LanguageCode.THAI, 
            new NewClassInstanceFactory<ITokenizer>(ThaiTokenizerAdapter.class));
        
        // Japanese is currently not supported. TODO: CARROT-903
        map.put(LanguageCode.JAPANESE, () -> { throw new RuntimeException("Unsupported."); });

        // Decorate everything with a fallback tokenizer.
        for (LanguageCode lc : LanguageCode.values())
        {
            if (map.containsKey(lc))
            {
                Supplier<ITokenizer> factory = map.get(lc);
                if (factory != whitespaceTokenizerFactory)
                {
                    map.put(lc, new FallbackFactory<>(factory,
                        whitespaceTokenizerFactory, tokenizerVerifier, logger,
                        "Tokenizer for " + lc.toString() + " (" + lc.getIsoCode()
                            + ") is not available."
                            + " This may degrade clustering quality of " + lc.toString()
                            + " content. Cause: {}"));
                }
            }
            else
            {
                map.put(lc, whitespaceTokenizerFactory);
            }
        }

        return map;
    }
}