// -*- tab-width: 4 -*-
// Title:         JetLite
// Version:       1.00
// Copyright (c): 2017
// Author:        Ralph Grishman
// Description:   A lightweight Java-based Information Extraction Tool

//package edu.nyu.jetlite;

import edu.nyu.jetlite.tipster.*;
import java.io.*;
import java.util.*;
import edu.nyu.jet.aceJet.*;
import opennlp.maxent.*;
import opennlp.maxent.io.*;
import opennlp.model.*;

/**
 *  Assigns semantic type information to entities.
 *  <p>
 *  In training the classifier, we do not have an explicit list of all entities,
 *  only those assigned an ACE type.  We approximate this list by using the
 *  output of the pipeline through the coref stage.
 */

public class EntityTagger extends edu.nyu.jetlite.tipster.Annotator {

    String modelFileName;

    GISModel model;

    public EntityTagger (Properties config) throws IOException {
	modelFileName = config.getProperty("EntityTagger.model.fileName");
    }

    /**
     *  Command-line-callable method for training and evaluating an entity tagger.
     *  <p>
     *  Takes 4 arguments:  training  test  documents  model <br>
     *  where  <br>
     *  training = file containing list of training documents  <br>
     *  test = file containing list of test documents  <br>
     *  documents = directory containing document files <br>
     *  model = file containing max ent model
     */

    public static void main (String[] args) throws IOException {
	if (args.length != 4) {
	    System.out.println ("Error, 4 arguments required:");
	    System.out.println ("   listOfTrainingDocs listOfTestDocs documentDirectory modelFileName");
	    System.exit(1);
	}
	String trainDocListFileName = args[0];
	String testDocListFileName = args[1];
	String docDir = args[2];
	String mfn = args[3];
	Properties p = new Properties();
	p.setProperty("EntityTagger.model.fileName", mfn);
	EntityTagger etagger = new EntityTagger(p);
	etagger.trainTagger(docDir, trainDocListFileName);
	etagger.evaluate(docDir, testDocListFileName);
    }

    /**
     *  Train the entity type tagger.
     *
     *  @param  docDir           directory containing training documents
     *  @param  docListFileName  file containing list of training documents
     */

    public void trainTagger (String docDir, String docListFileName) throws IOException {
	BufferedReader docListReader = new BufferedReader (new FileReader (docListFileName));
	PrintWriter eventWriter = new PrintWriter (new FileWriter ("events"));
        int docCount = 0;
	String line; 
	while ((line = docListReader.readLine()) != null) {
	    learnFromDocument (docDir + "/" + line.trim(), eventWriter);
            docCount++;
            if (docCount % 5 == 0) System.out.print(".");
        }
        eventWriter.close();
	MaxEnt.buildModel(modelFileName);
    }

