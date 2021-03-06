// -*- tab-width: 4 -*-
package edu.nyu.jet.ne;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import cc.mallet.types.Instance;
import cc.mallet.types.Token;
import cc.mallet.types.TokenSequence;
import edu.nyu.jet.lisp.FeatureSet;
import edu.nyu.jet.tipster.Annotation;
import edu.nyu.jet.tipster.Document;
import edu.nyu.jet.tipster.Span;

public class NamedEntityInDictionaryFeature extends cc.mallet.pipe.Pipe {
	private String prefix;

	public NamedEntityInDictionaryFeature(String prefix) {
		this.prefix = prefix;
	}

	@Override
	public Instance pipe(Instance carrier) {
		TokenSequence tokens = (TokenSequence) carrier.getData();
		Dictionary dict = (Dictionary) carrier.getProperty("dictionary");

		if (dict == null) {
			// if dictionary is not present, do nothing
			return carrier;
		}

		Document doc = (Document) carrier.getProperty("document");
		Span span = (Span) carrier.getProperty("span");
		DictionaryTagger tagger = new DictionaryTagger();
		tagger.setDictionary(dict);

		Logger.global.info(Integer.toString(tokens.size()));
		annotateNETokens(doc, tokens);

		tagger.annotate(doc, span);

		List<Annotation> neTokens = doc.annotationsOfType("NE_INTERNAL", span);

		assert tokens.size() == neTokens.size() : tokens.size() + " != " + neTokens.size();

		for (int i = 0; i < neTokens.size(); i++) {
			Token token = tokens.get(i);
			Annotation neToken = neTokens.get(i);
			Set<NamedEntityAttribute> categories = (Set<NamedEntityAttribute>) neToken
					.get("categories");

			for (NamedEntityAttribute attr : categories) {
				String type = attr.toString();
				String name = (prefix + type).intern();
				token.setFeatureValue(name, 1.0);
			}
		}

		doc.removeAnnotationsOfType("NE_INTERNAL");

		return carrier;
	}

	private void annotateNETokens(Document doc, TokenSequence tokens) {
		for (int i = 0; i < tokens.size(); i++) {
			Token token = tokens.get(i);
			Span span = (Span) token.getProperty("span");
			Set<NamedEntityAttribute> categories = new HashSet<NamedEntityAttribute>();
			FeatureSet fs = new FeatureSet();
			fs.put("categories", categories);

			doc.annotate("NE_INTERNAL", span, fs);
		}
	}
}
