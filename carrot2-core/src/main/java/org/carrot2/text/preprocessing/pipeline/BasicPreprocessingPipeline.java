
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

package org.carrot2.text.preprocessing.pipeline;

import org.carrot2.core.Document;
import org.carrot2.text.linguistic.LanguageModel;
import org.carrot2.text.preprocessing.*;
import org.carrot2.util.attribute.Bindable;

import java.util.List;

/**
 * Performs basic preprocessing steps on the provided documents. The preprocessing
 * consists of the following steps:
 * <ol>
 * <li>{@link Tokenizer}</li>
 * <li>{@link CaseNormalizer}</li>
 * <li>{@link LanguageModelStemmer}</li>
 * <li>{@link StopListMarker}</li>
 * </ol>
 */
@Bindable(prefix = "PreprocessingPipeline")
public class BasicPreprocessingPipeline implements IPreprocessingPipeline
{
    /**
     * Tokenizer used by the algorithm, contains bindable attributes.
     */
    public final Tokenizer tokenizer = new Tokenizer();

    /**
     * Case normalizer used by the algorithm, contains bindable attributes.
     */
    public final CaseNormalizer caseNormalizer = new CaseNormalizer();

    /**
     * Stemmer used by the algorithm, contains bindable attributes.
     */
    public final LanguageModelStemmer languageModelStemmer = new LanguageModelStemmer();

    /**
     * Stop list marker used by the algorithm, contains bindable attributes.
     */
    public final StopListMarker stopListMarker = new StopListMarker();

    /**
     * Performs preprocessing on the provided list of documents. Results can be obtained
     * from the returned {@link PreprocessingContext}.
     */
    @Override
    public PreprocessingContext preprocess(List<Document> documents, String query, LanguageModel langModel)
    {
        try (PreprocessingContext context = new PreprocessingContext(langModel)) {
            tokenizer.tokenize(context, documents.iterator());
            caseNormalizer.normalize(context);
            languageModelStemmer.stem(context, query);
            stopListMarker.mark(context);
            return context;
        }
    }
}