    void learnFromDocument (String docFileName, PrintWriter eventWriter) throws IOException {
	File docFile = new File(docFileName);
	Document doc = new Document(docFile);
	doc.setText(eraseXML(doc.text()));
	String apfFileName = docFileName.replace("sgm" , "apf.xml");
	AceDocument aceDoc = new AceDocument(docFileName, apfFileName);
	// --- split and pos tag
	Properties config = new Properties();
	config.setProperty("POStagger.model.fileName", "POSmodel");
	config.setProperty("NEtagger.model.fileName", "NEmodel");
	config.setProperty("annotators", "token sentence pos name");
	doc = Hub.processDocument(doc, config);
	// 
	findEntityMentions (aceDoc);
	// loop over tokens 
	Span span = Hub.getTEXTspan(doc);
	int posn = span.start();
	posn = doc.skipWhitespace(posn, span.end());
	//int nextPos = span.start() + 1;


	String prevToken = "";
	String prevPOS = "";

	while (posn < span.end()) {
	    Annotation tokenAnnotation = doc.tokenAt(posn);

		Annotation nextAnn = doc.tokenAt(span.end()+1);
		if (tokenAnnotation == null)
		return;
		String [] splitTokenAnn = tokenAnnotation.toString().split("\\s+");

	    String tokenText = doc.normalizedText(tokenAnnotation);
	    Datum d = entityFeatures(tokenText);
        AceEntityMention mention = mentionMap.get(posn);
	    String type = (mention == null) ? "other" : mention.entity.type;
		String nextToken = "";
//		if (nextAnn != null){
//			nextToken = "nextToken=" + doc.normalizedText(nextAnn);
//			d.addF(nextToken);
//		}

		if (mention != null) {
			// experiment with trigram?? 1st and 2nd token need identifiers that differ from 3rd.

			//String bigram = "bigram"+prevToken+tokenText;
			//d.addF(bigram);
			String prev = "prev=" + prevToken;
			d.addF(prev);
			String isCapped = "false";
			int caps = 0;
			for (int idx = 0; idx < tokenText.length(); idx++) {
				if (idx == 0 && Character.isUpperCase(tokenText.charAt(idx))) {
					isCapped = "true";
				}
				if (Character.isUpperCase(tokenText.charAt(idx))) {
					caps++;
				}
			}
			String capped = "isCap=" + isCapped;
			d.addF(capped);
			d.addF("caps=" + Integer.toString(caps));
			String tokenLength = "length=" + Integer.toString(tokenText.length());
			d.addF(tokenLength);

			// get POS

			String POStag = splitTokenAnn[splitTokenAnn.length - 1];
			d.addF(POStag);
			//d.addF(prevPOS);

			//analyze prevToken??



		}

		// tokenAnnotation.pos as a pos feature?
//		if (mention != null) {
//			String subtype = "sub=" + mention.entity.subtype;
//			d.addF(subtype);
//			String entClassType = "eClassType=" + mention.entity.entClass;
//			d.addF(entClassType);
//			String headText = "";
//			String menType = "";
//			for (int idx = 0; idx < mention.entity.mentions.size(); idx ++) {
//				headText += mention.entity.mentions.get(idx).headText;
//				menType += mention.entity.mentions.get(idx).type;
//			}
//			d.addF(headText);
//			d.addF(menType);
//		}


	    d.setOutcome(type);
	    eventWriter.println(d);
	    posn = tokenAnnotation.end();
		prevToken = tokenText;
		prevPOS = splitTokenAnn[splitTokenAnn.length - 1];
	}
    }

    static Map<Integer, AceEntityMention> mentionMap;

    static void findEntityMentions (AceDocument aceDoc) {
	mentionMap = new HashMap<Integer, AceEntityMention>();
	ArrayList entities = aceDoc.entities;
	for (int i=0; i<entities.size(); i++) {
	    AceEntity entity = (AceEntity) entities.get(i);
	    String type = entity.type;
	    String subtype = entity.subtype;
	    ArrayList mentions = entity.mentions;
	    for (int j=0; j<mentions.size(); j++) {
		AceEntityMention mention = (AceEntityMention) mentions.get(j);
		mentionMap.put(mention.jetHead.start(), mention);
	    }
	}
    }

    static Datum entityFeatures (String word) {
	Datum d = new Datum();
	d.addF(word);
	return d;
    }

    /**
     *  Removes all XML tags from a String.
     *  <p>
     *  In computing character offsets within a Document, Jet counts all characters.
     *  ACE does not count characters in XML tags.  To make the offsets compatible, we
     *  delete all XML tags from ACE training documents using eraseXML
     *
     *  @param  fileTextWithXML  the original ocument text
     *
     *  @return  the text with all XML tags removed
     */

    static String eraseXML (String fileTextWithXML) {
	boolean inTag = false;
	int length = fileTextWithXML.length();
	StringBuffer fileText = new StringBuffer();
	for (int i=0; i<length; i++) {
	    char c = fileTextWithXML.charAt(i);
	    if(c == '<') inTag = true;
	    if (!inTag) fileText.append(c);
	    if(c == '>') inTag = false;
	}
	return fileText.toString();
    }
    
    static int correctEntities;
    static int responseEntities;
    static int keyEntities;

	// stats for ORGANIZATION predictions

	static ArrayList<Integer> orgPred = new ArrayList<Integer>();
	static ArrayList<Integer> perPred = new ArrayList<Integer>();
	static ArrayList<Integer> facPred = new ArrayList<Integer>();
	static ArrayList<Integer> gpePred = new ArrayList<Integer>();
	static ArrayList<Integer> weaPred = new ArrayList<Integer>();
	static ArrayList<Integer> locPred = new ArrayList<Integer>();
	static ArrayList<Integer> vehPred = new ArrayList<Integer>();
	static ArrayList<Integer> otherPred = new ArrayList<Integer>();


	/**
     *  Evaluate the performance of the entity tagger.
     *
     *  @param  /docdir               the directory containing the test documents
     *  @param  testDocListFileName  the file containing a list of the test documents, one per line
     */

    void evaluate (String docDir, String testDocListFileName) throws IOException {
	correctEntities = 0;
	responseEntities = 0;
	keyEntities = 0;

		// index 0 = correct; index 1 = response; index 2 = key

		orgPred.add(0,0);
		orgPred.add(1,0);
		orgPred.add(2,0);

		perPred.add(0,0);
		perPred.add(1,0);
		perPred.add(2,0);

		facPred.add(0,0);
		facPred.add(1,0);
		facPred.add(2,0);

		gpePred.add(0,0);
		gpePred.add(1,0);
		gpePred.add(2,0);

		weaPred.add(0,0);
		weaPred.add(1,0);
		weaPred.add(2,0);

		locPred.add(0,0);
		locPred.add(1,0);
		locPred.add(2,0);

		vehPred.add(0,0);
		vehPred.add(1,0);
		vehPred.add(2,0);

		otherPred.add(0,0);
		otherPred.add(1,0);
		otherPred.add(2,0);


	model = MaxEnt.loadModel(modelFileName, "EntityTagger");
	BufferedReader docListReader = new BufferedReader (new FileReader (testDocListFileName));
	String line; 
	while ((line = docListReader.readLine()) != null)
	    evaluateOnDocument (docDir + "/" + line.trim());
	float recall = 100.0f * correctEntities / keyEntities;
	float precision = 100.0f * correctEntities / responseEntities;
	System.out.println ("correct: " + correctEntities + "   response: " + responseEntities
		+ "   key: " + keyEntities);
	System.out.println ("precision: " + precision + "   recall: " + recall);
	System.out.println();
		float orgRecall = 100.0f * orgPred.get(0) / orgPred.get(2);
		float orgPrec = 100.0f * orgPred.get(0) / orgPred.get(1);
		System.out.println("org correct: " + orgPred.get(0) + " org response: " + orgPred.get(1) + " org key: " + orgPred.get(2));
		System.out.println("org precision: " + orgPrec + " recall: " + orgRecall);

		// calculate person stats

		System.out.println();
		float perRecall = 100.0f * perPred.get(0) / perPred.get(2);
		float perPrec = 100.0f * perPred.get(0) / perPred.get(1);
		System.out.println("person correct: " + perPred.get(0) + " person response: " + perPred.get(1) + " person key: " + perPred.get(2));
		System.out.println("person precision: " + perPrec + " recall: " + perRecall);

		// calculate facility stats

		System.out.println();
		float facRecall = 100.0f * facPred.get(0) / facPred.get(2);
		float facPrec = 100.0f * facPred.get(0) / facPred.get(1);
		System.out.println("facility correct: " + facPred.get(0) + " response: " + facPred.get(1) + " key: " + facPred.get(2));
		System.out.println("facility precision: " + facPrec + " recall: " + facRecall);

		// calculate GPE stats

		System.out.println();
		float gpeRecall = 100.0f * gpePred.get(0) / gpePred.get(2);
		float gpePrec = 100.0f * gpePred.get(0) / gpePred.get(1);
		System.out.println("GPE correct: " + gpePred.get(0) + " response: " + gpePred.get(1) + " key: " + gpePred.get(2));
		System.out.println("GPE precision: " + gpePrec + " recall: " + gpeRecall);

		// calculate WEA stats

		System.out.println();
		float weaRecall = 100.0f * weaPred.get(0) / weaPred.get(2);
		float weaPrec = 100.0f * weaPred.get(0) / weaPred.get(1);
		System.out.println("Wea correct: " + weaPred.get(0) + " response: " + weaPred.get(1) + " key: " + weaPred.get(2));
		System.out.println("Wea precision: " + weaPrec + " recall: " + weaRecall);

		// calculate LOCATION stats

		System.out.println();
		float locRecall = 100.0f * locPred.get(0) / locPred.get(2);
		float locPrec = 100.0f * locPred.get(0) / locPred.get(1);
		System.out.println("Location correct: " + locPred.get(0) + " response: " + locPred.get(1) + " key: " + locPred.get(2));
		System.out.println("Location precision: " + locPrec + " recall: " + locRecall);

		// calculate VEH stats

		System.out.println();
		float vehRecall = 100.0f * vehPred.get(0) / vehPred.get(2);
		float vehPrec = 100.0f * vehPred.get(0) / vehPred.get(1);
		System.out.println("VEH correct: " + vehPred.get(0) + " response: " + vehPred.get(1) + " key: " + vehPred.get(2));
		System.out.println("VEH precision: " + vehPrec + " recall: " + vehRecall);

		// calculate OTHER stats

		System.out.println();
		float othRecall = 100.0f * otherPred.get(0) / otherPred.get(2);
		float othPrec = 100.0f * otherPred.get(0) / otherPred.get(1);
		System.out.println("Other correct: " + otherPred.get(0) + " response: " + otherPred.get(1) + " key: " + otherPred.get(2));
		System.out.println("Other precision: " + othPrec + " recall: " + othRecall);

    }

    void evaluateOnDocument (String docFileName) throws IOException {
	File docFile = new File(docFileName);
	Document doc = new Document(docFile);
	doc.setText(eraseXML(doc.text()));
	String apfFileName = docFileName.replace("sgm" , "apf.xml");
	AceDocument aceDoc = new AceDocument(docFileName, apfFileName);
	// --- split and pos tag
	Properties config = new Properties();
	config.setProperty("POStagger.model.fileName", "POSmodel");
	config.setProperty("NEtagger.model.fileName", "NEmodel");
	config.setProperty("annotators", "token sentence pos name");
	doc = Hub.processDocument(doc, config);
	// 
	findEntityMentions (aceDoc);
	// loop over tokens 
	Span span = Hub.getTEXTspan(doc);
	int posn = span.start();
	posn = doc.skipWhitespace(posn, span.end());

	String prevToken = "";
	while (posn < span.end()) {
	    Annotation tokenAnnotation = doc.tokenAt(posn);
	    if (tokenAnnotation == null)
		return;
		String [] splitTokenAnn = tokenAnnotation.toString().split("\\s+");
	    String tokenText = doc.normalizedText(tokenAnnotation);
	    Datum d = entityFeatures(tokenText);
	    AceEntityMention mention = mentionMap.get(posn);
	    String type = (mention == null) ? "other" : mention.entity.type;


		if (mention != null) {
			//String bigram = "bigram="+prevToken+tokenText;
			//d.addF(bigram);
			String prev = "prev=" + prevToken;
			d.addF(prev);

			String isCapped = "false";
			int caps = 0;
			for (int idx = 0; idx < tokenText.length(); idx++) {
				if (idx == 0 && Character.isUpperCase(tokenText.charAt(idx))) {
					isCapped = "true";
				}
				if (Character.isUpperCase(tokenText.charAt(idx))) {
					caps++;
				}
			}
			String capped = "isCap=" + isCapped;
			d.addF(capped);
			d.addF("caps=" + Integer.toString(caps));
			String tokenLength = "length=" + Integer.toString(tokenText.length());
			d.addF(tokenLength);

			String POStag = splitTokenAnn[splitTokenAnn.length - 1];
			d.addF(POStag);


		}
//		if (mention != null) {
//			String subtype = "sub=" + mention.entity.subtype;
//			d.addF(subtype);
//			String entClassType = "eClassType=" + mention.entity.entClass;
//			d.addF(entClassType);
//			String headText = "";
//			String menType = "";
//			for (int idx = 0; idx < mention.entity.mentions.size(); idx ++) {
//				headText += mention.entity.mentions.get(idx).headText;
//				menType += mention.entity.mentions.get(idx).type;
//			}
//			d.addF(headText);
//			d.addF(menType);
//		}

		prevToken = tokenText;

	    String prediction = model.getBestOutcome(model.eval(d.toArray()));
		//System.out.println(prediction);
//		if (!prediction.equals("other")){
//			System.out.println(prediction);
//		}
	    if (prediction.equals(type) && !prediction.equals("other"))
		correctEntities++;
	    if ( !prediction.equals("other"))
		responseEntities ++;
	    if ( !type.equals("other"))
		keyEntities++;
	    posn = tokenAnnotation.end();

		// now stats for individual categories

		if (prediction.equals(type) && !prediction.equals("other") && prediction.equals("ORGANIZATION")) {

			int corCount = orgPred.get(0) + 1;
			orgPred.remove(0);
			orgPred.add(0,corCount);
		}

		if (prediction.equals("ORGANIZATION")) {
			int resCount = orgPred.get(1) + 1;
			orgPred.remove(1);
			orgPred.add(1,resCount);
		}

		if (type.equals("ORGANIZATION")) {
			int keyCount = orgPred.get(2) + 1;
			orgPred.remove(2);
			orgPred.add(2,keyCount);
		}

		// for PERSON

		if (prediction.equals(type) && !prediction.equals("other") && prediction.equals("PERSON")) {

			int corCount = perPred.get(0) + 1;
			perPred.remove(0);
			perPred.add(0,corCount);
		}

		if (prediction.equals("PERSON")) {
			int resCount = perPred.get(1) + 1;
			perPred.remove(1);
			perPred.add(1,resCount);
		}

		if (type.equals("PERSON")) {
			int keyCount = perPred.get(2) + 1;
			perPred.remove(2);
			perPred.add(2,keyCount);
		}

		// for FACILITY

		if (prediction.equals(type) && !prediction.equals("other") && prediction.equals("FACILITY")) {

			int corCount = facPred.get(0) + 1;
			facPred.remove(0);
			facPred.add(0,corCount);
		}

		if (prediction.equals("FACILITY")) {
			int resCount = facPred.get(1) + 1;
			facPred.remove(1);
			facPred.add(1,resCount);
		}

		if (type.equals("FACILITY")) {
			int keyCount = facPred.get(2) + 1;
			facPred.remove(2);
			facPred.add(2,keyCount);
		}

		// GPE

		if (prediction.equals(type) && !prediction.equals("other") && prediction.equals("GPE")) {

			int corCount = gpePred.get(0) + 1;
			gpePred.remove(0);
			gpePred.add(0,corCount);
		}

		if (prediction.equals("GPE")) {
			int resCount = gpePred.get(1) + 1;
			gpePred.remove(1);
			gpePred.add(1,resCount);
		}

		if (type.equals("GPE")) {
			int keyCount = gpePred.get(2) + 1;
			gpePred.remove(2);
			gpePred.add(2,keyCount);
		}

		// WEA

		if (prediction.equals(type) && !prediction.equals("other") && prediction.equals("WEA")) {

			int corCount = weaPred.get(0) + 1;
			weaPred.remove(0);
			weaPred.add(0,corCount);
		}

		if (prediction.equals("WEA")) {
			int resCount = weaPred.get(1) + 1;
			weaPred.remove(1);
			weaPred.add(1,resCount);
		}

		if (type.equals("WEA")) {
			int keyCount = weaPred.get(2) + 1;
			weaPred.remove(2);
			weaPred.add(2,keyCount);
		}


		// LOCATION

		if (prediction.equals(type) && !prediction.equals("other") && prediction.equals("LOCATION")) {

			int corCount = locPred.get(0) + 1;
			locPred.remove(0);
			locPred.add(0,corCount);
		}

		if (prediction.equals("LOCATION")) {
			int resCount = locPred.get(1) + 1;
			locPred.remove(1);
			locPred.add(1,resCount);
		}

		if (type.equals("LOCATION")) {
			int keyCount = locPred.get(2) + 1;
			locPred.remove(2);
			locPred.add(2,keyCount);
		}


		// VEH

		if (prediction.equals(type) && !prediction.equals("other") && prediction.equals("VEH")) {

			int corCount = vehPred.get(0) + 1;
			vehPred.remove(0);
			vehPred.add(0,corCount);
		}

		if (prediction.equals("VEH")) {
			int resCount = vehPred.get(1) + 1;
			vehPred.remove(1);
			vehPred.add(1,resCount);
		}

		if (type.equals("VEH")) {
			int keyCount = vehPred.get(2) + 1;
			vehPred.remove(2);
			vehPred.add(2,keyCount);
		}


		// OTHER
		if (prediction.equals(type) && prediction.equals("other")) {

			int corCount = otherPred.get(0) + 1;
			otherPred.remove(0);
			otherPred.add(0,corCount);
		}

		if (prediction.equals("other")) {
			int resCount = otherPred.get(1) + 1;
			otherPred.remove(1);
			otherPred.add(1,resCount);
		}

		if (type.equals("other")) {
			int keyCount = otherPred.get(2) + 1;
			otherPred.remove(2);
			otherPred.add(2,keyCount);
		}




	}
    }

    public Document annotate (Document doc, Span span) {
	if (model == null)
	    model = MaxEnt.loadModel(modelFileName, "EntityTagger");
	Vector<Annotation> entities = doc.annotationsOfType("entity");
	if (entities == null)
	    return doc;
	for (Annotation entity : entities) {
	    String tokenText = doc.normalizedText(entity);
	    Datum d = entityFeatures(tokenText);
	    String prediction = model.getBestOutcome(model.eval(d.toArray()));
	    ((Entity) entity).setSemType(prediction);
	}
	return doc;
    }
}
